/*
 *  Copyright (C) 2011-2012 Axel Morgner
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



package org.structr.web.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.parser.markup.trac.TracWikiDialect;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.ByteArrayBuffer;

import org.pegdown.PegDownProcessor;

import org.structr.StructrServer;
import org.structr.common.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Image;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchAttributeGroup;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.web.auth.HttpAuthenticator;
import org.structr.web.common.ThreadLocalMatcher;
import org.structr.web.entity.Component;
import org.structr.web.entity.Condition;
import org.structr.web.entity.Content;
import org.structr.web.entity.Element;
import org.structr.web.entity.Page;
import org.structr.web.entity.View;
import org.structr.web.entity.html.HtmlElement;

import org.w3c.tidy.Tidy;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//~--- classes ----------------------------------------------------------------

/**
 * A first proof of concept for the new graph concept. This class has two modes,
 * one to create an example structure and one to traverse over the created
 * structure and return the collected content. Use the request parameter "create"
 * to create the test structure, use the request parameter "id" to retrieve the
 * pages, see log during "create" for IDs of the created pages.
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class HtmlServlet extends HttpServlet {

	private static final Map<String, Adapter<String, String>> contentConverters = new LinkedHashMap<String, Adapter<String, String>>();
	private static final ThreadLocalPegDownProcessor pegDownProcessor           = new ThreadLocalPegDownProcessor();
	private static final ThreadLocalTextileProcessor textileProcessor           = new ThreadLocalTextileProcessor();
	private static final ThreadLocalMediaWikiProcessor mediaWikiProcessor       = new ThreadLocalMediaWikiProcessor();
	private static final ThreadLocalTracWikiProcessor tracWikiProcessor         = new ThreadLocalTracWikiProcessor();
	private static final ThreadLocalMatcher threadLocalUUIDMatcher              = new ThreadLocalMatcher("[a-zA-Z0-9]{32}");
	private static final Logger logger                                          = Logger.getLogger(HtmlServlet.class.getName());
	private static final ThreadLocalConfluenceProcessor confluenceProcessor     = new ThreadLocalConfluenceProcessor();

	//~--- static initializers --------------------------------------------

	static {

		contentConverters.put("text/markdown", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {
				return pegDownProcessor.get().markdownToHtml(s);
			}

		});
		contentConverters.put("text/textile", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {
				return textileProcessor.get().parseToHtml(s);
			}

		});
		contentConverters.put("text/mediawiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {
				return mediaWikiProcessor.get().parseToHtml(s);
			}

		});
		contentConverters.put("text/tracwiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {
				return tracWikiProcessor.get().parseToHtml(s);
			}

		});
		contentConverters.put("text/confluence", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {
				return confluenceProcessor.get().parseToHtml(s);
			}

		});

	}

	//~--- fields ---------------------------------------------------------

	// area, base, br, col, command, embed, hr, img, input, keygen, link, meta, param, source, track, wbr
	private boolean edit, tidy;
	private Gson gson;

	//~--- methods --------------------------------------------------------

	private boolean postToRestUrl(HttpServletRequest request, final String pagePath, final Map<String, Object> parameters) {

		HttpClient httpClient = new HttpClient();
		ContentExchange contentExchange;
		String restUrl = null;

		gson = new GsonBuilder().create();

		try {

			httpClient.start();

			contentExchange = new ContentExchange();

			httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);

			restUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getLocalPort() + StructrServer.REST_URL + "/" + pagePath;

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
		// desc = Traversal.description().depthFirst().uniqueness(Uniqueness.RELATIONSHIP_PATH);    // .uniqueness(Uniqueness.NODE_GLOBAL);
	}

	@Override
	public void destroy() {}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) {

		Map<String, String[]> parameterMap = request.getParameterMap();
		String path                        = PathHelper.clean(request.getPathInfo());

		// Split by "//"
		String[] parts  = PathHelper.getParts(path);
		String pagePath = parts[parts.length - 1];

		postToRestUrl(request, pagePath, convert(parameterMap));

		String name = null;

		try {

			name = PathHelper.getParts(path)[0];

			response.sendRedirect("/" + name + "//" + pagePath);

		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not redirect to " + path, ex);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		String path = PathHelper.clean(request.getPathInfo());

		logger.log(Level.FINE, "Path info {0}", path);

		String[] urlParts = PathHelper.getParts(path);
		String searchFor  = null;

		if (urlParts.length > 1) {

			searchFor = StringUtils.substringBefore(urlParts[1], "?");

		}

		String[] pathParts = PathHelper.getParts(path);

		if ((pathParts == null) && (pathParts.length == 0)) {

			logger.log(Level.WARNING, "Could not get path parts from path {0}", path);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

			return;

		}

		String name                        = PathHelper.getName(pathParts[0]);
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

		// first part (before "//" is page path (file etc),
		// second part is nested component control
		// store remaining path parts in request
		Matcher matcher                 = threadLocalUUIDMatcher.get();
		boolean requestUriContainsUuids = false;

		for (int i = 1; i < pathParts.length; i++) {

			String[] parts = pathParts[i].split("[/]+");

			for (int j = 0; j < parts.length; j++) {

				// FIXME: index (j) in setAttribute might be
				// wrong here if we have multiple //-separated
				// parts!
				request.setAttribute(parts[j], j);
				matcher.reset(parts[j]);

				// set to "true" if part matches UUID pattern
				requestUriContainsUuids |= matcher.matches();
			}

		}

		// store information about UUIDs in path in request for later use in Component
		request.setAttribute(Component.REQUEST_CONTAINS_UUID_IDENTIFIER, requestUriContainsUuids);

		try {

			SecurityContext securityContext = SecurityContext.getInstance(this.getServletConfig(), request, response, AccessMode.Frontend);

			request.setCharacterEncoding("UTF-8");

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double start                = System.nanoTime();

			// 1: find entry point (Page, File or Image)
			AbstractNode node                 = findEntryPoint(name);
			Page page                         = null;
			org.structr.core.entity.File file = null;

			if (node instanceof Page) {

				page = (Page) node;

			} else if (node instanceof org.structr.core.entity.File) {

				file = (org.structr.core.entity.File) node;

			}

			if ((page != null) && securityContext.isVisible(page)) {

				String uuid                = page.getStringProperty(AbstractNode.Key.uuid);
				final StringBuilder buffer = new StringBuilder(10000);

				getContent(request, uuid, null, buffer, page, page, 0, false, searchFor, attrs, null, null);

				String content = buffer.toString();
				double end     = System.nanoTime();

				logger.log(Level.INFO, "Content collected in {0} seconds", decimalFormat.format((end - start) / 1000000000.0));

				String contentType = page.getStringProperty(Page.UiKey.contentType);

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

			// t.printStackTrace();
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

	private AbstractNode findEntryPoint(final String name) throws FrameworkException {

		if (name.length() > 0) {

			logger.log(Level.FINE, "File name {0}", name);

			List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

			searchAttrs.add(Search.andExactName(name));

			SearchAttributeGroup group = new SearchAttributeGroup(SearchOperator.AND);

			group.add(Search.orExactType(Page.class.getSimpleName()));
			group.add(Search.orExactType(org.structr.core.entity.File.class.getSimpleName()));
			group.add(Search.orExactType(Image.class.getSimpleName()));
			searchAttrs.add(group);

			// Searching for pages needs super user context anyway
			List<AbstractNode> results = (List<AbstractNode>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, searchAttrs);

			logger.log(Level.FINE, "{0} results", results.size());

			if (!results.isEmpty()) {

				return results.get(0);

			}

		}

		return null;
	}

	//~--- get methods ----------------------------------------------------

	private void getContent(HttpServletRequest request, final String pageId, final String componentId, final StringBuilder buffer, final AbstractNode page, final AbstractNode startNode,
				final int depth, boolean inBody, final String searchClass, final List<NodeAttribute> attrs, final AbstractNode viewComponent, final Condition condition) {

		String localComponentId = componentId;
		String content          = null;
		String tag              = null;
		StringBuilder indent    = new StringBuilder();

		for (int d = 1; d < depth; d++) {

			indent.append("  ");

		}

		String ind = indent.toString();

		if (startNode != null) {

			// If a search class is given, respect search attributes
			// Filters work with AND
			String kind = startNode.getStringProperty(Component.UiKey.kind);
			String id   = startNode.getStringProperty(AbstractNode.Key.uuid);

			tag = startNode.getStringProperty(Element.UiKey.tag);

			if ((kind != null) && kind.equals(EntityContext.normalizeEntityName(searchClass)) && (attrs != null)) {

				for (NodeAttribute attr : attrs) {

					String key = attr.getKey();
					Object val = attr.getValue();

					if (!val.equals(startNode.getProperty(key))) {

						return;

					}

				}

			}

			// this is the place where the "content" property is evaluated
			if (startNode instanceof Content) {

				Content contentNode = (Content) startNode;

				// fetch content with variable replacement
				content = contentNode.getPropertyWithVariableReplacement(page, pageId, componentId, viewComponent, Content.UiKey.content.name());

				// examine content type and apply converter
				String contentType = contentNode.getStringProperty(Content.UiKey.contentType);

				if (contentType != null) {

					Adapter<String, String> converter = contentConverters.get(contentType);

					if (converter != null) {

						try {

							// apply adapter
							content = converter.adapt(content);
						} catch (FrameworkException fex) {
							logger.log(Level.WARNING, "Unable to convert content: {0}", fex.getMessage());
						}

					}

				}

				// replace newlines with <br /> for rendering
				if (((contentType == null) || contentType.equals("text/plain")) && (content != null) &&!content.isEmpty()) {

					content = content.replaceAll("[\\n]{1}", "<br>\n");

				}

			}

			// check for component
			if (startNode instanceof Component) {

				localComponentId = startNode.getStringProperty(AbstractNode.Key.uuid);

			}

			// In edit mode, add an artificial 'div' tag around content nodes within body
			// to make them editable
			if (edit && inBody && (startNode instanceof Content)) {

				tag = "span";

				// Instead of adding a div tag, we mark the parent node with
				// the structr_element_id of this Content node
				// remove last character in buffer (should be '>')
				// buffer.delete(buffer.length() - 1, buffer.length());
				// buffer.append(" structr_content_id=\"").append(id).append("\">");

			}

			if (StringUtils.isNotBlank(tag) && (startNode instanceof HtmlElement)) {

				if (tag.equals("body")) {

					inBody = true;

				}

				buffer.append(ind).append("<").append(tag);

				if (edit && (id != null)) {

					if (depth == 1) {

						buffer.append(" structr_page_id='").append(pageId).append("'");

					}

					if (!(startNode instanceof Content)) {

						buffer.append(" structr_element_id=\"").append(id).append("\"");
						buffer.append(" structr_type=\"").append(startNode.getType()).append("\"");
						buffer.append(" structr_name=\"").append(startNode.getName()).append("\"");

					} else {

						buffer.append(" structr_content_id=\"").append(id).append("\"");

					}

				}

				HtmlElement htmlElement = (HtmlElement) startNode;

				for (String attribute : EntityContext.getPropertySet(startNode.getClass(), PropertyView.Html)) {

					try {

						String value = htmlElement.getPropertyWithVariableReplacement(page, pageId, localComponentId, viewComponent, attribute);

						if ((value != null) && StringUtils.isNotBlank(value)) {

							String key = attribute.substring(PropertyView.Html.length());

							buffer.append(" ").append(key).append("=\"").append(value).append("\"");

						}

					} catch (Throwable t) {
						t.printStackTrace();
					}

				}

				buffer.append(">\n");

			}

			if (content != null) {

				buffer.append(ind).append(content).append("\n");

			}
		}

		if (startNode instanceof View) {

			// fetch list of components from this view and
			List<GraphObject> components = ((View) startNode).getGraphObjects(request);

			for (GraphObject component : components) {

				// recursively render children
				List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode, pageId, localComponentId);

				for (AbstractRelationship rel : rels) {

					if ((condition == null) || ((condition != null) && condition.isSatisfied(request, rel))) {

						AbstractNode subNode = rel.getEndNode();

						getContent(request, pageId, localComponentId, buffer, page, subNode, depth, inBody, searchClass, attrs, (AbstractNode) component, condition);

					}

				}
			}
		} else if (startNode instanceof Condition) {

			// recursively render children
			List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode, pageId, localComponentId);
			Condition newCondition          = (Condition) startNode;

			for (AbstractRelationship rel : rels) {

				AbstractNode subNode = rel.getEndNode();

				getContent(request, pageId, localComponentId, buffer, page, subNode, depth + 1, inBody, searchClass, attrs, viewComponent, newCondition);

			}
		} else {

			// recursively render children
			List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode, pageId, localComponentId);

			for (AbstractRelationship rel : rels) {

				if ((condition == null) || ((condition != null) && condition.isSatisfied(request, rel))) {

					AbstractNode subNode = rel.getEndNode();

					getContent(request, pageId, localComponentId, buffer, page, subNode, depth + 1, inBody, searchClass, attrs, viewComponent, condition);

				}

			}
		}

		// render end tag, if needed (= if not singleton tags)
		if (StringUtils.isNotBlank(tag) && (startNode instanceof HtmlElement) &&!((HtmlElement) startNode).isVoidElement()) {

			buffer.append(ind).append("</").append(tag).append(">\n");

		}
	}

	//~--- inner classes --------------------------------------------------

	private static class ThreadLocalConfluenceProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {
			return new MarkupParser(new ConfluenceDialect());
		}
	}


	private static class ThreadLocalMediaWikiProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {
			return new MarkupParser(new MediaWikiDialect());
		}
	}


	private static class ThreadLocalPegDownProcessor extends ThreadLocal<PegDownProcessor> {

		@Override
		protected PegDownProcessor initialValue() {
			return new PegDownProcessor();
		}
	}


	private static class ThreadLocalTextileProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {
			return new MarkupParser(new TextileDialect());
		}
	}


	private static class ThreadLocalTracWikiProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {
			return new MarkupParser(new TracWikiDialect());
		}
	}
}
