/*
 *  Copyright (C) 2011 Axel Morgner
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



package org.structr.web.entity;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.entity.RelationClass.Cardinality;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a web element
 *
 * @author axel
 */
public class Element extends AbstractNode {

	private static final Logger logger = Logger.getLogger(Element.class.getName());

	protected static final String[] uiAttributes = {
		UiKey.name.name(), UiKey.tag.name(), UiKey.contents.name(), UiKey.elements.name(), UiKey.components.name(), UiKey.resource.name()
	};

	static {

		EntityContext.registerPropertySet(Element.class, PropertyView.All, uiAttributes);
		EntityContext.registerPropertySet(Element.class, PropertyView.Public, uiAttributes);
		EntityContext.registerPropertySet(Element.class, PropertyView.Ui, uiAttributes);
		EntityContext.registerEntityRelation(Element.class, Component.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Resource.class, RelType.CONTAINS, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Element.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);

		EntityContext.registerSearchablePropertySet(Element.class, NodeIndex.fulltext.name(), uiAttributes);
		EntityContext.registerSearchablePropertySet(Element.class, NodeIndex.keyword.name(), uiAttributes);

//              EntityContext.registerEntityRelation(Element.class,     Resource.class,         RelType.LINK,           Direction.OUTGOING, Cardinality.ManyToOne);

	}
	
	//~--- constant enums -------------------------------------------------

	public enum UiKey implements PropertyKey {
		name, tag, contents, elements, components, resource
	}


	
	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "";
	}
}
