/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.schema.compiler.NodeExtender;

/**
 *
 *
 */
public class SchemaService implements Service {

	private static final Logger logger                            = LoggerFactory.getLogger(SchemaService.class.getName());
	private static final AtomicBoolean compiling                  = new AtomicBoolean(false);
	private static final AtomicBoolean updating                   = new AtomicBoolean(false);
	private static final Map<String, String> builtinTypeMap       = new LinkedHashMap<>();

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public void initialize(final StructrServices services, final Properties config) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		services.registerInitializationCallback(new InitializationCallback() {

			@Override
			public void initializationDone() {
				reloadSchema(new ErrorBuffer());
			}
		});
	}

	public static void registerBuiltinTypeOverride(final String type, final String fqcn) {
		builtinTypeMap.put(type, fqcn);
	}

	public static boolean reloadSchema(final ErrorBuffer errorBuffer) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		boolean success = true;

		// compiling must only be done once
		if (compiling.compareAndSet(false, true)) {

			try {

				final Set<String> dynamicViews  = new LinkedHashSet<>();
				final NodeExtender nodeExtender = new NodeExtender();

				try (final Tx tx = StructrApp.getInstance().tx()) {

					SchemaService.ensureBuiltinTypesExist();

					// collect node classes
					final List<SchemaNode> schemaNodes = StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList();
					for (final SchemaNode schemaNode : schemaNodes) {

						nodeExtender.addClass(schemaNode.getClassName(), schemaNode.getSource(errorBuffer));

						final String auxSource = schemaNode.getAuxiliarySource();
						if (auxSource != null) {

							nodeExtender.addClass("_" + schemaNode.getClassName() + "Helper", auxSource);
						}

						dynamicViews.addAll(schemaNode.getViews());
					}

					// collect relationship classes
					for (final SchemaRelationshipNode schemaRelationship : StructrApp.getInstance().nodeQuery(SchemaRelationshipNode.class).getAsList()) {

						nodeExtender.addClass(schemaRelationship.getClassName(), schemaRelationship.getSource(errorBuffer));

						final String auxSource = schemaRelationship.getAuxiliarySource();
						if (auxSource != null) {

							nodeExtender.addClass("_" + schemaRelationship.getClassName() + "Helper", auxSource);
						}

						dynamicViews.addAll(schemaRelationship.getViews());
					}

					// this is a very critical section :)
					synchronized (SchemaService.class) {

						// clear propagating relationship cache (test)
						SchemaRelationshipNode.clearPropagatingRelationshipTypes();

						// compile all classes at once and register
						Map<String, Class> newTypes = nodeExtender.compile(errorBuffer);

						for (final Class newType : newTypes.values()) {

							// do full reload
							config.registerEntityType(newType);

							// instantiate classes to execute
							// static initializer of helpers
							try { newType.newInstance(); } catch (Throwable t) {}
						}
					}

					// create properties and views etc.
					for (final SchemaNode schemaNode : StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList()) {
						schemaNode.createBuiltInSchemaEntities(errorBuffer);
					}

					success = !errorBuffer.hasError();

					// inject views in configuration provider
					if (success) {

						// prevent inheritance map from leaking
						SearchCommand.clearInheritanceMap();
						NodeFactory.invalidateCache();
						RelationshipFactory.invalidateCache();
						AccessPathCache.invalidate();

						// clear relationship instance cache
						AbstractNode.clearRelationshipTemplateInstanceCache();


						config.registerDynamicViews(dynamicViews);
						tx.success();
					}

				} catch (Throwable t) {

					logger.error("Unable to compile dynamic schema.", t);

					success = false;
				}

				calculateHierarchy();
				updateIndexConfiguration();

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

		for (final Entry<String, String> entry : builtinTypeMap.entrySet()) {

			final String type = entry.getKey();
			final String fqcn = entry.getValue();

			SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName(type).getFirst();
			if (schemaNode == null) {

				schemaNode = app.create(SchemaNode.class, type);
			}

			schemaNode.setProperty(SchemaNode.extendsClass, fqcn);
			schemaNode.unlockSystemPropertiesOnce();
			schemaNode.setProperty(SchemaNode.isBuiltinType, true);
		}
	}

	@Override
	public boolean isVital() {
		return true;
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

	private static void updateIndexConfiguration() {

		final Thread indexUpdater = new Thread(new Runnable() {

			@Override
			public void run() {

				if (updating.compareAndSet(false, true)) {

					try {

						final Map<String, Object> params = new HashMap<>();
						final App app                    = StructrApp.getInstance();

						try (final Tx tx = app.tx()) {

							for (final Entry<String, Map<String, PropertyKey>> entry : StructrApp.getConfiguration().getTypeAndPropertyMapping().entrySet()) {

								final String type = StringUtils.substringAfterLast(entry.getKey(), ".");

								for (final PropertyKey key : entry.getValue().values()) {

									if (key.isIndexed() || key.isIndexedWhenEmpty()) {

										final Class declaringClass = key.getDeclaringClass();
										if (declaringClass != null) {

											// do not create indexes for relationship classes
											if (!RelationshipInterface.class.isAssignableFrom(declaringClass)) {

												app.cypher("CREATE INDEX ON :" + type + "(" + key.dbName() + ")", params);
											}

										} else {

											app.cypher("CREATE INDEX ON :NodeInterface(" + key.dbName() + ")", params);
										}
									}
								}

							}

							tx.success();
						}

					} catch (Throwable ignore) {

						// ignore for now..

					} finally {

						updating.set(false);
					}
				}
			}
		});

		indexUpdater.setDaemon(true);
		indexUpdater.start();
	}
}
