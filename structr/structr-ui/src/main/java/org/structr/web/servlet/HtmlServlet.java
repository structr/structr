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
import org.apache.commons.lang.StringEscapeUtils;
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
import org.structr.core.*;
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
import org.structr.web.entity.*;
import org.structr.web.entity.Component;
import org.structr.web.entity.Condition;
import org.structr.web.entity.Content;
import org.structr.web.entity.Element;
import org.structr.web.entity.Page;
import org.structr.web.entity.View;
import org.structr.web.entity.html.HtmlElement;

//~--- JDK imports ------------------------------------------------------------

import java.io.*;

import java.text.*;

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
	private static Set<Page> resultPages                                        = new HashSet<Page>();
	private static final Logger logger                                          = Logger.getLogger(HtmlServlet.class.getName());
	private static final ThreadLocalConfluenceProcessor confluenceProcessor     = new ThreadLocalConfluenceProcessor();
	private static Date lastModified;

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
		contentConverters.put("text/plain", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {
				return StringEscapeUtils.escapeHtml(s);
			}

		});

	}

	//~--- fields ---------------------------------------------------------

	// area, base, br, col, command, embed, hr, img, input, keygen, link, meta, param, source, track, wbr
	private boolean edit;
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
			request.setCharacterEncoding("UTF-8");

			restUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getLocalPort() + StructrServer.REST_URL + "/" + pagePath;

			contentExchange.setURL(restUrl);

			Buffer buf = new ByteArrayBuffer(gson.toJson(parameters), "UTF-8");

			contentExchange.setRequestContent(buf);
			contentExchange.setRequestContentType("application/json;charset=UTF-8");
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
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");

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

		try {

			request.setCharacterEncoding("UTF-8");

			// Important: Set character encoding before calling response.getWriter() !!, see Servlet Spec 5.4
			response.setCharacterEncoding("UTF-8");

			String path = PathHelper.clean(request.getPathInfo());

			logger.log(Level.FINE, "Path info {0}", path);

			String[] urlParts = PathHelper.getParts(path);
			String searchFor  = null;

			if (urlParts.length > 1) {

				searchFor = StringUtils.substringBefore(urlParts[1], "?");

			}

			String[] pathParts   = PathHelper.getParts(path);
			boolean tryIndexPage = false;
			String name          = "";

			if ((pathParts == null) || (pathParts.length == 0)) {

				// try to find a page with position==0
				tryIndexPage = true;

				logger.log(Level.INFO, "No path supplied, trying to find index page");

//                              response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//
//                              return;
			} else {

				name = PathHelper.getName(pathParts[0]);

			}

			List<NodeAttribute> attrs          = new LinkedList<NodeAttribute>();
			Map<String, String[]> parameterMap = request.getParameterMap();

			if ((parameterMap != null) && (parameterMap.size() > 0)) {

				attrs = convertToNodeAttributes(parameterMap);

			}

			edit = false;

			if (request.getParameter("edit") != null) {

				edit = true;

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

			SecurityContext securityContext = SecurityContext.getInstance(this.getServletConfig(), request, response, AccessMode.Frontend);
			DecimalFormat decimalFormat     = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double start                    = System.nanoTime();

			// 1: find entry point (Page, File or Image)
			AbstractNode node = tryIndexPage
					    ? findIndexPage()
					    : findEntryPoint(name);

			if (node == null) {

				HttpAuthenticator.writeNotFound(response);

				return;

			}

			Page page                         = null;
			org.structr.core.entity.File file = null;

			if (node instanceof Page) {

				page = (Page) node;

			} else if (node instanceof org.structr.core.entity.File) {

				file = (org.structr.core.entity.File) node;

			}

			if (edit) {

				response.setHeader("Pragma", "no-cache");

			} else {

				Date nodeLastMod = node.getLastModifiedDate();

				if ((lastModified == null) || nodeLastMod.after(lastModified)) {

					lastModified = nodeLastMod;

				}

			}

			if ((page != null) && securityContext.isVisible(page)) {

				PrintWriter out            = response.getWriter();
				String uuid                = page.getStringProperty(AbstractNode.Key.uuid);
				final StringBuilder buffer = new StringBuilder(8192);

				getContent(securityContext, uuid, null, buffer, page, page, 0, false, searchFor, attrs, null, null);

				if (!edit && setCachingHeader(request, response, node)) {

					out.flush();
					out.close();

				} else {

					String content = buffer.toString();
					double end     = System.nanoTime();

					logger.log(Level.INFO, "Content collected in {0} seconds", decimalFormat.format((end - start) / 1000000000.0));

					String contentType = page.getStringProperty(Page.UiKey.contentType);

					if (contentType != null) {

						if (contentType.equals("text/html")) {

							contentType = contentType.concat(";charset=UTF-8");

						}

						response.setContentType(contentType);

					} else {

						// Default
						response.setContentType("text/html;charset=UTF-8");
					}

//                                      if (tidy) {
//
//                                              StringWriter tidyOutput = new StringWriter();
//                                              Tidy tidy               = new Tidy();
//                                              Properties tidyProps    = new Properties();
//
//                                              tidyProps.setProperty("indent", "auto");
//                                              tidy.getConfiguration().addProps(tidyProps);
//                                              tidy.parse(new StringReader(content), tidyOutput);
//
//                                              content = tidyOutput.toString();
//
//                                      }
					// 3: output content
					out.append("<!DOCTYPE html>\n");
					HttpAuthenticator.writeContent(content, response);

				}

			} else if ((file != null) && securityContext.isVisible(file)) {

				OutputStream out = response.getOutputStream();

				if (!edit && setCachingHeader(request, response, node)) {

					out.flush();
					out.close();

				} else {

					// 2b: stream file to response
					InputStream in     = file.getInputStream();
					String contentType = file.getContentType();

					if (contentType != null) {

						response.setContentType(contentType);

					} else {

						// Default
						response.setContentType("application/octet-stream");
					}

					IOUtils.copy(in, out);

					// 3: output content
					out.flush();
					out.close();
					response.setStatus(HttpServletResponse.SC_OK);
				}

			} else {

				// Check if security context has set an 401 status
				if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {

					try {
						HttpAuthenticator.writeUnauthorized(response);
					} catch (IllegalStateException ise) {
						;
					}

				} else {

					HttpAuthenticator.writeNotFound(response);

				}
			}

		} catch (Throwable t) {

			// t.printStackTrace();
			logger.log(Level.WARNING, "Exception while processing request", t);
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

	private AbstractNode findIndexPage() throws FrameworkException {

		logger.log(Level.FINE, "Looking for an index page ...");

		// Get all pages
		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		searchAttrs.add(Search.orExactType(Page.class.getSimpleName()));

		List<Page> results = (List<Page>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, searchAttrs);

		logger.log(Level.FINE, "{0} results", results.size());

		if (!results.isEmpty()) {

			Collections.sort(results, new GraphObjectComparator(Page.UiKey.position.name(), AbstractNodeComparator.ASCENDING));

			return results.get(0);

		}

		return null;
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

	private static String indent(final int depth, final boolean newline) {

		StringBuilder indent = new StringBuilder();

		if (newline) {

			indent.append("\n");

		}

		for (int d = 0; d < depth; d++) {

			indent.append("  ");

		}

		return indent.toString();
	}

	//~--- get methods ----------------------------------------------------

	private void getContent(SecurityContext securityContext, final String pageId, final String componentId, final StringBuilder buffer, final AbstractNode page, final AbstractNode startNode,
				final int depth, boolean inBody, final String searchClass, final List<NodeAttribute> attrs, final AbstractNode viewComponent, final Condition condition) {

		String localComponentId    = componentId;
		String content             = null;
		String tag                 = null;
		String ind                 = "";
		HttpServletRequest request = securityContext.getRequest();
		HtmlElement el             = null;
		boolean isVoid             = (startNode instanceof HtmlElement) && ((HtmlElement) startNode).isVoidElement();

		if (startNode instanceof HtmlElement) {

			el = (HtmlElement) startNode;

			if (!el.avoidWhitespace()) {

				ind = indent(depth, true);

			}

		}

		if (startNode != null) {

			if (!edit) {

				Date nodeLastMod = startNode.getLastModifiedDate();

				if ((lastModified == null) || nodeLastMod.after(lastModified)) {

					lastModified = nodeLastMod;

				}

			}

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
				content = contentNode.getPropertyWithVariableReplacement(request, page, pageId, componentId, viewComponent, Content.UiKey.content.name());

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

					content = content.replaceAll("[\\n]{1}", "<br>");

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

			}

			if (StringUtils.isNotBlank(tag)) {

				if (tag.equals("body")) {

					inBody = true;

				}

				if ((startNode instanceof Content) || (startNode instanceof HtmlElement)) {

					buffer.append("<").append(tag);

					if (edit && (id != null)) {

						if (depth == 1) {

							buffer.append(" structr_page_id='").append(pageId).append("'");

						}

						if (el != null) {

							buffer.append(" structr_element_id=\"").append(id).append("\"");
							buffer.append(" structr_type=\"").append(startNode.getType()).append("\"");
							buffer.append(" structr_name=\"").append(startNode.getName()).append("\"");

						} else {

							buffer.append(" structr_content_id=\"").append(id).append("\"");

						}

					}

					if (el != null) {

						for (String attribute : EntityContext.getPropertySet(startNode.getClass(), PropertyView.Html)) {

							try {

								String value = el.getPropertyWithVariableReplacement(page, pageId, localComponentId, viewComponent, attribute);

								if ((value != null) && StringUtils.isNotBlank(value)) {

									String key = attribute.substring(PropertyView.Html.length());

									buffer.append(" ").append(key).append("=\"").append(value).append("\"");

								}

							} catch (Throwable t) {
								t.printStackTrace();
							}

						}

					}

					buffer.append(">");

					if (!isVoid) {

						buffer.append(ind);

					}

				}

			}

			if (content != null) {

				buffer.append(content);

			}

			if (startNode instanceof SearchResultView) {

				String searchString = (String) request.getParameter("search");

				if ((request != null) && StringUtils.isNotBlank(searchString)) {

					for (Page resultPage : getResultPages(securityContext, (Page) page)) {

						// recursively render children
						List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode, pageId, localComponentId);

						for (AbstractRelationship rel : rels) {

							if ((condition == null) || ((condition != null) && condition.isSatisfied(request, rel))) {

								AbstractNode subNode = rel.getEndNode();

								if (subNode.isNotDeleted() && subNode.isNotDeleted()) {

									getContent(securityContext, pageId, localComponentId, buffer, page, subNode, depth, inBody, searchClass, attrs, resultPage,
										   condition);

								}

							}

						}
					}

				}

			} else if (startNode instanceof View) {

				// fetch query results
				List<GraphObject> results = ((View) startNode).getGraphObjects(request);

				for (GraphObject result : results) {

					// recursively render children
					List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode, pageId, localComponentId);

					for (AbstractRelationship rel : rels) {

						if ((condition == null) || ((condition != null) && condition.isSatisfied(request, rel))) {

							AbstractNode subNode = rel.getEndNode();

							if (subNode.isNotDeleted() && subNode.isNotDeleted()) {

								getContent(securityContext, pageId, localComponentId, buffer, page, subNode, depth, inBody, searchClass, attrs, (AbstractNode) result,
									   condition);

							}

						}

					}
				}
			} else if (startNode instanceof Condition) {

				// recursively render children
				List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode, pageId, localComponentId);
				Condition newCondition          = (Condition) startNode;

				for (AbstractRelationship rel : rels) {

					AbstractNode subNode = rel.getEndNode();

					if (subNode.isNotDeleted() && subNode.isNotDeleted()) {

						getContent(securityContext, pageId, localComponentId, buffer, page, subNode, depth + 1, inBody, searchClass, attrs, viewComponent, newCondition);

					}

				}
			} else {

				// recursively render children
				List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode, pageId, localComponentId);

				for (AbstractRelationship rel : rels) {

					if ((condition == null) || ((condition != null) && condition.isSatisfied(request, rel))) {

						AbstractNode subNode = rel.getEndNode();

						if (subNode.isNotDeleted() && subNode.isNotDeleted()) {

							getContent(securityContext, pageId, localComponentId, buffer, page, subNode, depth + 1, inBody, searchClass, attrs, viewComponent, condition);

						}

					}

				}
			}

			boolean whitespaceOnly = false;
			int lastNewline        = buffer.lastIndexOf("\n");

			whitespaceOnly = StringUtils.isBlank((lastNewline > -1)
				? buffer.substring(lastNewline)
				: buffer.toString());

			if ((el != null) &&!el.avoidWhitespace()) {

				if (whitespaceOnly) {

					buffer.replace(buffer.length() - 2, buffer.length(), "");

				} else {

					buffer.append(indent(depth - 1, true));

				}

			}

			// render end tag, if needed (= if not singleton tags)
			if (StringUtils.isNotBlank(tag) && ((startNode instanceof Content) ||!isVoid)) {

				buffer.append("</").append(tag).append(">");

				if ((el != null) &&!el.avoidWhitespace()) {

					buffer.append(indent(depth - 1, true));

				}

			}

		}
	}

	/**
	 * Return (cached) result pages
	 *
	 * Search string is taken from SecurityContext's http request
	 * Given displayPage is substracted from search result (we don't want to return search result page in search results)
	 *
	 * @param securityContext
	 * @param displayPage
	 * @return
	 */
	public static Set<Page> getResultPages(final SecurityContext securityContext, final Page displayPage) {

		HttpServletRequest request = securityContext.getRequest();
		String search              = request.getParameter("search");

		if ((request == null) || StringUtils.isEmpty(search)) {

			return Collections.EMPTY_SET;

		}

		if (request != null) {

			resultPages = (Set<Page>) request.getAttribute("searchResults");

			if ((resultPages != null) &&!resultPages.isEmpty()) {

				return resultPages;

			}

		}

		if (resultPages == null) {

			resultPages = new HashSet<Page>();

		}

		// fetch search results
		// List<GraphObject> results              = ((SearchResultView) startNode).getGraphObjects(request);
		Command searchNode                     = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeletedAndHidden        = false;
		boolean publicOnly                     = false;

		searchAttributes.add(Search.andContent(search));
		searchAttributes.add(Search.andExactType(Content.class.getSimpleName()));

		try {

			List<Content> contentNodes = (List<Content>) searchNode.execute(topNode, includeDeletedAndHidden, publicOnly, searchAttributes);

			for (Content contentNode : contentNodes) {

				resultPages.addAll(HtmlElement.getPages(securityContext, contentNode));

			}

			// Remove result page itself
			resultPages.remove((Page) displayPage);

		} catch (FrameworkException fe) {
			logger.log(Level.WARNING, "Error while searching in content", fe);
		}

		return resultPages;
	}

	//~--- set methods ----------------------------------------------------

	private static boolean setCachingHeader(final HttpServletRequest request, HttpServletResponse response, final AbstractNode node) {

		boolean notModified = false;

		// add some caching directives to header
		// see http://weblogs.java.net/blog/2007/08/08/expires-http-header-magic-number-yslow
		DateFormat httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		Calendar cal              = new GregorianCalendar();
		Integer seconds           = node.getIntProperty(Page.UiKey.cacheForSeconds);

		if (seconds != null) {

			cal.add(Calendar.SECOND, seconds);
			response.addHeader("Cache-Control", "public, max-age=" + seconds + ", s-maxage=" + seconds + ", must-revalidate, proxy-revalidate");
			httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			response.addHeader("Expires", httpDateFormat.format(cal.getTime()));

		} else {

			response.addHeader("Cache-Control", "public, must-revalidate, proxy-revalidate");

		}

		if (lastModified != null) {

			response.addHeader("Last-Modified", httpDateFormat.format(lastModified));

			String ifModifiedSince = request.getHeader("If-Modified-Since");

			if (StringUtils.isNotBlank(ifModifiedSince)) {

				try {

					Date ifModSince = httpDateFormat.parse(ifModifiedSince);

					if ((ifModSince != null) && (lastModified.equals(ifModSince) || lastModified.before(ifModSince))) {

						notModified = true;

						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);

					}

				} catch (ParseException ex) {
					logger.log(Level.WARNING, "Could not parse If-Modified-Since header", ex);
				}

			}

		}

		return notModified;
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
