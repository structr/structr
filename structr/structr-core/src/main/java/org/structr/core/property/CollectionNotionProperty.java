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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.property.Property;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */


public class CollectionNotionProperty<T> extends Property<List<T>> {
	
	private static final Logger logger = Logger.getLogger(CollectionIdProperty.class.getName());
	
	private CollectionProperty base = null;
	private Notion<T> notion   = null;
	
	public CollectionNotionProperty(CollectionProperty base, Notion<T> notion) {
		this(null, base, notion);
	}
	
	public CollectionNotionProperty(String name, CollectionProperty base, Notion<T> notion) {
		
		super(name);
		
		this.notion = notion;
		this.base   = base;
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
	public PropertyConverter<List<T>, ?> databaseConverter(SecurityContext securityContext, GraphObject entitiy) {
		return null;
	}

	@Override
	public PropertyConverter<?, List<T>> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		try {
			
			return (notion.getCollectionAdapterForGetter(securityContext).adapt(base.getProperty(securityContext, obj, applyConverter)));
			
		} catch (FrameworkException fex) {
			
			logger.log(Level.WARNING, "Unable to apply notion of type {0} to property {1}", new Object[] { notion.getClass(), this } );
		}
		
		return null;
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, List<T> value) throws FrameworkException {
		base.setProperty(securityContext, obj, notion.getCollectionAdapterForSetter(securityContext).adapt(value));
	}
}
