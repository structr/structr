/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.Predicate;
import org.structr.api.index.NewIndexConfig;
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
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.*;

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

	public static final URI DynamicSchemaRootURI             = URI.create("https://structr.org/v2.0/#");
	private static final Logger logger                       = LoggerFactory.getLogger(SchemaService.class.getName());
	private static final Semaphore IndexUpdateSemaphore      = new Semaphore(1);
	private static final AtomicBoolean schemaIsBeingReplaced = new AtomicBoolean(false);
	private static final Set<String> blacklist               = new LinkedHashSet<>();

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException {
		return SchemaHelper.reloadSchema(new ErrorBuffer(), null, true, false);
	}

	public static ServiceResult reloadSchema(final ErrorBuffer errorBuffer, final String initiatedBySessionId, final boolean fullReload, final boolean notifyCluster) {

		// compiling must only be done once
		if (!schemaIsBeingReplaced.compareAndSet(false, true)) {

			errorBuffer.add(new InvalidSchemaToken("Base", "source", "token"));

		} else {

			final TraitsInstance existingSchema = TraitsManager.getCurrentInstance();
			final TraitsInstance newSchema      = TraitsManager.createCopyOfRootInstance();

			blacklist("Favoritable");

			final App app = StructrApp.getInstance();
			final long t0 = System.currentTimeMillis();

			try (final Tx tx = app.tx()) {

				tx.prefetchHint("Reload schema");

				final Map<String, Map<String, PropertyKey>> removedTypes = existingSchema.getDynamicSchemaTypes();

				// fetch schema relationships
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).getResultStream()) {

					final SchemaRelationshipNode schemaRel = node.as(SchemaRelationshipNode.class);
					final String name                      = schemaRel.getClassName();
					final TraitDefinition definition       = schemaRel.getTraitDefinition(newSchema);

					newSchema.registerTrait(new Trait(newSchema, definition, true));
					newSchema.registerDynamicRelationshipType(name, !schemaRel.changelogDisabled(), Set.of(name));

					// type still exists, was not removed, so we remove it from the map of removed types
					removedTypes.remove(name);
				}

				// fetch schema nodes
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_NODE).getResultStream()) {

					final SchemaNode schemaNode = node.as(SchemaNode.class);

					// migration entry point
					schemaNode.handleMigration();

					// create traits
					final TraitDefinition definition = schemaNode.getTraitDefinition(newSchema);
					final Set<String> traits         = new LinkedHashSet<>();
					final String name                = definition.getName();
					final String label               = definition.getLabel();

					traits.addAll(schemaNode.getInheritedTraits());
					traits.add(name);

					newSchema.registerTrait(new Trait(newSchema, definition, true));
					newSchema.registerDynamicNodeType(label, !schemaNode.changelogDisabled(), schemaNode.isServiceClass(), traits);

					// type still exists, was not removed, so we remove it from the map of removed types
					removedTypes.remove(label);
				}

				// fetch schema methods that extend the static schema (not attached to a schema node)
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(newSchema.getTraits(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), null).getResultStream()) {

					final SchemaMethod schemaMethod   = node.as(SchemaMethod.class);
					final String staticSchemaNodeName = schemaMethod.getStaticSchemaNodeName();

					if (StringUtils.isNotBlank(staticSchemaNodeName)) {

						// attach method to existing type
						if (newSchema.exists(staticSchemaNodeName)) {

							final Trait trait = newSchema.getTrait(staticSchemaNodeName);

							trait.registerDynamicMethod(schemaMethod);

						} else {

							throw new FrameworkException(422, "Invalid schema method " + schemaMethod.getUuid() + ": property staticSchemaNodeName contains unknown type " + staticSchemaNodeName);
						}
					}
				}

				// fetch schema properties that extend the static schema (not attached to a schema node)
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).key(newSchema.getTraits(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), null).getResultStream()) {

					final SchemaProperty schemaProperty = node.as(SchemaProperty.class);
					final String staticSchemaNodeName   = schemaProperty.getStaticSchemaNodeName();

					if (StringUtils.isNotBlank(staticSchemaNodeName)) {

						// attach method to existing type
						if (newSchema.exists(staticSchemaNodeName)) {

							final Trait trait = newSchema.getTrait(staticSchemaNodeName);

							trait.registerDynamicProperty(schemaProperty);

						} else {

							throw new FrameworkException(422, "Invalid schema property " + schemaProperty.getUuid() + ": property staticSchemaNodeName contains unknown type " + staticSchemaNodeName);
						}
					}
				}

				// fetch schema views that extend the static schema (not attached to a schema node)
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_VIEW).key(newSchema.getTraits(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), null).getResultStream()) {

					final SchemaView schemaView       = node.as(SchemaView.class);
					final String staticSchemaNodeName = schemaView.getStaticSchemaNodeName();

					if (StringUtils.isNotBlank(staticSchemaNodeName)) {

						// attach method to existing type
						if (newSchema.exists(staticSchemaNodeName)) {

							final Trait trait = newSchema.getTrait(staticSchemaNodeName);

							trait.registerDynamicView(schemaView);

						} else {

							throw new FrameworkException(422, "Invalid schema view " + schemaView.getUuid() + ": property staticSchemaNodeName contains unknown type " + staticSchemaNodeName);
						}
					}
				}

				// fetch schema grants that extend the static schema (not attached to a schema node)
				for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_GRANT).key(newSchema.getTraits(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY), null).getResultStream()) {

					final SchemaGrant schemaGrant     = node.as(SchemaGrant.class);
					final String staticSchemaNodeName = schemaGrant.getStaticSchemaNodeName();

					if (StringUtils.isNotBlank(staticSchemaNodeName)) {

						// attach method to existing type
						if (newSchema.exists(staticSchemaNodeName)) {

							final Trait trait = newSchema.getTrait(staticSchemaNodeName);

							trait.registerSchemaGrant(schemaGrant);

						} else {

							throw new FrameworkException(422, "Invalid schema grant " + schemaGrant.getUuid() + ": property staticSchemaNodeName contains unknown type " + staticSchemaNodeName);
						}
					}
				}

				newSchema.resolveTraitHierarchies();

				for (final NodeInterface node : app.nodeQuery(StructrTraits.ABSTRACT_SCHEMA_NODE).getResultStream()) {

					final String label = node.getName();

					// create views (was a post process before, but needs access to the new schema)
					AbstractSchemaNodeTraitDefinition.createViewNodesForClass(newSchema, node.as(AbstractSchemaNode.class), label);
				}

				updateIndexConfiguration(newSchema, removedTypes);

				tx.success();

				// lastly: replace schema
				TraitsManager.replaceCurrentInstance(newSchema);

			} catch (Throwable t) {

				logger.error(ExceptionUtils.getStackTrace(t));

			} finally {

				logger.info("Schema reload took a total of {} ms", System.currentTimeMillis() - t0);

				TransactionCommand.simpleBroadcast("SCHEMA_COMPILED", Map.of("success", true), Predicate.allExcept(initiatedBySessionId));

				schemaIsBeingReplaced.set(false);
			}
		}

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
	private static void updateIndexConfiguration(final TraitsInstance traitsInstance, final Map<String, Map<String, PropertyKey>> removedTypes) {

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

		final Thread indexUpdater = new Thread(() -> {

			try {

				if (!IndexUpdateSemaphore.tryAcquire(3, TimeUnit.MINUTES)) {

					logger.error("Unable to start index updater, waited for 3 minutes. Giving up.");
					return;
				}

				final Set<String> whitelist   = new LinkedHashSet<>(Set.of(StructrTraits.GRAPH_OBJECT, StructrTraits.NODE_INTERFACE));
				final DatabaseService graphDb = StructrApp.getInstance().getDatabaseService();

				final Map<String, NewIndexConfig> schemaIndexConfig  = new LinkedHashMap<>();

				for (final String type : traitsInstance.getAllTypes()) {

					final Traits traits          = traitsInstance.getTraits(type);
					final String typeName        = getIndexingTypeName(traitsInstance, type);
					final boolean isRelationship = traits.isRelationshipType();

					for (final PropertyKey key : traits.getAllPropertyKeys()) {

						final Trait trait = key.getDeclaringTrait();

						if (isRelationship) {

							// prevent creation of node property indexes on relationships
							if (!key.isNodeIndexOnly()) {

								if (key.isIndexed()) {

									final NewIndexConfig config = new NewIndexConfig(typeName, key.dbName(), false, true, false);
									final String identifier     = typeName + "_" + key.dbName();

									schemaIndexConfig.put(identifier, config);
								}

								if (key.isFulltextIndexed()) {

									final NewIndexConfig config = new NewIndexConfig(typeName, key.dbName(), false, false, true);
									final String identifier     = typeName + "_" + key.dbName() + "_fulltext";

									schemaIndexConfig.put(identifier, config);
								}
							}

						} else {

							if (trait == null || whitelist.contains(type) || type.equals(trait.getLabel())) {

								if (key.isIndexed()) {

									final NewIndexConfig config = new NewIndexConfig(typeName, key.dbName(), true, true, false);
									final String identifier     = typeName + "_" + key.dbName();

									schemaIndexConfig.put(identifier, config);
								}

								if (key.isFulltextIndexed()) {

									final NewIndexConfig config = new NewIndexConfig(typeName, key.dbName(), true, false, true);
									final String identifier     = typeName + "_" + key.dbName() + "_fulltext";

									schemaIndexConfig.put(identifier, config);
								}
							}
						}
					}
				}

				// use map to make list of index configs unique
				graphDb.updateIndexConfiguration(new LinkedList<>(schemaIndexConfig.values()));

			} catch (Throwable t) {

				t.printStackTrace();

			} finally {

				IndexUpdateSemaphore.release();
			}
		}, "Structr Index Updater");

		indexUpdater.setName("indexUpdater");
		indexUpdater.setDaemon(true);
		indexUpdater.start();
	}

	private static String getIndexingTypeName(final TraitsInstance traitsInstance, final String typeName) {

		if (StructrTraits.GRAPH_OBJECT.equals(typeName)) {
			return StructrTraits.NODE_INTERFACE;
		}

		final Traits traits = traitsInstance.getTraits(typeName);
		if (traits.isRelationshipType()) {

			final Relation relation = traits.getRelation();
			if (relation != null) {

				return relation.name();
			}
		}

		return typeName;
	}
}
