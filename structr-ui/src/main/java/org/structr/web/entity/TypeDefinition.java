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
package org.structr.web.entity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class TypeDefinition extends AbstractNode {
	
	public static final CollectionProperty<PropertyDefinition> properties   = new CollectionProperty<PropertyDefinition>("properties", PropertyDefinition.class, RelType.DEFINES_PROPERTY, Direction.OUTGOING, false);
	public static final CollectionProperty<DataNode>           dataNodes    = new CollectionProperty<DataNode>("dataNodes", DataNode.class, RelType.DEFINES_TYPE, Direction.OUTGOING, false);
	
	public static final Property<String>                       kind         = new StringProperty("kind");
	
	public static final org.structr.common.View publicView = new org.structr.common.View(TypeDefinition.class, PropertyView.Public,
	    kind, properties
	);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(TypeDefinition.class, PropertyView.Public,
	    kind, properties
	);
	
	// ----- private members -----
	private Map<String, PropertyDefinition> propertyMap = new LinkedHashMap<String, PropertyDefinition>();
	
	public PropertyDefinition getPropertyDefinition(String name) {
		return propertyMap.get(name);
	}
	
	public Collection<PropertyDefinition> getPropertyDefinitions() {
		return propertyMap.values();
	}
	
	// ----- overridden methods from superclass -----
	@Override
	public void onNodeInstantiation() {
		initializePropertyDefinitions();
	}
	
	// ----- private methods -----
	private void initializePropertyDefinitions() {
		
		if (propertyMap.isEmpty()) {
			
			for (PropertyDefinition propertyDefinition : getProperty(properties)) {
				
				propertyMap.put(propertyDefinition.getProperty(PropertyDefinition.name), propertyDefinition);
			}
		}
	}
}
