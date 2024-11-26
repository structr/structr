/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;


import jakarta.servlet.http.HttpServletRequest;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.search.SearchAttribute;

import java.util.Comparator;
import java.util.Map;

/**
 * Contains information about a related node property. This class can be used together
 * with {@link ReferenceGroup} to return a group of properties from both start and end
 * node of a relationship.
 *
 *
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
					return relationship.getSourceNode();

				case Relationship:
					return relationship;

				case EndNode:
					return relationship.getTargetNode();
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
	public void dbName(String dbName) {
		propertyKey.dbName(dbName);
	}

	@Override
	public void jsonName(String jsonName) {
		propertyKey.jsonName(jsonName);
	}

	@Override
	public String typeName() {
		return propertyKey.typeName();
	}

	@Override
	public Class valueType() {
		return propertyKey.valueType();
	}

	@Override
	public T defaultValue() {
		return propertyKey.defaultValue();
	}

	@Override
	public String format() {
		return propertyKey.format();
	}

	@Override
	public String readFunction() {
		return propertyKey.readFunction();
	}

	@Override
	public String writeFunction() {
		return propertyKey.writeFunction();
	}

	@Override
	public String openAPIReturnType() {
		return propertyKey.openAPIReturnType();
	}

	@Override
	public boolean cachingEnabled() { return propertyKey.cachingEnabled(); }

	@Override
	public SortType getSortType() {
		return propertyKey.getSortType();
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
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
	public Object fixDatabaseProperty(Object value) {
		return propertyKey.fixDatabaseProperty(value);
	}

	@Override
	public Class<? extends GraphObject> relatedType() {
		return propertyKey.relatedType();
	}

	@Override
	public boolean isUnvalidated() {
		return propertyKey.isUnvalidated();
	}

	@Override
	public boolean isSystemInternal() {
		return propertyKey.isSystemInternal();
	}

	@Override
	public boolean isReadOnly() {
		return propertyKey.isReadOnly();
	}

	@Override
	public boolean isWriteOnce() {
		return propertyKey.isWriteOnce();
	}

	@Override
	public boolean isIndexed() {
		return propertyKey.isIndexed();
	}

	@Override
	public boolean isNodeIndexOnly() {
		return propertyKey.isNodeIndexOnly();
	}

	@Override
	public boolean isPassivelyIndexed() {
		return propertyKey.isPassivelyIndexed();
	}

	@Override
	public boolean isIndexedWhenEmpty() {
		return propertyKey.isIndexedWhenEmpty();
	}

	@Override
	public boolean isCollection() {
		return propertyKey.isCollection();
	}

	@Override
	public void setDeclaringClass(Class declaringClass) {
	}

	@Override
	public Class<? extends GraphObject> getDeclaringTrait() {
		return propertyKey.getDeclaringTrait();
	}

	@Override
	public String getSourceUuid() {
		return propertyKey.getSourceUuid();
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {
		return propertyKey.getProperty(securityContext, obj, applyConverter);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		return propertyKey.setProperty(securityContext, obj, value);
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, T searchValue, boolean exactMatch, final Query query) {
		return propertyKey.getSearchAttribute(securityContext, occur, searchValue, exactMatch, query);
	}

	@Override
	public void registrationCallback(Class entityType) {
	}

	@Override
	public boolean requiresSynchronization() {
		return propertyKey.requiresSynchronization();
	}

	@Override
	public String getSynchronizationKey() {
		return propertyKey.getSynchronizationKey();
	}

	@Override
	public void extractSearchableAttribute(SecurityContext securityContext, HttpServletRequest request, final boolean exactMatch, final Query query) throws FrameworkException {
		propertyKey.extractSearchableAttribute(securityContext, request, exactMatch, query);
	}

	@Override
	public T convertSearchValue(SecurityContext securityContext, String requestParameter) throws FrameworkException {
		return propertyKey.convertSearchValue(securityContext, requestParameter);
	}

	@Override
	public Property<T> indexed() {
		return propertyKey.indexed();
	}

	@Override
	public Property<T> nodeIndexOnly() {
		return propertyKey.nodeIndexOnly();
	}

	@Override
	public Property<T> passivelyIndexed() {
		return propertyKey.passivelyIndexed();
	}

	@Override
	public Property<T> indexedWhenEmpty() {
		return propertyKey.indexedWhenEmpty();
	}

	@Override
	public int getProcessingOrderPosition() {
		return 0;
	}

	@Override
	public boolean isCompound() {
		return false;
	}

	@Override
	public boolean isUnique() {
		return false;
	}

	@Override
	public boolean isNotNull() {
		return false;
	}

	@Override
	public Property<T> defaultValue(final T defaultValue) {
		return null;
	}

	@Override
	public Property<T> format(final String format) {
		return null;
	}

	@Override
	public String typeHint() {
		return null;
	}

	@Override
	public PropertyKey<T> typeHint(String typeHint) {
		return null;
	}

	@Override
	public Property<T> readFunction(final String readFunction) {
		return null;
	}

	@Override
	public Property<T> writeFunction(final String writeFunction) {
		return null;
	}

	@Override
	public Property<T> openAPIReturnType(final String openAPIReturnType) {
		return null;
	}

	@Override
	public PropertyKey<T> cachingEnabled(boolean enabled) { return null; }

	@Override
	public Property<T> unique(final boolean unique) {
		return null;
	}

	@Override
	public Property<T> notNull(final boolean notNull) {
		return null;
	}

	@Override
	public boolean isDynamic() {
		return propertyKey.isDynamic();
	}

	@Override
	public PropertyKey<T> dynamic() {
		return propertyKey.dynamic();
	}

	@Override
	public int compareTo(PropertyKey o) {
		return propertyKey.compareTo(o);
	}

	@Override
	public boolean isPropertyTypeIndexable() {
		return propertyKey.isPropertyTypeIndexable();
	}

	@Override
	public boolean isPropertyValueIndexable(Object value) {
		return propertyKey.isPropertyValueIndexable(value);
	}

	@Override
	public PropertyKey<T> transformators(String... transformators) {
		return propertyKey.transformators(transformators);
	}

	@Override
	public boolean isPartOfBuiltInSchema() {
		return propertyKey.isPartOfBuiltInSchema();
	}

	@Override
	public PropertyKey<T> partOfBuiltInSchema() {
		return propertyKey.partOfBuiltInSchema();
	}

	@Override
	public Object getIndexValue(Object value) {
		return propertyKey.getIndexValue(value);
	}

	@Override
	public Comparator<GraphObject> sorted(final boolean descending) {
		return propertyKey.sorted(descending);
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return propertyKey.getExampleValue(type, viewName);
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {
		return propertyKey.describeOpenAPIOutputType(type, viewName, level);
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {
		return propertyKey.describeOpenAPIInputType(type, viewName, level);
	}
}
