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
package org.structr.flow.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.RelationshipType;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.impl.rels.*;
import org.structr.module.api.DeployableEntity;
import org.structr.web.entity.dom.DOMNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
public class FlowContainer extends AbstractNode implements DeployableEntity {

	public static final Property<FlowContainerPackage> flowPackage                        = new StartNode<>("flowPackage", FlowContainerPackageFlow.class);
	public static final Property<Iterable<FlowBaseNode>> flowNodes                        = new EndNodes<>("flowNodes", FlowContainerBaseNode.class);
	public static final Property<Iterable<FlowContainerConfiguration>> flowConfigurations = new StartNodes<>("flowConfigurations", FlowContainerConfigurationFlow.class);
	public static final Property<FlowContainerConfiguration> activeFlowConfiguration      = new StartNode<>("activeConfiguration", FlowActiveContainerConfiguration.class);
	public static final Property<FlowNode> startNode                                      = new EndNode<>("startNode", FlowContainerFlowNode.class).indexed();
	public static final Property<String> name                                             = new StringProperty("name").notNull().indexed();
	public static final Property<Object> effectiveName                                    = new FunctionProperty<>("effectiveName").indexed().unique().notNull().readFunction("if(empty(this.flowPackage), this.name, concat(this.flowPackage.effectiveName, \".\", this.name))").writeFunction("{\r\n\tlet self = Structr.get(\'this\');\r\n\tlet path = Structr.get(\'value\');\r\n\r\n\tfunction getOrCreatePackage(name, path) {\r\n\t\tlet effectiveName = Structr.empty(path) ? name : Structr.concat(path,\".\",name);\r\n\r\n\t\tlet pack = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", effectiveName));\r\n\r\n\t\tif (Structr.empty(path)) {\r\n\t\t\t\r\n\t\t\tif (Structr.empty(pack)) {\r\n\t\t\t\tpack = Structr.create(\"FlowContainerPackage\", \"name\", name);\r\n\t\t\t}\r\n\t\t} else {\r\n\t\t\tlet parent = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", path));\r\n\r\n\t\t\tif (Structr.empty(pack)) {\r\n\t\t\t\tpack = Structr.create(\"FlowContainerPackage\", \"name\", name, \"parent\", parent);\r\n\t\t\t}\r\n\t\t}\r\n\r\n\t\treturn pack;\r\n\t}\r\n\r\n\tif (!Structr.empty(path)) {\r\n\r\n\t\tif (path.length > 0) {\r\n\r\n\t\t\tlet flowName = null;\r\n\r\n\t\t\tif (path.indexOf(\".\") !== -1) {\r\n\r\n\t\t\t\tlet elements = path.split(\".\");\r\n\r\n\t\t\t\tif (elements.length > 1) {\r\n\r\n\t\t\t\t\tflowName = elements.pop();\r\n\t\t\t\t\tlet currentPath = \"\";\r\n\t\t\t\t\tlet parentPackage = null;\r\n\r\n\t\t\t\t\tfor (let el of elements) {\r\n\t\t\t\t\t\tlet pack = getOrCreatePackage(el, currentPath);\r\n\t\t\t\t\t\tparentPackage = pack;\r\n\t\t\t\t\t\tcurrentPath = pack.effectiveName;\r\n\t\t\t\t\t}\r\n\r\n\t\t\t\t\tself.flowPackage = parentPackage;\r\n\t\t\t\t} else {\r\n\r\n\t\t\t\t\tflowName = elements[0];\r\n\t\t\t\t}\r\n\r\n\t\t\t\tself.name = flowName;\r\n\t\t\t} else {\r\n\r\n\t\t\t\tself.name = path;\r\n\t\t\t}\r\n\r\n\t\t}\r\n\r\n\t}\r\n\r\n}").typeHint("String");
	public static final Property<Boolean> scheduledForIndexing                            = new BooleanProperty("scheduledForIndexing").defaultValue(false);
	public static final Property<Iterable<DOMNode>> repeaterNodes                         = new StartNodes<>("repeaterNodes", DOMNodeFLOWFlowContainer.class);
	public static final Property<String> apiSpecification                                 = new StringProperty("apiSpecification");


	public static final View defaultView       = new View(FlowContainer.class, PropertyView.Public, name, flowNodes, startNode, effectiveName, scheduledForIndexing, repeaterNodes, activeFlowConfiguration);
	public static final View uiView            = new View(FlowContainer.class, PropertyView.Ui,     name, flowNodes, startNode, flowPackage, effectiveName, scheduledForIndexing, repeaterNodes, activeFlowConfiguration, apiSpecification);
	public static final View effectiveNameView = new View(FlowContainer.class, "effectiveNameView", typeHandler, id, effectiveName);

	private static final Logger logger = LoggerFactory.getLogger(FlowContainer.class);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidStringNotBlank(this, FlowContainer.name, errorBuffer);
		valid &= ValidationHelper.isValidPropertyNotNull(this, FlowContainer.effectiveName, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, FlowContainer.effectiveName, errorBuffer);

		return valid;
	}

	@Export
	public Iterable<Object> evaluate(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {

		final FlowEngine engine       = new FlowEngine();
		final Context context         = new Context();
		context.setParameters(parameters);
		final FlowNode entry          = getProperty(startNode);
		final FlowResult flowResult       = engine.execute(context, entry);

		if (flowResult.getError() != null) {

			// Log in case of error
			if (flowResult.getError().getCause() != null) {

				logger.error("Unexpected exception in flow [" + getProperty(effectiveName) + "]:" , flowResult.getError().getCause());
			} else {

				logger.error("Unexpected exception in flow [" + getProperty(effectiveName) + "]:" + flowResult.getError().getMessage());
			}

			final List<Object> result = new ArrayList<>();
			result.add(flowResult.getError());
			return result;
		}

		if (flowResult.getResult() instanceof Iterable) {

			return (Iterable)flowResult.getResult();
		} else {

			final List<Object> result = new ArrayList<>();
			result.add(flowResult.getResult());
			return result;
		}

	}

	@Export
	public Iterable<FlowBaseNode> getFlowNodes(final SecurityContext securityContext) {

		App app = StructrApp.getInstance(securityContext);

		try (Tx tx = app.tx()) {

			return this.getProperty(FlowContainer.flowNodes);


		} catch (FrameworkException ex) {

			logger.warn("Error while trying to get flow nodes.", ex);
		}

		return null;
	}

	@Export
	public Iterable<AbstractRelationship> getFlowRelationships(final SecurityContext securityContext) {

		App app = StructrApp.getInstance(securityContext);

		List<AbstractRelationship> rels = null;

		try (Tx tx = app.tx()) {

			rels = new ArrayList<>();
			Iterable<FlowBaseNode> nodes = this.getProperty(FlowContainer.flowNodes);

			for (final FlowBaseNode node : nodes) {

				 rels.addAll(StreamSupport.stream(node.getRelationships().spliterator(), false).filter(rel -> {
					final RelationshipType relType = rel.getRelType();
					if (!"SECURITY".equals(relType.name()) && !"OWNS".equals(relType.name())) {
						return true;
					}
					return false;
				}).collect(Collectors.toList()));

				tx.success();

			}

		} catch (FrameworkException ex) {

			logger.warn("Error while trying to get flow relationships flow.", ex);
		}

		if (rels != null) {

			rels = rels.stream().distinct().collect(Collectors.toList());
		}

		return rels;

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
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {
		deleteChildren();
	}

	private void deleteChildren() {

		final Iterable<FlowBaseNode> nodes                 = getProperty(flowNodes);
		final Iterable<FlowContainerConfiguration> configs = getProperty(flowConfigurations);
		final App app                                      = StructrApp.getInstance();

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
