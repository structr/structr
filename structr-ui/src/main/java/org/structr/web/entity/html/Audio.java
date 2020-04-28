/**
 * Copyright (C) 2010-2020 Structr GmbH
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
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.web.entity.dom.DOMElement;

/**
 *
 */
public interface Audio extends DOMElement {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Audio");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Audio"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");

		type.addStringProperty("_html_src",         PropertyView.Html);
		type.addStringProperty("_html_crossorigin", PropertyView.Html);
		type.addStringProperty("_html_preload",     PropertyView.Html);
		type.addStringProperty("_html_autoplay",    PropertyView.Html);
		type.addStringProperty("_html_mediagroup",  PropertyView.Html);
		type.addStringProperty("_html_loop",        PropertyView.Html);
		type.addStringProperty("_html_muted",       PropertyView.Html);
		type.addStringProperty("_html_controls",    PropertyView.Html);
	}}

	/*
	public static final Property<String> _src		= new HtmlProperty("src");
	public static final Property<String> _crossorigin	= new HtmlProperty("crossorigin");
	public static final Property<String> _preload		= new HtmlProperty("preload");
	public static final Property<String> _autoplay		= new HtmlProperty("autoplay");
	public static final Property<String> _mediagroup	= new HtmlProperty("mediagroup");
	public static final Property<String> _loop		= new HtmlProperty("loop");
	public static final Property<String> _muted		= new HtmlProperty("muted");
	public static final Property<String> _controls		= new HtmlProperty("controls");

	public static final org.structr.common.View htmlView	= new org.structr.common.View(Audio.class, PropertyView.Html,
		_src, _crossorigin, _preload, _autoplay, _mediagroup, _loop, _muted, _controls
	);
	*/
}
