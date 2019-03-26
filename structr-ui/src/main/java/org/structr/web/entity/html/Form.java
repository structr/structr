/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html;

import java.net.URI;
import org.structr.common.PropertyView;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.entity.dom.DOMElement;

public interface Form extends DOMElement {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Form");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Form"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");

		type.addStringProperty("_html_accept-charset", PropertyView.Html);
		type.addStringProperty("_html_action",         PropertyView.Html);
		type.addStringProperty("_html_autocomplete",   PropertyView.Html);
		type.addStringProperty("_html_enctype",        PropertyView.Html);
		type.addStringProperty("_html_method",         PropertyView.Html);
		type.addStringProperty("_html_name",           PropertyView.Html);
		type.addStringProperty("_html_novalidate",     PropertyView.Html);
		type.addStringProperty("_html_target",         PropertyView.Html);

		type.overrideMethod("getHtmlAttributes", false, DOMElement.GET_HTML_ATTRIBUTES_CALL);
	}}

	/*
	public static final Property<String> _acceptCharset = new HtmlProperty("accept-charset");
	public static final Property<String> _action        = new HtmlProperty("action");
	public static final Property<String> _autocomplete  = new HtmlProperty("autocomplete");
	public static final Property<String> _enctype       = new HtmlProperty("enctype");
	public static final Property<String> _method        = new HtmlProperty("method");
	public static final Property<String> _name          = new HtmlProperty("name");
	public static final Property<String> _novalidate    = new HtmlProperty("novalidate");
	public static final Property<String> _target        = new HtmlProperty("target");

//	public static final EndNodes<Div>      divParents = new EndNodes<Div>("divParents", Div.class, RelType.CONTAINS, Direction.INCOMING, false);
//	public static final EndNodes<P>        pParents   = new EndNodes<P>("pParents", P.class, RelType.CONTAINS, Direction.INCOMING, false);
//	public static final EndNodes<Content>  contents   = new EndNodes<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Div>      divs       = new EndNodes<Div>("divs", Div.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Input>    inputs     = new EndNodes<Input>("inputs", Input.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Button>   buttons    = new EndNodes<Button>("buttons", Button.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Select>   selects    = new EndNodes<Select>("selects", Select.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Label>    labels     = new EndNodes<Label>("labels", Label.class, RelType.CONTAINS, Direction.OUTGOING, false);
//	public static final EndNodes<Textarea> textareas  = new EndNodes<Textarea>("textareas", Textarea.class, RelType.CONTAINS, Direction.OUTGOING, false);

	public static final View htmlView = new View(Form.class, PropertyView.Html,
	    _acceptCharset, _action, _autocomplete, _enctype, _method, _name, _novalidate, _target
	);

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
	*/
}
