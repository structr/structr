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
import org.structr.core.graph.search.SearchAttribute;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

/**
 * Base interface for typed property keys.
 *
 *
 * @param <T>
 */
public interface PropertyKey<T> extends Comparable<PropertyKey> {

	/**
	 * Return the JSON name of this property.
	 *
	 * @return jsonName
	 */
	String jsonName();

	/**
	 * Returns the database name of this property.
	 *
	 * @return dbName
	 */
	String dbName();

	/**
	 * Sets the name of this property in the JSON context. This
	 * is the key under which the property will be found in the
	 * JSON input/output.
	 *
	 * @param jsonName
	 */
	void jsonName(final String jsonName);

	/**
	 * Sets the name of this property in the database context. This
	 * is the key under which the property will be stored in the
	 * database.
	 *
	 * @param dbName
	 */
	void dbName(final String dbName);

	/**
	 * Use this method to mark a property for indexing. This
	 * method registers the property in both the keyword and
	 * the fulltext index. To select the appropriate index
	 * for yourself, use the other indexed() methods.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	Property<T> indexed();

	/**
	 * Use this method to mark an indexed property to be created
	 * for nodes only.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	Property<T> nodeIndexOnly();

	/**
	 * Use this method to indicate that a property key can change its value
	 * without setProperty() being called directly on it. This method causes
	 * the given property to be indexed at the end of a transaction instead
	 * of immediately on setProperty(). This method registers the property
	 * in both the keyword and the fulltext index. To select the appropriate
	 * index for yourself, use the other indexed() methods.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	Property<T> passivelyIndexed();

	Property<T> indexedWhenEmpty();

	/**
	 * Returns the desired type name that will be used in the error message if a
	 * wrong type was provided.
	 * @return typeName
	 */
	String typeName();

	/**
	 * Returns the type of the value this property returns.
	 *
	 * @return the value type
	 */
	Class valueType();

	/**
	 * Returns the type of the related property this property key references, or
	 * null if this is not a relationship property.
	 *
	 * @return relatedType
	 */
	Class relatedType();

	/**
	 * Returns the format value for this property.
	 *
	 * @return format
	 */
	String format();

	/**
	 * Returns the type hint for this property.
	 *
	 * @return typeHint
	 */
	String typeHint();

	/**
	 * Returns the default value for this property.
	 *
	 * @return defaultValue
	 */
	T defaultValue();

	/**
	 * Returns the readFunction value for this property.
	 *
	 * @return readFunction
	 */
	String readFunction();

	/**
	 * Returns the writeFunction value for this property.
	 *
	 * @return writeFunction
	 */
	String writeFunction();

	/**
	 * Returns the cachingEnabled value for this property.
	 *
	 * @return cachingEnabled
	 */
	boolean cachingEnabled();

	/**
	 * Returns the openAPIReturnType value for this property.
	 *
	 * @return openAPIReturnType
	 */
	String openAPIReturnType();


	PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext);
	PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity);
	PropertyConverter<?, T> inputConverter(final SecurityContext securityContext);
	Object fixDatabaseProperty(final Object value);

	boolean requiresSynchronization();
	String getSynchronizationKey();

	void setDeclaringClass(final Class declaringClass);
	Class getDeclaringClass();
	String getSourceUuid();

	T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter);
	T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate);
	Object setProperty(final SecurityContext securityContext, final GraphObject obj, final T value) throws FrameworkException;

	void registrationCallback(final Class<GraphObject> entityType);

	/**
	 * Indicates whether this property is an unvalidated property or not.
	 * If a transaction contains only modifications AND those modifications
	 * affect unvalidated properties only, structr will NOT call
	 * afterModification callbacks. This can be used to avoid endless
	 * loops after a transaction. Just mark the property key that causes
	 * the loop as unvalidated.
	 *
	 * @return whether this property is unvalidated
	 */
	boolean isUnvalidated();

	/**
	 * Indicates whether this property is read-only. Read-only properties
	 * will throw a FrameworkException with error code 422 when the value
	 * is modified unless the action is unlocked before.
	 *
	 * @return isReadOnly
	 */
	boolean isReadOnly();

	/**
	 * Indicates whether this property is an system-internal property.
	 * System properties will throw a FrameworkException with error code 422
	 * when the value is modified unless the action is unlocked before.
	 *
	 * @return isSystemInternal
	 */
	boolean isSystemInternal();

	/**
	 * Indicates whether this property is write-once. Write-once properties
	 * will throw a FrameworkException with error code 422 when the value
	 * is modified after it has been initially set.
	 *
	 * @return isWriteOnce
	 */
	boolean isWriteOnce();

	/**
	 * Indicates whether this property is indexed, i.e. searchable using
	 * REST queries.
	 *
	 * @return isIndexed
	 */
	boolean isIndexed();

	/**
	 * Indicates whether this property is indexed on nodes only.
	 *
	 * @return isIndexed
	 */
	boolean isNodeIndexOnly();

	/**
	 * Indicates whether this property is indexed. The difference to the
	 * above method is, that the value for indexing will be obtained at
	 * the end of the transaction, so you can use this method to achieve
	 * indexing (and searchability) of properties that are never directly
	 * set using setProperty.
	 *
	 * @return isPassivelyIndexed
	 */
	boolean isPassivelyIndexed();

	/**
	 * Indicates whether this property is searchable with an empty value.
	 * This behaviour is achieved by storing a special value for empty
	 * fields which can then later be found again.
	 *
	 * @return isIndexedWhenEmpty
	 */
	boolean isIndexedWhenEmpty();

	/**
	 * Indicates whether this property represents a collection or a single
	 * value in the JSON output.
	 *
	 * @return isCollection
	 */
	boolean isCollection();

	/**
	 * Indicates whether the value associated with this property is
	 * validated for uniqueness.
	 *
	 * @return whether this property value is validated for uniqueness
	 */
	boolean isUnique();

	/**
	 * Indicates whether the value associated with this property is
	 * validated for uniqueness in a compound index.
	 *
	 * @return whether this property value is validated for uniqueness
	 */
	boolean isCompound();

	/**
	 * Indicates whether the value associated with this property is
	 * may not be null.
	 *
	 * @return whether this property value is validated for uniqueness
	 */
	boolean isNotNull();

	/**
	 * Indicates whether this property is created from a database node.
	 *
	 * @return whether this property is dynamic
	 */
	boolean isDynamic();

	/**
	 * Indicates whether this property is a part of the internal Structr schema.
	 *
	 * @return whether this property is a part of the internal Structr schema
	 */
	boolean isPartOfBuiltInSchema();

	/**
	 * Returns the lucene sort type of this property.
	 * @return sortType
	 */
	SortType getSortType();

	/**
	 * Returns the hint for the property (if any)
	 *
	 * @return property hint
	 */
	default String hint() {
		return null;
	}

	/**
	 * Returns the category for the property (if any)
	 *
	 * @return property category
	 */
	default String category() {
		return null;
	}

	Object getIndexValue(final Object value);
	boolean isPropertyTypeIndexable();
	boolean isPropertyValueIndexable(final Object value);

	SearchAttribute getSearchAttribute(final SecurityContext securityContext, final Occurrence occur, final T searchValue, final boolean exactMatch, final Query query);
	void extractSearchableAttribute(final SecurityContext securityContext, final HttpServletRequest request, final boolean exactMatch, final Query query) throws FrameworkException;
	T convertSearchValue(final SecurityContext securityContext, final String requestParameter) throws FrameworkException;

	/**
	 * Returns the desired position of this property key type
	 * in the processing order.
	 * @return processingOrderPosition
	 */
	int getProcessingOrderPosition();

	PropertyKey<T> defaultValue(final T defaultValue);
	PropertyKey<T> notNull(final boolean notNull);
	PropertyKey<T> unique(final boolean unique);
	PropertyKey<T> format(final String format);
	PropertyKey<T> typeHint(final String typeHint);
	PropertyKey<T> partOfBuiltInSchema();
	PropertyKey<T> dynamic();
	PropertyKey<T> readFunction(final String readFunction);
	PropertyKey<T> writeFunction(final String writeFunction);
	PropertyKey<T> cachingEnabled(final boolean enabled);
	PropertyKey<T> openAPIReturnType(final String openAPIReturnType);
	PropertyKey<T> transformators(final String... transformators);

	Comparator<GraphObject> sorted(final boolean descending);

	// ----- OpenAPI -----
	Object getExampleValue(final String type, final String viewName);
	Map<String, Object> describeOpenAPIOutputSchema(final String type, final String viewName);
	Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level);
	Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level);
}
