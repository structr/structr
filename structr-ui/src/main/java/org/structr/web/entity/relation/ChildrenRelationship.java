/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.relation;

import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.View;
import org.structr.core.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.IntProperty;
import org.structr.web.entity.dom.DOMNode;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class ChildrenRelationship extends AbstractRelationship {

	public static final Property<String>   parentId = new StringProperty("parentId");
	public static final Property<String>   childId  = new StringProperty("childId");
	public static final Property<Integer>  position = new IntProperty("position");

	public static final View uiView = new View(ChildrenRelationship.class, PropertyView.Ui,
		parentId, childId, position
	);
	
	static {

		EntityContext.registerNamedRelation("children", ChildrenRelationship.class, DOMNode.class, DOMNode.class, RelType.CONTAINS);
		
		EntityContext.registerSearchablePropertySet(ChildrenRelationship.class, PropertyView.Ui, uiView.properties());

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public PropertyKey getStartNodeIdKey() {
		return parentId;
	}

	@Override
	public PropertyKey getEndNodeIdKey() {
		return childId;
	}
}
