/*
 *  Copyright (C) 2012 Axel Morgner
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



package org.structr.web.entity.html;

import java.util.LinkedHashMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.GetNodeByIdCommand;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.web.entity.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractRelationship;
import org.structr.web.common.Function;
import org.structr.web.entity.Component;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public abstract class HtmlElement extends Element {

	protected static final String[] htmlAttributes = new String[] {

		// Core attributes
		"accesskey", "class", "contenteditable", "contextmenu", "dir", "draggable", "dropzone", "hidden", "id", "lang", "spellcheck", "style", "tabindex", "title",

		// Event-handler attributes
		"onabort", "onblur", "oncanplay", "oncanplaythrough", "onchange", "onclick", "oncontextmenu", "ondblclick", "ondrag", "ondragend", "ondragenter", "ondragleave", "ondragover",
		"ondragstart", "ondrop", "ondurationchange", "onemptied", "onended", "onerror", "onfocus", "oninput", "oninvalid", "onkeydown", "onkeypress", "onkeyup", "onload", "onloadeddata",
		"onloadedmetadata", "onloadstart", "onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup", "onmousewheel", "onpause", "onplay", "onplaying", "onprogress",
		"onratechange", "onreadystatechange", "onreset", "onscroll", "onseeked", "onseeking", "onselect", "onshow", "onstalled", "onsubmit", "onsuspend", "ontimeupdate", "onvolumechange",
		"onwaiting",
	};
	private static final Logger logger           = Logger.getLogger(HtmlElement.class.getName());
	private static final Pattern templatePattern = Pattern.compile("\\$\\{[^}]*\\}");
	private static final Pattern functionPattern = Pattern.compile("([a-zA-Z0-9]+)\\((.+)\\)");

	private static final java.util.Map<String, Function<String, String>> functions = new LinkedHashMap<String, Function<String, String>>();

	static {
		
		functions.put("md5", new Function<String, String>() {

			@Override public String apply(String... s) {
				return s != null && s.length > 0 ? DigestUtils.md5Hex(s[0]) : null;
			}
			
		});
		
		functions.put("upper", new Function<String, String>() {

			@Override public String apply(String... s) {
				return s != null && s.length > 0 ? s[0].toUpperCase() : null;
			}
			
		});
		
		functions.put("lower", new Function<String, String>() {

			@Override public String apply(String... s) {
				return s != null && s.length > 0 ? s[0].toLowerCase() : null;
			}
			
		});
		
		functions.put("capitalize", new Function<String, String>() {

			@Override public String apply(String... s) {
				return s != null && s.length > 0 ? StringUtils.capitalize(s[0]) : null;
			}
			
		});
		
		functions.put("add", new Function<String, String>() {

			@Override public String apply(String... s) {
				
				int result = 0;
				
				if(s != null) {
					
					for(int i=0; i<s.length; i++) {

						try {
							result += Integer.parseInt(s[i]);
							
						} catch(Throwable t) {}
					}
				}
				
				return new Integer(result).toString();
			}
			
		});
	}

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.All, UiKey.values());
		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.Public, UiKey.values());
		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.Ui, UiKey.values());
		EntityContext.registerPropertySet(HtmlElement.class, PropertyView.Html, true, htmlAttributes);
		EntityContext.registerSearchablePropertySet(HtmlElement.class, NodeIndex.fulltext.name(), UiKey.values());
		EntityContext.registerSearchablePropertySet(HtmlElement.class, NodeIndex.keyword.name(), UiKey.values());

	}

	//~--- constant enums -------------------------------------------------

	public enum UiKey implements PropertyKey {
		name, tag, path, contents, elements, components, resource
	}

	//~--- methods --------------------------------------------------------

	public static String convertValueForHtml(java.lang.Object value) {

		if (value != null) {

			// TODO: to intelligent conversion here
			return value.toString();
		}

		return null;
	}

	//~--- get methods ----------------------------------------------------

	public String[] getHtmlAttributes() {
		return htmlAttributes;
	}
	
	public String getPropertyWithVariableReplacement(String resourceId, String componentId, Component viewComponent, String key) {

		return replaceVariables(securityContext, this, resourceId, componentId, viewComponent, super.getStringProperty(key));
	}

	// ----- static methods -----
	public static String replaceVariables(SecurityContext securityContext, AbstractNode node, String resourceId, String componentId, Component viewComponent, String rawValue) {

		String value = null;
		
		if ((rawValue != null) && (rawValue instanceof String)) {

			value = (String) rawValue;

			Matcher matcher = templatePattern.matcher(value);

			while (matcher.find()) {

				String group  = matcher.group();
				String source = group.substring(2, group.length() - 1);

				// fetch referenced property
				String partValue = extractFunctions(securityContext, node, resourceId, componentId, viewComponent, source);
				if (partValue != null) {

					value = value.replace(group, partValue);
				}
			}
		}

		return value;

	}
	
	public static AbstractNode getNodeById(SecurityContext securityContext, String id) {

		if (id == null) {

			return null;

		}

		try {
			return (AbstractNode) Services.command(securityContext, GetNodeByIdCommand.class).execute(id);
		} catch (Throwable t) {
			logger.log(Level.WARNING, "Unable to load node with id {0}, {1}", new java.lang.Object[] { id, t.getMessage() });
		}

		return null;
	}

	public static String extractFunctions(SecurityContext securityContext, AbstractNode node, String resourceId, String componentId, Component viewComponent, String source) {
		
		Matcher functionMatcher = functionPattern.matcher(source);
		if(functionMatcher.matches()) {

			String functionGroup = functionMatcher.group(1);
			String parameter     = functionMatcher.group(2);
			String functionName  = functionGroup.substring(0, functionGroup.length());
			
			
			Function<String, String> function = functions.get(functionName);
			if(function != null) {
				
				if(parameter.contains(",")) {

					String[] parameters = parameter.split("[,]+");
					String[] results    = new String[parameters.length];

					// collect results from comma-separated function parameter
					for(int i=0; i<parameters.length; i++) {
						
						results[i] = extractFunctions(securityContext, node, resourceId, componentId, viewComponent, StringUtils.strip(parameters[i]));
					}

					return function.apply(results);

				} else {

					String result = extractFunctions(securityContext, node, resourceId, componentId, viewComponent, StringUtils.strip(parameter));
					return function.apply(result);
				}
			}
		}

		// if any of the following conditions match, the literal source value is returned
		if(StringUtils.isNotBlank(source) && StringUtils.isNumeric(source)) {

			// return numeric value
			return source;
			
		} else if(source.startsWith("\"") && source.endsWith("\"")) {

			return source.substring(1, source.length() - 1);
			
		} else if(source.startsWith("'") && source.endsWith("'")) {

			return source.substring(1, source.length() - 1);
			
		} else {
			
			// return property key
			return convertValueForHtml(getReferencedProperty(securityContext, node, resourceId, componentId, viewComponent, source));
		}
	}

	public static java.lang.Object getReferencedProperty(SecurityContext securityContext, AbstractNode startNode, String resourceId, String componentId, Component viewComponent, String refKey) {

		AbstractNode node   = startNode;
		String[] parts      = refKey.split("[\\.]+");
		String referenceKey = parts[parts.length - 1];

		// walk through template parts
		for (int i = 0; (i < parts.length) && (node != null); i++) {

			String part = parts[i];

			// special keyword "component"
			if ("component".equals(part.toLowerCase())) {

				node = getNodeById(securityContext, componentId);

				continue;

			}

			// special keyword "resource"
			if ("resource".equals(part.toLowerCase())) {

				node = getNodeById(securityContext, resourceId);

				continue;

			}

			// special keyword "link"
			if ("link".equals(part.toLowerCase())) {

				for(AbstractRelationship rel : node.getRelationships(RelType.LINK, Direction.OUTGOING)) {
					node = rel.getEndNode();
					break;
				}

				continue;
			}

			// special keyword "parent"
			if ("parent".equals(part.toLowerCase())) {

				for(AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.INCOMING)) {
					node = rel.getStartNode();
					break;
				}

				continue;
			}

			// special keyword "parent"
			if ("owner".equals(part.toLowerCase())) {

				for(AbstractRelationship rel : node.getRelationships(RelType.OWNS, Direction.INCOMING)) {
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

		}

		if (node != null) {

			return node.getProperty(referenceKey);

		}

		return null;
	}
}
