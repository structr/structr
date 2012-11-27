/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.common.property;

import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.converter.RelatedNodePropertyMapper;

/**
 *
 * @author Christian Morgner
 */
public class RelatedNodeProperty<T> extends Property<T> {
	
	private PropertyKey targetKey = null;
	private Class targetType = null;
	
	public RelatedNodeProperty(String name, Class targetType, PropertyKey<T> targetKey) {
		super(name);
		
		this.targetType = targetType;
		this.targetKey  = targetKey;
		
		// make us known to the entity context
		EntityContext.registerConvertedProperty(this);
	}
	
	@Override
	public String typeName() {
		return "FIXME: RelatedNodeProperty.java:49";
	}
	
	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject currentObject) {
		return new RelatedNodePropertyMapper(securityContext, currentObject, targetType, targetKey);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return targetKey.inputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
}
