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
import org.neo4j.graphdb.Direction;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.core.property.CollectionProperty;
import org.structr.web.common.HtmlProperty;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Input extends HtmlElement {

	public static final Property<String> _accept         = new HtmlProperty("accept");
	public static final Property<String> _alt            = new HtmlProperty("alt");
	public static final Property<String> _autocomplete   = new HtmlProperty("autocomplete");
	public static final Property<String> _autofocus      = new HtmlProperty("autofocus");
	public static final Property<String> _checked        = new HtmlProperty("checked");
	public static final Property<String> _dirname        = new HtmlProperty("dirname");
	public static final Property<String> _disabled       = new HtmlProperty("disabled");
	public static final Property<String> _form           = new HtmlProperty("form");
	public static final Property<String> _formaction     = new HtmlProperty("formaction");
	public static final Property<String> _formenctype    = new HtmlProperty("formenctype");
	public static final Property<String> _formmethod     = new HtmlProperty("formmethod");
	public static final Property<String> _formnovalidate = new HtmlProperty("formnovalidate");
	public static final Property<String> _formtarget     = new HtmlProperty("formtarget");
	public static final Property<String> _height         = new HtmlProperty("height");
	public static final Property<String> _list           = new HtmlProperty("list");

	public static final Property<String> _max            = new HtmlProperty("max");
	public static final Property<String> _maxlength      = new HtmlProperty("maxlength");
	public static final Property<String> _min            = new HtmlProperty("min");
	public static final Property<String> _multiple       = new HtmlProperty("multiple");
	public static final Property<String> _name           = new HtmlProperty("name");
	public static final Property<String> _pattern        = new HtmlProperty("pattern");
	public static final Property<String> _placeholder    = new HtmlProperty("placeholder");
	public static final Property<String> _readonly       = new HtmlProperty("readonly");
	public static final Property<String> _required       = new HtmlProperty("required");
	public static final Property<String> _size           = new HtmlProperty("size");
	public static final Property<String> _src            = new HtmlProperty("src");
	public static final Property<String> _step           = new HtmlProperty("step");
	public static final Property<String> _type           = new HtmlProperty("type");
	public static final Property<String> _value          = new HtmlProperty("value");
	public static final Property<String> _width          = new HtmlProperty("width");

	public static final CollectionProperty<Form> forms   = new CollectionProperty<Form>("forms", Form.class, RelType.CONTAINS, Direction.INCOMING, false);
	
	public static final View htmlView = new View(Input.class, PropertyView.Html,
	    
		_accept, _alt, _autocomplete, _autofocus, _checked, _dirname, _disabled, _form, _formaction, _formenctype, _formmethod,
		_formnovalidate, _formtarget, _height, _list, _max, _maxlength, _min, _multiple, _name, _pattern, _placeholder,
		_readonly, _required, _size, _src, _step, _type, _value, _width
	 );

	//~--- get methods ----------------------------------------------------

	@Override
	public boolean isVoidElement() {

		return true;

	}

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

}
