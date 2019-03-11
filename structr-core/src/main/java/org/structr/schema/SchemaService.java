/**
 * Copyright (C) 2010-2019 Structr GmbH
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.schema;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.api.service.Service;
import org.structr.api.service.ServiceDependency;
import org.structr.api.service.StructrServices;
import org.structr.api.util.Iterables;
import org.structr.common.AccessPathCache;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InstantiationErrorToken;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.schema.compiler.BlacklistSchemaNodeWhenMissingPackage;
import org.structr.schema.compiler.BlacklistUnlicensedTypes;
import org.structr.schema.compiler.ExtendNotionPropertyWithUuid;
import org.structr.schema.compiler.MigrationHandler;
import org.structr.schema.compiler.NodeExtender;
import org.structr.schema.compiler.RemoveClassesWithUnknownSymbols;
import org.structr.schema.compiler.RemoveDuplicateClasses;
import org.structr.schema.compiler.RemoveMethodsWithUnusedSignature;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonSchema;

/**
 * Structr Schema Service for dynamic class support at runtime.
 */
@ServiceDependency(NodeService.class)
public class SchemaService implements Service {

	public static final URI DynamicSchemaRootURI                  = URI.create("https://structr.org/v2.0/#");
	private static final Logger logger                            = LoggerFactory.getLogger(SchemaService.class.getName());
	private static final List<MigrationHandler> migrationHandlers = new LinkedList<>();
	private static final JsonSchema dynamicSchema                 = StructrSchema.newInstance(DynamicSchemaRootURI);
	private static final AtomicBoolean compiling                  = new AtomicBoolean(false);
	private static final AtomicBoolean updating                   = new AtomicBoolean(false);
	private static final Set<String> blacklist                    = new LinkedHashSet<>();
	private static GraphQLSchema graphQLSchema                    = null;

	static {

		migrationHandlers.add(new BlacklistSchemaNodeWhenMissingPackage());
		migrationHandlers.add(new RemoveMethodsWithUnusedSignature());
		migrationHandlers.add(new ExtendNotionPropertyWithUuid());
		migrationHandlers.add(new BlacklistUnlicensedTypes());
		migrationHandlers.add(new RemoveDuplicateClasses());
		migrationHandlers.add(new RemoveClassesWithUnknownSymbols());
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return reloadSchema(new ErrorBuffer(), null);
	}

	public static JsonSchema getDynamicSchema() {
		return dynamicSchema;
	}

	public static synchronized GraphQLSchema getGraphQLSchema() {
		return graphQLSchema;
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer, final String initiatedBySessionId) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final App app                      = StructrApp.getInstance();
		boolean success                    = true;
		int retryCount                     = 2;

		// compiling must only be done once
		if (compiling.compareAndSet(false, true)) {

			FlushCachesCommand.flushAll();

			final long t0 = System.currentTimeMillis();

			try {

				try (final Tx tx = app.tx()) {

					while (retryCount-- > 0) {

						final Map<String, Map<String, PropertyKey>> removedClasses = new HashMap<>(config.getTypeAndPropertyMapping());
						final Map<String, GraphQLType> graphQLTypes                = new LinkedHashMap<>();
						final Map<String, SchemaNode> schemaNodes                  = new LinkedHashMap<>();
						final NodeExtender nodeExtender                            = new NodeExtender(initiatedBySessionId);
						final Set<String> dynamicViews                             = new LinkedHashSet<>();

						// collect auto-generated schema nodes
						SchemaService.ensureBuiltinTypesExist(app);

						// collect list of schema nodes
						app.nodeQuery(SchemaNode.class).getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

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

							schemaInfo.handleMigration();

							final String sourceCode = SchemaHelper.getSource(schemaInfo, schemaNodes, blacklist, errorBuffer);
							if (sourceCode != null) {

								final String className = schemaInfo.getClassName();

								// only load dynamic node if there were no errors while generating
								// the source code (missing modules etc.)
								nodeExtender.addClass(className, sourceCode);
								dynamicViews.addAll(schemaInfo.getDynamicViews());

								// initialize GraphQL engine as well
								schemaInfo.initializeGraphQL(schemaNodes, graphQLTypes, blacklist);
							}
						}

						// collect relationship classes
						for (final SchemaRelationshipNode schemaRelationship : app.nodeQuery(SchemaRelationshipNode.class).getAsList()) {

							final String sourceType = schemaRelationship.getSchemaNodeSourceType();
							final String targetType = schemaRelationship.getSchemaNodeTargetType();

							if (!blacklist.contains(sourceType) && !blacklist.contains(targetType)) {

								nodeExtender.addClass(schemaRelationship.getClassName(), schemaRelationship.getSource(schemaNodes, errorBuffer));
								dynamicViews.addAll(schemaRelationship.getDynamicViews());

								// initialize GraphQL engine as well
								schemaRelationship.initializeGraphQL(graphQLTypes);
							}
						}

						// this is a very critical section :)
						synchronized (SchemaService.class) {

							// clear propagating relationship cache
							SchemaRelationshipNode.clearPropagatingRelationshipTypes();

							// compile all classes at once and register
							final Map<String, Class> newTypes = nodeExtender.compile(errorBuffer);

							for (final Class newType : newTypes.values()) {

								// instantiate classes to execute static initializer of helpers
								try {

									// do full reload
									config.registerEntityType(newType);
									newType.newInstance();

								} catch (final Throwable t) {

									// abstract classes and interfaces will throw errors here
									if (newType.isInterface() || Modifier.isAbstract(newType.getModifiers())) {
										// ignore
									} else {

										// everything else is a severe problem and should be not only reported but also
										// make the schema compilation fail (otherwise bad things will happen later)
										errorBuffer.add(new InstantiationErrorToken(newType.getName(), t));
										logger.error("Unable to instantiate dynamic entity {}", newType.getName(), t);
									}
								}
							}

							// calculate difference between previous and new classes
							removedClasses.keySet().removeAll(StructrApp.getConfiguration().getTypeAndPropertyMapping().keySet());

							if (errorBuffer.hasError()) {

								if (Settings.SchemAutoMigration.getValue()) {

									logger.info("Attempting auto-migration of built-in schema..");

									// try to handle certain errors automatically
									handleAutomaticMigration(errorBuffer);

									if (retryCount == 0) {

										for (final ErrorToken token : errorBuffer.getErrorTokens()) {
											logger.error("{}", token.toString());
										}

										return false;

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

						// create properties and views etc.
						for (final SchemaNode schemaNode : schemaNodes.values()) {
							schemaNode.createBuiltInSchemaEntities(errorBuffer);
						}

						success = !errorBuffer.hasError();

						if (success) {

							// prevent inheritance map from leaking
							SearchCommand.clearInheritanceMap();
							AccessPathCache.invalidate();

							// clear relationship instance cache
							AbstractNode.clearRelationshipTemplateInstanceCache();

							// clear permission cache
							AbstractNode.clearCaches();

							// inject views in configuration provider
							config.registerDynamicViews(dynamicViews);

							if (Services.calculateHierarchy() || !Services.isTesting()) {

								calculateHierarchy(schemaNodes);
							}

							if (Services.updateIndexConfiguration() || !Services.isTesting()) {

								updateIndexConfiguration(removedClasses);
							}

							tx.success();


							final GraphQLObjectType.Builder queryTypeBuilder         = GraphQLObjectType.newObject();
							final Map<String, GraphQLInputObjectType> selectionTypes = new LinkedHashMap<>();
							final Set<String> existingQueryTypeNames                 = new LinkedHashSet<>();

							// register types in "Query" type
							for (final Entry<String, GraphQLType> entry : graphQLTypes.entrySet()) {

								final String className = entry.getKey();
								final GraphQLType type = entry.getValue();

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
										.argument(SchemaHelper.getGraphQLQueryArgumentsForType(schemaNodes, selectionTypes, existingQueryTypeNames, className))
									);

								} catch (Throwable t) {
									logger.warn("Unable to add GraphQL type {}: {}", className, t.getMessage());
								}
							}

							// exchange graphQL schema after successful build
							synchronized (SchemaService.class) {

								try {

									graphQLSchema = GraphQLSchema
										.newSchema()
										.query(queryTypeBuilder.name("Query").build())
										.build(new LinkedHashSet<>(graphQLTypes.values()));

								} catch (Throwable t) {
									logger.warn("Unable to build GraphQL schema: {}", t.getMessage());
								}
							}
						}
				}

				} catch (FrameworkException fex) {

					fex.printStackTrace();

					FlushCachesCommand.flushAll();

					logger.error("Unable to compile dynamic schema: {}", fex.getMessage());
					success = false;

					errorBuffer.getErrorTokens().addAll(fex.getErrorBuffer().getErrorTokens());

				} catch (Throwable t) {

					t.printStackTrace();

					FlushCachesCommand.flushAll();

					t.printStackTrace();

					logger.error("Unable to compile dynamic schema: {}", t.getMessage());
					success = false;
				}

				if (!success) {

					FlushCachesCommand.flushAll();
				}

			} finally {

				logger.info("Schema build took a total of {} ms", System.currentTimeMillis() - t0);

				// compiling done
				compiling.set(false);

			}
		}

		return success;
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

	public static void ensureBuiltinTypesExist(final App app) throws FrameworkException {

		try {

			StructrSchema.extendDatabaseSchema(app, dynamicSchema);

		} catch (Exception ex) {

			ex.printStackTrace();
		}
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

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "core";
	}

	// ----- private methods -----
	private static void calculateHierarchy(final Map<String, SchemaNode> schemaNodes) {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Set<String> alreadyCalculated = new HashSet<>();

			// calc hierarchy
			for (final SchemaNode schemaNode : schemaNodes.values()) {

				final int relCount = Iterables.count(schemaNode.getProperty(SchemaNode.relatedFrom)) + Iterables.count(schemaNode.getProperty(SchemaNode.relatedTo));
				final int level     = recursiveGetHierarchyLevel(schemaNodes, alreadyCalculated, schemaNode, 0);

				schemaNode.setProperty(SchemaNode.hierarchyLevel, level);
				schemaNode.setProperty(SchemaNode.relCount, relCount);
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	private static int recursiveGetHierarchyLevel(final Map<String, SchemaNode> map, final Set<String> alreadyCalculated, final SchemaNode schemaNode, final int depth) {

		// stop at level 20
		if (depth > 20) {
			return 20;
		}

		String superclass = schemaNode.getProperty(SchemaNode.extendsClass);
		if (superclass == null) {

			return 0;

		} else if (superclass.startsWith("org.structr.dynamic.")) {

			// find hierarchy level
			superclass = superclass.substring(superclass.lastIndexOf(".") + 1);

			// recurse upwards
			final SchemaNode superSchemaNode = map.get(superclass);
			if (superSchemaNode != null) {

				return recursiveGetHierarchyLevel(map, alreadyCalculated, superSchemaNode, depth + 1) + 1;
			}
		}

		return 0;
	}

	private static void updateIndexConfiguration(final Map<String, Map<String, PropertyKey>> removedClasses) {

		final Thread indexUpdater = new Thread(new Runnable() {

			@Override
			public void run() {

				// critical section, only one thread should update the index at a time
				if (updating.compareAndSet(false, true)) {

					try {

						final DatabaseService graphDb = StructrApp.getInstance().getDatabaseService();

						final Map<String, Map<String, Boolean>> schemaIndexConfig    = new HashMap();
						final Map<String, Map<String, Boolean>> removedClassesConfig = new HashMap();

						for (final Entry<String, Map<String, PropertyKey>> entry : StructrApp.getConfiguration().getTypeAndPropertyMapping().entrySet()) {

							final Class type = getType(entry.getKey());
							if (type != null) {

								final String typeName = type.getSimpleName();

								final Boolean alreadySeenType = schemaIndexConfig.containsKey(typeName);
								final Map<String, Boolean> typeConfig = (alreadySeenType ? schemaIndexConfig.get(typeName) : new HashMap());

								if (!alreadySeenType) {
									schemaIndexConfig.put(typeName, typeConfig);
								}

								for (final PropertyKey key : entry.getValue().values()) {

									boolean createIndex = key.isIndexed() || key.isIndexedWhenEmpty();

									createIndex &= !NonIndexed.class.isAssignableFrom(type);
									createIndex &= NodeInterface.class.equals(type) || !GraphObject.id.equals(key);

									typeConfig.put(key.dbName(), createIndex);
								}
							}
						}

						for (final Entry<String, Map<String, PropertyKey>> entry : removedClasses.entrySet()) {

							final String typeName = StringUtils.substringAfterLast(entry.getKey(), ".");

							final Map<String, Boolean> typeConfig = new HashMap();
							removedClassesConfig.put(typeName, typeConfig);

							for (final PropertyKey key : entry.getValue().values()) {

								final boolean wasIndexed = key.isIndexed() || key.isIndexedWhenEmpty();
								final boolean wasIdIndex = GraphObject.id.equals(key);
								final boolean dropIndex  = wasIndexed && !wasIdIndex;

								typeConfig.put(key.dbName(), dropIndex);
							}
						}

						graphDb.updateIndexConfiguration(schemaIndexConfig, removedClassesConfig);

					} finally {

						updating.set(false);
					}
				}
			}
		});

		indexUpdater.setName("indexUpdater");
		indexUpdater.setDaemon(true);
		indexUpdater.start();
	}

	private static Class getType(final String name) {

		try { return Class.forName(name); } catch (ClassNotFoundException ignore) {}

		// fallback: use dynamic class from simple name
		return StructrApp.getConfiguration().getNodeEntityClass(StringUtils.substringAfterLast(name, "."));
	}

	private static void handleAutomaticMigration(final ErrorBuffer errorBuffer) throws FrameworkException {

		for (final ErrorToken errorToken : errorBuffer.getErrorTokens()) {

			for (final MigrationHandler handler : migrationHandlers) {

				handler.handleMigration(errorToken);
			}
		}

	}
}
