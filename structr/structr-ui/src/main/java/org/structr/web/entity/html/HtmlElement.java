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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

	private static final java.util.Map<String, Method> functions = new LinkedHashMap<String, Method>();

	static {
		try {
			functions.put("md5", HtmlElement.class.getMethod("md5", String.class));
		} catch (NoSuchMethodException ex) {
			Logger.getLogger(HtmlElement.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SecurityException ex) {
			Logger.getLogger(HtmlElement.class.getName()).log(Level.SEVERE, null, ex);
		}
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

	public String getReferencedProperty(String resourceId, String componentId, String key) {

		java.lang.Object rawValue = super.getProperty(key);
		String value              = null;

		if ((rawValue != null) && (rawValue instanceof String)) {

			value = (String) rawValue;

			Matcher matcher = templatePattern.matcher(value);

			while (matcher.find()) {

				String group  = matcher.group();
				String refKey = group.substring(2, group.length() - 1);
				boolean md5   = false;

				if (refKey.startsWith("md5(") && refKey.endsWith(")")) {

					refKey = StringUtils.stripStart(refKey, "md5(");
					refKey = StringUtils.stripEnd(refKey, ")");
					md5    = true;

				}

				// fetch referenced property
				String partValue = convertValueForHtml(getReferencedProperty(securityContext, this, resourceId, componentId, refKey));

				// Eventually replace by MD5 hash
				if (md5) {

					Method function = functions.get("md5");
					try {
						partValue = (String) function.invoke(HtmlElement.class, partValue);
					} catch (IllegalAccessException ex) {
						Logger.getLogger(HtmlElement.class.getName()).log(Level.SEVERE, null, ex);
					} catch (IllegalArgumentException ex) {
						Logger.getLogger(HtmlElement.class.getName()).log(Level.SEVERE, null, ex);
					} catch (InvocationTargetException ex) {
						Logger.getLogger(HtmlElement.class.getName()).log(Level.SEVERE, null, ex);
					}

				}

				if (partValue != null) {

					value = value.replace(group, partValue);

				}

			}

		}

		return value;
	}

	public static java.lang.Object getReferencedProperty(SecurityContext securityContext, AbstractNode startNode, String resourceId, String componentId, String refKey) {

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

		}

		if (node != null) {

			return node.getProperty(referenceKey);

		}

		return null;
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

	public static String md5(final String in) { return in != null ? DigestUtils.md5Hex(in) : null; }
}
