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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.QueryGroup;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.graph.search.PropertySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.traits.Trait;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for all property types.
 */
public abstract class Property<T> implements PropertyKey<T> {

	private static final Logger logger               = LoggerFactory.getLogger(Property.class.getName());
	private static final Pattern RANGE_QUERY_PATTERN = Pattern.compile("\\[(.*) TO (.*)\\]");

	protected final List<String> transformators = new LinkedList<>();
	protected UpdateCallback<T> updateCallback  = null;
	protected Trait declaringTrait              = null;
	protected T defaultValue                    = null;
	protected boolean readOnly                  = false;
	protected boolean systemInternal            = false;
	protected boolean writeOnce                 = false;
	protected boolean unvalidated               = false;
	protected boolean indexed                   = false;
	protected boolean indexedPassively          = false;
	protected boolean indexedWhenEmpty          = false;
	protected boolean fulltextIndexed           = false;
	protected boolean compound                  = false;
	protected boolean unique                    = false;
	protected boolean notNull                   = false;
	protected boolean dynamic                   = false;
	protected boolean cachingEnabled            = false;
	protected boolean nodeOnly                  = false;
	protected boolean isAbstract                = false;
	protected boolean serializationDisabled     = false;
	protected String dbName                     = null;
	protected String jsonName                   = null;
	protected String format                     = null;
	protected String typeHint                   = null;
	protected String readFunction               = null;
	protected String writeFunction              = null;
	protected String openAPIReturnType          = null;
	protected String hint                       = null;
	protected String category                   = null;
	protected String sourceUuid                 = null;

	private boolean requiresSynchronization     = false;

	protected Property(final String name) {
		this(name, name);
	}

	protected Property(final String jsonName, final String dbName) {
		this(jsonName, dbName, null);
	}

	protected Property(final String jsonName, final String dbName, final T defaultValue) {
		this.defaultValue = defaultValue;
		this.jsonName = jsonName;
		this.dbName = dbName;
	}

	@Override
	public abstract Object fixDatabaseProperty(final Object value);

	/**
	 * Use this method to mark a property as being unvalidated. This
	 * method will cause no callbacks to be executed when only
	 * unvalidated properties are modified.
	 *
	 * @return  the Property to satisfy the builder pattern
	 */
	public Property<T> unvalidated() {
		this.unvalidated = true;
		return this;
	}

	/**
	 * Use this method to mark a property as being read-only.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> readOnly() {
		this.readOnly = true;
		return this;
	}

	/**
	 * Use this method to mark a property as being system-internal.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> systemInternal() {
		this.systemInternal = true;
		return this;
	}

	/**
	 * Use this method to mark a property as being write-once.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> writeOnce() {
		this.writeOnce = true;
		return this;
	}

	/**
	 * Use this method to mark a property as being unique. Please note that
	 * using this method will not actually cause a uniqueness check, just
	 * notify the system that this property should be treated as having a
	 * unique value.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> unique() {
		this.unique                  = true;
		this.requiresSynchronization = true;
		return this;
	}

	/**
	 * Use this method to mark a property as being unique. Please note that
	 * using this method will not actually cause a uniqueness check, just
	 * notify the system that this property should be treated as having a
	 * unique value.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> compound() {
		this.compound                = true;
		this.requiresSynchronization = true;
		return this;
	}

	/**
	 * Use this method to mark a property as being not-null. Please note that
	 * using this method will not actually cause a not-null check, just
	 * notify the system that this property should be treated as such.
	 *
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> notNull() {
		this.notNull = true;
		return this;
	}

	@Override
	public Property<T> indexed() {

		this.indexed = true;

		return this;
	}

	@Override
	public Property<T> nodeIndexOnly() {

		this.nodeOnly = true;

		return this;
	}

	@Override
	public Property<T> passivelyIndexed() {

		this.indexedPassively = true;
		this.indexed = true;

		return this;
	}

	@Override
	public Property<T> indexedWhenEmpty() {

		passivelyIndexed();
		this.indexedWhenEmpty = true;

		return this;
	}

	@Override
	public Property<T> fulltextIndexed() {

		this.fulltextIndexed = true;
		return this;

	}

	public Property<T> hint(final String hint) {
		this.hint = hint;
		return this;
	}

	@Override
	public String hint() {
		return hint;
	}

	public Property<T> category(final String category) {
		this.category = category;
		return this;
	}

	@Override
	public String category() {
		return category;
	}

	public Property<T> setSourceUuid(final String sourceUuid) {
		this.sourceUuid = sourceUuid;
		return this;
	}

	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public boolean requiresSynchronization() {
		return requiresSynchronization;
	}

	@Override
	public String getSynchronizationKey() {

		if (declaringTrait != null) {

			return declaringTrait.getLabel() + "." + dbName;
		}

		return "GraphObject." + dbName;
	}

	@Override
	public void setDeclaringTrait(final Trait declaringTrait) {
		this.declaringTrait = declaringTrait;
	}

	@Override
	public void registrationCallback(final Trait trait) {
		this.declaringTrait = trait;
	}

	@Override
	public Trait getDeclaringTrait() {
		return declaringTrait;
	}

	@Override
	public String getSourceUuid() {
		return sourceUuid;
	}

	@Override
	public String toString() {
		return jsonName();
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
	public void dbName(final String dbName) {
		this.dbName = dbName;
	}

	@Override
	public void jsonName(final String jsonName) {
		this.jsonName = jsonName;
	}

	@Override
	public Property<T> defaultValue(final T defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	@Override
	public T defaultValue() {
		return defaultValue;
	}

	@Override
	public String format() {
		return format;
	}

	@Override
	public Property<T> format(final String format) {
		this.format = format;
		return this;
	}

	@Override
	public Property<T> unique(final boolean unique) {

		this.unique = unique;

		if (unique) {
			this.requiresSynchronization = true;
		}

		return this;
	}

	@Override
	public Property<T> setIsAbstract(final boolean isAbstract) {

		this.isAbstract = isAbstract;

		return this;
	}

	@Override
	public Property<T> notNull(final boolean notNull) {
		this.notNull = notNull;
		return this;
	}

	@Override
	public Property<T> dynamic() {
		this.dynamic = true;
		return this;
	}

	@Override
	public String readFunction() {
		return readFunction;
	}

	@Override
	public Property<T> readFunction(final String readFunction) {
		this.readFunction = readFunction;
		return this;
	}

	@Override
	public String writeFunction() {
		return writeFunction;
	}

	@Override
	public Property<T> writeFunction(final String writeFunction) {
		this.writeFunction = writeFunction;
		return this;
	}

	@Override
	public Property<T> cachingEnabled(final boolean enabled) {
		this.cachingEnabled = enabled;
		return this;
	}

	public Property<T> disableSerialization(final boolean disableSerialization) {
		this.serializationDisabled = disableSerialization;
		return this;
	}

	@Override
	public String openAPIReturnType() {
		return openAPIReturnType;
	}

	@Override
	public Property<T> openAPIReturnType(final String openAPIReturnType) {
		this.openAPIReturnType = openAPIReturnType;
		return this;
	}

	@Override
	public String typeHint() {
		return typeHint;
	}

	@Override
	public Property<T> typeHint(final String typeHint) {
		this.typeHint = typeHint;
		return this;
	}

	@Override
	public Property<T> transformators(final String... transformators) {
		this.transformators.addAll(Arrays.asList(transformators));
		return this;
	}

	/**
	 * Register a callback that gets notified when this property is set.
	 * Note that this is currently only implemented in the StartNode,
	 * EndNode, StartNodes and EndNodes properties!
	 *
	 * @param callback
	 * @return
	 */
	public Property<T> updateCallback(final UpdateCallback<T> callback) {
		this.updateCallback = callback;
		return this;
	}

	@Override
	public int hashCode() {

		// make hashCode function work for subtypes that override jsonName() etc. as well
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
	public boolean equals(final Object o) {

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
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public boolean isSystemInternal() {
		return systemInternal;
	}

	@Override
	public boolean isWriteOnce() {
		return writeOnce;
	}

	@Override
	public boolean isIndexed() {
		return indexed;
	}

	@Override
	public boolean isNodeIndexOnly() {
		return nodeOnly;
	}

	@Override
	public boolean isPassivelyIndexed() {
		return indexedPassively;
	}

	@Override
	public boolean isFulltextIndexed() {
		return fulltextIndexed;
	}

	@Override
	public boolean isIndexedWhenEmpty() {
		return indexedWhenEmpty;
	}

	@Override
	public boolean isCompound() {
		return compound;
	}

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public boolean isNotNull() {
		return notNull;
	}

	@Override
	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public boolean cachingEnabled() {
		return cachingEnabled;
	}

	@Override
	public boolean serializationDisabled() {
		return serializationDisabled;
	}

	@Override
	public Object getIndexValue(final Object value) {
		return value;
	}

	@Override
	public boolean isPropertyTypeIndexable() {

		final Class valueType = valueType();
		if (valueType != null) {

			if (Services.getInstance().getDatabaseService().nodeIndex().supports(valueType)) {
				return true;
			}

			if (valueType.equals(Date.class)) {
				return true;
			}

			if (valueType.isEnum()) {
				return true;
			}

			if (valueType.isArray()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isPropertyValueIndexable(final Object value) {

		if (value != null) {

			final Class valueType = value.getClass();
			if (valueType != null) {

				// indexable indicated by value type
				if (Services.getInstance().getDatabaseService().nodeIndex().supports(valueType)) {
					return true;
				}

				if (valueType.equals(Date.class)) {
					return true;
				}

				if (valueType.isEnum()) {
					return true;
				}

				if (valueType.isArray()) {
					return true;
				}
			}

		} else {

			return isPassivelyIndexed();
		}

		// index empty as well
		return isIndexedWhenEmpty();
	}

	@Override
	public SearchAttribute getSearchAttribute(final SecurityContext securityContext, final T searchValue, final boolean exactMatch, final QueryGroup query) {
		return new PropertySearchAttribute(this, searchValue, exactMatch);
	}

	@Override
	public void extractSearchableAttribute(final SecurityContext securityContext, final HttpServletRequest request, final boolean exactMatch, final QueryGroup query) throws FrameworkException {

		final String[] searchValues = request.getParameterValues(jsonName());
		if (searchValues != null) {

			for (String searchValue : searchValues) {

				determineSearchType(securityContext, searchValue, exactMatch, query);
			}
		}
	}

	@Override
	public T convertSearchValue(final SecurityContext securityContext, final String requestParameter) throws FrameworkException {

		PropertyConverter inputConverter = inputConverter(securityContext, true);
		Object convertedSearchValue      = requestParameter;

		if (inputConverter != null) {

			convertedSearchValue = inputConverter.convert(convertedSearchValue);
		}

		return (T)convertedSearchValue;
	}

	@Override
	public int getProcessingOrderPosition() {
		return 0;
	}

	@Override
	public Comparator<GraphObject> sorted(final boolean descending) {
		return new DefaultSortOrder(this, descending);
	}

	// ----- interface Comparable -----
	@Override
	public int compareTo(final PropertyKey other) {
		return dbName().compareTo(other.dbName());
	}

	// ----- protected methods -----
	protected boolean multiValueSplitAllowed() {
		return true;
	}

	protected final String removeQuotes(final String searchValue) {
		String resultStr = searchValue;

		if (resultStr.contains("\"")) {
			resultStr = resultStr.replaceAll("[\"]+", "");
		}

		if (resultStr.contains("'")) {
			resultStr = resultStr.replaceAll("[']+", "");
		}

		return resultStr;
	}

	protected void determineSearchType(final SecurityContext securityContext, final String requestParameter, final boolean exactMatch, final QueryGroup query) throws FrameworkException {

		if (StringUtils.startsWith(requestParameter, "[") && StringUtils.endsWith(requestParameter, "]")) {

			// check for existence of range query string
			Matcher matcher = RANGE_QUERY_PATTERN.matcher(requestParameter);
			if (matcher.matches()) {

				if (matcher.groupCount() == 2) {

					final String rangeStart = matcher.group(1);
					final String rangeEnd   = matcher.group(2);

					final PropertyConverter inputConverter = inputConverter(securityContext, false);
					Object rangeStartConverted = (rangeStart.equals("")) ? null : rangeStart;
					Object rangeEndConverted   = (rangeEnd.equals(""))   ? null : rangeEnd;

					if (inputConverter != null) {

						if (rangeStartConverted != null) {
							rangeStartConverted = inputConverter.convert(rangeStartConverted);
						}

						if (rangeEndConverted != null) {
							rangeEndConverted   = inputConverter.convert(rangeEndConverted);
						}
					}

					query.range(this, rangeStartConverted, rangeEndConverted);

					return;
				}

				logger.warn("Unable to determine range query bounds for {}", requestParameter);

			} else {

				if ("[]".equals(requestParameter)) {

					if (isIndexedWhenEmpty()) {

						// requestParameter contains only [],
						// which we use as a "not-blank" selector
						query.notBlank(this);

						return;

					} else {

						throw new FrameworkException(400, "PropertyKey " + jsonName() + " must be indexedWhenEmpty() to be used in not-blank search query.");
					}

				} else {

					throw new FrameworkException(422, "Invalid range pattern.");
				}
			}
 		}

		if (requestParameter.contains(",") && requestParameter.contains(";")) {
			throw new FrameworkException(422, "Mixing of AND and OR not allowed in request parameters");
		}

		if (requestParameter.contains(";")) {

			final QueryGroup or = query.or();

			if (multiValueSplitAllowed()) {

				// descend into a new group

				for (final String part : requestParameter.split("[;]+")) {

					or.key(this, convertSearchValue(securityContext, part), exactMatch);
				}

			} else {

				or.key(this, convertSearchValue(securityContext, requestParameter), exactMatch);
			}

		} else if (requestParameter.contains(",")) {

			// descend into a new group
			final QueryGroup and = query.and();

			for (final String part : requestParameter.split("[,]+")) {

				and.key(this, convertSearchValue(securityContext, part), exactMatch);
			}

		} else {

			query.and().key(this, convertSearchValue(securityContext, requestParameter), exactMatch);
		}
	}

	protected <T extends NodeInterface> Set<T> getRelatedNodesReverse(final SecurityContext securityContext, final NodeInterface obj, final Class destinationType, final Predicate<GraphObject> predicate) {
		// this is the default implementation
		return Collections.emptySet();
	}
}
