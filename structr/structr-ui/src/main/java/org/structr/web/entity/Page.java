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



package org.structr.web.entity;

import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.node.NodeService;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.entity.Principal;
import org.structr.web.entity.html.Html;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a page resource
 *
 * @author axel
 */
public class Page extends Linkable {

	public enum UiKey implements PropertyKey {
		name, tag, components, elements, linkingElements, contentType, ownerId, position
	}

	static {
		EntityContext.registerPropertyRelation(AbstractNode.class, UiKey.ownerId, Principal.class, RelType.OWNS, Direction.INCOMING, RelationClass.Cardinality.ManyToOne, new PropertyNotion(AbstractNode.Key.uuid));
                
		EntityContext.registerPropertySet(Page.class,	PropertyView.All,	UiKey.values());
		EntityContext.registerPropertySet(Page.class,	PropertyView.Public,	UiKey.values());
		EntityContext.registerPropertySet(Page.class,	PropertyView.Ui,	UiKey.values());
		
		EntityContext.registerEntityRelation(Page.class,	Component.class,	RelType.CONTAINS,	Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Page.class,	Element.class,		RelType.CONTAINS,	Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Page.class,	Content.class,		RelType.CONTAINS,	Direction.OUTGOING, Cardinality.ManyToMany);
//		EntityContext.registerEntityRelation(Page.class,	Element.class,		RelType.LINK,		Direction.INCOMING, Cardinality.OneToMany);

		EntityContext.registerEntityRelation(Page.class, Html.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne);

		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.fulltext.name(), Element.UiKey.values());
		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.keyword.name(), Element.UiKey.values());
	}

	//~--- get methods ----------------------------------------------------

}
