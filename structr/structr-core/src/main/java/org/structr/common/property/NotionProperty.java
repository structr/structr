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
import java.util.Collection;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */
public class NotionProperty<T extends Collection<?>> extends Property<T> {
	
	private PropertyKey propertyKey = null;
	private Notion notion           = null;
	
	public NotionProperty(String name, PropertyKey<? extends Collection<? extends GraphObject>> propertyKey, Notion notion) {
		super(name);
		
		this.propertyKey = propertyKey;
		this.notion = notion;
		
		// make us known to the entity context
		EntityContext.registerConvertedProperty(this);
	}
	
	@Override
	public String typeName() {
		return propertyKey.typeName();
	}
	
	@Override
	public PropertyConverter<T, Object> databaseConverter(SecurityContext securityContext, GraphObject currentObject) {
		return new NotionConverter(securityContext, currentObject);
	}

	private class NotionConverter extends PropertyConverter<T, Object> {

		public NotionConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public T revert(Object source) throws FrameworkException {
			
			PropertyConverter<Collection<GraphObject>, Object> converter = propertyKey.databaseConverter(securityContext, currentObject);
			Adapter<Collection<GraphObject>, T> adapter                  = notion.getCollectionAdapterForGetter(securityContext);
			Collection<GraphObject> collection                           = converter.revert(source);
			
			return adapter.adapt(collection);
		}

		@Override
		public Object convert(T source) throws FrameworkException {
			return null;
		}
	}
}
