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


import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.web.entity.Element;

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
		"onwaiting"
	};

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

	//~--- get methods ----------------------------------------------------

	public String[] getHtmlAttributes() {
		return htmlAttributes;
	}
}
