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
public class Footer extends HtmlElement {

	static {
//
//		EntityContext.registerPropertySet(Footer.class, PropertyView.All, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Footer.class, PropertyView.Public, HtmlElement.UiKey.values());
//		EntityContext.registerPropertySet(Footer.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		
		EntityContext.registerEntityRelation(Footer.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Div.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Span.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, B.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, P.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Label.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Textarea.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Input.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Address.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Br.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Hr.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, A.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, H1.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, H2.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, H3.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, H4.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, H5.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, H6.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Img.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Ul.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Ol.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Form.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Script.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Footer.class, Table.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);

	}

}
