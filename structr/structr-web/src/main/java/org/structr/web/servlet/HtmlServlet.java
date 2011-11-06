/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.web.servlet;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.Predicate;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.IterableFilter;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.web.common.RelType;
import org.structr.web.entity.Content;
import org.structr.web.entity.Resource;

/**
 * A first proof of concept for the new graph concept. This class has two modes,
 * one to create an example structure and one to traverse over the created
 * structure and return the collected content. Use the request parameter "create"
 * to create the test structure, use the request parameter "id" to retrieve the
 * resources, see log during "create" for IDs of the created resources.
 *
 * @author Christian Morgner
 */
public class HtmlServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(HtmlServlet.class.getName());

	private TraversalDescription desc = null;

	@Override
	public void init() {

		// create prototype traversal description
		desc = Traversal.description();
		desc = desc.depthFirst();
		desc = desc.uniqueness(Uniqueness.NODE_GLOBAL);
	}

	@Override
	public void destroy() {
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		if(request.getParameter("create") != null) {

			createTestStructure();

			response.setStatus(HttpServletResponse.SC_CREATED);
			return;
		}

		try {
			request.setCharacterEncoding("UTF-8");

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double start = System.nanoTime();

			// 1: find entry point (Resource)
			long id = Long.parseLong(request.getParameter("id"));
			Resource resource = (Resource)Services.command(SecurityContext.getSuperUserInstance(), FindNodeCommand.class).execute(id);

			// 2: do a traversal and collect content
			String content = getContent(resource);

			double end = System.nanoTime();
			logger.log(Level.INFO, "Content collected in {0} seconds", decimalFormat.format((end - start) / 1000000000.0));

			// 3: output content
			response.getWriter().append(content);
			response.getWriter().flush();
			response.getWriter().close();

			response.setStatus(HttpServletResponse.SC_OK);


		} catch(Throwable t) {

			logger.log(Level.WARNING, "Exception while processing request", t);
		}
	}

	private String getContent(Resource resource) {

		final StringBuilder builder = new StringBuilder();

		TraversalDescription localDesc = desc.expand(new ResourceExpander(resource.getIdString()));
		localDesc = localDesc.evaluator(new Evaluator() {

				@Override
				public Evaluation evaluate(Path path) {

					Node node = path.endNode();

					try {

						String type = (String)node.getProperty(AbstractNode.Key.type.name());

						if("Content".equals(type)) {

							// Content node reached, collect content and stop traversal here
							builder.append(node.getProperty(Content.Key.content.name()));
							return Evaluation.EXCLUDE_AND_PRUNE;

						} else {

							// continue traversal
							return Evaluation.EXCLUDE_AND_CONTINUE;
						}


					} catch(Throwable t) {
						// fail fast, no check
					}

					return Evaluation.EXCLUDE_AND_PRUNE;
				}
			}
		);

		// do traversal to retrieve paths
		for(Path path : localDesc.traverse(resource.getNode())) {}

		return builder.toString();
	}

	private void createTestStructure() {

		Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				logger.log(Level.INFO, "Creating test structure..");

				AbstractNode page1 = createNode("Resource", "page1");
				AbstractNode page2 = createNode("Resource", "page2");

				logger.log(Level.INFO, "Created page1 with id {0}, page2 with id {1}", new Object[] { page1.getId(), page2.getId() } );

				AbstractNode doc = createNode("Element", "doc");
				AbstractNode body = createNode("Element", "body");
				AbstractNode article1 = createNode("Element", "article1");
				AbstractNode article2 = createNode("Element", "article2");

				AbstractNode header = createNode("Content", "header", new NodeAttribute("content", "<html>"));
				AbstractNode startBody = createNode("Content", "start_body", new NodeAttribute("content", "<body>"));
				AbstractNode endBody = createNode("Content", "end_body", new NodeAttribute("content", "</body>"));
				AbstractNode footer = createNode("Content", "footer", new NodeAttribute("content", "</html>"));
				AbstractNode foo = createNode("Content", "content1", new NodeAttribute("content", "<h1>Dies ist Seite 1</h1>"));
				AbstractNode bar = createNode("Content", "content2", new NodeAttribute("content", "<h1>Dies ist Seite 2</h1>"));

				String idOfPage1 = page1.getIdString();
				String idOfPage2 = page2.getIdString();

				// page 1
				linkNodes(page1, doc, idOfPage1, 0);
				linkNodes(doc, header, idOfPage1, 0);
				linkNodes(doc, body, idOfPage1, 1);
				linkNodes(doc, footer, idOfPage1, 2);
				linkNodes(body, startBody, idOfPage1, 0);
				linkNodes(body, article1, idOfPage1, 1);
				linkNodes(body, endBody, idOfPage1, 2);
				linkNodes(article1, foo, idOfPage1, 0);

				// page 2
				linkNodes(page2, doc, idOfPage2, 0);
				linkNodes(doc, header, idOfPage2, 0);
				linkNodes(doc, body, idOfPage2, 1);
				linkNodes(doc, footer, idOfPage2, 2);
				linkNodes(body, startBody, idOfPage2, 0);
				linkNodes(body, article2, idOfPage2, 1);
				linkNodes(body, endBody, idOfPage2, 2);
				linkNodes(article2, bar, idOfPage2, 0);

				return null;
			}
		});
	}

	private AbstractNode createNode(String type, String name, NodeAttribute... attributes) {

		SecurityContext context = SecurityContext.getSuperUserInstance();
		Command createNodeCommand = Services.command(context, CreateNodeCommand.class);

		Map<String, Object> attrs = new HashMap<String, Object>();
		attrs.put(AbstractNode.Key.type.name(), type);
		attrs.put(AbstractNode.Key.name.name(), name);
		for(NodeAttribute attr : attributes) {
			attrs.put(attr.getKey(), attr.getValue());
		}

		return (AbstractNode)createNodeCommand.execute(attrs);
	}

	private StructrRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, String resourceId, int index) {

		SecurityContext context = SecurityContext.getSuperUserInstance();
		Command createRelCommand = Services.command(context, CreateRelationshipCommand.class);

		StructrRelationship rel = (StructrRelationship)createRelCommand.execute(startNode, endNode, RelType.CONTAINS);
		rel.setProperty(resourceId, index);

		return rel;
	}

	private static class ResourceExpander implements RelationshipExpander {

		private Direction direction = Direction.OUTGOING;
		private String resourceId = null;

		public ResourceExpander(final String resourceId) {
			this.resourceId = resourceId;
		}

		public void setDirection(Direction direction) {
			this.direction = direction;
		}

		@Override
		public Iterable<Relationship> expand(Node node) {

			/**
			 * Expand outgoing relationships of type CONTAINS and check for
			 * resourceId property. If property exists, let TreeMap do the
			 * sorting for us and return sorted values from map.
			 */
			Map<Integer, Relationship> sortedRelationshipMap = new TreeMap<Integer, Relationship>();
			for(Relationship rel : node.getRelationships(RelType.CONTAINS, direction)) {

				try {

					Integer position = (Integer)rel.getProperty(resourceId);
					sortedRelationshipMap.put(position, rel);

				} catch(Throwable t) {
					// fail fast, no check
				}
			}

			return sortedRelationshipMap.values();
		}

		@Override
		public RelationshipExpander reversed() {
			
			ResourceExpander reversed = new ResourceExpander(resourceId);
			reversed.setDirection(Direction.INCOMING);

			return reversed;
		}
	}
}
