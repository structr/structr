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
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.DOMElement;

import java.net.URI;

public interface Link extends LinkSource {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Link");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Link"));
		type.setExtends(URI.create("#/definitions/LinkSource"));
		type.setCategory("html");

		type.addStringProperty("_html_href",     PropertyView.Html);
		type.addStringProperty("_html_rel",      PropertyView.Html);
		type.addStringProperty("_html_media",    PropertyView.Html);
		type.addStringProperty("_html_hreflang", PropertyView.Html);
		type.addStringProperty("_html_type",     PropertyView.Html);
		type.addStringProperty("_html_sizes",    PropertyView.Html);

		type.overrideMethod("isVoidElement",     false, "return true;");
		type.overrideMethod("getHtmlAttributes", false, DOMElement.GET_HTML_ATTRIBUTES_CALL);
	}}

	/*
	public static final Property<String> _href     = new HtmlProperty("href");
	public static final Property<String> _rel      = new HtmlProperty("rel");
	public static final Property<String> _media    = new HtmlProperty("media");
	public static final Property<String> _hreflang = new HtmlProperty("hreflang");
	public static final Property<String> _type     = new HtmlProperty("type");
	public static final Property<String> _sizes    = new HtmlProperty("sizes");

//	public static final EndNodes<Head> heads      = new EndNodes<Head>("heads", Head.class, RelType.CONTAINS, Direction.INCOMING, false);

	public static final Property<Linkable> linkable   = new EndNode<>("linkable", ResourceLink.class, new PropertyNotion(AbstractNode.name));
	public static final Property<String>   linkableId = new EntityIdProperty("linkableId", linkable);

	public static final View uiView = new View(Link.class, PropertyView.Ui,
		linkableId, linkable
	);

	public static final View htmlView = new View(Link.class, PropertyView.Html,
		_href, _rel, _media, _hreflang, _type, _sizes
	);

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

	@Override
	public boolean isVoidElement() {

		return true;

	}
	*/
}
