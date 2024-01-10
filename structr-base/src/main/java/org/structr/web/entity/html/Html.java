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

public interface Html extends DOMElement {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Html");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Html"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("html");

		type.addStringProperty("_html_manifest",   PropertyView.Html, PropertyView.Ui);
		type.addStringProperty("customOpeningTag", PropertyView.Ui);

		type.overrideMethod("getHtmlAttributes", false, DOMElement.GET_HTML_ATTRIBUTES_CALL);
		type.overrideMethod("openingTag", false,
			"final String custTag = getProperty(customOpeningTagProperty);\n" +
			"if (custTag != null) {\n" +
			"	arg0.append(custTag);\n" +
			"} else {\n" +
			"	super.openingTag(arg0, arg1, arg2, arg3, arg4);\n" +
			"}"
		);
	}}

	/*
	public static final Property<String> _manifest = new HtmlProperty("manifest");

	/** If set, the custom opening tag is rendered instead of just <html> to allow things like IE conditional comments:
	 *
	 * <!--[if lt IE 7]>      <html class="no-js ie8 ie7 ie6"> <![endif]-->
	 * <!--[if IE 7]>         <html class="no-js ie8 ie7"> <![endif]-->
	 * <!--[if IE 8]>         <html class="no-js ie8"> <![endif]-->
	 * <!--[if gt IE 8]><!--> <html class="no-js ie9"> <!--<![endif]-->
	 *
	public static final Property<String> _customOpeningTag = new StringProperty("customOpeningTag");

	//public static final Property<Head> head = new EndNode<>("head", HtmlHead.class);
	//public static final Property<Body> body = new EndNode<>("body", HtmlBody.class);

	public static final View htmlView = new View(Html.class, PropertyView.Html,
		_manifest
	);

	public static final View uiView = new View(Html.class, PropertyView.Ui,
		_manifest, _customOpeningTag
	);

	@Override
	public void openingTag(final AsyncBuffer out, final String tag, final RenderContext.EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {

		String custTag = getProperty(_customOpeningTag);

		if (custTag != null) {

			out.append(custTag);

		} else {

			super.openingTag(out, tag, editMode, renderContext, depth);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}
	*/
}
