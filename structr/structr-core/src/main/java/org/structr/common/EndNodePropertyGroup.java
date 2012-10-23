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

import org.structr.common.property.PropertyKey;
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
public class EndNodePropertyGroup implements PropertyGroup<Map<String, Object>> {

	private PropertyKey[] keys = null;

	public EndNodePropertyGroup(PropertyKey... keys) {
		this.keys = keys;
	}

	@Override
	public Map<String, Object> getGroupedProperties(GraphObject source) {

		if(source instanceof AbstractRelationship) {

			Map<String, Object> props = new LinkedHashMap<String, Object>();
			AbstractRelationship rel  = (AbstractRelationship)source;
			AbstractNode endNode      = rel.getEndNode();

			for(PropertyKey key : keys) {
				props.put(key.name(), endNode.getProperty(key));
			}

			return props;
		}

		return null;
	}

	@Override
	public void setGroupedProperties(Map<String, Object> source, GraphObject destination) throws FrameworkException {
	}
}
