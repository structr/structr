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

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.entity.RelationClass;
import org.structr.core.notion.PropertyNotion;

//~--- classes ----------------------------------------------------------------

/**
 * @author Axel Morgner
 */
public class Link extends HtmlElement {

	private static final String[] htmlAttributes = new String[] {

		"href", "rel", "media", "hreflang", "type", "sizes"
	};

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(Link.class, PropertyView.All, HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(Link.class, PropertyView.Public, HtmlElement.UiKey.values());
		EntityContext.registerPropertySet(Link.class, PropertyView.Html, PropertyView.Html, htmlAttributes);
		EntityContext.registerEntityRelation(Link.class, Head.class, RelType.CONTAINS, Direction.INCOMING, RelationClass.Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Link.class, Linkable.class, RelType.LINK, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne, new PropertyNotion(AbstractNode.name),
			RelationClass.DELETE_NONE);
		EntityContext.registerPropertyRelation(Link.class, Link.UiKey.linkable_id, Linkable.class, RelType.LINK, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne,
			new PropertyNotion(AbstractNode.uuid), RelationClass.DELETE_NONE);

		//EntityContext.registerPropertyRelation(Linkable.class, Linkable.Key.linkingElements, Link.class, RelType.LINK, Direction.INCOMING, RelationClass.Cardinality.OneToMany, new PropertyNotion(AbstractNode.uuid));

	}

	//~--- constant enums -------------------------------------------------

	public enum UiKey implements PropertyKey{ linkable, linkable_id }

	//~--- get methods ----------------------------------------------------

	@Override
	public String[] getHtmlAttributes() {

		return (String[]) ArrayUtils.addAll(super.getHtmlAttributes(), htmlAttributes);

	}

	@Override
	public boolean isVoidElement() {

		return true;

	}

}
