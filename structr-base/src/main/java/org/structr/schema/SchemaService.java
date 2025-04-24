/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema;

import graphql.Scalars;
import graphql.schema.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.index.IndexConfig;
import org.structr.api.index.NodeIndexConfig;
import org.structr.api.index.RelationshipIndexConfig;
import org.structr.api.service.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSchemaToken;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Trait;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaGrantTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
import org.structr.schema.compiler.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Structr Schema Service for dynamic class support at runtime.
 */
@ServiceDependency(NodeService.class)
public class SchemaService implements Service {

	public static final URI DynamicSchemaRootURI                  = URI.create("https://structr.org/v2.0/#");
	private static final Logger logger                            = LoggerFactory.getLogger(SchemaService.class.getName());
	private static final List<MigrationHandler> migrationHandlers = new LinkedList<>();
	private static final Semaphore IndexUpdateSemaphore           = new Semaphore(1, true);
	private static final AtomicBoolean schemaIsBeingReplaced      = new AtomicBoolean(false);
	private static final Set<String> blacklist                    = new LinkedHashSet<>();
	private static GraphQLSchema graphQLSchema                    = null;

	static {

		migrationHandlers.add(new BlacklistSchemaNodeWhenMissingPackage());
		migrationHandlers.add(new RemoveMethodsWithUnusedSignature());
		migrationHandlers.add(new ExtendNotionPropertyWithUuid());
		migrationHandlers.add(new BlacklistUnlicensedTypes());
		migrationHandlers.add(new RemoveDuplicateClasses());
		migrationHandlers.add(new RemoveClassesWithUnknownSymbols());
		migrationHandlers.add(new RemoveExportedMethodsWithoutSecurityContext());
		migrationHandlers.add(new RemoveFileSetPropertiesMethodWithoutParameters());
		migrationHandlers.add(new RemoveIncompatibleTypes());
		migrationHandlers.add(new RemoveTypesWithNonExistentPackages());
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException {
		return SchemaHelper.reloadSchema(new ErrorBuffer(), null, true, false);
	}

	public static synchronized GraphQLSchema getGraphQLSchema() {
		return graphQLSchema;
	}

	public static ServiceResult reloadSchema(final ErrorBuffer errorBuffer, final String initiatedBySessionId, final boolean fullReload, final boolean notifyCluster) {

		// compiling must only be done once
		if (!schemaIsBeingReplaced.compareAndSet(false, true)) {

			errorBuffer.add(new InvalidSchemaToken("Base", "source", "token"));

		} else {

			final App app = StructrApp.getInstance();
			final long t0 = System.currentTimeMillis();

			try (final Tx tx = app.tx()) {

				tx.prefetchHint("Reload schema");

				final Map<String, Map<String, PropertyKey>> removedTypes = Traits.clearDynamicSchema();

				// fetch schema relationships
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).getResultStream()) {

					final SchemaRelationshipNode schemaRel = node.as(SchemaRelationshipNode.class);
					final String name                      = schemaRel.getClassName();
					final TraitDefinition[] definitions    = schemaRel.getTraitDefinitions();

					StructrTraits.registerDynamicRelationshipType(name, !schemaRel.changelogDisabled(), definitions);

					// type still exists, was not removed, so we remove it from the map of removed types
					removedTypes.remove(name);
				}

				// fetch schema nodes
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_NODE).getResultStream()) {

					final SchemaNode schemaNode = node.as(SchemaNode.class);

					// migration entry point
					schemaNode.handleMigration();

					// create traits
					final String name                   = schemaNode.getClassName();
					final TraitDefinition[] definitions = schemaNode.getTraitDefinitions();

					StructrTraits.registerDynamicNodeType(name, !schemaNode.changelogDisabled(), schemaNode.isServiceClass(), definitions);

					// type still exists, was not removed, so we remove it from the map of removed types
					removedTypes.remove(name);
				}

				// fetch schema methods that extend the static schema (not attached to a schema node)
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), null).getResultStream()) {

					final SchemaMethod schemaMethod   = node.as(SchemaMethod.class);
					final String staticSchemaNodeName = schemaMethod.getStaticSchemaNodeName();

					if (StringUtils.isNotBlank(staticSchemaNodeName)) {

						// attach method to existing type
						if (Traits.exists(staticSchemaNodeName)) {

							final Trait trait = Traits.getTrait(staticSchemaNodeName);

							trait.registerDynamicMethod(schemaMethod);

						} else {

							throw new FrameworkException(422, "Invalid schema method " + schemaMethod.getUuid() + ": property staticSchemaNodeName contains unknown type " + staticSchemaNodeName);
						}
					}
				}

				// fetch schema properties that extend the static schema (not attached to a schema node)
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).key(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), null).getResultStream()) {

					final SchemaProperty schemaProperty = node.as(SchemaProperty.class);
					final String staticSchemaNodeName   = schemaProperty.getStaticSchemaNodeName();

					if (StringUtils.isNotBlank(staticSchemaNodeName)) {

						// attach method to existing type
						if (Traits.exists(staticSchemaNodeName)) {

							final Trait trait = Traits.getTrait(staticSchemaNodeName);

							trait.registerDynamicProperty(schemaProperty);

						} else {

							throw new FrameworkException(422, "Invalid schema property " + schemaProperty.getUuid() + ": property staticSchemaNodeName contains unknown type " + staticSchemaNodeName);
						}
					}
				}

				// fetch schema views that extend the static schema (not attached to a schema node)
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_VIEW).key(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), null).getResultStream()) {

					final SchemaView schemaView       = node.as(SchemaView.class);
					final String staticSchemaNodeName = schemaView.getStaticSchemaNodeName();

					if (StringUtils.isNotBlank(staticSchemaNodeName)) {

						// attach method to existing type
						if (Traits.exists(staticSchemaNodeName)) {

							final Trait trait = Traits.getTrait(staticSchemaNodeName);

							trait.registerDynamicView(schemaView);

						} else {

							throw new FrameworkException(422, "Invalid schema view " + schemaView.getUuid() + ": property staticSchemaNodeName contains unknown type " + staticSchemaNodeName);
						}
					}
				}

				// fetch schema grants that extend the static schema (not attached to a schema node)
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_GRANT).key(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY), null).getResultStream()) {

					final SchemaGrant schemaGrant     = node.as(SchemaGrant.class);
					final String staticSchemaNodeName = schemaGrant.getStaticSchemaNodeName();

					if (StringUtils.isNotBlank(staticSchemaNodeName)) {

						// attach method to existing type
						if (Traits.exists(staticSchemaNodeName)) {

							final Trait trait = Traits.getTrait(staticSchemaNodeName);

							trait.registerSchemaGrant(schemaGrant);

						} else {

							throw new FrameworkException(422, "Invalid schema grant " + schemaGrant.getUuid() + ": property staticSchemaNodeName contains unknown type " + staticSchemaNodeName);
						}
					}
				}

				updateIndexConfiguration(removedTypes);

				final GraphQLObjectType.Builder queryTypeBuilder         = GraphQLObjectType.newObject();
				final Map<String, GraphQLInputObjectType> selectionTypes = new LinkedHashMap<>();
				final Set<String> existingQueryTypeNames                 = new LinkedHashSet<>();
				final Map<String, GraphQLType> graphQLTypes              = new LinkedHashMap<>();
				final GraphQLHelper graphQLHelper                        = new GraphQLHelper();

				for (final String nodeType : Traits.getAllTypes(t -> t.isNodeType())) {
					graphQLHelper.initializeGraphQLForNodeType(nodeType, graphQLTypes, blacklist);
				}

				for (final String relType : Traits.getAllTypes(t -> t.isRelationshipType())) {
					graphQLHelper.initializeGraphQLForRelationshipType(relType, graphQLTypes);
				}

				// register types in "Query" type
				for (final Map.Entry<String, GraphQLType> entry : graphQLTypes.entrySet()) {

					final String className = entry.getKey();
					final GraphQLType type = entry.getValue();

					if (!Traits.exists(className)) {
						continue;
					}

					try {

						// register type in query type
						queryTypeBuilder.field(GraphQLFieldDefinition
							.newFieldDefinition()
							.name(className)
							.type(new GraphQLList(type))
							.argument(GraphQLArgument.newArgument().name("id").type(Scalars.GraphQLString).build())
							.argument(GraphQLArgument.newArgument().name("type").type(Scalars.GraphQLString).build())
							.argument(GraphQLArgument.newArgument().name("name").type(Scalars.GraphQLString).build())
							.argument(GraphQLArgument.newArgument().name("_page").type(Scalars.GraphQLInt).build())
							.argument(GraphQLArgument.newArgument().name("_pageSize").type(Scalars.GraphQLInt).build())
							.argument(GraphQLArgument.newArgument().name("_sort").type(Scalars.GraphQLString).build())
							.argument(GraphQLArgument.newArgument().name("_desc").type(Scalars.GraphQLBoolean).build())
							.arguments(graphQLHelper.getGraphQLQueryArgumentsForType(selectionTypes, existingQueryTypeNames, className))
						);

					} catch (Throwable t) {
						t.printStackTrace();
						logger.warn("Unable to add GraphQL type {}: {}", className, t.getMessage());
					}
				}

				// exchange graphQL schema after successful build
				synchronized (SchemaService.class) {

					try {

						graphQLSchema = GraphQLSchema
							.newSchema()
							.query(queryTypeBuilder.name("Query").build())
							.additionalTypes(new LinkedHashSet<>(graphQLTypes.values()))
							.build();

					} catch (Throwable t) {
						logger.warn("Unable to build GraphQL schema: {}", t.getMessage());
						logger.error(ExceptionUtils.getStackTrace(t));
					}
				}

				tx.success();

			} catch (Throwable t) {

				logger.error(ExceptionUtils.getStackTrace(t));

			} finally {

				logger.info("Schema reload took a total of {} ms", System.currentTimeMillis() - t0);

				schemaIsBeingReplaced.set(false);
			}
		}
		/*

		final ConfigurationProvider config = StructrApp.getConfiguration();
		boolean success                    = true;
		int retryCount                     = 2;

		// compiling must only be done once
		if (!compiling.compareAndSet(false, true)) {

			errorBuffer.add(new InvalidSchemaToken("Base", "source", "token"));

		} else {

			FlushCachesCommand.flushAll();
			StructrApp.initializeSchemaIds();

			final long t0 = System.currentTimeMillis();

			try {

				try (final Tx tx = app.tx()) {

					tx.prefetchHint("Diff schema");

					SchemaService.prefetchSchemaNodes(tx);

					final JsonSchema databaseSchema = StructrSchema.createFromDatabase(app);

					// diff and merge
					databaseSchema.diff(dynamicSchema);

					// commit changes before trying to build the schema
					tx.success();

				} catch (Throwable t) {
					logger.error(ExceptionUtils.getStackTrace(t));
				}

				// dynamicSchema contains all classes defined in Structr, including those only available in licensed editions
				// We need to make sure that unlicensed classes are blacklisted; this was previously done in ensureBuiltInTypesExist,
				// but this method is not called in cluster mode, when the instance is not the cluster coordinator.
				for (final JsonType t : dynamicSchema.getTypes()) {

					final String name = t.getName();

					for (final URI schemaURI : t.getImplements()) {

						// remove query
						final URI uri = URI.create(schemaURI.getScheme() + "://" + schemaURI.getHost() + schemaURI.getPath());

						if (!uri.toString().startsWith("https://structr.org/v1.1/static/") && dynamicSchema.resolveURI(schemaURI) == null && StructrApp.resolveSchemaId(uri) == null) {

							logger.warn("Unable to resolve built-in interface {} of type {} against Structr schema, source was {}", uri, name, schemaURI);

							SchemaService.blacklist(name);
						}
					}
				}

				try (final Tx tx = app.tx()) {

					SchemaService.prefetchSchemaNodes(tx);

					tx.prefetchHint("Reload schema");

					while (retryCount-- > 0) {

						final Map<String, Map<String, PropertyKey>> removedClasses = translateRelationshipClassesToRelTypes(config.getTypeAndPropertyMapping());
						final Map<String, SchemaNode> schemaNodes                  = new LinkedHashMap<>();
						final NodeExtender nodeExtender                            = new NodeExtender(initiatedBySessionId, fullReload);
						final Set<String> dynamicViews                             = new LinkedHashSet<>();

						// only create built-in types if we have exclusive database access
						if (Services.getInstance().hasExclusiveDatabaseAccess()) {

							// collect auto-generated schema nodes
							SchemaService.ensureBuiltinTypesExist(app);
						}

						// collect list of schema nodes
						app.nodeQuery(StructrTraits.SCHEMA_NODE).getAsList().stream().forEach(n -> {
							schemaNodes.put(n.getName(), n);
						});

						// check licenses prior to source code generation
						for (final SchemaNode schemaInfo : schemaNodes.values()) {
							blacklist.addAll(SchemaHelper.getUnlicensedTypes(schemaInfo));
						}

						// add schema nodes from database
						for (final SchemaNode schemaInfo : schemaNodes.values()) {

							final String name = schemaInfo.getName();
							if (blacklist.contains(name)) {

								continue;
							}

							schemaInfo.handleMigration(schemaNodes);

							final String className      = schemaInfo.getClassName();
							final SourceFile sourceFile = new SourceFile(className);

							// generate source code
							SchemaHelper.getSource(sourceFile, schemaInfo, schemaNodes, blacklist, errorBuffer);

							// only load dynamic node if there were no errors while generating the source code (missing modules etc.)
							// if addClass returns true, the class needs to be recompiled and cached methods must be cleared
							if (nodeExtender.addClass(className, sourceFile)) {
								schemaInfo.clearCachedSchemaMethodsForInstance();
							}
							dynamicViews.addAll(schemaInfo.getDynamicViews());
						}

						// collect relationship classes
						for (final SchemaRelationshipNode schemaRelationship : app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).getAsList()) {

							final String sourceType = schemaRelationship.getSchemaNodeSourceType();
							final String targetType = schemaRelationship.getSchemaNodeTargetType();

							if (!blacklist.contains(sourceType) && !blacklist.contains(targetType)) {

								final SourceFile relationshipSource = new SourceFile(schemaRelationship.getClassName());

								// generate source code
								schemaRelationship.getSource(relationshipSource, schemaNodes, errorBuffer);

								nodeExtender.addClass(schemaRelationship.getClassName(), relationshipSource);
								dynamicViews.addAll(schemaRelationship.getDynamicViews());
							}
						}

						// this is a very critical section :)
						synchronized (SchemaService.class) {

							// clear propagating relationship cache
							Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key("clearPropagatingRelationshipTypes")();

							// compile all classes at once and register
							final Map<String, Class> newTypes = nodeExtender.compile(errorBuffer);

							if (!errorBuffer.hasError()) {

								// only do this if there was no compile-time error!
								for (final Class newType : newTypes.values()) {

									// instantiate classes to execute static initializer of helpers
									try {

										// do full reload
										config.registerEntityType(newType);
										newType.getDeclaredConstructor().newInstance();

									} catch (final Throwable t) {

										// abstract classes and interfaces will throw errors here
										if (newType.isInterface() || Modifier.isAbstract(newType.getModifiers())) {
											// ignore
										} else {

											// everything else is a severe problem and should be not only reported but also
											// make the schema compilation fail (otherwise bad things will happen later)
											errorBuffer.add(new InstantiationErrorToken(newType.getName(), t));
											logger.error("Unable to instantiate dynamic entity {}", newType.getName(), t);

											t.printStackTrace();
										}
									}
								}
							}

							// calculate difference between previous and new classes
							removedClasses.keySet().removeAll(translateRelationshipClassesToRelTypes(StructrApp.getConfiguration().getTypeAndPropertyMapping()).keySet());

							if (errorBuffer.hasError()) {

								if (Settings.SchemaAutoMigration.getValue() && (Boolean.FALSE.equals(Services.getInstance().isInitialized()) || DeployCommand.isDeploymentActive())) {

									logger.info("Attempting auto-migration of built-in schema..");

									// try to handle certain errors automatically
									handleAutomaticMigration(errorBuffer);

									if (retryCount == 0) {

										for (final ErrorToken token : errorBuffer.getErrorTokens()) {
											logger.error(token.toString());
										}

										return new ServiceResult(false);

									} else {

										// clear errors for next try
										errorBuffer.getErrorTokens().clear();

										// back to top..
										continue;
									}

								} else {

									logger.error("Unable to compile dynamic schema, and automatic migration is not enabled. Please set application.schema.automigration = true in structr.conf to enable modification of existing schema classes.");
								}

							} else {

								// no retry
								retryCount = 0;
							}

						}

						if (Services.getInstance().hasExclusiveDatabaseAccess()) {

							// create properties and views etc.
							for (final SchemaNode schemaNode : schemaNodes.values()) {
								schemaNode.createBuiltInSchemaEntities(errorBuffer);
							}
						}

						success = !errorBuffer.hasError();

						if (success) {

							// prevent inheritance map from leaking
							SearchCommand.clearInheritanceMap();
							AccessPathCache.invalidate();

							// clear relationship instance cache
							NodeInterface.clearRelationshipTemplateInstanceCache();

							// clear permission cache
							AccessControllableTraitDefinition.clearCaches();

							// inject views in configuration provider
							config.registerDynamicViews(dynamicViews);

							updateIndexConfiguration(removedClasses);

							// moved success() call for the transaction to the bottom..
							tx.success();
						}
					}

				} catch (FrameworkException fex) {

					logger.error("Unable to compile dynamic schema: {}", fex.getMessage());
					logger.error(ExceptionUtils.getStackTrace(fex));
					success = false;

					errorBuffer.getErrorTokens().addAll(fex.getErrorBuffer().getErrorTokens());

					fex.printStackTrace();

				} catch (Throwable t) {

					logger.error("Unable to compile dynamic schema: {}", t.getMessage());
					logger.error(ExceptionUtils.getStackTrace(t));

					success = false;

					t.printStackTrace();
				}

				if (!success) {

					FlushCachesCommand.flushAll();
				}

			} finally {

				logger.info("Schema build took a total of {} ms", System.currentTimeMillis() - t0);

				// compiling done
				compiling.set(false);

				if (notifyCluster && Settings.ClusterModeEnabled.getValue(false) == true) {
					Services.getInstance().broadcastSchemaChange();
				}
			}
		}
		*/

		return new ServiceResult(true);
	}

	@Override
	public void initialized() {
	}

	@Override
	public void shutdown() {
	}

	@Override
	public String getName() {
		return SchemaService.class.getName();
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	public static void blacklist(final String typeName) {
		SchemaService.blacklist.add(typeName);
	}

	public static Set<String> getBlacklist() {
		return SchemaService.blacklist;
	}

	@Override
	public boolean isVital() {
		return true;
	}

	@Override
	public boolean waitAndRetry() {
		return true;
	}

	@Override
	public int getRetryCount() {
		return 3;
	}

	@Override
	public int getRetryDelay() {
		return 1;
	}

	public static boolean getSchemaIsBeingReplaced() {
		return schemaIsBeingReplaced.get();
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "core";
	}

	// ----- private methods -----
	private static void updateIndexConfiguration(final Map<String, Map<String, PropertyKey>> removedTypes) {

		if (Services.overrideIndexManagement()) {

			if (Services.skipIndexConfiguration()) {

				logger.info("Skipping index creation because of manual override.");
				return;
			}

		} else {

			if (Services.isTesting()) {

				logger.info("Skipping index creation in test mode.");
				return;
			}
		}

		final Thread indexUpdater = new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					if (!IndexUpdateSemaphore.tryAcquire(3, TimeUnit.MINUTES)) {

						logger.error("Unable to start index updater, waited for 3 minutes. Giving up.");
						return;
					}

					final Set<String> whitelist   = new LinkedHashSet<>(Set.of(StructrTraits.GRAPH_OBJECT, StructrTraits.NODE_INTERFACE));
					final DatabaseService graphDb = StructrApp.getInstance().getDatabaseService();

					final Map<String, Map<String, IndexConfig>> schemaIndexConfig  = new HashMap();
					final Map<String, Map<String, IndexConfig>> removedTypesConfig = new HashMap();

					for (final String type : Traits.getAllTypes()) {

						final Traits traits                 = Traits.of(type);
						final String typeName               = getIndexingTypeName(type);
						Map<String, IndexConfig> typeConfig = schemaIndexConfig.get(typeName);
						final boolean isRelationship        = traits.isRelationshipType();

						if (typeConfig == null) {

							typeConfig = new LinkedHashMap<>();
							schemaIndexConfig.put(typeName, typeConfig);
						}


						for (final PropertyKey key : traits.getAllPropertyKeys()) {

							boolean createIndex = key.isIndexed() || key.isIndexedWhenEmpty();
							final Trait trait   = key.getDeclaringTrait();

							if (isRelationship) {

								// prevent creation of node property indexes on relationships
								if (!key.isNodeIndexOnly()) {

									typeConfig.put(key.dbName(), new RelationshipIndexConfig(createIndex));
								}

							} else {

								createIndex &= (trait == null || whitelist.contains(type) || type.equals(trait.getLabel()));
								//createIndex &= (!NonIndexed.class.isAssignableFrom(type));

								typeConfig.put(key.dbName(), new NodeIndexConfig(createIndex));
							}
						}
					}

					for (final Map.Entry<String, Map<String, PropertyKey>> entry : removedTypes.entrySet()) {

						String typeName        = entry.getKey();
						boolean isRelationship = true;

						// remove fqcn parts if present (relationships dont have it)
						if (typeName.contains(".")) {

							typeName = StringUtils.substringAfterLast(typeName, ".");
							isRelationship = false;
						}

						final Map<String, IndexConfig> typeConfig = new HashMap();
						removedTypesConfig.put(typeName, typeConfig);

						for (final PropertyKey propertyKey : entry.getValue().values()) {

							final boolean wasIndexed = propertyKey.isIndexed() || propertyKey.isIndexedWhenEmpty();

							if (isRelationship) {

								final boolean dropIndex = wasIndexed;

								typeConfig.put(propertyKey.dbName(), new RelationshipIndexConfig(dropIndex));

							} else {

								final boolean wasIdIndex = "id".equals(propertyKey.jsonName());
								final boolean dropIndex  = wasIndexed && !wasIdIndex;

								typeConfig.put(propertyKey.dbName(), new NodeIndexConfig(dropIndex));
							}
						}
					}

					graphDb.updateIndexConfiguration(schemaIndexConfig, removedTypesConfig, false);

				} catch (Throwable t) {

					t.printStackTrace();

				} finally {

					IndexUpdateSemaphore.release();
				}
			}
		}, "Structr Index Updater");

		indexUpdater.setName("indexUpdater");
		indexUpdater.setDaemon(true);
		indexUpdater.start();
	}

	private static Class getType(final String name) {

		/*

		try { return Class.forName(name); } catch (ClassNotFoundException ignore) {}

		final Class nodeClass = Traits.of(StringUtils.substringAfterLast(name, "."));
		if (nodeClass != null) {

			return nodeClass;
		}

		final Class relClass = StringUtils.substringAfterLast(name, "."));
		if (relClass != null) {

			return relClass;
		}
		*/

		return null;
	}

	/*
	private static Map<String, Map<String, PropertyKey>> translateRelationshipClassesToRelTypes(final Map<String, Map<String, PropertyKey>> source) {

		// we need to replace all relationship type classes with their respective relationship type
		// (e.g. DOMNodeCONTAINSDOMNode => CONTAINS)

		final Map<String, Map<String, PropertyKey>> translated = new LinkedHashMap<>();

		for (final String key : source.keySet()) {

			final Class type = getType(key);
			if (type != null) {

				if (Relation.class.isAssignableFrom(type)) {

					final String typeName = getIndexingTypeName(type);
					if (typeName != null) {

						// create a deep copy
						translated.put(typeName, new LinkedHashMap<>(source.get(key)));
					}

				} else {

					// create a deep copy
					translated.put(key, new LinkedHashMap<>(source.get(key)));
				}
			}
		}

		return translated;
	}

	private static void handleAutomaticMigration(final ErrorBuffer errorBuffer) throws FrameworkException {

		for (final ErrorToken errorToken : errorBuffer.getErrorTokens()) {

			for (final MigrationHandler handler : migrationHandlers) {

				handler.handleMigration(errorToken);
			}
		}

	}
	*/

	private static String getIndexingTypeName(final String typeName) {

		if (StructrTraits.GRAPH_OBJECT.equals(typeName)) {
			return StructrTraits.NODE_INTERFACE;
		}

		final Traits traits = Traits.of(typeName);
		if (traits.isRelationshipType()) {

			final Relation relation = traits.getRelation();
			if (relation != null) {

				return relation.name();
			}
		}

		return typeName;
	}
}
