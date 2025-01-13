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

import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.DOMElement;

public interface Link extends DOMElement, LinkSource {

	/*
	public static final Property<String> _href     = new HtmlProperty("href");
	public static final Property<String> _rel      = new HtmlProperty("rel");
	public static final Property<String> _media    = new HtmlProperty("media");
	public static final Property<String> _hreflang = new HtmlProperty("hreflang");
	public static final Property<String> _type     = new HtmlProperty("type");
	public static final Property<String> _sizes    = new HtmlProperty("sizes");

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
