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

import org.structr.core.property.PropertyKey;
import org.structr.web.common.RelType;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class RenderTreeRelationship extends AbstractRelationship {

	public static final Property<String>       sourceNode = new StringProperty("sourceNode");
	public static final Property<String>       targetNode = new StringProperty("targetNode");
	public static final Property<List<String>> names    = new GenericProperty<List<String>>("names");
	
	static {

		EntityContext.registerNamedRelation("renderTree", RenderTreeRelationship.class, AbstractNode.class, AbstractNode.class, RelType.RENDER_TREE);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {

		Set<PropertyKey> keys = new LinkedHashSet<PropertyKey>();

		keys.add(sourceNode);
		keys.add(targetNode);
		keys.add(names);

		if (dbRelationship != null) {

			for (String key : dbRelationship.getPropertyKeys()) {

				keys.add(EntityContext.getPropertyKeyForDatabaseName(entityType, key));
			}

		}

		return keys;

	}

	@Override
	public PropertyKey getStartNodeIdKey() {

		return LinkRelationship.sourceId;

	}

	@Override
	public PropertyKey getEndNodeIdKey() {

		return LinkRelationship.targetId;

	}

}
