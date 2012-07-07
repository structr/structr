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

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.notion.PropertyNotion;
import org.structr.web.entity.Content;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class A extends HtmlElement {

	private static final String[] htmlAttributes = new String[] {

		"href", "target", "ping", "rel", "media", "hreflang", "type"
	};

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(A.class, PropertyView.Ui, UiKey.values());
		EntityContext.registerPropertySet(A.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		EntityContext.registerEntityRelation(A.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Span.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Img.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Div.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Section.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, P.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H1.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H2.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H3.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H4.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H5.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, H6.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Div.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, P.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(A.class, Li.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);

//              EntityContext.registerPropertyRelation(A.class, "_html_href", Resource.class, RelType.LINK, Direction.OUTGOING, Cardinality.ManyToOne, new PropertyNotion(AbstractNode.Key.name), RelationClass.DELETE_NONE);
		EntityContext.registerEntityRelation(A.class, Linkable.class, RelType.LINK, Direction.OUTGOING, Cardinality.ManyToOne, new PropertyNotion(AbstractNode.Key.name),
			RelationClass.DELETE_NONE);
		EntityContext.registerPropertyRelation(A.class, UiKey.linkable_id, Linkable.class, RelType.LINK, Direction.OUTGOING, Cardinality.ManyToOne, new PropertyNotion(AbstractNode.Key.uuid),
			RelationClass.DELETE_NONE);

		//EntityContext.registerPropertyRelation(Linkable.class, Linkable.Key.linkingElements, A.class, RelType.LINK, Direction.INCOMING, RelationClass.Cardinality.OneToMany, new PropertyNotion(AbstractNode.Key.uuid));

	}

	//~--- constant enums -------------------------------------------------

	public enum UiKey implements PropertyKey{ linkable, linkable_id }

	//~--- methods --------------------------------------------------------

	@Override
	public boolean avoidWhitespace() {

		return true;

	}

	;

}
