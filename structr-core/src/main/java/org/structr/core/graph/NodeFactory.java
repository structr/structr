/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

import java.lang.reflect.Constructor;

import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.module.ModuleService;
import org.neo4j.gis.spatial.indexprovider.SpatialRecordHits;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractRelationship;

//~--- classes ----------------------------------------------------------------

/**
 * A factory for structr nodes.
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class NodeFactory<T extends AbstractNode> extends Factory<Node, T> {

	private static final Logger logger        = Logger.getLogger(NodeFactory.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<Class, Constructor<T>> constructors = new LinkedHashMap<Class, Constructor<T>>();

	//~--- constructors ---------------------------------------------------

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

	//~--- methods --------------------------------------------------------

	@Override
	public T instantiate(final Node node) throws FrameworkException {

		String nodeType = factoryDefinition.determineNodeType(node);

		return instantiateWithType(node, nodeType, false);

	}

	@Override
	public T instantiateWithType(final Node node, final String nodeType, boolean isCreation) throws FrameworkException {

		SecurityContext securityContext = factoryProfile.getSecurityContext();
		T newNode = (T)securityContext.lookup(node);
		
		if (newNode == null) {

			Class<T> nodeClass = Services.getService(ModuleService.class).getNodeEntityClass(nodeType);
			if (nodeClass != null) {

				try {

					Constructor<T> constructor = constructors.get(nodeClass);
					if (constructor == null) {

						constructor = nodeClass.getConstructor();

						constructors.put(nodeClass, constructor);

					}

					// newNode = (AbstractNode) nodeClass.newInstance();
					newNode = constructor.newInstance();

				} catch (Throwable t) {

					newNode = null;

				}

			}

			if (newNode == null) {
				// FIXME
				newNode = (T)factoryDefinition.createGenericNode();
			}


			newNode.init(factoryProfile.getSecurityContext(), node);
			newNode.onNodeInstantiation();

			String newNodeType = newNode.getProperty(AbstractNode.type);
			if (newNodeType == null) { //  || (newNodeType != null && !newNodeType.equals(nodeType))) {
				
				try {

					newNode.unlockReadOnlyPropertiesOnce();
					newNode.setProperty(AbstractNode.type, nodeType);

				} catch (Throwable t) {

					logger.log(Level.SEVERE, "Unable to set type property {0} on node {1}: {2}", new Object[] { nodeType, newNode, t.getMessage() } );
				}
			}
			
			// cache node for this request
			securityContext.store(newNode);
		}
		
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

	public T instantiateDummy(final String nodeType) throws FrameworkException {

		Class<T> nodeClass = Services.getService(ModuleService.class).getNodeEntityClass(nodeType);
		T newNode          = null;

		if (nodeClass != null) {

			try {

				Constructor<T> constructor = constructors.get(nodeClass);

				if (constructor == null) {

					constructor = nodeClass.getConstructor();

					constructors.put(nodeClass, constructor);

				}

				// newNode = (AbstractNode) nodeClass.newInstance();
				newNode = constructor.newInstance();

			} catch (Throwable t) {

				newNode = null;

			}

		}

		return newNode;

	}
	
	private Result resultFromSpatialRecords(final SpatialRecordHits spatialRecordHits) throws FrameworkException {

		final int pageSize                    = factoryProfile.getPageSize();
		final SecurityContext securityContext = factoryProfile.getSecurityContext();
		final boolean includeDeletedAndHidden = factoryProfile.includeDeletedAndHidden();
		final boolean publicOnly              = factoryProfile.publicOnly();
		List<T> nodes                         = new LinkedList<T>();
		int size                              = spatialRecordHits.size();
		int position                          = 0;
		int count                             = 0;
		int offset                            = 0;

		for (Node node : spatialRecordHits) {

			Node realNode = node;
			if (realNode != null) {

				// FIXME: type cast is not good here...
				T n = instantiate(realNode);
				
				nodes.add(n);

				// Check is done in createNodeWithType already, so we don't have to do it again
				if (n != null) {    // && isReadable(securityContext, n, includeDeletedAndHidden, publicOnly)) {

					List<AbstractNode> nodesAt = getNodesAt(n);

					size += nodesAt.size();

					for (AbstractNode nodeAt : nodesAt) {

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

		return new Result(nodes, size, true, false);

	}

	/**
	 * Return all nodes which are connected by an incoming IS_AT relationships
	 *
	 * @param locationNode
	 * @return
	 */
	protected List<AbstractNode> getNodesAt(final AbstractNode locationNode) {

		List<AbstractNode> nodes = new LinkedList<AbstractNode>();

		for (AbstractRelationship rel : locationNode.getRelationships(RelType.IS_AT, Direction.INCOMING)) {

			AbstractNode startNode = rel.getStartNode();
			
			nodes.add(startNode);
			
			// add more nodes which are "at" this one
			nodes.addAll(getNodesAt(startNode));
		}

		return nodes;

	}
}
