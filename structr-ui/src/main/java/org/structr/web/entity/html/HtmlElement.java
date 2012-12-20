/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
import org.structr.web.entity.Element;
import org.structr.web.entity.Page;
import org.structr.web.servlet.HtmlServlet;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringEscapeUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.core.property.CollectionProperty;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.PageHelper;
import org.structr.web.entity.Component;
import org.structr.web.entity.PageElement;
import org.structr.web.entity.RemoteView;
import org.structr.web.entity.View;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public abstract class HtmlElement extends PageElement implements Element {

	private static final Logger logger                                      = Logger.getLogger(HtmlElement.class.getName());
		
	public static final CollectionProperty<HtmlElement> parents             = new CollectionProperty<HtmlElement>("parents", HtmlElement.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final CollectionProperty<RemoteView>  remoteViews         = new CollectionProperty<RemoteView>("remoteViews", RemoteView.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<View>        views               = new CollectionProperty<View>("views", View.class, RelType.CONTAINS, Direction.OUTGOING, false);
       
	public static final Property<String>                path                = new StringProperty("path");
	
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
	    name, tag, path, parents, paths
	);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(HtmlElement.class, PropertyView.Ui,
	    
		name, tag, path, parents, paths,
	    
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

	}

	//~--- methods --------------------------------------------------------

	public boolean avoidWhitespace() {

		return false;

	}

	;

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

				Set<Page> pages = HtmlServlet.getResultPages(securityContext, (Page) page);

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
	
}
