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
package org.structr.core.graph;


import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.NodeHasLocation;

//~--- classes ----------------------------------------------------------------

/**
 * A factory for structr nodes.
 *
 * @param <T>
 *
 *
 *
 */
public class NodeFactory<T extends NodeInterface & AccessControllable> extends Factory<Node, T> {

	private static final Logger logger = LoggerFactory.getLogger(NodeFactory.class.getName());

	public NodeFactory(final SecurityContext securityContext) {
		super(securityContext);
	}

	public NodeFactory(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly) {
		super(securityContext, includeDeletedAndHidden, publicOnly);
	}

	public NodeFactory(final SecurityContext securityContext, final int pageSize, final int page, final String offsetId) {
		super(securityContext, pageSize, page, offsetId);
	}

	public NodeFactory(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly, final int pageSize, final int page, final String offsetId) {
		super(securityContext, includeDeletedAndHidden, publicOnly, pageSize, page, offsetId);
	}

	@Override
	public T instantiate(final Node node) {
		return instantiate(node, null);
	}

	@Override
	public T instantiate(final Node node, final Relationship pathSegment) {

		if (node == null) {
			return null;
		}

		if (TransactionCommand.isDeleted(node)) {
			return (T)instantiateWithType(node, null, pathSegment, false);
		}

		return (T) instantiateWithType(node, factoryDefinition.determineNodeType(node), pathSegment, false);
	}

	@Override
	public T instantiateWithType(final Node node, final Class<T> nodeClass, final Relationship pathSegment, boolean isCreation) {

		// cannot instantiate node without type
		if (nodeClass == null) {
			return null;
		}

		SecurityContext securityContext = factoryProfile.getSecurityContext();
		T newNode                       = null;

		try {
			newNode = nodeClass.newInstance();

		} catch (NoClassDefFoundError|InstantiationException|IllegalAccessException itex) {
			newNode = null;
		}

		if (newNode == null) {
			newNode = (T)factoryDefinition.createGenericNode();
		}

		newNode.init(factoryProfile.getSecurityContext(), node, nodeClass, isCreation);
		newNode.setRawPathSegment(pathSegment);
		newNode.onNodeInstantiation(isCreation);

		// check access
		if (isCreation || securityContext.isReadable(newNode, factoryProfile.includeDeletedAndHidden(), factoryProfile.publicOnly())) {

			return newNode;
		}

		return null;
	}

	@Override
	public T instantiate(final Node node, final boolean includeDeletedAndHidden, final boolean publicOnly) throws FrameworkException {

		factoryProfile.setIncludeDeletedAndHidden(includeDeletedAndHidden);
		factoryProfile.setPublicOnly(publicOnly);

		return instantiate(node);
	}

	@Override
	public T instantiateDummy(final Node entity, final String entityType) throws FrameworkException {

		Map<String, Class<? extends NodeInterface>> entities = StructrApp.getConfiguration().getNodeEntities();
		Class<T> nodeClass                                   = (Class<T>)entities.get(entityType);
		T newNode                                            = null;

		if (nodeClass != null) {

			try {

				newNode = nodeClass.newInstance();
				newNode.init(factoryProfile.getSecurityContext(), entity, nodeClass, false);

			} catch (InstantiationException|IllegalAccessException itex) {

				logger.warn("", itex);
			}

		}

		return newNode;

	}

	/**
	 * Return all nodes which are connected by an incoming IS_AT relationships
	 *
	 * @param locationNode
	 * @return connected nodes
	 */
	protected List<NodeInterface> getNodesAt(final NodeInterface locationNode) {

		final List<NodeInterface> nodes = new LinkedList<>();

		for(RelationshipInterface rel : locationNode.getIncomingRelationships(NodeHasLocation.class)) {

			NodeInterface startNode = rel.getSourceNode();

			nodes.add(startNode);

			// add more nodes which are "at" this one
			nodes.addAll(getNodesAt(startNode));
		}

		return nodes;

	}
}
