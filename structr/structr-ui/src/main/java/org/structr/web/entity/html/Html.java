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
import org.structr.web.entity.Page;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Html extends HtmlElement {

	public static final Property<String> _manifest = new HtmlProperty("manifest");

	public static final View htmlView = new View(Html.class, PropertyView.Html,
		_manifest
	);
	
	//~--- static initializers --------------------------------------------

	static {

//		EntityContext.registerPropertySet(Html.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Html.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Html.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		
		EntityContext.registerEntityRelation(Html.class, Page.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Html.class, Head.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Html.class, Body.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Property[] getHtmlAttributes() {

		return (Property[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlView.properties());

	}

}
