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

import java.lang.reflect.ParameterizedType;
import org.structr.core.notion.Notion;

/**
 *
 * @author Axel Morgner
 */
public class RelationProperty<T> extends Property<T> {
	
	private Class remoteClass;
	private boolean isCollection;
	private Notion notion;

	public RelationProperty(final String name, final Class remoteClass, final boolean isCollection, final Notion notion) {
		this(name);
		this.remoteClass = remoteClass;
		this.isCollection = isCollection;
		this.notion = notion;
	}
	
	public RelationProperty(final String name) {
		this(name, name);
	}
	
	public RelationProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
	}
	
	@Override
	public String typeName() {
		ParameterizedType pType = (ParameterizedType) getClass().getGenericSuperclass();
		return pType.getRawType().toString();
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
	
	public Class getRemoteClass() {
		return remoteClass;
	}
	
	public boolean isCollection() {
		return isCollection;
	}
	
	public Notion getNotion() {
		return notion;
	}
}
