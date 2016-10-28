/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import javax.servlet.http.HttpServletRequest;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.SearchAttribute;

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
	public String jsonName();

	/**
	 * Returns the database name of this property.
	 *
	 * @return dbName
	 */
	public String dbName();

	/**
	 * Sets the name of this property in the JSON context. This
	 * is the key under which the property will be found in the
	 * JSON input/output.
	 *
	 * @param jsonName
	 */
	public void jsonName(final String jsonName);

	/**
	 * Sets the name of this property in the database context. This
	 * is the key under which the property will be stored in the
	 * database.
	 *
	 * @param dbName
	 */
	public void dbName(final String dbName);

	/**
	 * Use this method to mark a property for indexing. This
	 * method registers the property in both the keyword and
	 * the fulltext index. To select the appropriate index
	 * for yourself, use the other indexed() methods.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> indexed();

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
	public Property<T> passivelyIndexed();

	public Property<T> indexedWhenEmpty();

	/**
	 * Use this method to indicate that a property key is accessible via
	 * CMIS.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> cmis();

	/**
	 * Returns the desired type name that will be used in the error message if a
	 * wrong type was provided.
	 * @return typeName
	 */
	public String typeName();

	/**
	 * Returns the type of the value this property returns.
	 *
	 * @return the value type
	 */
	public Class valueType();

	/**
	 * Returns the type of the related property this property key references, or
	 * null if this is not a relationship property.
	 *
	 * @return relatedType
	 */
	public Class relatedType();

	/**
	 * Returns the format value for this property.
	 *
	 * @return format
	 */
	public String format();

	/**
	 * Returns the default value for this property.
	 *
	 * @return defaultValue
	 */
	public T defaultValue();

	/**
	 * Returns the readFunction value for this property.
	 *
	 * @return readFunction
	 */
	public String readFunction();

	/**
	 * Returns the writeFunction value for this property.
	 *
	 * @return writeFunction
	 */
	public String writeFunction();


	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext);
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity);
	public PropertyConverter<?, T> inputConverter(final SecurityContext securityContext);
	public Object fixDatabaseProperty(final Object value);

	public boolean requiresSynchronization();
	public String getSynchronizationKey();

	public void setDeclaringClass(final Class declaringClass);
	public Class getDeclaringClass();

	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter);
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate);
	public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final T value) throws FrameworkException;

	public void registrationCallback(final Class<GraphObject> entityType);

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
	public boolean isUnvalidated();

	/**
	 * Indicates whether this property is read-only. Read-only properties
	 * will throw a FrameworkException with error code 422 when the value
	 * is modified unless the action is unlocked before.
	 *
	 * @return isReadOnly
	 */
	public boolean isReadOnly();

	/**
	 * Indicates whether this property is an system-internal property.
	 * System properties will throw a FrameworkException with error code 422
	 * when the value is modified unless the action is unlocked before.
	 *
	 * @return isSystemInternal
	 */
	public boolean isSystemInternal();

	/**
	 * Indicates whether this property is write-once. Write-once properties
	 * will throw a FrameworkException with error code 422 when the value
	 * is modified after it has been initially set.
	 *
	 * @return isWriteOnce
	 */
	public boolean isWriteOnce();

	/**
	 * Indicates whether this property is indexed, i.e. searchable using
	 * REST queries.
	 *
	 * @return isIndexed
	 */
	public boolean isIndexed();

	/**
	 * Indicates whether this property is indexed. The difference to the
	 * above method is, that the value for indexing will be obtained at
	 * the end of the transaction, so you can use this method to achieve
	 * indexing (and searchability) of properties that are never directly
	 * set using setProperty.
	 *
	 * @return isPassivelyIndexed
	 */
	public boolean isPassivelyIndexed();

	/**
	 * Indicates whether this property is searchable with an empty value.
	 * This behaviour is achieved by storing a special value for empty
	 * fields which can then later be found again.
	 *
	 * @return isIndexedWhenEmpty
	 */
	public boolean isIndexedWhenEmpty();

	/**
	 * Indicates whether this property represents a collection or a single
	 * value in the JSON output.
	 *
	 * @return isCollection
	 */
	public boolean isCollection();

	/**
	 * Indicates whether the value associated with this property is
	 * validated for uniqueness.
	 *
	 * @return whether this property value is validated for uniqueness
	 */
	public boolean isUnique();

	/**
	 * Indicates whether the value associated with this property is
	 * may not be null.
	 *
	 * @return whether this property value is validated for uniqueness
	 */
	public boolean isNotNull();

	/**
	 * Indicates whether this property is created from a database node.
	 *
	 * @return whether this property is dynamic
	 */
	public boolean isDynamic();

	/**
	 * Returns the lucene sort type of this property.
	 * @return sortType
	 */
	public SortType getSortType();

	public void index(GraphObject entity, Object value);

	public SearchAttribute getSearchAttribute(final SecurityContext securityContext, final Occurrence occur, final T searchValue, final boolean exactMatch, final Query query);
	public void extractSearchableAttribute(final SecurityContext securityContext, final HttpServletRequest request, final boolean exactMatch, final Query query) throws FrameworkException;
	public T convertSearchValue(final SecurityContext securityContext, final String requestParameter) throws FrameworkException;

	/**
	 * Returns the desired position of this property key type
	 * in the processing order.
	 * @return processingOrderPosition
	 */
	public int getProcessingOrderPosition();

	public PropertyKey<T> defaultValue(final T defaultValue);
	public PropertyKey<T> notNull(final boolean notNull);
	public PropertyKey<T> unique(final boolean unique);
	public PropertyKey<T> format(final String format);
	public PropertyKey<T> dynamic();
	public PropertyKey<T> readFunction(final String readFunction);
	public PropertyKey<T> writeFunction(final String writeFunction);

	// ----- CMIS support -----
	public PropertyType getDataType();
	public boolean isCMISProperty();
}
