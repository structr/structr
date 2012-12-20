/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity;

import org.structr.core.property.Property;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.property.LongProperty;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.NodeService;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a component. A component is an assembly of elements
 *
 * @author axel
 */
public class Component extends AbstractNode implements Element {

	private static final int MAX_DEPTH                          = 10;
	public static final String REQUEST_CONTAINS_UUID_IDENTIFIER = "request_contains_uuids";
	private static final Logger logger                          = Logger.getLogger(Component.class.getName());

	public static final Property<String> componentId = new StringProperty("componentId");
	public static final Property<String> pageId      = new StringProperty("pageId");
	public static final Property<String> kind        = new StringProperty("kind");
	
	public static final org.structr.common.View uiView = new org.structr.common.View(Component.class, PropertyView.Ui,
		type, name, kind, paths
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(Component.class, PropertyView.Public,
		type, name, kind, paths
	);
	
	
	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerSearchablePropertySet(Component.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Component.class, NodeService.NodeIndex.keyword.name(),  uiView.properties());

	}

	//~--- fields ---------------------------------------------------------

	private Map<String, AbstractNode> contentNodes = new WeakHashMap<String, AbstractNode>();
	private Set<String> subTypes                   = new LinkedHashSet<String>();

	//~--- methods --------------------------------------------------------

	@Override
	public void onNodeInstantiation() {
		collectProperties(this, getProperty(AbstractNode.uuid), 0, null);
	}

	@Override
	public void onNodeDeletion() {

		try {

			DeleteNodeCommand deleteCommand = Services.command(securityContext, DeleteNodeCommand.class);
			boolean cascade                 = true;

			for (AbstractNode contentNode : contentNodes.values()) {

				deleteCommand.execute(contentNode, cascade);

			}

			// delete linked components
//                      for(AbstractRelationship rel : getRelationships(RelType.DATA, Direction.INCOMING)) {
//                              deleteCommand.execute(rel.getStartNode());
//                      }

		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Exception while deleting nested Components: {0}", t.getMessage());
		}
	}

	// ----- private methods ----
	private void collectProperties(AbstractNode startNode, String componentId, int depth, AbstractRelationship ref) {

		if (depth > MAX_DEPTH) {

			return;

		}

		if (ref != null) {

			if (componentId.equals(ref.getProperty(Component.componentId))) {

				String dataKey = startNode.getProperty(Content.dataKey);

				if (dataKey != null) {

					contentNodes.put(dataKey, startNode);

					return;

				}

			}

		}

		// collection of properties must not depend on page
		for (AbstractRelationship rel : getChildRelationships(null, startNode, null, componentId)) {

			AbstractNode endNode = rel.getEndNode();

			if (endNode == null) {

				continue;

			}

			if (endNode instanceof Component) {

				String subType = endNode.getProperty(Component.kind);

				if (subType != null) {

					subTypes.add(subType);

				}

			} else {

				collectProperties(endNode, componentId, depth + 1, rel);

			}

		}
	}

	private void collectChildren(List<Component> children, AbstractNode startNode, String componentId, int depth, AbstractRelationship ref) {

		if (depth > MAX_DEPTH) {

			return;

		}

		if (ref != null) {

			if (startNode instanceof Component) {

				children.add((Component) startNode);

				return;

			}

		}

		// collection of properties must not depend on page
		for (AbstractRelationship rel : getChildRelationships(null, startNode, null, componentId)) {

			AbstractNode endNode = rel.getEndNode();

			if (endNode == null) {

				continue;

			}

			collectChildren(children, endNode, componentId, depth + 1, rel);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		Set<PropertyKey> augmentedPropertyKeys = new LinkedHashSet<PropertyKey>();

		for (PropertyKey key : super.getPropertyKeys(propertyView)) {

			augmentedPropertyKeys.add(key);

		}

		// FIXME: use getPropertyKeyForName() of specific node type
		for (String key : contentNodes.keySet()) {
			augmentedPropertyKeys.add(new GenericProperty(key));
		}

		for (String subType : subTypes) {

			augmentedPropertyKeys.add(new GenericProperty(subType.toLowerCase().concat("s")));

		}

		return augmentedPropertyKeys;
	}

	@Override
	public <T> T getProperty(PropertyKey<T> key) {

		// try local properties first
		if (contentNodes.containsKey(key.dbName())) {

			AbstractNode node = contentNodes.get(key.dbName());

			if ((node != null) && (node != this)) {

				return (T)node.getProperty(Content.content);

			}

		} else if (subTypes.contains(EntityContext.normalizeEntityName(key.dbName()))) {

			String componentId      = getProperty(AbstractNode.uuid);
			List<Component> results = new LinkedList<Component>();

			collectChildren(results, this, componentId, 0, null);

			return (T)results;

		}

		return super.getProperty(key);
	}

	public Map<String, AbstractNode> getContentNodes() {
		return contentNodes;
	}
	
	public String getComponentId() {

		for (AbstractRelationship in : getRelationships(RelType.CONTAINS, Direction.INCOMING)) {

			String componentId = in.getProperty(Component.componentId);

			if (componentId != null) {

				return componentId;

			}

		}

		return null;
	}

//      public String getPageId() {
//
//              for (AbstractRelationship in : getRelationships(RelType.CONTAINS, Direction.INCOMING)) {
//
//                      String pageId = in.getProperty(Key.pageId.name());
//
//                      if (pageId != null) {
//
//                              return pageId;
//
//                      }
//
//              }
//
//              return null;
//      }
	public static List<AbstractRelationship> getChildRelationships(final HttpServletRequest request, final AbstractNode node, final String pageId, final String componentId) {

		List<AbstractRelationship> rels = new LinkedList<AbstractRelationship>();

		for (AbstractRelationship abstractRelationship : node.getOutgoingRelationships(RelType.CONTAINS)) {

			Relationship rel = abstractRelationship.getRelationship();

			if ((pageId == null) || ((pageId != null) && rel.hasProperty(pageId)) || rel.hasProperty("*")) {

				AbstractNode endNode = abstractRelationship.getEndNode();

				if (endNode == null || (endNode instanceof Component && !isVisible(request, endNode, abstractRelationship, componentId))) {

					continue;

				}

				if ((componentId != null) && ((endNode instanceof Content) || (endNode instanceof Component))) {
					
					// Add content nodes if they don't have the data-key property set
					if (endNode instanceof Content && endNode.getProperty(Content.dataKey) == null) {
						
						rels.add(abstractRelationship);
						
					// Add content or component nodes if rel's componentId attribute matches
					} else if (componentId.equals(abstractRelationship.getProperty(Component.componentId))) {

						rels.add(abstractRelationship);

					}
						
				} else {

					rels.add(abstractRelationship);

				}

			}

		}

		if (pageId != null) {

			Collections.sort(rels, new Comparator<AbstractRelationship>() {

				@Override
				public int compare(AbstractRelationship o1, AbstractRelationship o2) {

					Long pos1 = getPosition(o1, pageId);
					Long pos2 = getPosition(o2, pageId);

					return pos1.compareTo(pos2);
				}

			});

		}

		return rels;
	}

	public static long getPosition(final AbstractRelationship relationship, final String pageId) {

//              final Relationship rel = relationship.getRelationship();
		long position = 0;

		try {

//                      Map<Integer, Relationship> sortedRelationshipMap = new TreeMap<Integer, Relationship>();
			PropertyKey<Long> pageIdProperty = new LongProperty(pageId);
			PropertyKey wildcardProperty     = new LongProperty("*");
			Object prop = null;
			final String key;

			// "*" is a wildcard for "matches any page id"
			// TOOD: use pattern matching here?
			if (relationship.getProperty(wildcardProperty) != null) {

				prop = relationship.getProperty(wildcardProperty);
				key  = "*";

			} else if (relationship.getProperty(pageIdProperty) != null) {

				prop = relationship.getLongProperty(pageIdProperty);
				key  = pageId;

			} else {

				key = null;

			}

			if ((key != null) && (prop != null)) {

				if (prop instanceof Long) {

					position = (Long) prop;

				} else if (prop instanceof Integer) {

					position = ((Integer) prop).longValue();

				} else if (prop instanceof String) {

					position = Long.parseLong((String) prop);

				} else {

					throw new java.lang.IllegalArgumentException("Expected Long, Integer or String");

				}

//
//                              Integer originalPos = position;
//
//                              // find free slot
//                              while (sortedRelationshipMap.containsKey(position)) {
//
//                                      position++;
//
//                              }
//
//                              sortedRelationshipMap.put(position, rel);
//
//                              if (originalPos != position) {
//
//                                      final Integer newPos = position;
//
//                                      Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {
//
//                                              @Override
//                                              public Object execute() throws FrameworkException {
//
//                                                      rel.setProperty(key, newPos);
//
//                                                      return null;
//                                              }
//
//                                      });
//
//                              }

			}
		} catch (Throwable t) {

			// fail fast, no check
			logger.log(Level.SEVERE, "While reading property " + pageId, t);
		}

		return position;
	}

	private static boolean hasAttribute(HttpServletRequest request, String key) {
		return (key != null) && (request.getAttribute(key) != null);
	}

	// ----- public static methods -----
	public static boolean isVisible(HttpServletRequest request, AbstractNode node, AbstractRelationship incomingRelationship, String parentComponentId) {

		if (request == null) {

			return true;

		}

		// check if component is in "list" mode
		if (node instanceof Component) {

			Boolean requestContainsUuidsValue = (Boolean) request.getAttribute(REQUEST_CONTAINS_UUID_IDENTIFIER);
			boolean requestContainsUuids      = false;

			if (requestContainsUuidsValue != null) {

				requestContainsUuids = requestContainsUuidsValue.booleanValue();

			}

			String componentId = node.getProperty(AbstractNode.uuid);

			// new default behaviour: make all components visible
			// only filter if uuids are present in the request URI
			// and we are examining a top-level component (children
			// of filtered components are not reached anyway)
			if (requestContainsUuids) {

				if (hasAttribute(request, componentId) || (parentComponentId != null)) {

					return true;

				}

				return false;

			} else {

				return true;

			}

		}

		// we can return false here by default, as we're only examining nodes of type Component
		return false;
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public <T> void setProperty(PropertyKey<T> key, T value) throws FrameworkException {

		if (contentNodes.containsKey(key.dbName())) {

			AbstractNode node = contentNodes.get(key.dbName());

			if (node != null) {

				node.setProperty(Content.content, value.toString());

			}

		} else {

			super.setProperty(key, value);

		}
	}
}
