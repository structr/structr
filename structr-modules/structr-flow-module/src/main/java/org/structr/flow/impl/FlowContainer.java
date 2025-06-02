/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.traits.definitions.FlowContainerTraitDefinition;
import org.structr.module.api.DeployableEntity;
import org.structr.web.entity.dom.DOMNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FlowContainer extends AbstractNodeTraitWrapper implements DeployableEntity {

	private static final Logger logger = LoggerFactory.getLogger(FlowContainer.class);

	public FlowContainer(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public FlowNode getStartNode() {

		final NodeInterface startNode = wrappedObject.getProperty(traits.key(FlowContainerTraitDefinition.START_NODE_PROPERTY));
		if (startNode != null) {

			return startNode.as(FlowNode.class);
		}

		return null;
	}

	public Iterable<FlowBaseNode> getFlowNodes() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(FlowContainerTraitDefinition.FLOW_NODES_PROPERTY));

		return Iterables.map(n -> n.as(FlowBaseNode.class), nodes);
	}

	public Iterable<FlowContainerConfiguration> getFlowConfigurations() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(FlowContainerTraitDefinition.FLOW_CONFIGURATIONS_PROPERTY));

		return Iterables.map(n -> n.as(FlowContainerConfiguration.class), nodes);
	}

	public String getEffectiveName() {
		return wrappedObject.getProperty(traits.key(FlowContainerTraitDefinition.EFFECTIVE_NAME_PROPERTY));
	}

	public void setEffectiveName(final String effectiveName) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowContainerTraitDefinition.EFFECTIVE_NAME_PROPERTY), effectiveName);
	}

	public void setScheduledForIndexing(final boolean b) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowContainerTraitDefinition.SCHEDULED_FOR_INDEXING_PROPERTY), false);
	}

	public void setStartNode(final FlowNode next) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowContainerTraitDefinition.START_NODE_PROPERTY), next);
	}

	public void setRepeaterNodes(final Iterable<DOMNode> repeaterNodes) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowContainerTraitDefinition.REPEATER_NODES_PROPERTY), repeaterNodes);
	}

	public Iterable<Object> evaluate(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {

		final FlowEngine engine = new FlowEngine();
		final Context context   = new Context();

		context.setParameters(parameters);

		final FlowNode entry        = getStartNode();
		final FlowResult flowResult = engine.execute(context, entry);

		if (flowResult.getError() != null) {

			// Log in case of error
			if (flowResult.getError().getCause() != null) {

				logger.error("Unexpected exception in flow [" + getEffectiveName() + "]:" , flowResult.getError().getCause());
			} else {

				logger.error("Unexpected exception in flow [" + getEffectiveName() + "]:" + flowResult.getError().getMessage());
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

	public Iterable<FlowBaseNode> getFlowNodes(final SecurityContext securityContext) {

		App app = StructrApp.getInstance(securityContext);

		try (Tx tx = app.tx()) {

			final Iterable<FlowBaseNode> nodes = getFlowNodes();

			tx.success();

			return nodes;

		} catch (FrameworkException ex) {

			logger.warn("Error while trying to get flow nodes.", ex);
		}

		return null;
	}

	public Iterable<RelationshipInterface> getFlowRelationships(final SecurityContext securityContext) {

		App app = StructrApp.getInstance(securityContext);

		List<RelationshipInterface> rels = null;

		try (Tx tx = app.tx()) {

			rels = new ArrayList<>();
			Iterable<FlowBaseNode> nodes = this.getFlowNodes();

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

		final Map<String, Object> result = new TreeMap<>();

		result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             wrappedObject.getUuid());
		result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           wrappedObject.getType());
		result.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                         wrappedObject.getName());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        wrappedObject.isVisibleToPublicUsers());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, wrappedObject.isVisibleToAuthenticatedUsers());

		return result;
	}

	public void deleteChildren() {

		final Iterable<FlowContainerConfiguration> configs = getFlowConfigurations();
		final Iterable<FlowBaseNode> nodes                 = getFlowNodes();
		final App app                                      = StructrApp.getInstance();

		try (Tx tx = app.tx()) {

			for (final FlowBaseNode node: nodes) {
				app.delete(node);
			}

			for (final FlowContainerConfiguration conf: configs) {
				app.delete(conf);
			}

			tx.success();

		} catch (FrameworkException ex) {
			logger.warn("Could not handle onDelete for FlowContainer: " + ex.getMessage());
		}
	}
}
