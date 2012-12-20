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
package org.structr.core.property;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.notion.Notion;

/**
* A property that wraps a {@see PropertyNotion} with the given notion around an {@see EntityProperty}.
 *
 * @author Christian Morgner
 */


public class EntityNotionProperty<S extends GraphObject, T> extends Property<T> {
	
	private static final Logger logger = Logger.getLogger(EntityIdProperty.class.getName());
	
	private Property<S> base    = null;
	private Notion<S, T> notion = null;
	
	public EntityNotionProperty(String name, Property<S> base, Notion<S, T> notion) {
		
		super(name);
		
		this.notion = notion;
		this.base   = base;
		
		notion.setType(base.relatedType());
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public String typeName() {
		return "";
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entitiy) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		try {
			return notion.getAdapterForGetter(securityContext).adapt(base.getProperty(securityContext, obj, applyConverter));
			
		} catch (FrameworkException fex) {
			
			logger.log(Level.WARNING, "Unable to apply notion of type {0} to property {1}", new Object[] { notion.getClass(), this } );
		}
		
		return null;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		
		if (value != null) {
	
			base.setProperty(securityContext, obj, notion.getAdapterForSetter(securityContext).adapt(value));
			
		} else {
			
			base.setProperty(securityContext, obj, null);
		}
	}

	@Override
	public Class relatedType() {
		return base.relatedType();
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
