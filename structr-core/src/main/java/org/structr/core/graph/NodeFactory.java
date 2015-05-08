/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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

import org.neo4j.graphdb.Node;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;


import java.util.*;
import java.util.logging.Logger;
import org.neo4j.gis.spatial.indexprovider.SpatialRecordHits;
import org.neo4j.graphdb.index.IndexHits;
import org.structr.common.AccessControllable;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.NodeHasLocation;

//~--- classes ----------------------------------------------------------------

/**
 * A factory for structr nodes.
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class NodeFactory<T extends NodeInterface & AccessControllable> extends Factory<Node, T> {

	private static final Logger logger = Logger.getLogger(NodeFactory.class.getName());

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
	public T instantiate(final Node node) throws FrameworkException {
		return (T) instantiateWithType(node, factoryDefinition.determineNodeType(node), false);
	}

	@Override
	public T instantiateWithType(final Node node, final Class<T> nodeClass, boolean isCreation) throws FrameworkException {

		// cannot instantiate node without type
		if (nodeClass == null) {
			return null;
		}

		SecurityContext securityContext = factoryProfile.getSecurityContext();
		T newNode                       = null;

		try {
			newNode = nodeClass.newInstance();

		} catch (InstantiationException|IllegalAccessException itex) {
			newNode = null;
		}

		if (newNode == null) {
			newNode = (T)factoryDefinition.createGenericNode();
		}

		newNode.init(factoryProfile.getSecurityContext(), node, nodeClass, isCreation);
		newNode.onNodeInstantiation();

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
	public Result instantiate(final IndexHits<Node> input) throws FrameworkException {

		if (input != null && input instanceof SpatialRecordHits) {
			return resultFromSpatialRecords((SpatialRecordHits) input);
		}

		return super.instantiate(input);
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

			} catch (InstantiationException|IllegalAccessException itex) { itex.printStackTrace(); }

		}

		return newNode;

	}

	private Result resultFromSpatialRecords(final SpatialRecordHits spatialRecordHits) throws FrameworkException {

		final int pageSize                    = factoryProfile.getPageSize();
		final SecurityContext securityContext = factoryProfile.getSecurityContext();
		final boolean includeDeletedAndHidden = factoryProfile.includeDeletedAndHidden();
		final boolean publicOnly              = factoryProfile.publicOnly();
		List<T> nodes                         = new LinkedList<>();
		int size                              = spatialRecordHits.size();
		int position                          = 0;
		int count                             = 0;
		int offset                            = 0;

		try (final SpatialRecordHits closeable = spatialRecordHits) {

			for (Node node : closeable) {

				Node realNode = node;
				if (realNode != null) {

					// FIXME: type cast is not good here...
					T n = instantiate(realNode);

					nodes.add(n);

					// Check is done in createNodeWithType already, so we don't have to do it again
					if (n != null) {    // && isReadable(securityContext, n, includeDeletedAndHidden, publicOnly)) {

						List<T> nodesAt = (List<T>)getNodesAt(n);

						size += nodesAt.size();

						for (T nodeAt : nodesAt) {

							if (nodeAt != null && securityContext.isReadable(nodeAt, includeDeletedAndHidden, publicOnly)) {

								if (++position > offset) {

									// stop if we got enough nodes
									if (++count > pageSize) {

										return new Result(nodes, size, true, false);
									}

									nodes.add((T)nodeAt);
								}

							}

						}

					}

				}

			}
		}

		return new Result(nodes, size, true, false);

	}

	/**
	 * Return all nodes which are connected by an incoming IS_AT relationships
	 *
	 * @param locationNode
	 * @return connected nodes
	 */
	protected List<NodeInterface> getNodesAt(final NodeInterface locationNode) {

		final List<NodeInterface> nodes = new LinkedList<>();

		// FIXME this was getRelationships before..
//		for(RelationshipInterface rel : locationNode.getRelationships(NodeHasLocation.class)) {
		for(RelationshipInterface rel : locationNode.getIncomingRelationships(NodeHasLocation.class)) {

			NodeInterface startNode = rel.getSourceNode();

			nodes.add(startNode);

			// add more nodes which are "at" this one
			nodes.addAll(getNodesAt(startNode));
		}

		return nodes;

	}
}
