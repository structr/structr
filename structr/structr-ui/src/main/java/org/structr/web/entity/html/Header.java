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

import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.RelationClass;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Header extends HtmlElement {

	static {

//		EntityContext.registerPropertySet(Header.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Header.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Header.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		
		EntityContext.registerEntityRelation(Header.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Div.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Span.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, B.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, P.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Label.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Textarea.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Input.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Address.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Footer.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Br.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Hr.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, A.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, H1.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, H2.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, H3.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, H4.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, H5.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, H6.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Img.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Ul.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Ol.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Form.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Script.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Header.class, Table.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);

	}

}
