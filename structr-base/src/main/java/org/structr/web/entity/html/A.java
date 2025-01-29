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

import org.apache.commons.lang3.ArrayUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.DOMElement;

public class A extends DOMElement implements LinkSource {

	public static final Property<String> htmlHrefProperty     = new StringProperty("_html_href");
	public static final Property<String> htmlTargetProperty   = new StringProperty("_html_target");
	public static final Property<String> htmlPingProperty     = new StringProperty("_html_ping");
	public static final Property<String> htmlRelProperty      = new StringProperty("_html_rel");
	public static final Property<String> htmlMediaProperty    = new StringProperty("_html_media");
	public static final Property<String> htmlHrefLangProperty = new StringProperty("_html_hreflang");
	public static final Property<String> htmlTypeProperty     = new StringProperty("_html_type");

	public static final View htmlView = new View(A.class, PropertyView.Html,
		htmlHrefProperty, htmlTargetProperty, htmlPingProperty, htmlRelProperty, htmlMediaProperty, htmlHrefLangProperty, htmlTypeProperty
	);

	@Override
	public boolean avoidWhitespace() {
		return true;
	}

	@Override
	public Property[] getHtmlAttributes() {
		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());
	}
}