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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.search.PropertySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A property that returns grouped properties from a set of {@link Reference} elements.
 *
 *
 */
public class ReferenceGroup extends Property<PropertyMap> implements PropertyGroup<PropertyMap> {

	private static final Logger logger = LoggerFactory.getLogger(ReferenceGroup.class.getName());

	// indicates whether this group property is
	protected Map<String, PropertyKey> propertyKeys    = new LinkedHashMap<>();
	protected Class<? extends GraphObject> entityClass = null;
	protected Property<Boolean> nullValuesOnlyProperty = null;

	public ReferenceGroup(String name, Class<? extends GraphObject> entityClass, Reference... properties) {

		super(name);

		for (PropertyKey key : properties) {
			propertyKeys.put(key.jsonName(), key);
		}

		this.nullValuesOnlyProperty = new BooleanProperty(name.concat(".").concat("nullValuesOnly"));
		this.entityClass            = entityClass;

		// register in entity context
		// FIXME StructrApp.getConfiguration().registerProperty(entityClass, nullValuesOnlyProperty);
		StructrApp.getConfiguration().registerPropertyGroup(entityClass, this, this);
	}

	// ----- interface PropertyGroup -----
	@Override
	public PropertyMap getGroupedProperties(SecurityContext securityContext, GraphObject source) {

		if(source instanceof AbstractRelationship) {

			AbstractRelationship rel = (AbstractRelationship)source;
			PropertyMap properties   = new PropertyMap();

			for (PropertyKey key : propertyKeys.values()) {

				Reference reference = (Reference)key;

				GraphObject referencedEntity = reference.getReferencedEntity(rel);
				PropertyKey referenceKey     = reference.getReferenceKey();
				PropertyKey propertyKey      = reference.getPropertyKey();

				if (referencedEntity != null) {

					properties.put(propertyKey, referencedEntity.getProperty(referenceKey));
				}
			}

			return properties;
		}

		return null;
	}

	@Override
	public void setGroupedProperties(SecurityContext securityContext, PropertyMap source, GraphObject destination) throws FrameworkException {

		if(destination instanceof AbstractRelationship) {

			AbstractRelationship rel = (AbstractRelationship)destination;

			for (PropertyKey key : propertyKeys.values()) {

				Reference reference = (Reference)key;

				GraphObject referencedEntity = reference.getReferencedEntity(rel);
				PropertyKey referenceKey     = reference.getReferenceKey();
				PropertyKey propertyKey      = reference.getPropertyKey();

				if (referencedEntity != null && !reference.isReadOnly()) {

					Object value = source.get(propertyKey);
					referencedEntity.setProperty(referenceKey, value);
				}
			}
		}
	}

	@Override
	public String typeName() {
		return "Object";
	}

	@Override
	public Class valueType() {
		return null;
	}

	@Override
	public PropertyConverter<PropertyMap, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<PropertyMap, ?> databaseConverter(SecurityContext securityContext, GraphObject currentObject) {
		return null;
	}

	@Override
	public PropertyConverter<Map<String, Object>, PropertyMap> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, PropertyMap searchValues, boolean exactMatch, final Query query) {

		SearchAttributeGroup group = new SearchAttributeGroup(occur);

		for (PropertyKey key : propertyKeys.values()) {

			Object value = searchValues.get(new GenericProperty(key.jsonName()));
			if (value != null) {

				group.add(new PropertySearchAttribute(key, value.toString(), Occurrence.REQUIRED, exactMatch));
			}
		}

		return group;
	}

	/**
	 * Returns the nested group property for the given name. The PropertyKey returned by
	 * this method can be used to get and/or set the property value in a PropertyMap that
	 * is obtained or stored in the group property.
	 *
	 * @param <T>
	 * @param name
	 * @param type
	 * @return property
	 */
	public <T> PropertyKey<T> getNestedProperty(String name, Class<T> type) {

		if (!propertyKeys.containsKey(name)) {
			throw new IllegalArgumentException("ReferenceGroup " + dbName + " does not contain grouped property " + name + "!");
		}

		return propertyKeys.get(name);
	}

	/**
	 * Returns a wrapped group property that can be used to access a nested group
	 * property directly, i.e. without having to fetch the group first.
	 *
	 * @param <T>
	 * @param name
	 * @param type
	 * @return property
	 */
	public <T> PropertyKey<T> getDirectAccessReferenceGroup(String name, Class<T> type) {

		if (!propertyKeys.containsKey(name)) {
			throw new IllegalArgumentException("ReferenceGroup " + dbName + " does not contain grouped property " + name + "!");
		}

		return new GenericProperty(propertyKeys.get(name).dbName());
	}

	private class InputConverter extends PropertyConverter<Map<String, Object>, PropertyMap> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Map<String, Object> revert(PropertyMap source) throws FrameworkException {
			return PropertyMap.javaTypeToInputType(securityContext, entityClass, source);
		}

		@Override
		public PropertyMap convert(Map<String, Object> source) throws FrameworkException {
			return PropertyMap.inputTypeToJavaType(securityContext, entityClass, source);
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public PropertyMap getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public PropertyMap getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {
		return getGroupedProperties(securityContext, obj);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, PropertyMap value) throws FrameworkException {

		setGroupedProperties(securityContext, value, obj);
		return null;
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public void setDeclaringClass(Class declaringClass) {

		for (PropertyKey key : propertyKeys.values()) {

			key.setDeclaringClass(declaringClass);
		}
	}

	@Override
	public void extractSearchableAttribute(SecurityContext securityContext, HttpServletRequest request, final boolean exactMatch, final Query query) throws FrameworkException {

		for (PropertyKey key : propertyKeys.values()) {

			key.extractSearchableAttribute(securityContext, request, exactMatch, query);
		}
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(java.lang.String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {
		return Collections.EMPTY_MAP;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {
		return Collections.EMPTY_MAP;
	}
}
