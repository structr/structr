/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.flow;

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
import org.structr.flow.impl.*;
import org.structr.flow.impl.rels.*;
import org.structr.module.api.DeployableEntity;
import org.structr.schema.SchemaHelper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class FlowDeploymentHandler {

	private static final Logger logger = LoggerFactory.getLogger(FlowDeploymentHandler.class.getName());

	private static final Class[] classesToExport = {
			FlowAction.class,
			FlowAnd.class,
			FlowCall.class,
			FlowContainer.class,
			FlowDataSource.class,
			FlowDecision.class,
			FlowForEach.class,
			FlowGetProperty.class,
			FlowKeyValue.class,
			FlowNot.class,
			FlowNotNull.class,
			FlowObjectDataSource.class,
			FlowOr.class,
			FlowParameterInput.class,
			FlowParameterDataSource.class,
			FlowReturn.class,
			FlowScriptCondition.class,
			FlowStore.class,
			FlowAggregate.class,
			FlowConstant.class,
			FlowContainerConfiguration.class,
			FlowCollectionDataSource.class,
			FlowExceptionHandler.class
	};

	private static final Class[] relsToExport = {
			FlowCallContainer.class,
			FlowCallParameter.class,
			FlowConditionCondition.class,
			FlowConditionDataInput.class,
			FlowContainerBaseNode.class,
			FlowContainerFlowNode.class,
			FlowDataInput.class,
			FlowDataInputs.class,
			FlowDataSourceForEach.class,
			FlowDecisionCondition.class,
			FlowDecisionFalse.class,
			FlowDecisionTrue.class,
			FlowForEachBody.class,
			FlowKeySource.class,
			FlowKeyValueObjectInput.class,
			FlowNameDataSource.class,
			FlowNodeDataSource.class,
			FlowNodes.class,
			FlowValueSource.class,
			FlowAggregateStartValue.class,
			FlowScriptConditionSource.class,
			FlowContainerConfigurationFlow.class,
			FlowContainerConfigurationPrincipal.class,
			FlowExceptionHandlerNodes.class
	};

	public static void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {

		final App app                                = StructrApp.getInstance();
		final Path flowEngineFile					 = target.resolve("flow-engine.json");
		final List<Map<String, Object>> flowElements = new LinkedList<>();



		try (final Tx tx = app.tx()) {

			for (Class c : classesToExport) {

				for (final Object current : app.nodeQuery(c).getAsList()) {

					if (current instanceof DeployableEntity && ((NodeInterface)current).getType().equals(c.getSimpleName()) ) {

						flowElements.add( ((DeployableEntity)current).exportData() );

					}

				}

			}

			for (Class c : relsToExport) {

				for (final Object current : app.relationshipQuery(c).getAsList()) {

					Map<String, Object> attrs = new HashMap<>();
					attrs.put("type", c.getSimpleName());
					attrs.put("relType", ((RelationshipInterface)current).getRelType().name());
					attrs.put("sourceId", ((RelationshipInterface)current).getSourceNodeId());
					attrs.put("targetId", ((RelationshipInterface)current).getTargetNodeId());
					flowElements.add(attrs);

				}

			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(flowEngineFile.toFile()))) {

			gson.toJson(flowElements, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

	}





	public static void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {

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

					for (Class c : relsToExport) {

						for (final Object toDelete : app.relationshipQuery(c).getAsList()) {

							if (toDelete instanceof RelationshipInterface) {
								app.delete((RelationshipInterface) toDelete);
							}

						}

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
