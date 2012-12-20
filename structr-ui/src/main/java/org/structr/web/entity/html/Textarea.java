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

import org.apache.commons.lang.ArrayUtils;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.web.common.HtmlProperty;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Textarea extends HtmlElement {

	public static final Property<String> _name        = new HtmlProperty("name");
	public static final Property<String> _disabled    = new HtmlProperty("disabled");
	public static final Property<String> _form        = new HtmlProperty("form");
	public static final Property<String> _readonly    = new HtmlProperty("readonly");
	public static final Property<String> _maxlenght   = new HtmlProperty("maxlenght");
	public static final Property<String> _autofocus   = new HtmlProperty("autofocus");
	public static final Property<String> _required    = new HtmlProperty("required");
	public static final Property<String> _placeholder = new HtmlProperty("placeholder");
	public static final Property<String> _dirname     = new HtmlProperty("dirname");
	public static final Property<String> _rows        = new HtmlProperty("rows");
	public static final Property<String> _wrap        = new HtmlProperty("wrap");
	public static final Property<String> _cols        = new HtmlProperty("cols");
	
	public static final View htmlView = new View(Textarea.class, PropertyView.Html,
	    _name, _disabled, _form, _readonly, _maxlenght, _autofocus, _required, _placeholder, _dirname, _rows, _wrap, _cols
	);
	

	//~--- static initializers --------------------------------------------
//
//	static {
//
//		EntityContext.registerPropertySet(Textarea.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Textarea.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Textarea.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
//
//	}

	//~--- methods --------------------------------------------------------

	@Override
	public boolean avoidWhitespace() {

		return true;

	}

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
}
