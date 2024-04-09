/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.deployment;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowContainerConfiguration;
import org.structr.flow.impl.rels.FlowContainerConfigurationFlow;
import org.structr.module.api.DeployableEntity;
import org.structr.schema.SchemaHelper;
import org.structr.web.common.AbstractMapComparator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FlowLegacyDeploymentHandler extends FlowAbstractDeploymentHandler implements FlowDeploymentInterface {

	private static final Logger logger = LoggerFactory.getLogger(FlowLegacyDeploymentHandler.class.getName());

	@Override
	public void doExport(final Path target, final Gson gson) throws FrameworkException {
		final App app                                		= StructrApp.getInstance();
		final Path flowEngineFile							= target.resolve("flow-engine.json");
		final List<Map<String, ?>> flowElements 			= new LinkedList<>();
		final List<Map<String, String>> flowRelationships 	= new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (Class c : classesToExport) {

				for (final Object current : app.nodeQuery(c).sort(StructrApp.key(NodeInterface.class, "id")).getAsList()) {

					if (current instanceof DeployableEntity && ((NodeInterface)current).getType().equals(c.getSimpleName()) ) {

						flowElements.add( ((DeployableEntity)current).exportData() );
					}
				}
			}

			// Special handling for FlowContainerConfiguration: Only export last modified layout

			for (final FlowContainer cont: app.nodeQuery(FlowContainer.class).sort(StructrApp.key(NodeInterface.class, "id")).getAsList()) {

				final FlowContainerConfiguration conf = app.nodeQuery(FlowContainerConfiguration.class).and(FlowContainerConfiguration.flow, cont).sort(StructrApp.key(NodeInterface.class,"lastModifiedDate"), true).getFirst();

				if (conf != null) {

					flowElements.add(conf.exportData());

					// Export Rel
					Map<String, String> attrs = new TreeMap<>();
					attrs.put("type", FlowContainerConfigurationFlow.class.getSimpleName());
					attrs.put("relType", "CONTAINS_CONFIGURATION_FOR");
					attrs.put("sourceId", conf.getUuid());
					attrs.put("targetId", cont.getUuid());
					flowRelationships.add(attrs);
				}

			}

			for (Class c : relsToExport) {

				for (final Object current : app.relationshipQuery(c).getAsList()) {

					Map<String, String> attrs = new TreeMap<>();
					attrs.put("type", c.getSimpleName());
					attrs.put("relType", ((RelationshipInterface)current).getRelType().name());
					attrs.put("sourceId", ((RelationshipInterface)current).getSourceNodeId());
					attrs.put("targetId", ((RelationshipInterface)current).getTargetNodeId());
					flowRelationships.add(attrs);
				}
			}

			flowRelationships.sort(new AbstractMapComparator<String>() {
				@Override
				public String getKey (Map<String, String> map) {
					return map.get("sourceId").concat(map.get("targetId"));
				}
			});

			flowElements.addAll(flowRelationships);

			tx.success();
		}


		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(flowEngineFile.toFile()))) {

			gson.toJson(flowElements, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	@Override
	public void doImport(final Path source, final Gson gson) throws FrameworkException {
		final Path flowPath = source.resolve("flow-engine.json");
		if (Files.exists(flowPath)) {

			logger.info("Reading {}..", flowPath);

			try (final Reader reader = Files.newBufferedReader(flowPath, Charset.forName("utf-8"))) {

				final List<Map<String, Object>> flowElements = gson.fromJson(reader, List.class);

				final SecurityContext context = SecurityContext.getSuperUserInstance();
				context.setDoTransactionNotifications(false);

				final App app                 = StructrApp.getInstance(context);

				try (final Tx tx = app.tx()) {

					for (Class c : classesToExport) {

						for (final Object toDelete : app.nodeQuery(c).getAsList()) {

							if (toDelete instanceof NodeInterface) {
								app.delete((NodeInterface) toDelete);
							}
						}
					}

					// Special handling for FlowContainerConfiguration
					for (final FlowContainerConfiguration toDelete : app.nodeQuery(FlowContainerConfiguration.class).getAsList()) {

						app.delete(toDelete);
					}


					for (Class c : relsToExport) {

						for (final Object toDelete : app.relationshipQuery(c).getAsList()) {

							if (toDelete instanceof RelationshipInterface) {
								app.delete((RelationshipInterface) toDelete);
							}
						}
					}

					// Special handling for FlowContainerConfigurationFlow
					for (final FlowContainerConfigurationFlow toDelete : app.relationshipQuery(FlowContainerConfigurationFlow.class).getAsList()) {

						app.delete(toDelete);
					}

					for (final Map<String, Object> entry : flowElements) {

						final Object rawType = entry.get("type");

						if (rawType != null ) {

							final String type = (String)rawType;
							final Class clazz = SchemaHelper.getEntityClassForRawType(type);

							if (clazz != null) {

								if ( !entry.containsKey("relType") ) {
									final PropertyMap map = PropertyMap.inputTypeToJavaType(context, clazz, entry);
									app.create(clazz, map);
								} else {

									final NodeInterface sourceNode = app.nodeQuery().uuid((String)entry.get("sourceId")).getFirst();
									final NodeInterface targetNode = app.nodeQuery().uuid((String)entry.get("targetId")).getFirst();

									app.create(sourceNode, targetNode, SchemaHelper.getEntityClassForRawType((String)entry.get("type")));

								}
							}
						}
					}

					tx.success();
				}

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}
	}

}
