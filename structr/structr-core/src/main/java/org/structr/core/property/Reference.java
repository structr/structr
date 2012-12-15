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

import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchOperator;

/**
 * Contains information about a related node property. This class can be used together
 * with {@see ReferenceGroup} to return a group of properties from both start and end
 * node of a relationship.
 * 
 * @author Christian Morgner
 */
public class Reference<T> implements PropertyKey<T> {

	public enum Key {
		StartNode, Relationship, EndNode
	}

	private PropertyKey<T> referenceKey = null;
	private PropertyKey<T> propertyKey  = null;
	private Key referenceType           = null;
	
	public Reference(PropertyKey propertyKey, Key referenceType, PropertyKey<T> referenceKey) {
		this.referenceType = referenceType;
		this.referenceKey = referenceKey;
		this.propertyKey = propertyKey;
	}
	
	public PropertyKey<T> getReferenceKey() {
		return referenceKey;
	}

	public PropertyKey<T> getPropertyKey() {
		return propertyKey;
	}

	public GraphObject getReferencedEntity(AbstractRelationship relationship) {
		
		if (relationship != null) {

			switch (referenceType) {

				case StartNode:
					return relationship.getStartNode();

				case Relationship:
					return relationship;

				case EndNode:
					return relationship.getEndNode();
			}
		}
		
		return null;
	}
	
	// interface PropertyKey
	@Override
	public String dbName() {
		return propertyKey.dbName();
	}

	@Override
	public String jsonName() {
		return propertyKey.jsonName();
	}
	
	@Override
	public String typeName() {
		return propertyKey.typeName();
	}

	@Override
	public T defaultValue() {
		return propertyKey.defaultValue();
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return propertyKey.databaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return propertyKey.inputConverter(securityContext);
	}

	@Override
	public Class<? extends GraphObject> relatedType() {
		return propertyKey.relatedType();
	}

	@Override
	public boolean isSystemProperty() {
		return propertyKey.isSystemProperty();
	}

	@Override
	public boolean isReadOnlyProperty() {
		return propertyKey.isReadOnlyProperty();
	}

	@Override
	public boolean isWriteOnceProperty() {
		return propertyKey.isWriteOnceProperty();
	}

	@Override
	public boolean isCollection() {
		return propertyKey.isCollection();
	}

	@Override
	public void setDeclaringClass(Class<? extends GraphObject> declaringClass) {
	}
	
	@Override
	public Class<? extends GraphObject> getDeclaringClass() {
		return propertyKey.getDeclaringClass();
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return propertyKey.getProperty(securityContext, obj, applyConverter);
	}

	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		propertyKey.setProperty(securityContext, obj, value);
	}

	@Override
	public SearchAttribute getSearchAttribute(SearchOperator op, T searchValue, boolean exactMatch) {
		return propertyKey.getSearchAttribute(op, searchValue, exactMatch);
	}

	@Override
	public void registerSearchableProperties(Set<PropertyKey> searchableProperties) {
		propertyKey.registerSearchableProperties(searchableProperties);
	}

	@Override
	public void registrationCallback(Class entityType) {
	}
}
