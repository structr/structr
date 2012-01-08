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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import org.structr.common.RelType;
import org.structr.common.ResourceExpander;
import org.structr.common.SecurityContext;
import org.structr.common.TreeNode;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Image;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrNodeFactory;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.structr.core.node.search.SearchAttributeGroup;
import org.structr.core.node.search.SearchOperator;
import org.structr.web.entity.Content;
import org.structr.web.entity.Resource;

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
		desc = Traversal.description().depthFirst().uniqueness(Uniqueness.NODE_GLOBAL);
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

			// 1: find entry point (Resource, File or Image)
			Resource resource                 = null;
			org.structr.core.entity.File file = null;
			Image image                       = null;
			String path                       = request.getPathInfo();

			logger.log(Level.INFO, "Path info {0}", path);

			String name = path.substring(path.lastIndexOf("/") + 1);

			if (name.length() > 0) {

				logger.log(Level.INFO, "File name {0}", name);

				List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

				searchAttrs.add(Search.andExactName(name));
				SearchAttributeGroup group = new SearchAttributeGroup(SearchOperator.AND);
				group.add(Search.orExactType(Resource.class.getSimpleName()));
				group.add(Search.orExactType(org.structr.core.entity.File.class.getSimpleName()));
				group.add(Search.orExactType(Image.class.getSimpleName()));
				searchAttrs.add(group);

				List<AbstractNode> results = (List<AbstractNode>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false,
								     searchAttrs);

				logger.log(Level.INFO, "{0} results", results.size());

				if (!results.isEmpty()) {

					AbstractNode node = results.get(0);

					if (node instanceof Resource) {

						resource = (Resource) node;

					} else if (node instanceof org.structr.core.entity.File) {

						file = (org.structr.core.entity.File) node;

					}
				}

			}

			if (resource != null) {

				// 2a: do a traversal and collect content
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

			} else if (file != null) {

				// 2b: stream file to response
				
				InputStream in = file.getInputStream();
				OutputStream out = response.getOutputStream();

				String contentType = file.getContentType();

				if (contentType != null) {

					response.setContentType(contentType);

				} else {

					// Default
					response.setContentType("text/html; charset=utf-8");
				}

				IOUtils.copy(in, out);
				
				// 3: output content
				out.flush();
				out.close();
				response.setStatus(HttpServletResponse.SC_OK);
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
				AbstractNode doc    = createNode("Element", "doc", new NodeAttribute("tag", "html"));
				AbstractNode head   = createNode("Element", "header", new NodeAttribute("tag", "head"));
				AbstractNode script = createNode("Content", "script",
								 new NodeAttribute("content", "<script src=\"list.js\" language=\"JavaScript\" type=\"text/javascript\"></script>"));
				AbstractNode body     = createNode("Element", "body", new NodeAttribute("tag", "body"), new NodeAttribute("onload", "start()"));
				AbstractNode article1 = createNode("Element", "article1", new NodeAttribute("tag", "div"));
				AbstractNode article2 = createNode("Element", "article2", new NodeAttribute("tag", "div"));
				AbstractNode foo      = createNode("Content", "content1", new NodeAttribute("content", "Dies ist Seite 1"), new NodeAttribute("tag", "h1"));
				AbstractNode bar      = createNode("Content", "content2", new NodeAttribute("content", "Dies ist Seite 2"), new NodeAttribute("tag", "h1"));
				AbstractNode log      = createNode("Content", "log", new NodeAttribute("tag", "div"));

				// content
				AbstractNode foo2     = createNode("Content", "content3");
				String uuid           = foo2.getStringProperty("uuid");
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

				String idOfPage1 = page1.getIdString();
				String idOfPage2 = page2.getIdString();

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
				AbstractNode graphEditorCssContent = createNode("Content", "graph_editor_css_content", new NodeAttribute("content", readFile("/ge/css/ge.css")));

				linkNodes(graphEditorCss, graphEditorCssContent, graphEditorCss.getIdString(), 0);

				AbstractNode graphEditorHtml        = createNode("Resource", "ge.html");
				AbstractNode graphEditorHtmlContent = createNode("Content", "graph_editor_html_content", new NodeAttribute("content", readFile("/ge/ge.html")));

				linkNodes(graphEditorHtml, graphEditorHtmlContent, graphEditorHtml.getIdString(), 0);

				AbstractNode graphEditorJs        = createNode("Resource", "ge.js");
				AbstractNode graphEditorJsContent = createNode("Content", "graph_editor_js_content", new NodeAttribute("content", readFile("/ge/js/ge.js")));

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

//                      BufferedReader reader = new BufferedReader(new FileReader("/home/axel/NetBeansProjects/structr/structr/structr-web/src/main/resources"
//                                                      + path));
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

	private void printNodes(StringBuilder buffer, TreeNode root, int depth) {

		AbstractNode node = root.getData();
		String content    = null;
		String tag        = null;

		if (node != null) {

			if (node instanceof Content) {
				content = node.getStringProperty("content");
			}
			tag     = node.getStringProperty("tag");

			if (tag != null) {

				String onload = node.getStringProperty("onload");
				String id     = node.getStringProperty("uuid");

				buffer.append("<").append(tag);

				if (edit && id != null) {

					buffer.append(" structr_id='").append(id).append("'");

				}

				if (onload != null) {

					buffer.append(" onload='").append(onload).append("'");

				}

				buffer.append(">");

			}

			if (content != null) {

				buffer.append(content);

			}

		}

		// render children
		for (TreeNode subNode : root.getChildren()) {

			printNodes(buffer, subNode, depth + 1);

		}

		// render end tag
		if (tag != null) {

			buffer.append("</").append(tag).append(">");

		}
	}

	//~--- get methods ----------------------------------------------------

	private String getContent(final SecurityContext securityContext, final Resource resource) {

		TraversalDescription localDesc   = desc.expand(new ResourceExpander(resource.getStringProperty(AbstractNode.Key.uuid.name())));
		final StructrNodeFactory factory = new StructrNodeFactory(securityContext);
		final TreeNode root              = new TreeNode(null);

		localDesc = localDesc.evaluator(new Evaluator() {

			@Override
			public Evaluation evaluate(Path path) {

				Node node = path.endNode();

				if (node.hasProperty(AbstractNode.Key.type.name())) {

					String type          = (String) node.getProperty(AbstractNode.Key.type.name());
					TreeNode newTreeNode = new TreeNode(factory.createNode(securityContext, node, type));
					Relationship rel     = path.lastRelationship();

					if (rel != null) {

						Node parentNode         = rel.getStartNode();
						TreeNode parentTreeNode = root.getNode((String) parentNode.getProperty("uuid"));

						if (parentTreeNode == null) {

							root.addChild(newTreeNode);
							logger.log(Level.FINEST, "New tree node: {0} --> {1}", new Object[] { newTreeNode, root });
							logger.log(Level.FINE, "New tree node: {0} --> {1}", new Object[] { newTreeNode.getData().getName(), "root" });

						} else {

							parentTreeNode.addChild(newTreeNode);
							logger.log(Level.FINEST, "New tree node: {0} --> {1}", new Object[] { newTreeNode, parentTreeNode });
							logger.log(Level.FINE, "New tree node: {0} --> {1}", new Object[] { newTreeNode.getData().getName(), parentTreeNode.getData().getName() });

						}

					} else {

						root.addChild(newTreeNode);
						logger.log(Level.INFO, "Added {0} to root", newTreeNode);

					}

					return Evaluation.INCLUDE_AND_CONTINUE;

				} else {

					return Evaluation.EXCLUDE_AND_CONTINUE;

				}
			}

		});

		// do traversal to retrieve paths
		for (Node node : localDesc.traverse(resource.getNode()).nodes()) {

			System.out.println(node.getProperty("type") + "[" + node.getProperty("uuid") + "]: " + node.getProperty("name"));

		}

		StringBuilder buffer = new StringBuilder(10000);    // FIXME: use sensible initial size..

		printNodes(buffer, root, 0);

		return buffer.toString();
	}
}
