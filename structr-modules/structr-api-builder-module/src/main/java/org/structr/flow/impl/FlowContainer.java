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
package org.structr.flow.impl;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.impl.rels.DOMNodeFLOWFlowContainer;
import org.structr.flow.impl.rels.FlowContainerBaseNode;
import org.structr.flow.impl.rels.FlowContainerConfigurationFlow;
import org.structr.flow.impl.rels.FlowContainerFlowNode;
import org.structr.flow.impl.rels.FlowContainerPackageFlow;
import org.structr.module.api.DeployableEntity;
import org.structr.web.entity.dom.DOMNode;

/**
 *
 */
public class FlowContainer extends AbstractNode implements DeployableEntity {

	public static final Property<FlowContainerPackage> flowPackage                    = new StartNode<>("flowPackage", FlowContainerPackageFlow.class);
	public static final Property<List<FlowBaseNode>> flowNodes                        = new EndNodes<>("flowNodes", FlowContainerBaseNode.class);
	public static final Property<List<FlowContainerConfiguration>> flowConfigurations = new StartNodes<>("flowConfigurations", FlowContainerConfigurationFlow.class);
	public static final Property<FlowNode> startNode                                  = new EndNode<>("startNode", FlowContainerFlowNode.class).indexed();
	public static final Property<String> name                                         = new StringProperty("name").notNull().indexed();
	public static final Property<Object> effectiveName                                = new FunctionProperty<>("effectiveName").indexed().unique().notNull().readFunction("if(empty(this.flowPackage), this.name, concat(this.flowPackage.effectiveName, \".\", this.name))").typeHint("String");
	public static final Property<Boolean> scheduledForIndexing                        = new BooleanProperty("scheduledForIndexing").defaultValue(false);
	public static final Property<List<DOMNode>> repeaterNodes                         = new StartNodes<>("repeaterNodes", DOMNodeFLOWFlowContainer.class);


	public static final View defaultView = new View(FlowContainer.class, PropertyView.Public, name, flowNodes, startNode, flowPackage, effectiveName, scheduledForIndexing, repeaterNodes);
	public static final View uiView      = new View(FlowContainer.class, PropertyView.Ui,     name, flowNodes, startNode, flowPackage, effectiveName, scheduledForIndexing, repeaterNodes);

	private static final Logger logger = LoggerFactory.getLogger(FlowContainer.class);

	@Export
	public Map<String, Object> evaluate(final Map<String, Object> parameters) {

		final FlowEngine engine       = new FlowEngine(new Context(null, parameters));
		final FlowResult result       = engine.execute(getProperty(startNode));
		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("error",  result.getError());
		map.put("result", result.getResult());

		return map;
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("name", this.getName());

		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		super.onCreation(securityContext, errorBuffer);

		this.setProperty(visibleToAuthenticatedUsers, true);
		this.setProperty(visibleToPublicUsers, true);
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		super.onModification(securityContext, errorBuffer, modificationQueue);
		setProperty(scheduledForIndexing, false);
	}

	@Override
	public void onNodeDeletion() {
		deleteChildren();
	}

	private void deleteChildren() {

		List<FlowBaseNode> nodes = getProperty(flowNodes);
		List<FlowContainerConfiguration> configs = getProperty(flowConfigurations);

		App app = StructrApp.getInstance();

		try (Tx tx = app.tx()) {
			for (FlowBaseNode node: nodes) {
				app.delete(node);
			}

			for (FlowContainerConfiguration conf: configs) {
				app.delete(conf);
			}

			tx.success();
		} catch (FrameworkException ex) {
			logger.warn("Could not handle onDelete for FlowContainer: " + ex.getMessage());
		}

	}

}
