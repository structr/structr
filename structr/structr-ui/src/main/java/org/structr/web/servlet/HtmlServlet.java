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

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.ArrayUtils;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import org.structr.common.*;
import org.structr.common.RelType;
import org.structr.common.ResourceExpander;
import org.structr.common.SecurityContext;
import org.structr.common.TreeNode;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Image;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.NodeFactory;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchAttributeGroup;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.web.entity.Content;
import org.structr.web.entity.Resource;
import org.structr.web.entity.html.HtmlElement;

import org.w3c.tidy.Tidy;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;

//~--- classes ----------------------------------------------------------------

/**
 * A first proof of concept for the new graph concept. This class has two modes,
 * one to create an example structure and one to traverse over the created
 * structure and return the collected content. Use the request parameter "create"
 * to create the test structure, use the request parameter "id" to retrieve the
 * resources, see log during "create" for IDs of the created resources.
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class HtmlServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(HtmlServlet.class.getName());

	//~--- fields ---------------------------------------------------------

	private TraversalDescription desc = null;
	private String[] html5VoidTags    = new String[] {

		"area", "base", "br", "col", "command", "embed", "hr", "img", "input", "keygen", "link", "meta", "param", "source", "track", "wbr"

	};

	// area, base, br, col, command, embed, hr, img, input, keygen, link, meta, param, source, track, wbr

	private boolean edit, tidy;

	//~--- methods --------------------------------------------------------

	@Override
	public void init() {

		// create prototype traversal description
		desc = Traversal.description().depthFirst().uniqueness(Uniqueness.RELATIONSHIP_PATH);    // .uniqueness(Uniqueness.NODE_GLOBAL);
	}

	@Override
	public void destroy() {}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		edit = false;
		tidy = false;

		if (request.getParameter("edit") != null) {

			edit = true;

		}

		if (request.getParameter("tidy") != null) {

			tidy = true;

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

			// Remove trailing /
			path = StringUtils.stripEnd(path, "/");

			String name = StringUtils.substringAfterLast(path, "/");

			if (name.length() > 0) {

				logger.log(Level.FINE, "File name {0}", name);

				List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

				searchAttrs.add(Search.andExactName(name));

				SearchAttributeGroup group = new SearchAttributeGroup(SearchOperator.AND);

				group.add(Search.orExactType(Resource.class.getSimpleName()));
				group.add(Search.orExactType(org.structr.core.entity.File.class.getSimpleName()));
				group.add(Search.orExactType(Image.class.getSimpleName()));
				searchAttrs.add(group);

				List<AbstractNode> results = (List<AbstractNode>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false,
								     searchAttrs);

				logger.log(Level.FINE, "{0} results", results.size());

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

				if (tidy) {

					StringWriter tidyOutput = new StringWriter();
					Tidy tidy               = new Tidy();
					Properties tidyProps    = new Properties();

					tidyProps.setProperty("indent", "auto");
					tidy.getConfiguration().addProps(tidyProps);
					tidy.parse(new StringReader(content), tidyOutput);

					content = tidyOutput.toString();

				}

				// 3: output content
				response.getWriter().append("<!DOCTYPE html>\n").append(content);
				response.getWriter().flush();
				response.getWriter().close();
				response.setStatus(HttpServletResponse.SC_OK);
			} else if (file != null) {

				// 2b: stream file to response
				InputStream in     = file.getInputStream();
				OutputStream out   = response.getOutputStream();
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
//
//	private AbstractNode createNode(String type, String name, NodeAttribute... attributes) throws FrameworkException {
//
//		SecurityContext context   = SecurityContext.getSuperUserInstance();
//		Command createNodeCommand = Services.command(context, CreateNodeCommand.class);
//		Map<String, Object> attrs = new HashMap<String, Object>();
//
//		attrs.put(AbstractNode.Key.type.name(), type);
//		attrs.put(AbstractNode.Key.name.name(), name);
//
//		for (NodeAttribute attr : attributes) {
//
//			attrs.put(attr.getKey(), attr.getValue());
//
//		}
//
//		AbstractNode node = (AbstractNode) createNodeCommand.execute(attrs);
//
//		logger.log(Level.INFO, "Created node with name {0} and id {1}", new Object[] { node.getName(), node.getId() });
//
//		return node;
//	}
//
//	private AbstractRelationship linkNodes(AbstractNode startNode, AbstractNode endNode, String resourceId, int index) throws FrameworkException {
//
//		SecurityContext context  = SecurityContext.getSuperUserInstance();
//		Command createRelCommand = Services.command(context, CreateRelationshipCommand.class);
//		AbstractRelationship rel = (AbstractRelationship) createRelCommand.execute(startNode, endNode, RelType.CONTAINS);
//
//		rel.setProperty(resourceId, index);
//
//		return rel;
//	}

	private void printNodes(String resourceId, StringBuilder buffer, TreeNode root, int depth, boolean inBody) {

		AbstractNode node         = root.getData();
		String content            = null;
		String tag                = null;
		AbstractRelationship link = null;

		if (node != null) {

			String id = node.getStringProperty("uuid");

			if (node instanceof Content) {

				content = node.getStringProperty("content");

//                              OutputStream out    = new ByteArrayOutputStream();
//                              String rawContent   = node.getStringProperty("content");
//                              Converter converter = new DefaultConverter();
//
//                              try {
//
//                                      InputReaderWrapper input   = InputReaderWrapper.valueOf(new StringReader(rawContent), "apt", converter.getInputFormats());
//                                      OutputStreamWrapper output = OutputStreamWrapper.valueOf(out, "xhtml", "UTF-8", converter.getOutputFormats());
//
//                                      converter.convert(input, output);
//
//                                      content = out.toString();
//
//                              } catch (UnsupportedFormatException e) {
//                                      e.printStackTrace();
//                              } catch (Exception e) {
//                                      e.printStackTrace();
//                              }
				List<AbstractRelationship> links = node.getOutgoingLinkRelationships();

				if ((links != null) &&!links.isEmpty()) {

					link = links.get(0);    // first link wins

				}

			}

			if (link != null) {

				buffer.append("<a href=\"").append(link.getEndNode().getName()).append("\">");

			}

			tag = node.getStringProperty("tag");

			// In edit mode, add an artificial 'div' tag around content nodes within body
			// to make them editable
			if (edit && inBody && (node instanceof Content)) {

				//tag = "div";
				// Instead of adding a div tag, we mark the parent node with
				// the structr_element_id of this Content node
				
				// remove last character in buffer (should be '>')
				buffer.delete(buffer.length()-1, buffer.length());
				
				buffer.append(" structr_content_id=\"").append(id).append("\">");

			}

			if (StringUtils.isNotBlank(tag)) {

				if (tag.equals("body")) {

					inBody = true;

				}

//                              String onload = node.getStringProperty("onload");

				buffer.append("<").append(tag);

				if (edit && (id != null)) {

					if (depth == 1) {

						buffer.append(" structr_resource_id='").append(resourceId).append("'");

					}

					if (node instanceof Content) {

						//buffer.append(" class=\"structr-content-container\" structr_content_id='").append(id).append("'");

					} else {

						String htmlClass = node.getStringProperty("_html_class");

						buffer.append(" class=\"structr-element-container ").append((htmlClass != null)
							? htmlClass
							: "").append("\" structr_element_id='").append(id).append("'");

						buffer.append(" structr_type='").append(node.getType()).append("'");
						buffer.append(" structr_name='").append(node.getName()).append("'");
					}


				}

				if (node instanceof HtmlElement) {

					for (String attribute : EntityContext.getPropertySet(node.getClass(), PropertyView.Html)) {

						if (node.getProperty(attribute) != null) {

							String key = attribute.substring(PropertyView.Html.length());

							buffer.append(" ").append(key).append("=\"").append(node.getProperty(attribute)).append("\"");

						}

					}

				}

//                              if (onload != null) {
//
//                                      buffer.append(" onload='").append(onload).append("'");
//
//                              }
				buffer.append(">");

			}

			if (content != null) {

				buffer.append(content);

			}

		}

		// render children
		for (TreeNode subNode : root.getChildren()) {

			printNodes(resourceId, buffer, subNode, depth + 1, inBody);

		}

		// render end tag, if needed (= if not singleton tags)
		if (StringUtils.isNotBlank(tag) &&!(ArrayUtils.contains(html5VoidTags, tag))) {

			buffer.append("</").append(tag).append(">");

		}

		if (link != null) {

			buffer.append("</a>");

		}
	}

	//~--- get methods ----------------------------------------------------

	private String getContent(final SecurityContext securityContext, final Resource resource) {

		TraversalDescription localDesc = desc.expand(new ResourceExpander(resource.getStringProperty(AbstractNode.Key.uuid.name())));
		final NodeFactory factory      = new NodeFactory(securityContext);
		final TreeNode root            = new TreeNode(resource);

		localDesc = localDesc.evaluator(new Evaluator() {

			@Override
			public Evaluation evaluate(Path path) {

				Node node = path.endNode();

				if (node.hasProperty(AbstractNode.Key.type.name())) {

					try {

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
								logger.log(Level.FINE, "New tree node: {0} --> {1}", new Object[] { newTreeNode.getData().getName(),
									parentTreeNode.getData().getName() });

							}

						} else {

							root.addChild(newTreeNode);
							logger.log(Level.FINE, "Added {0} to root", newTreeNode);

						}

					} catch (FrameworkException fex) {
						logger.log(Level.WARNING, "Unable to instantiate node", fex);
					}

					return Evaluation.INCLUDE_AND_CONTINUE;

				} else {

					return Evaluation.EXCLUDE_AND_CONTINUE;

				}
			}

		});

		// do traversal to retrieve paths
		for (Node node : localDesc.traverse(resource.getNode()).nodes()) {

//                      String name = node.hasProperty("name")
//                                    ? (String) node.getProperty("name")
//                                    : "unknown";
			// System.out.println(node.getProperty("type") + "[" + node.getProperty("uuid") + "]: " + name);
		}

		StringBuilder buffer = new StringBuilder(10000);    // FIXME: use sensible initial size..

		printNodes(resource.getStringProperty(Resource.Key.uuid), buffer, root, 0, false);

		return buffer.toString();
	}
}
