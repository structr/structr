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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;
import org.structr.web.common.RelType;
import org.structr.web.entity.Content;
import org.structr.web.entity.Resource;

//~--- JDK imports ------------------------------------------------------------

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//~--- classes ----------------------------------------------------------------

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

	//~--- fields ---------------------------------------------------------

	private TraversalDescription desc = null;

	//~--- methods --------------------------------------------------------

	@Override
	public void init() {

		// create prototype traversal description
		desc = Traversal.description();
		desc = desc.depthFirst();
		desc = desc.uniqueness(Uniqueness.NODE_GLOBAL);
	}

	@Override
	public void destroy() {}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		if (request.getParameter("create") != null) {

			createTestStructure();
			response.setStatus(HttpServletResponse.SC_CREATED);

			return;
		}

		if(request.getParameter("editor") != null) {

			createEditorStructure();

			response.setStatus(HttpServletResponse.SC_CREATED);
			return;
		}

		try {

			request.setCharacterEncoding("UTF-8");

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double start = System.nanoTime();

			// 1: find entry point (Resource)
			Resource resource = null;

			String path = request.getPathInfo();

			logger.log(Level.INFO, "Path info {0}", path);

			String fileName = path.substring(path.lastIndexOf("/") + 1);
			if(fileName.length() > 0) {

				logger.log(Level.INFO, "File name {0}", fileName);

				List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();
				searchAttrs.add(new TextualSearchAttribute("name", fileName, SearchOperator.AND));

				List<AbstractNode> results = (List<AbstractNode>)Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, searchAttrs);

				logger.log(Level.INFO, "{0} results", results.size());

				if(!results.isEmpty()) {
					resource = (Resource)results.get(0);
				}
			}

			if(resource != null) {

				// 2: do a traversal and collect content
				String content = getContent(resource);

				double end = System.nanoTime();
				logger.log(Level.INFO, "Content collected in {0} seconds", decimalFormat.format((end - start) / 1000000000.0));

				// 3: output content
				response.getWriter().append(content);
				response.getWriter().flush();
				response.getWriter().close();

				response.setStatus(HttpServletResponse.SC_OK);

			} else {

				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}


		} catch(Throwable t) {

			logger.log(Level.WARNING, "Exception while processing request", t);
		}
	}

	private void createTestStructure() {

		Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				logger.log(Level.INFO, "Creating test structure..");

				AbstractNode page1 = createNode("Resource", "page1");
				AbstractNode page2 = createNode("Resource", "page2");

				logger.log(Level.INFO, "Created page1 with id {0}, page2 with id {1}", new Object[] { page1.getId(), page2.getId() });

				AbstractNode doc       = createNode("Element", "doc");
				AbstractNode body      = createNode("Element", "body");
				AbstractNode article1  = createNode("Element", "article1");
				AbstractNode article2  = createNode("Element", "article2");
				AbstractNode header    = createNode("Content", "header", new NodeAttribute("content", "<html>"));
				AbstractNode startBody = createNode("Content", "start_body", new NodeAttribute("content", "<body>"));
				AbstractNode endBody   = createNode("Content", "end_body", new NodeAttribute("content", "</body>"));
				AbstractNode footer    = createNode("Content", "footer", new NodeAttribute("content", "</html>"));
				AbstractNode foo       = createNode("Content", "content1", new NodeAttribute("content", "<h1>Dies ist Seite 1</h1>"));
				AbstractNode bar       = createNode("Content", "content2", new NodeAttribute("content", "<h1>Dies ist Seite 2</h1>"));
				String idOfPage1       = page1.getIdString();
				String idOfPage2       = page2.getIdString();

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

	private void createEditorStructure() {

		Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				logger.log(Level.INFO, "Creating test structure..");

				AbstractNode geLibJs = createNode("Resource", "ge_lib.js");
				AbstractNode geLibContent = createNode("Content", "ge_lib_content", new NodeAttribute("content", readFile("/ge/js/ge_lib.js")));
				linkNodes(geLibJs, geLibContent, geLibJs.getIdString(), 0);

				AbstractNode geObjJs = createNode("Resource", "ge_obj.js");
				AbstractNode geObjContent = createNode("Content", "ge_obj_content", new NodeAttribute("content", readFile("/ge/js/ge_obj.js")));
				linkNodes(geObjJs, geObjContent, geObjJs.getIdString(), 0);

				AbstractNode graphEditorCss = createNode("Resource", "ge.css");
				AbstractNode graphEditorCssContent = createNode("Content", "graph_editor_css_content", new NodeAttribute("content", readFile("/ge/css/ge.css")));
				linkNodes(graphEditorCss, graphEditorCssContent, graphEditorCss.getIdString(), 0);

				AbstractNode graphEditorHtml = createNode("Resource", "ge.html");
				AbstractNode graphEditorHtmlContent = createNode("Content", "graph_editor_html_content", new NodeAttribute("content", readFile("/ge/ge.html")));
				linkNodes(graphEditorHtml, graphEditorHtmlContent, graphEditorHtml.getIdString(), 0);

				AbstractNode graphEditorJs = createNode("Resource", "ge.js");
				AbstractNode graphEditorJsContent = createNode("Content", "graph_editor_js_content", new NodeAttribute("content", readFile("/ge/js/ge.js")));
				linkNodes(graphEditorJs, graphEditorJsContent, graphEditorJs.getIdString(), 0);

				AbstractNode jqueryMousewheelMinJs = createNode("Resource", "jquery.mousewheel.min.js");
				AbstractNode jqueryMousewheelMinJsContent = createNode("Content", "jquery_mousewheel_min_js_content", new NodeAttribute("content", readFile("/ge/js/jquery-mousewheel.min.js")));
				linkNodes(jqueryMousewheelMinJs, jqueryMousewheelMinJsContent, jqueryMousewheelMinJs.getIdString(), 0);

				return null;
			}
		});
	}

	private String readFile(String path) {

		StringBuilder content = new StringBuilder();

		try {
			System.out.println(new File(".").getAbsolutePath());
			BufferedReader reader = new BufferedReader(new FileReader("/home/axel/NetBeansProjects/structr/structr/structr-web/src/main/resources" + path));
//			BufferedReader reader = new BufferedReader(new InputStreamReader(getServletContext().getResourceAsStream(path)));
			String line = null;
			do {
				line = reader.readLine();
				if(line != null) {
					content.append(line);
					content.append("\n");
				}
			} while(line != null);

		} catch(Throwable t) {
			t.printStackTrace();
		}

		return content.toString();
	}

	private AbstractNode createNode(String type, String name, NodeAttribute... attributes) {

		SecurityContext context   = SecurityContext.getSuperUserInstance();
		Command createNodeCommand = Services.command(context, CreateNodeCommand.class);
		Map<String, Object> attrs = new HashMap<String, Object>();

		attrs.put(AbstractNode.Key.type.name(), type);
		attrs.put(AbstractNode.Key.name.name(), name);

		for (NodeAttribute attr : attributes) {
			attrs.put(attr.getKey(), attr.getValue());
		}

		AbstractNode node = (AbstractNode)createNodeCommand.execute(attrs);

		logger.log(Level.INFO, "Created node with name {0} and id {1}", new Object[] { node.getName(), node.getId() } );

		return node;
	}

	private StructrRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, String resourceId, int index) {

		SecurityContext context  = SecurityContext.getSuperUserInstance();
		Command createRelCommand = Services.command(context, CreateRelationshipCommand.class);
		StructrRelationship rel  = (StructrRelationship) createRelCommand.execute(startNode, endNode, RelType.CONTAINS);

		rel.setProperty(resourceId, index);

		return rel;
	}

	//~--- get methods ----------------------------------------------------

	private String getContent(Resource resource) {

		final StringBuilder builder    = new StringBuilder();
		TraversalDescription localDesc = desc.expand(new ResourceExpander(resource.getIdString()));

		localDesc = localDesc.evaluator(new Evaluator() {

			@Override
			public Evaluation evaluate(Path path) {

				Node node = path.endNode();

				try {

					String type = (String) node.getProperty(AbstractNode.Key.type.name());

					if ("Content".equals(type)) {

						// Content node reached, collect content and stop traversal here
						builder.append(node.getProperty(Content.Key.content.name()));

						return Evaluation.EXCLUDE_AND_PRUNE;
					} else {

						// continue traversal
						return Evaluation.EXCLUDE_AND_CONTINUE;
					}

				} catch (Throwable t) {

					// fail fast, no check
				}

				return Evaluation.EXCLUDE_AND_PRUNE;
			}

		});

		// do traversal to retrieve paths
		for (Path path : localDesc.traverse(resource.getNode())) {}

		return builder.toString();
	}

	//~--- inner classes --------------------------------------------------

	private static class ResourceExpander implements RelationshipExpander {

		private Direction direction = Direction.OUTGOING;
		private String resourceId   = null;

		//~--- constructors -------------------------------------------

		public ResourceExpander(final String resourceId) {
			this.resourceId = resourceId;
		}

		//~--- methods ------------------------------------------------

		@Override
		public Iterable<Relationship> expand(Node node) {

			/**
			 * Expand outgoing relationships of type CONTAINS and check for
			 * resourceId property. If property exists, let TreeMap do the
			 * sorting for us and return sorted values from map.
			 */
			Map<Integer, Relationship> sortedRelationshipMap = new TreeMap<Integer, Relationship>();

			for (Relationship rel : node.getRelationships(RelType.CONTAINS, direction)) {

				try {

					Integer position = (Integer) rel.getProperty(resourceId);

					sortedRelationshipMap.put(position, rel);

				} catch (Throwable t) {

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

		//~--- set methods --------------------------------------------

		public void setDirection(Direction direction) {
			this.direction = direction;
		}
	}
}
