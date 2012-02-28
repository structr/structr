/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr;

import org.apache.commons.lang.StringUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.structr.common.CaseHelper;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.StandaloneTestHelper;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.*;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.net.URL;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author axel
 */
public class Slurp {

	private static final Logger logger = Logger.getLogger(Slurp.class.getName());

	//~--- constructors ---------------------------------------------------

	public Slurp() {}

	//~--- methods --------------------------------------------------------

	public static void main(String[] args) throws Exception {

		StandaloneTestHelper.prepareStandaloneTest("/home/axel/NetBeansProjects/structr/structr/structr-ui/db");

		String address      = "http://www.morgner.de/index";
		final Document page = Jsoup.parse(new URL(address), 5000);

		Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				AbstractNode page1 = createNode("Resource", "slurp14");

				createChildNodes(page, page1, page1.getStringProperty(AbstractNode.Key.uuid));

				return null;
			}

		});
		StandaloneTestHelper.finishStandaloneTest();
	}

	private static void createChildNodes(final Node startNode, final AbstractNode parent, final String resourceId) throws FrameworkException {

		List<Node> children = startNode.childNodes();
		int localIndex      = 0;

		for (Node node : children) {

			String tag                = node.nodeName();
			String type               = CaseHelper.toUpperCamelCase(tag);
			String content            = null;
			String id                 = null;
			StringBuilder classString = new StringBuilder();

			if (type.equals("#declaration") || type.equals("#comment") || type.equals("#data")) {

				continue;

			}

			if (node instanceof Element) {

				Element el          = ((Element) node);
				Set<String> classes = el.classNames();

				for (String cls : classes) {

					classString.append(cls).append(" ");

				}

				id = el.id();

			}

			if (type.equals("#text")) {

				type    = "Content";
				tag     = "";
				content = ((TextNode) node).text();

				// Don't add content node for whitespace
				if (StringUtils.isBlank(content)) {

					continue;

				}

			}

			List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

			if (content != null) {

				attrs.add(new NodeAttribute("content", content));

			}

			attrs.add(new NodeAttribute("tag", tag));

			if (StringUtils.isNotBlank(id)) {

				attrs.add(new NodeAttribute(PropertyView.Html + "id", id));

			}

			if (StringUtils.isNotBlank(classString.toString())) {

				attrs.add(new NodeAttribute(PropertyView.Html + "class", classString.toString()));

			}

			for (Attribute nodeAttr : node.attributes()) {

				attrs.add(new NodeAttribute(PropertyView.Html + nodeAttr.getKey(), nodeAttr.getValue()));

			}

			AbstractNode newNode = createNode(type, "New " + type + " " + Math.random(), (NodeAttribute[]) attrs.toArray(new NodeAttribute[attrs.size()]));

			linkNodes(parent, newNode, resourceId, localIndex);
			createChildNodes(node, newNode, resourceId);

			localIndex++;

		}
	}

	private static AbstractNode createNode(String type, String name, NodeAttribute... attributes) throws FrameworkException {

		SecurityContext context   = SecurityContext.getSuperUserInstance();
		Command createNodeCommand = Services.command(context, CreateNodeCommand.class);
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put(AbstractNode.Key.type.name(), type);
		attrs.put(AbstractNode.Key.name.name(), name);

		if (attributes != null) {

			for (NodeAttribute attr : attributes) {

				attrs.put(attr.getKey(), attr.getValue());

			}

		}

		AbstractNode node = (AbstractNode) createNodeCommand.execute(attrs);

		if (node != null) {

			logger.log(Level.INFO, "Created node with name {0}, type {1} and id {2}", new Object[] { node.getName(), node.getType(), node.getId() });

		} else {

			logger.log(Level.WARNING, "Could not create node with name {0} and type {1}", new Object[] { name, type });

		}

		return node;
	}

	private static AbstractRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, String resourceId, int index) throws FrameworkException {

		SecurityContext context  = SecurityContext.getSuperUserInstance();
		Command createRelCommand = Services.command(context, CreateRelationshipCommand.class);
		AbstractRelationship rel = (AbstractRelationship) createRelCommand.execute(startNode, endNode, RelType.CONTAINS);

		rel.setProperty(resourceId, index);

		return rel;
	}
}
