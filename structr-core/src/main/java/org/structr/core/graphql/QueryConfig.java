/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.core.graphql;

import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Field;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.common.GraphObjectComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;

/**
 */
public class QueryConfig implements GraphQLQueryConfiguration {

	private final Map<PropertyKey, SearchAttribute> attributes = new LinkedHashMap<>();
	private final Set<PropertyKey> propertyKeys                = new LinkedHashSet<>();
	private PropertyKey sortKey                                = null;
	private boolean sortDescending                             = false;
	private int pageSize                                       = Integer.MAX_VALUE;
	private int page                                           = 1;

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return propertyKeys;
	}

	@Override
	public Predicate getPredicateForPropertyKey(final PropertyKey key) {

		if (key != null) {

			final SearchAttribute attribute = attributes.get(key);
			if (attribute != null) {

				if (sortKey != null) {
					// insert comparator to allow sorting
					attribute.setComparator(new GraphObjectComparator(sortKey, sortDescending));
				}

				// SearchAttribute implements Predicate
				return attribute;

			} else if (sortKey != null) {

				// accept-all predicate just for sorting
				return new Predicate<GraphObject>() {

					@Override
					public boolean accept(final GraphObject value) {
						return true;
					}

					@Override
					public Comparator<GraphObject> comparator() {
						return new GraphObjectComparator(sortKey, sortDescending);
					}

				};
			}
		}

		return null;
	}

	public void addPropertyKey(final PropertyKey key) {
		propertyKeys.add(key);
	}

	public void setPage(final int page) {
		this.page = page;
	}

	@Override
	public int getPage() {
		return page;
	}

	public void setPageSize(final int pageSize) {
		this.pageSize = pageSize;
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}

	public void setSortKey(final PropertyKey sortKey) {
		this.sortKey = sortKey;
	}

	public PropertyKey getSortKey() {
		return sortKey;
	}

	public void setSortDescending(final boolean sortDescending) {
		this.sortDescending = sortDescending;
	}

	public boolean sortDescending() {
		return this.sortDescending;
	}

	public void configureQuery(final Query query) {

		query.page(page);
		query.pageSize(pageSize);
		query.attributes(new LinkedList<>(attributes.values()));

		if (sortDescending) {
			query.sortDescending(sortKey);
		} else {
			query.sortAscending(sortKey);
		}
	}

	public void handleTypeArguments(final SecurityContext securityContext, final Class type, final List<Argument> arguments) throws FrameworkException {

		// parse arguments
		for (final Argument argument : arguments) {

			final String name = argument.getName();
			final Value value = argument.getValue();

			switch (name) {

				case "_page":
					this.page = getIntegerValue(value, 1);
					break;

				case "_pageSize":
				case "_first":
					this.pageSize = getIntegerValue(value, Integer.MAX_VALUE);
					break;

				case "_sort":
					this.sortKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, getStringValue(value, "name"));
					break;

				case "_desc":
					this.sortDescending = getBooleanValue(value, false);
					break;

				default:
					// handle simple selections like an _equals on the field
					final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, name, false);
					if (key != null) {

						addAttribute(key, key.getSearchAttribute(securityContext, Occurrence.REQUIRED, castValue(securityContext, type, key, value), true, null), Occurrence.REQUIRED);
					}
					break;
			}
		}
	}

	public void handleFieldArguments(final SecurityContext securityContext, final Class type, final Field parentField, final Field field) throws FrameworkException {

		final ConfigurationProvider config   = StructrApp.getConfiguration();
		final PropertyKey parentKey          = config.getPropertyKeyForJSONName(type, parentField.getName());
		final PropertyKey key                = config.getPropertyKeyForJSONName(type, field.getName(), false);
		final List<SearchTuple> searchTuples = new LinkedList<>();
		Occurrence occurrence                = Occurrence.REQUIRED;

		// parse arguments
		for (final Argument argument : field.getArguments()) {

			final String name = argument.getName();
			final Value value = argument.getValue();

			switch (name) {

				case "_equals":
					searchTuples.add(new SearchTuple(castValue(securityContext, key.relatedType(), key, value), true));
					break;

				case "_contains":
					searchTuples.add(new SearchTuple(castValue(securityContext, key.relatedType(), key, value), false));
					break;

				case "_conj":
					occurrence = getOccurrence(value);
					break;

				default:
					break;
			}
		}

		// only add field if a value was set
		for (final SearchTuple tuple : searchTuples) {

			addAttribute(parentKey, key.getSearchAttribute(securityContext, occurrence, tuple.value, tuple.exact, null), occurrence);
		}
	}

	// ----- private methods -----
	private boolean getBooleanValue(final Value value, final boolean defaultValue) {

		if (value != null && value instanceof BooleanValue) {

			return ((BooleanValue)value).isValue();
		}

		return defaultValue;
	}

	private int getIntegerValue(final Value value, final int defaultValue) {

		if (value != null && value instanceof IntValue) {

			return ((IntValue)value).getValue().intValue();
		}

		return defaultValue;
	}

	private String getStringValue(final Value value, final String defaultValue) {

		if (value != null && value instanceof StringValue) {

			return ((StringValue)value).getValue();
		}

		return defaultValue;
	}

	private Map<String, Object> getMapValue(final SecurityContext securityContext, final Class type, final Value value) throws FrameworkException {

		final Map<String, Object> map = new LinkedHashMap<>();

		if (value != null && value instanceof ObjectValue) {

			final ObjectValue obj = (ObjectValue)value;

			for (final ObjectField field : obj.getObjectFields()) {

				// recurse
				map.put(field.getName(), castValue(securityContext, type, null, field.getValue()));
			}
		}

		return map;
	}

	private Occurrence getOccurrence(final Value value) {

		final String val = getStringValue(value, null);
		if (val != null) {

			switch (val.toLowerCase()) {

				case "and":
					return Occurrence.REQUIRED;

				case "or":
					return Occurrence.OPTIONAL;

				case "not":
					return Occurrence.FORBIDDEN;
			}
		}

		return Occurrence.REQUIRED;
	}

	private Object castValue(final SecurityContext securityContext, final Class type, final PropertyKey key, final Value value) throws FrameworkException {

		if (value instanceof StringValue) {
			return getStringValue(value, null);
		}

		if (value instanceof IntValue) {
			return getIntegerValue(value, -1);
		}

		if (value instanceof BooleanValue) {
			return getBooleanValue(value, false);
		}

		if (value instanceof ObjectValue && key != null) {

			final Map<String, Object> parameters = new LinkedHashMap<>();

			parameters.put(key.jsonName(), getMapValue(securityContext, type, value));

			final PropertyMap propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type, parameters);

			// return converted result (should be replaced by NodeInterface)
			return propertyMap.get(key);

		}

		return null;
	}

	private void addAttribute(final PropertyKey parentKey, final SearchAttribute newAttribute, final Occurrence occurrence) {

		final SearchAttribute existingAttribute = attributes.get(parentKey);
		if (existingAttribute == null) {

			// single value, no group, no existing attribute
			attributes.put(parentKey, newAttribute);

		} else {

			// we need to combine the attributes
			if (existingAttribute instanceof SearchAttributeGroup) {

				// attribute already set, add
				((SearchAttributeGroup)existingAttribute).add(newAttribute);

			} else {

				// create group from two single attributes
				final SearchAttributeGroup group = new SearchAttributeGroup(occurrence);
				group.add(existingAttribute);
				group.add(newAttribute);

				attributes.put(parentKey, group);
			}
		}
	}

	// ----- nested classes -----
	private class SearchTuple {

		public boolean exact = false;
		public Object value  = null;

		public SearchTuple(final Object value, final boolean exact) {
			this.value = value;
			this.exact = exact;
		}
	}
}

