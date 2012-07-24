/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

package org.structr.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 *
 * @author Christian Morgner
 */
public class StartNodePropertyGroup implements PropertyGroup {

	private PropertyKey[] keys = null;

	public StartNodePropertyGroup(PropertyKey... keys) {
		this.keys = keys;
	}

	@Override
	public Object getGroupedProperties(GraphObject source) {

		if(source instanceof AbstractRelationship) {

			Map<String, Object> props = new LinkedHashMap<String, Object>();
			AbstractRelationship rel  = (AbstractRelationship)source;
			AbstractNode startNode    = rel.getStartNode();

			for(PropertyKey key : keys) {
				props.put(key.name(), startNode.getProperty(key));
			}

			return props;
		}

		return null;
	}

	@Override
	public void setGroupedProperties(Object source, GraphObject destination) throws FrameworkException {
	}
}
