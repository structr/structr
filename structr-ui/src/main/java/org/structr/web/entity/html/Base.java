/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import org.structr.web.entity.dom.DOMElement;
import org.apache.commons.lang3.ArrayUtils;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.web.common.HtmlProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 */
public class Base extends DOMElement {

	public static final Property<String> _href   = new HtmlProperty("href");
	public static final Property<String> _target = new HtmlProperty("target");

	public static final View htmlView = new View(Base.class, PropertyView.Html,
		_href, _target
	);

//	//~--- static initializers --------------------------------------------
//
//	static {
//
//		StructrApp.getConfiguration().registerPropertySet(Base.class, PropertyView.All, HtmlElement.UiKey.values());
//		StructrApp.getConfiguration().registerPropertySet(Base.class, PropertyView.Public, HtmlElement.UiKey.values());
//		StructrApp.getConfiguration().registerPropertySet(Base.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
//
//	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

	@Override
	public boolean isVoidElement() {

		return true;

	}
}
