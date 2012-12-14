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

import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Linkable;
import org.structr.core.graph.NodeService;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityProperty;
import org.structr.web.entity.html.Html;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a page resource
 *
 * @author axel
 */
public class Page extends AbstractNode implements Linkable {

	public static final Property<String>              tag             = new StringProperty("tag");
	public static final Property<String>              contentType     = new StringProperty("contentType");
	public static final Property<Integer>             position        = new IntProperty("position");
	public static final Property<Integer>             cacheForSeconds = new IntProperty("cacheForSeconds");
	public static final Property<Integer>             version         = new IntProperty("version").systemProperty().readOnly();

	public static final EntityProperty<Html>          html            = new EntityProperty<Html>("html", Html.class, RelType.CONTAINS, true);
	
	public static final CollectionProperty<Component> components      = new CollectionProperty<Component>("components", Component.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Element>   elements        = new CollectionProperty<Element>("elements", Element.class, RelType.CONTAINS, Direction.OUTGOING, false);
	public static final CollectionProperty<Content>   contents        = new CollectionProperty<Content>("contents", Content.class, RelType.CONTAINS, Direction.OUTGOING, false);

	public static final org.structr.common.View uiView = new org.structr.common.View(Page.class, PropertyView.Ui,
		name, tag, components, elements, linkingElements, contentType, ownerId, position, cacheForSeconds, version
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(Page.class, PropertyView.Public,
		name, tag, components, elements, linkingElements, contentType, ownerId, position, cacheForSeconds, version
	);
	
	
	static {
		
		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.keyword.name(),  uiView.properties());
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
