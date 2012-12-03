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
import java.util.Set;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchOperator;
import org.structr.core.graph.search.TextualSearchAttribute;

/**
 *
 * @author Christian Morgner
 */
public abstract class Property<JavaType> implements PropertyKey<JavaType> {

	private static final Logger logger = Logger.getLogger(Property.class.getName());

	protected Class<? extends GraphObject> declaringClass  = null;
	protected JavaType defaultValue                        = null;
	protected boolean isReadOnlyProperty                   = false;
	protected boolean isWriteOnceProperty                  = false;
	protected boolean isSystemProperty                     = false;
	protected String dbName                                = null;
	protected String jsonName                              = null;
	
	protected Property(String name) {
		this(name, name);
	}
	
	protected Property(String jsonName, String dbName) {
		this(jsonName, dbName, null);
	}
	
	protected Property(String jsonName, String dbName, JavaType defaultValue) {
		this.defaultValue = defaultValue;
		this.jsonName = jsonName;
		this.dbName = dbName;
	}
	
	public abstract Object fixDatabaseProperty(Object value);
	
	public Property<JavaType> systemProperty() {
		this.isSystemProperty = true;
		return this;
	}
	
	public Property<JavaType> readOnly() {
		this.isReadOnlyProperty = true;
		return this;
	}
	
	public Property<JavaType> writeOnce() {
		this.isWriteOnceProperty = true;
		return this;
	}

	@Override
	public void setDeclaringClass(Class<? extends GraphObject> declaringClass) {
		this.declaringClass = declaringClass;
	}
	
	@Override
	public Class<? extends GraphObject> getDeclaringClass() {
		return declaringClass;
	}

	
	@Override
	public String toString() {
		return "(".concat(jsonName()).concat("|").concat(dbName()).concat(")");
	}
	
	@Override
	public String dbName() {
		return dbName;
	}
	
	@Override
	public String jsonName() {
		return jsonName;
	}
	
	@Override
	public JavaType defaultValue() {
		return defaultValue;
	}
	
	@Override
	public int hashCode() {
		
		// make hashCode funtion work for subtypes that override jsonName() etc. as well
		if (dbName() != null && jsonName() != null) {
			return (dbName().hashCode() * 31) + jsonName().hashCode();
		}
		
		if (dbName() != null) {
			return dbName().hashCode();
		}
		
		if (jsonName() != null) {
			return jsonName().hashCode();
		}
		
		// TODO: check if it's ok if null key is not unique
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o instanceof PropertyKey) {
		
			return o.hashCode() == hashCode();
		}
		
		return false;
	}
	
	@Override
	public Class<? extends GraphObject> relatedType() {
		return null;
	}
	
	@Override
	public boolean isSystemProperty() {
		return isSystemProperty;
	}

	@Override
	public boolean isReadOnlyProperty() {
		return isReadOnlyProperty;
	}
	
	@Override
	public boolean isWriteOnceProperty() {
		return isWriteOnceProperty;
	}

	@Override
	public boolean isCollection() {
		return false;
	}
	
	@Override
	public SearchAttribute getSearchAttribute(SearchOperator op, JavaType searchValue, boolean exactMatch) {
		
		// return empty string on null value here to enable searching for empty values
		String searchString = searchValue != null ? searchValue.toString() : "";
		String search       = exactMatch ? Search.exactMatch(searchString) : searchString;
		
		return new TextualSearchAttribute(this, search, op);
	}

	@Override
	public void registerSearchableProperties(Set<PropertyKey> searchableProperties) {
		searchableProperties.add(this);
	}
}
