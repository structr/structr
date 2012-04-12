/*
 *  Copyright (C) 2011-2012 Axel Morgner
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import org.structr.StructrServer;
import org.structr.common.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Image;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchAttributeGroup;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.web.auth.HttpAuthenticator;
import org.structr.web.entity.Component;
import org.structr.web.entity.Content;
import org.structr.web.entity.Resource;
import org.structr.web.entity.html.HtmlElement;

import org.w3c.tidy.Tidy;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.*;
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
 * @author Axel Morgner
 */
public class HtmlServlet extends HttpServlet {

	private static final String REST_PATH_SEP = "//";
	private static final Logger logger        = Logger.getLogger(HtmlServlet.class.getName());

	//~--- fields ---------------------------------------------------------

	private TraversalDescription desc = null;
	private String[] html5VoidTags    = new String[] {

		"area", "base", "br", "col", "command", "embed", "hr", "img", "input", "keygen", "link", "meta", "param", "source", "track", "wbr"

	};

	// area, base, br, col, command, embed, hr, img, input, keygen, link, meta, param, source, track, wbr
	private boolean edit, tidy;
	private Gson gson;

	//~--- methods --------------------------------------------------------

	private boolean postToRestUrl(HttpServletRequest request, final String resourcePath, final Map<String, Object> parameters) {

		HttpClient httpClient = new HttpClient();
		ContentExchange contentExchange;
		String restUrl = null;

		gson = new GsonBuilder().create();

		try {

			httpClient.start();

			contentExchange = new ContentExchange();

			httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);

			restUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getLocalPort() + StructrServer.REST_URL + "/" + resourcePath;

			contentExchange.setURL(restUrl);

			Buffer buf = new ByteArrayBuffer(gson.toJson(parameters), "UTF-8");

			contentExchange.setRequestContent(buf);
			contentExchange.setRequestContentType("application/json");
			contentExchange.setMethod("POST");

			String[] userAndPass = HttpAuthenticator.getUsernameAndPassword(request);

			if ((userAndPass != null) && (userAndPass.length == 2) && (userAndPass[0] != null) && (userAndPass[1] != null)) {

				contentExchange.addRequestHeader("X-User", userAndPass[0]);
				contentExchange.addRequestHeader("X-Password", userAndPass[1]);

			}

			httpClient.send(contentExchange);
			contentExchange.waitForDone();

			return contentExchange.isDone();

		} catch (Exception ex) {

			logger.log(Level.WARNING, "Error while POSTing to REST url " + restUrl, ex);

			return false;
		}
	}

	@Override
	public void init() {

		// create prototype traversal description
		desc = Traversal.description().depthFirst().uniqueness(Uniqueness.RELATIONSHIP_PATH);    // .uniqueness(Uniqueness.NODE_GLOBAL);
	}

	@Override
	public void destroy() {}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {

		Map<String, String[]> parameterMap = request.getParameterMap();
		String path                        = clean(request.getPathInfo());

		// String resourcePath                = getParts(path)[1];
		postToRestUrl(request, path, convert(parameterMap));

		String name = null;

		try {

			name = getParts(path)[0];

			response.sendRedirect("/" + name);

			// doGet(request, response);

		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not redirect to " + name, ex);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		String path = clean(request.getPathInfo());

		logger.log(Level.INFO, "Path info {0}", path);

		String[] urlParts = getParts(path);
		String searchFor  = null;

		if (urlParts.length > 1) {

			searchFor = StringUtils.substringBefore(urlParts[1], "?");

		}

		String name                        = getName(getParts(path)[0]);
		List<NodeAttribute> attrs          = new LinkedList<NodeAttribute>();
		Map<String, String[]> parameterMap = request.getParameterMap();

		if ((parameterMap != null) && (parameterMap.size() > 0)) {

			attrs = convertToNodeAttributes(parameterMap);

		}

		edit = false;
		tidy = false;

		if (request.getParameter("edit") != null) {

			edit = true;

		}

		if (request.getParameter("tidy") != null) {

			tidy = true;

		}

		try {

			SecurityContext securityContext = SecurityContext.getInstance(this.getServletConfig(), request, response, AccessMode.Frontend);

			request.setCharacterEncoding("UTF-8");

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double start                = System.nanoTime();

			// 1: find entry point (Resource, File or Image)
			AbstractNode node                 = findEntryPoint(name);
			Resource resource                 = null;
			org.structr.core.entity.File file = null;

			if (node instanceof Resource) {

				resource = (Resource) node;

			} else if (node instanceof org.structr.core.entity.File) {

				file = (org.structr.core.entity.File) node;

			}

			if ((resource != null) && securityContext.isVisible(resource)) {

				String uuid                = resource.getStringProperty(Resource.Key.uuid);
				final StringBuilder buffer = new StringBuilder(10000);

				getContent(uuid, null, buffer, resource, 0, false, searchFor, attrs);

				String content = buffer.toString();
				double end     = System.nanoTime();

				logger.log(Level.INFO, "Content collected in {0} seconds", decimalFormat.format((end - start) / 1000000000.0));

				String contentType = resource.getStringProperty(Resource.UiKey.contentType);

				if (contentType != null) {

					response.setContentType(contentType);

				} else {

					// Default
					response.setContentType("text/html; charset=utf-8");
					response.getWriter().append("<!DOCTYPE html>\n");
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
				HttpAuthenticator.writeContent(content, response);

			} else if ((file != null) && securityContext.isVisible(file)) {

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
			} else {

				// Check if security context has set an 401 status
				if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

					HttpAuthenticator.writeUnauthorized(response);

				} else {

					HttpAuthenticator.writeNotFound(response);

				}
			}

		} catch (Throwable t) {

			// logger.log(Level.WARNING, "Exception while processing request", t);
			HttpAuthenticator.writeInternalServerError(response);
		}
	}

	/**
	 * Convert parameter map so that after conversion, all map values
	 * are a single String instead of an one-element String[]
	 *
	 * @param parameterMap
	 * @return
	 */
	private Map<String, Object> convert(final Map<String, String[]> parameterMap) {

		Map parameters = new HashMap<String, Object>();

		for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {

			String[] values = param.getValue();
			Object val;

			if (values.length == 1) {

				val = values[0];

			} else {

				val = values;

			}

			parameters.put(param.getKey(), val);

		}

		return parameters;
	}

	/**
	 * Convert parameter map to list of node attributes
	 *
	 * @param parameterMap
	 * @return
	 */
	private List<NodeAttribute> convertToNodeAttributes(final Map<String, String[]> parameterMap) {

		List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

		for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {

			String[] values = param.getValue();
			Object val;

			if (values.length == 1) {

				val = values[0];

			} else {

				val = values;

			}

			NodeAttribute attr = new NodeAttribute(param.getKey(), val);

			attrs.add(attr);

		}

		return attrs;
	}

	private String clean(String path) {

		// Remove leading and trailing /
		return StringUtils.strip(path, "/");
	}

	private AbstractNode findEntryPoint(final String name) throws FrameworkException {

		if (name.length() > 0) {

			logger.log(Level.FINE, "File name {0}", name);

			List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

			searchAttrs.add(Search.andExactName(name));

			SearchAttributeGroup group = new SearchAttributeGroup(SearchOperator.AND);

			group.add(Search.orExactType(Resource.class.getSimpleName()));
			group.add(Search.orExactType(org.structr.core.entity.File.class.getSimpleName()));
			group.add(Search.orExactType(Image.class.getSimpleName()));
			searchAttrs.add(group);

			// Searching for resources needs super user context anyway
			List<AbstractNode> results = (List<AbstractNode>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, searchAttrs);

			logger.log(Level.FINE, "{0} results", results.size());

			if (!results.isEmpty()) {

				return results.get(0);

			}

		}

		return null;
	}

	//~--- get methods ----------------------------------------------------

	private String getName(final String pathPart) {

		if (pathPart.contains("/")) {

			return StringUtils.substringAfterLast(pathPart, "/");

		} else {

			return pathPart;

		}
	}

	private String[] getParts(String path) {

		path = clean(path);

		return new String[] { StringUtils.substringBefore(path, REST_PATH_SEP), StringUtils.substringAfter(path, REST_PATH_SEP) };
	}

	private void getContent(final String resourceId, final String componentId, final StringBuilder buffer, final AbstractNode node, final int depth, boolean inBody, final String searchClass,
				final List<NodeAttribute> attrs) {

		String localComponentId   = componentId;
		String content            = null;
		String tag                = null;
		AbstractRelationship link = null;

		if (node != null) {

			// If a search class is given, respect search attributes
			// Filters work with AND
			String structrClass = node.getStringProperty(Component.UiKey.structrclass);

			if ((structrClass != null) && structrClass.equals(EntityContext.normalizeEntityName(searchClass)) && (attrs != null)) {

				for (NodeAttribute attr : attrs) {

					String key = attr.getKey();
					Object val = attr.getValue();

					if (!val.equals(node.getProperty(key))) {

						return;

					}

				}

			}

			String id = node.getStringProperty("uuid");

			for (int d = 0; d < depth; d++) {

				System.out.print(" ");

			}

			// System.out.println("id: " + id + " [" + node.getStringProperty("type") + "] " + node.getStringProperty("name") + ", depth: " + depth + ", res: " + resourceId + ", comp: " + componentId);
			if (node instanceof Content) {

				content = node.getStringProperty("content");

				List<AbstractRelationship> links = node.getOutgoingLinkRelationships();

				if ((links != null) &&!links.isEmpty()) {

					link = links.get(0);    // first link wins

				}

			}

			// check for component
			if (node instanceof Component) {

				localComponentId = node.getStringProperty(AbstractNode.Key.uuid);

			}

			if (link != null) {

				buffer.append("<a href=\"").append(link.getEndNode().getName()).append("\">");

			}

			tag = node.getStringProperty("tag");

			// In edit mode, add an artificial 'div' tag around content nodes within body
			// to make them editable
			if (edit && inBody && (node instanceof Content)) {

				// tag = "div";
				// Instead of adding a div tag, we mark the parent node with
				// the structr_element_id of this Content node
				// remove last character in buffer (should be '>')
				buffer.delete(buffer.length() - 1, buffer.length());
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

						// buffer.append(" class=\"structr-content-container\" structr_content_id='").append(id).append("'");
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

						if (StringUtils.isNotBlank(node.getStringProperty(attribute))) {

							String key = attribute.substring(PropertyView.Html.length());

							buffer.append(" ").append(key).append("=\"").append(node.getProperty(attribute)).append("\"");

						}

					}

				}

				buffer.append(">");

			}

			if (content != null) {

				buffer.append(content);

			}
		}

		// collect children
		List<AbstractRelationship> rels = new LinkedList<AbstractRelationship>();

		for (AbstractRelationship abstractRelationship : node.getOutgoingRelationships(RelType.CONTAINS)) {

			Relationship rel = abstractRelationship.getRelationship();

			if (rel.hasProperty(resourceId) || rel.hasProperty("*")) {

				AbstractNode endNode = abstractRelationship.getEndNode();

				if ((localComponentId != null) && ((endNode instanceof Content) || (endNode instanceof Component))) {

					// only add relationship if (nested) componentId matches
					if (rel.hasProperty(localComponentId)) {

						rels.add(abstractRelationship);

					}
				} else {

					rels.add(abstractRelationship);

				}

			}

		}

		Collections.sort(rels, new Comparator<AbstractRelationship>() {

			@Override
			public int compare(AbstractRelationship o1, AbstractRelationship o2) {

				Integer pos1 = getPosition(o1, resourceId);
				Integer pos2 = getPosition(o2, resourceId);

				return pos1.compareTo(pos2);
			}

		});

		// recursively render children
		for (AbstractRelationship rel : rels) {

			AbstractNode subNode = rel.getEndNode();

			getContent(resourceId, localComponentId, buffer, subNode, depth + 1, inBody, searchClass, attrs);

		}

		// render end tag, if needed (= if not singleton tags)
		if (StringUtils.isNotBlank(tag) &&!(ArrayUtils.contains(html5VoidTags, tag))) {

			buffer.append("</").append(tag).append(">");

		}

		if (link != null) {

			buffer.append("</a>");

		}
	}

	private int getPosition(final AbstractRelationship relationship, final String resourceId) {

		final Relationship rel = relationship.getRelationship();
		Integer position       = 0;

		try {

			Map<Integer, Relationship> sortedRelationshipMap = new TreeMap<Integer, Relationship>();
			Object prop                                      = null;
			final String key;

			// "*" is a wildcard for "matches any resource id"
			// TOOD: use pattern matching here?
			if (rel.hasProperty("*")) {

				prop = rel.getProperty("*");
				key  = "*";

			} else if (rel.hasProperty(resourceId)) {

				prop = rel.getProperty(resourceId);
				key  = resourceId;

			} else {

				key = null;

			}

			if ((key != null) && (prop != null)) {

				if (prop instanceof Integer) {

					position = (Integer) prop;

				} else if (prop instanceof String) {

					position = Integer.parseInt((String) prop);

				} else {

					throw new java.lang.IllegalArgumentException("Expected Integer or String");

				}

				Integer originalPos = position;

				// find free slot
				while (sortedRelationshipMap.containsKey(position)) {

					position++;

				}

				sortedRelationshipMap.put(position, rel);

				if (originalPos != position) {

					final Integer newPos = position;

					Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							rel.setProperty(key, newPos);

							return null;
						}

					});

				}

			}

		} catch (Throwable t) {

			// fail fast, no check
			logger.log(Level.SEVERE, "While reading property " + resourceId, t);
		}

		return position;
	}
}
