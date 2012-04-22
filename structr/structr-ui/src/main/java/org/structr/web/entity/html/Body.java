/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity.html;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Body extends HtmlElement {

	static {

		EntityContext.registerPropertySet(Body.class, PropertyView.All, HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(Body.class, PropertyView.Public, HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(Body.class, PropertyView.Html, PropertyView.Html,	HtmlElement.htmlAttributes);
		
		EntityContext.registerEntityRelation(Body.class, Html.class,		RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);

		EntityContext.registerEntityRelation(Body.class, Div.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Body.class, P.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Body.class, Span.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Body.class, Table.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Body.class, Header.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Body.class, Footer.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Body.class, Article.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Body.class, Blockquote.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);

	}
}
