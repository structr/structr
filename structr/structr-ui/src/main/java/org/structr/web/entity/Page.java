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

import java.util.List;
import org.neo4j.graphdb.Direction;

import org.structr.common.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.common.property.GenericProperty;
import org.structr.common.property.IntProperty;
import org.structr.common.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.entity.Principal;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.graph.NodeService;
import org.structr.core.notion.PropertyNotion;
import org.structr.web.entity.html.Html;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a page resource
 *
 * @author axel
 */
public class Page extends AbstractNode implements Linkable {

//	

	public static final Property<List<Component>> components      = new GenericProperty<List<Component>>("components");
	public static final Property<List<Element>>   elements        = new GenericProperty<List<Element>>("elements");
	public static final Property<String>          tag             = new StringProperty("tag");
	public static final Property<String>          contentType     = new StringProperty("contentType");
	public static final Property<Integer>         position        = new IntProperty("position");
	public static final Property<Integer>         cacheForSeconds = new IntProperty("cacheForSeconds");
	public static final Property<Integer>         version         = new IntProperty("version").systemProperty().readOnly();

	public static final org.structr.common.View uiView = new org.structr.common.View(Page.class, PropertyView.Ui,
		name, tag, components, elements, linkingElements, contentType, ownerId, position, cacheForSeconds, version
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(Page.class, PropertyView.Public,
		name, tag, components, elements, linkingElements, contentType, ownerId, position, cacheForSeconds, version
	);
	
	
	static {

//		EntityContext.registerPropertySet(Page.class, PropertyView.All, UiKey.values());
//		EntityContext.registerPropertySet(Page.class, PropertyView.Public, UiKey.values());
//		EntityContext.registerPropertySet(Page.class, PropertyView.Ui, UiKey.values());

		EntityContext.registerPropertyRelation(AbstractNode.class, ownerId, Principal.class, RelType.OWNS, Direction.INCOMING, RelationClass.Cardinality.ManyToOne, new PropertyNotion(AbstractNode.uuid));

		EntityContext.registerEntityRelation(Page.class, Component.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Page.class, Element.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Page.class, Content.class, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany);
		EntityContext.registerEntityRelation(Page.class, Html.class, RelType.CONTAINS, Direction.OUTGOING, RelationClass.Cardinality.ManyToOne);
		
		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.keyword.name(),  uiView.properties());
		
//		EntityContext.registerReadOnlyProperty(Page.class, version);

	}

	//~--- constant enums -------------------------------------------------
	
	public void increaseVersion() throws FrameworkException {
		
		Integer version = getIntProperty(Page.version);
		
		if (version == null) {
			setProperty(Page.version, 1);
		} else {
			setProperty(Page.version, version+1);
		}
		
	}

}
