/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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



package org.structr.web.entity.html;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.Direction;

import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.web.common.Function;
import org.structr.web.common.ThreadLocalMatcher;
import org.structr.web.entity.Page;
import org.structr.web.servlet.HtmlServlet;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.parser.markup.trac.TracWikiDialect;
import org.apache.commons.lang.StringEscapeUtils;
import org.pegdown.PegDownProcessor;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.PageHelper;
import org.structr.web.entity.Component;
import org.structr.web.entity.Condition;
import org.structr.web.entity.Content;
import org.structr.web.entity.RemoteView;
import org.structr.web.entity.SearchResultView;
import org.structr.web.entity.View;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.w3c.dom.UserDataHandler;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public abstract class HtmlElement extends AbstractNode implements Element {

	private static final Logger logger = Logger.getLogger(HtmlElement.class.getName());

	private static final java.util.Map<String, Adapter<String, String>> contentConverters = new LinkedHashMap<String, Adapter<String, String>>();
		
	public static final CollectionProperty<RemoteView>  remoteViews         = new CollectionProperty<RemoteView>("remoteViews", RemoteView.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<View>        views               = new CollectionProperty<View>("views", View.class, RelType.CONTAINS, Direction.OUTGOING, false);
       
	public static final CollectionProperty<HtmlElement> children            = new CollectionProperty<HtmlElement>("children", HtmlElement.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final EntityProperty<HtmlElement>     parent              = new EntityProperty<HtmlElement>("parent", HtmlElement.class, RelType.CONTAINS, Direction.INCOMING, false);
	
	public static final Property<String>                tag                 = new StringProperty("tag");
	public static final Property<String>                path                = new StringProperty("path");
	public static final Property<Integer>               position            = new IntProperty("position");
	public static final Property<Long>                  version             = new LongProperty("version");
	
	// Core attributes
	public static final Property<String>                _accesskey          = new HtmlProperty("accesskey");
	public static final Property<String>                _class              = new HtmlProperty("class");
	public static final Property<String>                _contenteditable    = new HtmlProperty("contenteditable");
	public static final Property<String>                _contextmenu        = new HtmlProperty("contextmenu");
	public static final Property<String>                _dir                = new HtmlProperty("dir");
	public static final Property<String>                _draggable          = new HtmlProperty("draggable");
	public static final Property<String>                _dropzone           = new HtmlProperty("dropzone");
	public static final Property<String>                _hidden             = new HtmlProperty("hidden");
	public static final Property<String>                _id                 = new HtmlProperty("id");
	public static final Property<String>                _lang               = new HtmlProperty("lang");
	public static final Property<String>                _spellcheck         = new HtmlProperty("spellcheck");
	public static final Property<String>                _style              = new HtmlProperty("style");
	public static final Property<String>                _tabindex           = new HtmlProperty("tabindex");
	public static final Property<String>                _title              = new HtmlProperty("title");
	               
	// Event-handler attributes               
	public static final Property<String>                _onabort            = new HtmlProperty("onabort");
	public static final Property<String>                _onblur             = new HtmlProperty("onblur");
	public static final Property<String>                _oncanplay          = new HtmlProperty("oncanplay");
	public static final Property<String>                _oncanplaythrough   = new HtmlProperty("oncanplaythrough");
	public static final Property<String>                _onchange           = new HtmlProperty("onchange");
	public static final Property<String>                _onclick            = new HtmlProperty("onclick");
	public static final Property<String>                _oncontextmenu      = new HtmlProperty("oncontextmenu");
	public static final Property<String>                _ondblclick         = new HtmlProperty("ondblclick");
	public static final Property<String>                _ondrag             = new HtmlProperty("ondrag");
	public static final Property<String>                _ondragend          = new HtmlProperty("ondragend");
	public static final Property<String>                _ondragenter        = new HtmlProperty("ondragenter");
	public static final Property<String>                _ondragleave        = new HtmlProperty("ondragleave");
	public static final Property<String>                _ondragover         = new HtmlProperty("ondragover");
	public static final Property<String>                _ondragstart        = new HtmlProperty("ondragstart");
	public static final Property<String>                _ondrop             = new HtmlProperty("ondrop");
	public static final Property<String>                _ondurationchange   = new HtmlProperty("ondurationchange");
	public static final Property<String>                _onemptied          = new HtmlProperty("onemptied");
	public static final Property<String>                _onended            = new HtmlProperty("onended");
	public static final Property<String>                _onerror            = new HtmlProperty("onerror");
	public static final Property<String>                _onfocus            = new HtmlProperty("onfocus");
	public static final Property<String>                _oninput            = new HtmlProperty("oninput");
	public static final Property<String>                _oninvalid          = new HtmlProperty("oninvalid");
	public static final Property<String>                _onkeydown          = new HtmlProperty("onkeydown");
	public static final Property<String>                _onkeypress         = new HtmlProperty("onkeypress");
	public static final Property<String>                _onkeyup            = new HtmlProperty("onkeyup");
	public static final Property<String>                _onload             = new HtmlProperty("onload");
	public static final Property<String>                _onloadeddata       = new HtmlProperty("onloadeddata");
	public static final Property<String>                _onloadedmetadata   = new HtmlProperty("onloadedmetadata");
	public static final Property<String>                _onloadstart        = new HtmlProperty("onloadstart");
	public static final Property<String>                _onmousedown        = new HtmlProperty("onmousedown");
	public static final Property<String>                _onmousemove        = new HtmlProperty("onmousemove");
	public static final Property<String>                _onmouseout         = new HtmlProperty("onmouseout");
	public static final Property<String>                _onmouseover        = new HtmlProperty("onmouseover");
	public static final Property<String>                _onmouseup          = new HtmlProperty("onmouseup");
	public static final Property<String>                _onmousewheel       = new HtmlProperty("onmousewheel");
	public static final Property<String>                _onpause            = new HtmlProperty("onpause");
	public static final Property<String>                _onplay             = new HtmlProperty("onplay");
	public static final Property<String>                _onplaying          = new HtmlProperty("onplaying");
	public static final Property<String>                _onprogress         = new HtmlProperty("onprogress");
	public static final Property<String>                _onratechange       = new HtmlProperty("onratechange");
	public static final Property<String>                _onreadystatechange = new HtmlProperty("onreadystatechange");
	public static final Property<String>                _onreset            = new HtmlProperty("onreset");
	public static final Property<String>                _onscroll           = new HtmlProperty("onscroll");
	public static final Property<String>                _onseeked           = new HtmlProperty("onseeked");
	public static final Property<String>                _onseeking          = new HtmlProperty("onseeking");
	public static final Property<String>                _onselect           = new HtmlProperty("onselect");
	public static final Property<String>                _onshow             = new HtmlProperty("onshow");
	public static final Property<String>                _onstalled          = new HtmlProperty("onstalled");
	public static final Property<String>                _onsubmit           = new HtmlProperty("onsubmit");
	public static final Property<String>                _onsuspend          = new HtmlProperty("onsuspend");
	public static final Property<String>                _ontimeupdate       = new HtmlProperty("ontimeupdate");
	public static final Property<String>                _onvolumechange     = new HtmlProperty("onvolumechange");
	public static final Property<String>                _onwaiting          = new HtmlProperty("onwaiting");
               
	// needed for Importer               
	public static final Property<String>                _data               = new HtmlProperty("data");
	
	public static final org.structr.common.View publicView = new org.structr.common.View(HtmlElement.class, PropertyView.Public,
	    name, tag, path, parent
	);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(HtmlElement.class, PropertyView.Ui,
	    
		name, tag, path, parent, children,
	    
		_accesskey, _class, _contenteditable, _contextmenu, _dir, _draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style, _tabindex, _title,
	    
		_onabort, _onblur, _oncanplay, _oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick, _ondrag, _ondragend, _ondragenter, _ondragleave,
		_ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied, _onended, _onerror, _onfocus, _oninput, _oninvalid, _onkeydown, _onkeypress,
		_onkeyup, _onload, _onloadeddata, _onloadedmetadata, _onloadstart, _onmousedown, _onmousemove, _onmouseout, _onmouseover, _onmouseup, _onmousewheel,
		_onpause, _onplay, _onplaying, _onprogress, _onratechange, _onreadystatechange, _onreset, _onscroll, _onseeked, _onseeking, _onselect, _onshow,
		_onstalled, _onsubmit, _onsuspend, _ontimeupdate, _onvolumechange, _onwaiting
	    
	);
	
	public static final org.structr.common.View htmlView = new org.structr.common.View(HtmlElement.class, PropertyView.Html,
	    
		_accesskey, _class, _contenteditable, _contextmenu, _dir, _draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style, _tabindex, _title,
	    
		_onabort, _onblur, _oncanplay, _oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick, _ondrag, _ondragend, _ondragenter, _ondragleave,
		_ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied, _onended, _onerror, _onfocus, _oninput, _oninvalid, _onkeydown, _onkeypress,
		_onkeyup, _onload, _onloadeddata, _onloadedmetadata, _onloadstart, _onmousedown, _onmousemove, _onmouseout, _onmouseover, _onmouseup, _onmousewheel,
		_onpause, _onplay, _onplaying, _onprogress, _onratechange, _onreadystatechange, _onreset, _onscroll, _onseeked, _onseeking, _onselect, _onshow,
		_onstalled, _onsubmit, _onsuspend, _ontimeupdate, _onvolumechange, _onwaiting
	    
	);
	
	private static final java.util.Map<String, Function<String, String>> functions = new LinkedHashMap<String, Function<String, String>>();
	private static final ThreadLocalMatcher threadLocalFunctionMatcher             = new ThreadLocalMatcher("([a-zA-Z0-9_]+)\\((.+)\\)");
	private static final ThreadLocalMatcher threadLocalTemplateMatcher             = new ThreadLocalMatcher("\\$\\{[^}]*\\}");
	private static final ThreadLocalPegDownProcessor pegDownProcessor              = new ThreadLocalPegDownProcessor();
	private static final ThreadLocalTextileProcessor textileProcessor              = new ThreadLocalTextileProcessor();
	private static final ThreadLocalMediaWikiProcessor mediaWikiProcessor          = new ThreadLocalMediaWikiProcessor();
	private static final ThreadLocalTracWikiProcessor tracWikiProcessor            = new ThreadLocalTracWikiProcessor();
	private static final ThreadLocalConfluenceProcessor confluenceProcessor        = new ThreadLocalConfluenceProcessor();
	
	public static SearchNodeCommand searchNodesAsSuperuser		               = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);

	
	private DecimalFormat decimalFormat                                            = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	private static Set<Page> resultPages                                           = new HashSet<Page>();

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerSearchablePropertySet(HtmlElement.class, NodeIndex.fulltext.name(), publicView.properties());
		EntityContext.registerSearchablePropertySet(HtmlElement.class, NodeIndex.keyword.name(),  publicView.properties());
		
		functions.put("md5", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? DigestUtils.md5Hex(s[0])
				       : null;

			}

		});
		functions.put("upper", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? s[0].toUpperCase()
				       : null;

			}

		});
		functions.put("lower", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? s[0].toLowerCase()
				       : null;

			}

		});
		functions.put("capitalize", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? StringUtils.capitalize(s[0])
				       : null;

			}

		});
		functions.put("if", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s.length < 3) {

					return "";
				}

				if (s[0].equals("true")) {

					return s[1];
				} else {

					return s[2];
				}

			}

		});
		functions.put("equal", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				logger.log(Level.FINE, "Length: {0}", s.length);

				if (s.length < 2) {

					return "true";
				}

				logger.log(Level.FINE, "Comparing {0} to {1}", new java.lang.Object[] { s[0], s[1] });

				return s[0].equals(s[1])
				       ? "true"
				       : "false";

			}

		});
		functions.put("add", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				int result = 0;

				if (s != null) {

					for (int i = 0; i < s.length; i++) {

						try {

							result += Integer.parseInt(s[i]);

						} catch (Throwable t) {}

					}

				}

				return new Integer(result).toString();

			}

		});
		functions.put("active", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s.length == 0) {

					return "";
				}

				String data = this.dataId.get();
				String page = this.pageId.get();

				if (data != null && page != null) {

					if (data.equals(page)) {

						// return first argument if condition is true
						return s[0];
					} else if (s.length > 1) {

						// return second argument if condition is false and second argument exists
						return s[1];
					}

				}

				return "";

			}

		});

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

	//~--- methods --------------------------------------------------------

	public boolean avoidWhitespace() {

		return false;

	}

	//~--- methods --------------------------------------------------------

	public static String convertValueForHtml(java.lang.Object value) {

		if (value != null) {

			// TODO: do more intelligent conversion here
			return value.toString();
		}

		return null;

	}

	// ----- static methods -----
	public static String replaceVariables(SecurityContext securityContext, AbstractNode page, AbstractNode startNode, String pageId, String componentId, AbstractNode viewComponent,
		String rawValue) throws FrameworkException {

		String value = null;

		if ((rawValue != null) && (rawValue instanceof String)) {

			value = (String) rawValue;

			// re-use matcher from previous calls
			Matcher matcher = threadLocalTemplateMatcher.get();

			matcher.reset(value);

			while (matcher.find()) {

				String group  = matcher.group();
				String source = group.substring(2, group.length() - 1);

				// fetch referenced property
				String partValue = extractFunctions(securityContext, page, startNode, pageId, componentId, viewComponent, source);

				if (partValue != null) {

					value = value.replace(group, partValue);
				}

			}

		}

		return value;

	}

	public static String extractFunctions(SecurityContext securityContext, AbstractNode page, AbstractNode startNode, String pageId, String componentId, AbstractNode viewComponent,
		String source) throws FrameworkException {

		// re-use matcher from previous calls
		Matcher functionMatcher = threadLocalFunctionMatcher.get();

		functionMatcher.reset(source);

		if (functionMatcher.matches()) {

			String viewComponentId            = viewComponent != null
				? viewComponent.getProperty(AbstractNode.uuid)
				: null;
			String functionGroup              = functionMatcher.group(1);
			String parameter                  = functionMatcher.group(2);
			String functionName               = functionGroup.substring(0, functionGroup.length());
			Function<String, String> function = functions.get(functionName);

			if (function != null) {

				// store thread "state" in function
				function.setDataId(viewComponentId);
				function.setPageId(pageId);

				if (parameter.contains(",")) {

					String[] parameters = split(parameter);
					String[] results    = new String[parameters.length];

					// collect results from comma-separated function parameter
					for (int i = 0; i < parameters.length; i++) {

						results[i] = extractFunctions(securityContext, page, startNode, pageId, componentId, viewComponent, StringUtils.strip(parameters[i]));
					}

					return function.apply(results);

				} else {

					String result = extractFunctions(securityContext, page, startNode, pageId, componentId, viewComponent, StringUtils.strip(parameter));

					return function.apply(new String[] { result });

				}
			}

		}

		// if any of the following conditions match, the literal source value is returned
		if (StringUtils.isNotBlank(source) && StringUtils.isNumeric(source)) {

			// return numeric value
			return source;
		} else if (source.startsWith("\"") && source.endsWith("\"")) {

			return source.substring(1, source.length() - 1);
		} else if (source.startsWith("'") && source.endsWith("'")) {

			return source.substring(1, source.length() - 1);
		} else {

			// return property key
			return convertValueForHtml(getReferencedProperty(securityContext, page, startNode, pageId, componentId, viewComponent, source));
		}
	}

	public static String[] split(String source) {

		ArrayList<String> tokens   = new ArrayList<String>(20);
		boolean inDoubleQuotes     = false;
		boolean inSingleQuotes     = false;
		int len                    = source.length();
		int level                  = 0;
		StringBuilder currentToken = new StringBuilder(len);

		for (int i = 0; i < len; i++) {

			char c = source.charAt(i);

			// do not strip away separators in nested functions!
			if ((level != 0) || (c != ',')) {

				currentToken.append(c);
			}

			switch (c) {

				case '(' :
					level++;

					break;

				case ')' :
					level--;

					break;

				case '"' :
					if (inDoubleQuotes) {

						inDoubleQuotes = false;

						level--;

					} else {

						inDoubleQuotes = true;

						level++;

					}

					break;

				case '\'' :
					if (inSingleQuotes) {

						inSingleQuotes = false;

						level--;

					} else {

						inSingleQuotes = true;

						level++;

					}

					break;

				case ',' :
					if (level == 0) {

						tokens.add(currentToken.toString().trim());
						currentToken.setLength(0);

					}

					break;

			}

		}

		if (currentToken.length() > 0) {

			tokens.add(currentToken.toString().trim());
		}

		return tokens.toArray(new String[0]);

	}

	//~--- get methods ----------------------------------------------------

	public HtmlElement getParent() {
		return getProperty(parent);
	}

	public Property[] getHtmlAttributes() {
		return htmlView.properties();
	}

	public String getPropertyWithVariableReplacement(AbstractNode page, String pageId, String componentId, AbstractNode viewComponent, PropertyKey<String> key) throws FrameworkException {
		return replaceVariables(securityContext, page, this, pageId, componentId, viewComponent, super.getProperty(key));
	}

	public static java.lang.Object getReferencedProperty(SecurityContext securityContext, AbstractNode page, AbstractNode startNode, String pageId, String componentId, AbstractNode viewComponent,
		String refKey) throws FrameworkException {

		AbstractNode node                = startNode;
		String[] parts                   = refKey.split("[\\.]+");
		String referenceKey              = parts[parts.length - 1];

		PropertyKey pageIdProperty       = new StringProperty(pageId);
		PropertyKey referenceKeyProperty = new StringProperty(referenceKey);

		// walk through template parts
		for (int i = 0; (i < parts.length) && (node != null); i++) {

			String part = parts[i];

			// special keyword "request"
			if ("request".equals(part.toLowerCase())) {

				HttpServletRequest request = securityContext.getRequest();

				if (request != null) {

					return StringUtils.defaultString(request.getParameter(referenceKey));
				}

			}

			// special keyword "component"
			if ("component".equals(part.toLowerCase())) {

				node = PageHelper.getNodeById(securityContext, componentId);

				continue;

			}

			// special keyword "resource"
			if ("resource".equals(part.toLowerCase())) {

				node = PageHelper.getNodeById(securityContext, pageId);

				continue;

			}

			// special keyword "page"
			if ("page".equals(part.toLowerCase())) {

				node = page;

				continue;

			}

			// special keyword "link"
			if ("link".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : node.getRelationships(RelType.LINK, Direction.OUTGOING)) {

					node = rel.getEndNode();

					break;

				}

				continue;

			}

			// special keyword "parent"
			if ("parent".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.INCOMING)) {

					if (rel.getProperty(pageIdProperty) != null) {

						node = rel.getStartNode();

						break;

					}

				}

				continue;

			}

			// special keyword "owner"
			if ("owner".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : node.getRelationships(RelType.OWNS, Direction.INCOMING)) {

					node = rel.getStartNode();

					break;

				}

				continue;

			}

			// special keyword "data"
			if ("data".equals(part.toLowerCase())) {

				node = viewComponent;

				continue;

			}

			// special keyword "root": Find containing page
			if ("root".equals(part.toLowerCase())) {

				List<Page> pages = PageHelper.getPages(securityContext, viewComponent);

				if (pages.isEmpty()) {

					continue;
				}

				node = pages.get(0);

				continue;

			}

			// special keyword "result_size"
			if ("result_size".equals(part.toLowerCase())) {

				Set<Page> pages = getResultPages(securityContext, (Page) page);

				if (!pages.isEmpty()) {

					return pages.size();
				}

				return 0;

			}
			
			// special keyword "rest_result"
			if ("rest_result".equals(part.toLowerCase())) {

				HttpServletRequest request = securityContext.getRequest();

				if (request != null) {

					return StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.defaultString((String) request.getAttribute(HtmlServlet.REST_RESPONSE)), "\n", ""));
				}

				return 0;

			}
		}

		if (node != null) {

			return node.getProperty(referenceKeyProperty);
		}

		return null;

	}

	public boolean isVoidElement() {
		return false;
	}
	
	// ----- getContent -----
	
	public void getContent(SecurityContext securityContext, StringBuilder buffer) {
		
		
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

	private void getContent(SecurityContext securityContext, final String pageId, final String componentId, final StringBuilder buffer, final AbstractNode page, final AbstractNode startNode,
				int depth, boolean inBody, final String searchClass, final List<NodeAttribute> attrs, final AbstractNode viewComponent, final Condition condition) throws FrameworkException {

		String localComponentId    = componentId;
		String content             = null;
		String tag                 = null;
		String ind                 = "";
		HttpServletRequest request = securityContext.getRequest();
		HtmlElement el             = null;
		boolean isVoid             = (startNode instanceof HtmlElement) && ((HtmlElement) startNode).isVoidElement();

		if (startNode instanceof Component) {
			depth--;
		}

		if (startNode instanceof HtmlElement) {

			el = (HtmlElement) startNode;

			if (!el.avoidWhitespace()) {
				
				ind = indent(depth, true);

			}

		}

		if (startNode != null) {

//			if (!edit) {
//
//				Date nodeLastMod = startNode.getLastModifiedDate();
//
//				if ((lastModified == null) || nodeLastMod.after(lastModified)) {
//
//					lastModified = nodeLastMod;
//
//				}
//
//			}
			
			String id   = startNode.getUuid();
			tag = startNode.getProperty(HtmlElement.tag);

			if (startNode instanceof Component && searchClass != null) {
			
				// If a search class is given, respect search attributes
				// Filters work with AND
				String kind = startNode.getProperty(Component.kind);

				if ((kind != null) && kind.equals(EntityContext.normalizeEntityName(searchClass)) && (attrs != null)) {

					for (NodeAttribute attr : attrs) {

						PropertyKey key      = attr.getKey();
						java.lang.Object val = attr.getValue();

						if (!val.equals(startNode.getProperty(key))) {

							return;

						}

					}

				}
			}
			
			// this is the place where the "content" property is evaluated
			if (startNode instanceof Content) {

				Content contentNode = (Content) startNode;

				// fetch content with variable replacement
				content = contentNode.getPropertyWithVariableReplacement(request, page, pageId, componentId, viewComponent, Content.content);

				// examine content type and apply converter
				String contentType = contentNode.getProperty(Content.contentType);

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

				localComponentId = startNode.getProperty(AbstractNode.uuid);

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

					double start     = System.nanoTime();
					
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

						for (PropertyKey attribute : EntityContext.getPropertySet(startNode.getClass(), PropertyView.Html)) {

							try {

								String value = el.getPropertyWithVariableReplacement(page, pageId, localComponentId, viewComponent, attribute);

								if ((value != null) && StringUtils.isNotBlank(value)) {

									String key = attribute.jsonName().substring(PropertyView.Html.length());

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

					double end     = System.nanoTime();
					logger.log(Level.FINE, "Render node {0} in {1} seconds", new java.lang.Object[] { startNode.getUuid(), decimalFormat.format((end - start) / 1000000000.0)});

				}

			}

			if (content != null) {

				buffer.append(content);

			}

			if (startNode instanceof SearchResultView) {

				double startSearchResultView     = System.nanoTime();

				String searchString = (String) request.getParameter("search");

				if ((request != null) && StringUtils.isNotBlank(searchString)) {

					for (Page resultPage : getResultPages(securityContext, (Page) page)) {

						// recursively render children
						List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode);

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
				
				double endSearchResultView     = System.nanoTime();
				logger.log(Level.FINE, "Get graph objects for search {0} in {1} seconds", new java.lang.Object[] { searchString, decimalFormat.format((endSearchResultView - startSearchResultView) / 1000000000.0)});

			} else if (startNode instanceof View) {

				double startView     = System.nanoTime();
				
				// fetch query results
				List<GraphObject> results = ((View) startNode).getGraphObjects(request);
				
				double endView     = System.nanoTime();
				logger.log(Level.FINE, "Get graph objects for {0} in {1} seconds", new java.lang.Object[] { startNode.getUuid(), decimalFormat.format((endView - startView) / 1000000000.0)});

				for (GraphObject result : results) {

					// recursively render children
					List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode);

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
				List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode);
				Condition newCondition          = (Condition) startNode;

				for (AbstractRelationship rel : rels) {

					AbstractNode subNode = rel.getEndNode();

					if (subNode.isNotDeleted() && subNode.isNotDeleted()) {

						getContent(securityContext, pageId, localComponentId, buffer, page, subNode, depth + 1, inBody, searchClass, attrs, viewComponent, newCondition);

					}

				}
			} else {

				// recursively render children
				List<AbstractRelationship> rels = Component.getChildRelationships(request, startNode);

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
			if ((startNode instanceof HtmlElement || startNode instanceof Content) && StringUtils.isNotBlank(tag) && (!isVoid)) {

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
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

		searchAttributes.add(Search.andContent(search));
		searchAttributes.add(Search.andExactType(Content.class.getSimpleName()));

		try {

			Result<Content> result = searchNodesAsSuperuser.execute(searchAttributes);
			for (Content content : result.getResults()) {

				resultPages.addAll(PageHelper.getPages(securityContext, content));

			}

			// Remove result page itself
			resultPages.remove((Page) displayPage);

		} catch (FrameworkException fe) {
			logger.log(Level.WARNING, "Error while searching in content", fe);
		}

		return resultPages;
	}

	// ----- interface org.w3c.dom.Element -----

	@Override
	public String getTagName() {
		return getProperty(tag);
	}

	@Override
	public String getAttribute(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setAttribute(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeAttribute(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr getAttributeNode(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr setAttributeNode(Attr attr) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr removeAttributeNode(Attr attr) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public NodeList getElementsByTagName(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getAttributeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setAttributeNS(String string, String string1, String string2) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeAttributeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr getAttributeNodeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr setAttributeNodeNS(Attr attr) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public NodeList getElementsByTagNameNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasAttribute(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasAttributeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setIdAttribute(String string, boolean bln) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setIdAttributeNS(String string, String string1, boolean bln) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setIdAttributeNode(Attr attr, boolean bln) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getNodeName() {
		return getProperty(name);
	}

	@Override
	public String getNodeValue() throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setNodeValue(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Node getParentNode() {
		return getParent();
	}

	@Override
	public NodeList getChildNodes() {
		return new StructrNodeList(getProperty(children));
	}

	@Override
	public Node getFirstChild() {
		
		List<HtmlElement> _children = getProperty(children);
		
		if (!_children.isEmpty()) {
			
			return _children.get(0);
		}
		
		return null;
	}

	@Override
	public Node getLastChild() {
		
		List<HtmlElement> _children = getProperty(children);
		
		if (!_children.isEmpty()) {
			
			return _children.get(_children.size() - 1);
		}
		
		return null;
	}

	@Override
	public Node getPreviousSibling() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Node getNextSibling() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public NamedNodeMap getAttributes() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Document getOwnerDocument() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Node insertBefore(Node node, Node node1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Node replaceChild(Node node, Node node1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Node removeChild(Node node) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Node appendChild(Node node) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasChildNodes() {
		return !getProperty(children).isEmpty();
	}

	@Override
	public Node cloneNode(boolean bln) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void normalize() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isSupported(String string, String string1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getNamespaceURI() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getPrefix() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setPrefix(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getLocalName() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasAttributes() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getBaseURI() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public short compareDocumentPosition(Node node) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getTextContent() throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setTextContent(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isSameNode(Node node) {
		
		if (node instanceof HtmlElement) {
			
			return getProperty(GraphObject.uuid).equals(((HtmlElement)node).getProperty(GraphObject.uuid));
		}
		
		return false;
	}

	@Override
	public String lookupPrefix(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isDefaultNamespace(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String lookupNamespaceURI(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isEqualNode(Node node) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getFeature(String string, String string1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getUserData(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public java.lang.Object setUserData(String string, java.lang.Object o, UserDataHandler udh) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	// ----- nested classes -----
	private static class StructrNodeList extends ArrayList<HtmlElement> implements NodeList {

		public StructrNodeList(List<HtmlElement> children) {
			super(children);
		}
		
		@Override
		public Node item(int i) {
			return get(i);
		}

		@Override
		public int getLength() {
			return size();
		}
	}

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
