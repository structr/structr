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

/**
 * @author Axel Morgner
 */
public class P extends HtmlElement {

	static {
		EntityContext.registerPropertySet(P.class, PropertyView.All,	HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(P.class, PropertyView.Public,	HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(P.class, PropertyView.Html, PropertyView.Html,	HtmlElement.htmlAttributes);

		EntityContext.registerEntityRelation(P.class, P.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Content.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Label.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Textarea.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Input.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Address.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Footer.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Br.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, A.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Img.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Form.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Content.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);

		EntityContext.registerEntityRelation(P.class, Div.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Span.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, B.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Label.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Textarea.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Input.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Address.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Footer.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Hr.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, H1.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, H2.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, H3.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, H4.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, H5.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, H6.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Ul.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Ol.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(P.class, Script.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Code.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Strong.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Small.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, S.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Cite.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, G.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Dfn.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Abbr.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Time.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Var.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Samp.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Kbd.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Sub.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Sup.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, I.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, U.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Mark.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Ruby.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Rt.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Rp.class,		RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Bdi.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Bdo.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		
		EntityContext.registerEntityRelation(P.class, Wbr.class,	RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToMany);		

	}
}
