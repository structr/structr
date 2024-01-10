/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.schema.SchemaService;
import org.structr.web.entity.dom.DOMElement;

import java.net.URI;

public interface Input extends DOMElement {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Input");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Input"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");

		type.addStringProperty("_html_accept",         PropertyView.Html);
		type.addStringProperty("_html_alt",            PropertyView.Html);
		type.addStringProperty("_html_autocomplete",   PropertyView.Html);
		type.addStringProperty("_html_autofocus",      PropertyView.Html);
		type.addStringProperty("_html_checked",        PropertyView.Html);
		type.addStringProperty("_html_dirname",        PropertyView.Html);
		type.addStringProperty("_html_disabled",       PropertyView.Html);
		type.addStringProperty("_html_form",           PropertyView.Html);
		type.addStringProperty("_html_formaction",     PropertyView.Html);
		type.addStringProperty("_html_formenctype",    PropertyView.Html);
		type.addStringProperty("_html_formmethod",     PropertyView.Html);
		type.addStringProperty("_html_formnovalidate", PropertyView.Html);
		type.addStringProperty("_html_formtarget",     PropertyView.Html);
		type.addStringProperty("_html_height",         PropertyView.Html);
		type.addStringProperty("_html_list",           PropertyView.Html);
		type.addStringProperty("_html_max",            PropertyView.Html);
		type.addStringProperty("_html_maxlength",      PropertyView.Html);
		type.addStringProperty("_html_min",            PropertyView.Html);
		type.addStringProperty("_html_multiple",       PropertyView.Html);
		type.addStringProperty("_html_name",           PropertyView.Html);
		type.addStringProperty("_html_pattern",        PropertyView.Html);
		type.addStringProperty("_html_placeholder",    PropertyView.Html);
		type.addStringProperty("_html_readonly",       PropertyView.Html);
		type.addStringProperty("_html_required",       PropertyView.Html);
		type.addStringProperty("_html_size",           PropertyView.Html);
		type.addStringProperty("_html_src",            PropertyView.Html);
		type.addStringProperty("_html_step",           PropertyView.Html);
		type.addStringProperty("_html_type",           PropertyView.Html);
		type.addStringProperty("_html_value",          PropertyView.Html);
		type.addStringProperty("_html_width",          PropertyView.Html);

		type.overrideMethod("getHtmlAttributes", false, DOMElement.GET_HTML_ATTRIBUTES_CALL);
		type.overrideMethod("isVoidElement",     false, "return true;");
	}}

	/*

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

	//public static final Property<List<Form>> forms       = new StartNodes<>("forms", FormInput.class);

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
	*/
}
