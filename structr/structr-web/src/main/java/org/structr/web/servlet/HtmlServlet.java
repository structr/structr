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
import org.structr.web.common.RelType;
import org.structr.web.entity.Resource;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

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
import org.structr.core.node.StructrNodeFactory;
import org.structr.core.node.search.Search;

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
	private boolean edit;

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

		SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		edit = false;

		if (request.getParameter("create") != null) {

			createTestStructure();
			response.setStatus(HttpServletResponse.SC_CREATED);

			return;

		}

		if (request.getParameter("editor") != null) {

			createEditorStructure();
			response.setStatus(HttpServletResponse.SC_CREATED);

			return;

		}

		if (request.getParameter("edit") != null) {

			edit = true;

		}

		try {

			request.setCharacterEncoding("UTF-8");

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double start                = System.nanoTime();

			// 1: find entry point (Resource)
			Resource resource = null;
			String path       = request.getPathInfo();

			logger.log(Level.INFO, "Path info {0}", path);

			String resourceName = path.substring(path.lastIndexOf("/") + 1);

			if (resourceName.length() > 0) {

				logger.log(Level.INFO, "File name {0}", resourceName);

				List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

				searchAttrs.add(Search.andExactName(resourceName));
				searchAttrs.add(Search.andExactType(Resource.class.getSimpleName()));

				List<AbstractNode> results = (List<AbstractNode>) Services.command(SecurityContext.getSuperUserInstance(),
								     SearchNodeCommand.class).execute(null, false, false, searchAttrs);

				logger.log(Level.INFO, "{0} results", results.size());

				if (!results.isEmpty()) {

					resource = (Resource) results.get(0);

				}

			}

			if (resource != null) {

				// 2: do a traversal and collect content
				String content = getContent(securityContext, resource);
				double end     = System.nanoTime();

				logger.log(Level.INFO, "Content collected in {0} seconds", decimalFormat.format((end - start) / 1000000000.0));

				String contentType = resource.getContentType();

				if (contentType != null) {

					response.setContentType(contentType);

				} else {

					// Default
					response.setContentType("text/html; charset=utf-8");
				}

				// 3: output content
				response.getWriter().append(content);
				response.getWriter().flush();
				response.getWriter().close();
				response.setStatus(HttpServletResponse.SC_OK);
			} else {

				response.setStatus(HttpServletResponse.SC_NOT_FOUND);

			}

		} catch (Throwable t) {
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

				// list.js resource
				AbstractNode listJs        = createNode("Resource", "list.js");
				AbstractNode listJsContent = createNode("Content", "list", new NodeAttribute("content", readFile("/ge/js/list.js")));
				linkNodes(listJs, listJsContent, listJs.getIdString(), 0);

				// page resource
				AbstractNode doc       = createNode("Element", "doc", new NodeAttribute("tag", "html"));
				AbstractNode head      = createNode("Element", "header", new NodeAttribute("tag", "head"));
				AbstractNode script    = createNode("Content", "script", new NodeAttribute("content", "<script src=\"list.js\" language=\"JavaScript\" type=\"text/javascript\"></script>"));
				AbstractNode body      = createNode("Element", "body", new NodeAttribute("tag", "body"), new NodeAttribute("onload", "start()"));
				AbstractNode article1  = createNode("Element", "article1", new NodeAttribute("tag", "div"));
				AbstractNode article2  = createNode("Element", "article2", new NodeAttribute("tag", "div"));
				AbstractNode foo       = createNode("Content", "content1", new NodeAttribute("content", "Dies ist Seite 1"), new NodeAttribute("tag", "h1"));
				AbstractNode bar       = createNode("Content", "content2", new NodeAttribute("content", "Dies ist Seite 2"), new NodeAttribute("tag", "h1"));
				AbstractNode log       = createNode("Content", "log", new NodeAttribute("tag", "div"));

				// content
				AbstractNode foo2      = createNode("Content", "content3");
				
  				String uuid = foo2.getStringProperty("uuid");
				StringBuilder content = new StringBuilder();

				content.append("<input type=\"hidden\" name=\"token\" id=\"token\" />");
				content.append("<script language=\"JavaScript\" type=\"text/javascript\">\n");
				content.append("function load").append(uuid).append("() {\n");
				content.append("loadList(\"").append(uuid).append("\", \"User\", function(parent, element) {\n");
				content.append("parent.innerHTML += (\"<div>\" + element.realName + \"</div>\");\n");
				content.append("});\n");
				content.append("}\n");
				content.append("window.setTimeout(\"load").append(uuid).append("()\", 500);\n");
				content.append("</script>\n");
				
				foo2.setProperty("content", content.toString());
				foo2.setProperty("tag", "div");


				String idOfPage1       = page1.getIdString();
				String idOfPage2       = page2.getIdString();

				// page 1
				linkNodes(page1, doc, idOfPage1, 0);
				linkNodes(doc, head, idOfPage1, 0);
				linkNodes(doc, body, idOfPage1, 1);
				linkNodes(head, script, idOfPage1, 0);
				linkNodes(body, article1, idOfPage1, 1);
				linkNodes(article1, foo, idOfPage1, 0);
				linkNodes(article1, foo2, idOfPage1, 1);
				linkNodes(body, log, idOfPage1, 2);

				// page 2
				linkNodes(page2, doc, idOfPage2, 0);
				linkNodes(doc, head, idOfPage2, 0);
				linkNodes(doc, body, idOfPage2, 1);
				linkNodes(head, script, idOfPage2, 0);
				linkNodes(body, article2, idOfPage2, 1);
				linkNodes(article2, bar, idOfPage2, 0);
				linkNodes(body, log, idOfPage2, 2);

				return null;
			}

		});
	}

	private void createEditorStructure() {

		Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				logger.log(Level.INFO, "Creating test structure..");

				AbstractNode geLibJs      = createNode("Resource", "ge_lib.js");
				AbstractNode geLibContent = createNode("Content", "ge_lib_content", new NodeAttribute("content", readFile("/ge/js/ge_lib.js")));

				linkNodes(geLibJs, geLibContent, geLibJs.getIdString(), 0);

				AbstractNode geObjJs      = createNode("Resource", "ge_obj.js");
				AbstractNode geObjContent = createNode("Content", "ge_obj_content", new NodeAttribute("content", readFile("/ge/js/ge_obj.js")));

				linkNodes(geObjJs, geObjContent, geObjJs.getIdString(), 0);

				AbstractNode graphEditorCss        = createNode("Resource", "ge.css");
				AbstractNode graphEditorCssContent = createNode("Content", "graph_editor_css_content",
									     new NodeAttribute("content", readFile("/ge/css/ge.css")));

				linkNodes(graphEditorCss, graphEditorCssContent, graphEditorCss.getIdString(), 0);

				AbstractNode graphEditorHtml        = createNode("Resource", "ge.html");
				AbstractNode graphEditorHtmlContent = createNode("Content", "graph_editor_html_content",
									      new NodeAttribute("content", readFile("/ge/ge.html")));

				linkNodes(graphEditorHtml, graphEditorHtmlContent, graphEditorHtml.getIdString(), 0);

				AbstractNode graphEditorJs        = createNode("Resource", "ge.js");
				AbstractNode graphEditorJsContent = createNode("Content", "graph_editor_js_content",
									    new NodeAttribute("content", readFile("/ge/js/ge.js")));

				linkNodes(graphEditorJs, graphEditorJsContent, graphEditorJs.getIdString(), 0);

				AbstractNode jqueryMousewheelMinJs        = createNode("Resource", "jquery.mousewheel.min.js");
				AbstractNode jqueryMousewheelMinJsContent = createNode("Content", "jquery_mousewheel_min_js_content",
										    new NodeAttribute("content", readFile("/ge/js/jquery-mousewheel.min.js")));

				linkNodes(jqueryMousewheelMinJs, jqueryMousewheelMinJsContent, jqueryMousewheelMinJs.getIdString(), 0);

				return null;
			}

		});
	}

	private String readFile(String path) {

		StringBuilder content = new StringBuilder();

		try {

			System.out.println(new File(".").getAbsolutePath());

//			BufferedReader reader = new BufferedReader(new FileReader("/home/axel/NetBeansProjects/structr/structr/structr-web/src/main/resources"
//							+ path));

			BufferedReader reader = new BufferedReader(new FileReader(getServletContext().getRealPath(path)));



//                      BufferedReader reader = new BufferedReader(new InputStreamReader(getServletContext().getResourceAsStream(path)));
			String line = null;

			do {

				line = reader.readLine();

				if (line != null) {

					content.append(line);
					content.append("\n");

				}

			} while (line != null);

		} catch (Throwable t) {
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

		AbstractNode node = (AbstractNode) createNodeCommand.execute(attrs);

		logger.log(Level.INFO, "Created node with name {0} and id {1}", new Object[] { node.getName(), node.getId() });

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

	private String getContent(final SecurityContext securityContext, final Resource resource) {

		TraversalDescription localDesc = desc.expand(new ResourceExpander(resource.getIdString()));
		final StructrNodeFactory factory = new StructrNodeFactory(securityContext);
		final ContentTreeNode root = new ContentTreeNode(null, null);

		localDesc = localDesc.evaluator(new Evaluator() {

			ContentTreeNode localRoot = root;

			@Override
			public Evaluation evaluate(Path path) {

				Node node = path.endNode();

				try {

					if (node.hasProperty(AbstractNode.Key.type.name())) {

						String type = (String) node.getProperty(AbstractNode.Key.type.name());
						
						ContentTreeNode newTreeNode = new ContentTreeNode(localRoot, factory.createNode(securityContext, node, type));
						localRoot.addChild(newTreeNode);

						Evaluation evaluation;

						if ("Content".equals(type)) {

							evaluation = Evaluation.EXCLUDE_AND_PRUNE;

							// step one up
							localRoot = localRoot.getParent();

						} else {

							evaluation = Evaluation.EXCLUDE_AND_CONTINUE;

							// step one down
							localRoot = newTreeNode;

						}

						return evaluation;

					}

				} catch (Throwable t) {

					// fail fast, no check
					logger.log(Level.SEVERE, "While evaluating path " + path, t);
				}

				return Evaluation.EXCLUDE_AND_CONTINUE;
			}

		});

		// do traversal to retrieve paths
		for (Node node : localDesc.traverse(resource.getNode()).nodes()) {}

		StringBuilder buffer = new StringBuilder(10000);	// FIXME: use sensible initial size..
		printNodes(buffer, root, 0);

		return buffer.toString();
	}

	private void printNodes(StringBuilder buffer, ContentTreeNode root, int depth) {

		AbstractNode node = root.getData();
		String content = null;
		String tag = null;

		if(node != null) {

			content = (String)node.getProperty("content");
			tag = (String)node.getProperty("tag");

			if(tag != null) {

				String onload = (String)node.getProperty("onload");
				String id = (String)node.getProperty("uuid");

				buffer.append("<").append(tag);

				if(id != null)		buffer.append(" id='").append(id).append("'");
				if(onload != null)	buffer.append(" onload='").append(onload).append("'");

				buffer.append(">");
			}

			if(content != null) {
				buffer.append(content);
			}
		}

		// render children
		for(ContentTreeNode subNode : root.getChildren()) {
			printNodes(buffer, subNode, depth+1);
		}

		// render end tag
		if(tag != null) {
			buffer.append("</").append(tag).append(">");
		}
	}

	//~--- inner classes --------------------------------------------------

	private static class ContentTreeNode {

		private List<ContentTreeNode> children = new LinkedList<ContentTreeNode>();
		private ContentTreeNode parent = null;
		private AbstractNode data = null;

		public ContentTreeNode(ContentTreeNode parent, AbstractNode data) {
			this.parent = parent;
			this.data = data;
		}

		public AbstractNode getData() {
			return data;
		}

		public ContentTreeNode getParent() {
			return parent;
		}

		public void addChild(ContentTreeNode treeNode) {
			children.add(treeNode);
		}

		public List<ContentTreeNode> getChildren() {
			return children;
		}

	}

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

					Integer position = null;

					if (rel.hasProperty(resourceId)) {

						Object prop = rel.getProperty(resourceId);

						if (prop instanceof Integer) {

							position = (Integer) prop;

						} else if (prop instanceof String) {

							position = Integer.parseInt((String) prop);

						} else {

							throw new java.lang.IllegalArgumentException("Expected Integer or String");

						}

						sortedRelationshipMap.put(position, rel);

					}

				} catch (Throwable t) {

					// fail fast, no check
					logger.log(Level.SEVERE, "While reading property " + resourceId, t);
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
