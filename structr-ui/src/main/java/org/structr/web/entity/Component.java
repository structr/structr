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


package org.structr.web.entity;

import org.structr.web.entity.dom.Content;
import org.structr.core.property.Property;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.StringProperty;

import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
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
import org.structr.common.SecurityContext;
import org.structr.core.property.IntProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a component. A component is an assembly of elements
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class Component extends DOMElement {

	private static final int MAX_DEPTH                          = 10;
	public static final String REQUEST_CONTAINS_UUID_IDENTIFIER = "request_contains_uuids";
	private static final Logger logger                          = Logger.getLogger(Component.class.getName());

	public static final Property<Integer> position   = new IntProperty("position");
	public static final Property<String>  kind       = new StringProperty("kind");
	
	public static final org.structr.common.View uiView = new org.structr.common.View(Component.class, PropertyView.Ui,
		type, name, kind
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(Component.class, PropertyView.Public,
		type, name, kind
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
		// collectProperties(this, getProperty(AbstractNode.uuid), 0, null);
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
	/*
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
	*/
	
	private void collectChildren(List<Component> children, DOMNode startNode, int depth, AbstractRelationship ref) {

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
		for (AbstractRelationship rel : startNode.treeGetChildRelationships()) {

			DOMNode endNode = (DOMNode)rel.getEndNode();

			if (endNode == null) {

				continue;

			}

			collectChildren(children, endNode, depth + 1, rel);

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

			List<Component> results = new LinkedList<Component>();

			collectChildren(results, this, 0, null);

			return (T)results;

		}

		return super.getProperty(key);
	}

	public Map<String, AbstractNode> getContentNodes() {
		return contentNodes;
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

	@Override
	public short getNodeType() {
		return ELEMENT_NODE;
	}
	
	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {
		super.render(securityContext, renderContext, depth - 1);
	}
}
