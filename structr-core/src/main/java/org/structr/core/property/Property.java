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
package org.structr.core.property;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.BooleanClause.Occur;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.StringSearchAttribute;

/**
 * Abstract base class for all property types.
 *
 * @author Christian Morgner
 */
public abstract class Property<T> implements PropertyKey<T> {

	private static final Logger logger = Logger.getLogger(Property.class.getName());
	
	protected List<PropertyValidator<T>> validators        = new LinkedList<PropertyValidator<T>>();
	protected Class<? extends GraphObject> declaringClass  = null;
	protected T defaultValue                               = null;
	protected boolean readOnly                             = false;
	protected boolean writeOnce                            = false;
	protected boolean unvalidated                          = false;
	protected boolean indexed                              = false;
	protected String dbName                                = null;
	protected String jsonName                              = null;

	private boolean requiresSynchronization                = false;
	
	protected Property(String name) {
		this(name, name);
	}
	
	protected Property(String jsonName, String dbName) {
		this(jsonName, dbName, null);
	}
	
	protected Property(String jsonName, String dbName, T defaultValue) {
		this.defaultValue = defaultValue;
		this.jsonName = jsonName;
		this.dbName = dbName;
	}
	
	public abstract Object fixDatabaseProperty(Object value);

	public Property<T> unvalidated() {
		this.unvalidated = true;
		return this;
	}
	
	public Property<T> readOnly() {
		this.readOnly = true;
		return this;
	}
	
	public Property<T> writeOnce() {
		this.writeOnce = true;
		return this;
	}
	
	public Property<T> indexed() {
		this.indexed = true;
		return this;
	}

	@Override
	public void addValidator(PropertyValidator<T> validator) {
		
		validators.add(validator);
		
		// fetch synchronization requirement from validator
		if (validator.requiresSynchronization()) {
			this.requiresSynchronization = true;
		}
	}
	
	public Property<T> validator(PropertyValidator<T> validator) {
		addValidator(validator);
		return this;
	}

	@Override
	public List<PropertyValidator<T>> getValidators() {
		return validators;
	}

	@Override
	public boolean requiresSynchronization() {
		return requiresSynchronization;
	}
	
	@Override
	public String getSynchronizationKey() {
		return dbName;
	}
	
	@Override
	public void setDeclaringClass(Class<? extends GraphObject> declaringClass) {
		this.declaringClass = declaringClass;
	}
	
	@Override
	public void registrationCallback(Class type) {
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
	public T defaultValue() {
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
	public boolean isUnvalidated() {
		return unvalidated;
	}

	@Override
	public boolean isReadOnlyProperty() {
		return readOnly;
	}
	
	@Override
	public boolean isWriteOnceProperty() {
		return writeOnce;
	}
	
	@Override
	public boolean isIndexedProperty() {
		return indexed;
	}
	
	@Override
	public SearchAttribute getSearchAttribute(Occur occur, T searchValue, boolean exactMatch) {
		
		Object convertedValue = getSearchValue(searchValue);
		String value          = convertedValue != null ? convertedValue.toString() : "";
		
		
		return new StringSearchAttribute(this, value, occur, exactMatch);
	}

	@Override
	public void registerSearchableProperties(Set<PropertyKey> searchableProperties) {
		searchableProperties.add(this);
	}
	
	@Override
	public Object getSearchValue(T source) {
		
		PropertyConverter databaseConverter = databaseConverter(SecurityContext.getSuperUserInstance());
		Object convertedSearchValue         = source;

		if (databaseConverter != null) {

			try {
				convertedSearchValue = databaseConverter.convert(source);

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Unable to convert search value {0} for key {1}", new Object[] { source, this });
			}
		}
		
		return convertedSearchValue;
	}
}
