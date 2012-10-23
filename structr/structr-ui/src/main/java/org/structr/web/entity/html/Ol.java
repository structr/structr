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
import org.neo4j.graphdb.Direction;
import org.structr.common.Property;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass;
import org.structr.web.common.HtmlProperty;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Ol extends HtmlElement {

	public static final Property<String> _reversed = new HtmlProperty("reversed");
	public static final Property<String> _start    = new HtmlProperty("start");
	
	public static final View htmlView = new View(Ol.class, PropertyView.Html,
	    _reversed, _start
	);

	//~--- static initializers --------------------------------------------

	static {

//		EntityContext.registerPropertySet(Ol.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Ol.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Ol.class, PropertyView.Html, PropertyView.Html, htmlAttributes);

		EntityContext.registerEntityRelation(Ol.class, Li.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);

	}

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

}
