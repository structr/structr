/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.html;

import org.structr.web.entity.dom.DOMElement;
import org.apache.commons.lang.ArrayUtils;

import org.neo4j.graphdb.Direction;
import org.structr.core.property.Property;

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.View;
import org.structr.core.property.CollectionProperty;
import org.structr.web.common.HtmlProperty;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Meta extends DOMElement {

	public static final Property<String> _name      = new HtmlProperty("name");
	public static final Property<String> _httpEquiv = new HtmlProperty("http-equiv");
	public static final Property<String> _content   = new HtmlProperty("content");
	public static final Property<String> _charset   = new HtmlProperty("charset");

	public static final CollectionProperty<Head> heads = new CollectionProperty<Head>("heads", Head.class, RelType.CONTAINS, Direction.INCOMING, false);

	public static final View htmlView = new View(Meta.class, PropertyView.Html,
		_name, _httpEquiv, _content, _charset
	);
	
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
