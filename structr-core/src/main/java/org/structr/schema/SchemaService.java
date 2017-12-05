/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.Command;
import org.structr.api.service.InitializationCallback;
import org.structr.api.service.Service;
import org.structr.api.service.StructrServices;
import org.structr.common.AccessPathCache;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.schema.compiler.NodeExtender;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonSchema;

/**
 *
 *
 */
public class SchemaService implements Service {

	private static final Logger logger            = LoggerFactory.getLogger(SchemaService.class.getName());
	private static final JsonSchema dynamicSchema = StructrSchema.newInstance(URI.create("https://structr.org/v2.0/#"));
	private static final AtomicBoolean compiling  = new AtomicBoolean(false);
	private static final AtomicBoolean updating   = new AtomicBoolean(false);

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public boolean initialize(final StructrServices services) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		services.registerInitializationCallback(new InitializationCallback() {

			@Override
			public void initializationDone() {
				reloadSchema(new ErrorBuffer(), null);
			}
		});

		return true;
	}

	public static JsonSchema getDynamicSchema() {
		return dynamicSchema;
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer, final String initiatedBySessionId) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		boolean success = true;

		// compiling must only be done once
		if (compiling.compareAndSet(false, true)) {

			try {

				final Map<String, Map<String, PropertyKey>> removedClasses = new HashMap<>(StructrApp.getConfiguration().getTypeAndPropertyMapping());
				final NodeExtender nodeExtender                            = new NodeExtender(initiatedBySessionId);
				final Set<String> dynamicViews                             = new LinkedHashSet<>();

				try (final Tx tx = StructrApp.getInstance().tx()) {

					// collect auto-generated schema nodes
					SchemaService.ensureBuiltinTypesExist();

					// add schema nodes from database
					for (final SchemaNode schemaInfo : StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList()) {

						schemaInfo.handleMigration();

						final String sourceCode = SchemaHelper.getSource(schemaInfo, errorBuffer);
						if (sourceCode != null) {

							// only load dynamic node if there were no errors while generating
							// the source code (missing modules etc.)
							nodeExtender.addClass(schemaInfo.getClassName(), sourceCode);
							dynamicViews.addAll(schemaInfo.getDynamicViews());
						}
					}

					// collect relationship classes
					for (final SchemaRelationshipNode schemaRelationship : StructrApp.getInstance().nodeQuery(SchemaRelationshipNode.class).getAsList()) {

						nodeExtender.addClass(schemaRelationship.getClassName(), schemaRelationship.getSource(errorBuffer));
						dynamicViews.addAll(schemaRelationship.getDynamicViews());
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

							} catch (Throwable ignore) {}
						}

						// calculate difference between previous and new classes
						removedClasses.keySet().removeAll(StructrApp.getConfiguration().getTypeAndPropertyMapping().keySet());
					}

					// create properties and views etc.
					for (final SchemaNode schemaNode : StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList()) {
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
						AbstractNode.clearPermissionResolutionCache();

						// inject views in configuration provider
						config.registerDynamicViews(dynamicViews);

						if (Services.calculateHierarchy() || !Services.isTesting()) {

							calculateHierarchy();
						}

						if (Services.updateIndexConfiguration() || !Services.isTesting()) {

							updateIndexConfiguration(removedClasses);
						}

						tx.success();
					}

				} catch (Throwable t) {

					t.printStackTrace();

					logger.error("Unable to compile dynamic schema: {}", t.getMessage());
					success = false;
				}

			} finally {

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

	public static void ensureBuiltinTypesExist() throws FrameworkException {

		final App app = StructrApp.getInstance();

		try {

			StructrSchema.extendDatabaseSchema(app, dynamicSchema);

		} catch (URISyntaxException ex) {

			ex.printStackTrace();
		}
	}

	@Override
	public boolean isVital() {
		return true;
	}

	// ----- interface Feature -----
	@Override
	public String getModuleName() {
		return "core";
	}

	// ----- private methods -----
	private static void calculateHierarchy() {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final List<SchemaNode> schemaNodes  = StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList();
			final Set<String> alreadyCalculated = new HashSet<>();
			final Map<String, SchemaNode> map   = new LinkedHashMap<>();

			// populate lookup map
			for (final SchemaNode schemaNode : schemaNodes) {
				map.put(schemaNode.getName(), schemaNode);
			}

			// calc hierarchy
			for (final SchemaNode schemaNode : schemaNodes) {

				final int relCount = schemaNode.getProperty(SchemaNode.relatedFrom).size() + schemaNode.getProperty(SchemaNode.relatedTo).size();
				final int level    = recursiveGetHierarchyLevel(map, alreadyCalculated, schemaNode, 0);

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

						final Map<String, Object> params = new HashMap<>();
						final App app                    = StructrApp.getInstance();

						// create indices for properties of existing classes
						for (final Entry<String, Map<String, PropertyKey>> entry : StructrApp.getConfiguration().getTypeAndPropertyMapping().entrySet()) {

							final Class type = getType(entry.getKey());
							if (type != null) {

								final String typeName = type.getSimpleName();

								try (final Tx tx = app.tx()) {

									for (final PropertyKey key : entry.getValue().values()) {

										final String indexKey    = "index." + typeName + "." + key.dbName();
										final String value       = app.getGlobalSetting(indexKey, null);
										final boolean alreadySet = "true".equals(value);
										boolean createIndex      = key.isIndexed() || key.isIndexedWhenEmpty();

										createIndex &= !NonIndexed.class.isAssignableFrom(type);
										createIndex &= NodeInterface.class.equals(type) || !GraphObject.id.equals(key);

										if (createIndex) {

											if (!alreadySet) {

												try {

													// create index
													app.cypher("CREATE INDEX ON :" + typeName + "(" + key.dbName() + ")", params);

												} catch (Throwable t) {
													logger.warn("", t);
												}

												// store the information that we already created this index
												app.setGlobalSetting(indexKey, "true");
											}

										} else if (alreadySet) {

											try {

												// drop index
												app.cypher("DROP INDEX ON :" + typeName + "(" + key.dbName() + ")", params);

											} catch (Throwable t) {
												logger.warn("", t);
											}

											// remove entry from config file
											app.setGlobalSetting(indexKey, null);
										}

									}

									tx.success();

								} catch (Throwable ignore) {
									logger.warn("", ignore);
								}
							}
						}

						// drop indices for all indexed properties of removed classes
						for (final Entry<String, Map<String, PropertyKey>> entry : removedClasses.entrySet()) {

							final String typeName = StringUtils.substringAfterLast(entry.getKey(), ".");

							for (final PropertyKey key : entry.getValue().values()) {

								try {

									final String indexKey = "index." + typeName + "." + key.dbName();
									final String value    = app.getGlobalSetting(indexKey, null);
									final boolean exists  = "true".equals(value);
									boolean dropIndex     = key.isIndexed() || key.isIndexedWhenEmpty();

									dropIndex &= !GraphObject.id.equals(key);

									if (dropIndex && exists) {

										try (final Tx tx = app.tx()) {

											// drop index
											app.cypher("DROP INDEX ON :" + typeName + "(" + key.dbName() + ")", params);

											tx.success();

										} catch (Throwable t) {
											logger.warn("", t);
										}

										// remove entry from config file
										app.setGlobalSetting(indexKey, null);
									}

								} catch (FrameworkException ignore) {}
							}
						}

					} finally {

						updating.set(false);
					}
				}
			}
		});

		indexUpdater.setDaemon(true);
		indexUpdater.start();
	}

	private static Class getType(final String name) {

		try { return Class.forName(name); } catch (ClassNotFoundException ignore) {}

		// fallback: use dynamic class from simple name
		return StructrApp.getConfiguration().getNodeEntityClass(StringUtils.substringAfterLast(name, "."));
	}
}
