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
public class Base extends HtmlElement {

	public static final Property<String> _href   = new HtmlProperty("href");
	public static final Property<String> _target = new HtmlProperty("target");

	public static final View htmlView = new View(Base.class, PropertyView.Html,
		_href, _target
	);

//	//~--- static initializers --------------------------------------------
//
//	static {
//
//		EntityContext.registerPropertySet(Base.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Base.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Base.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
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
