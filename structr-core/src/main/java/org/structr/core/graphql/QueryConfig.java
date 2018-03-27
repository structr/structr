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
import graphql.language.StringValue;
import graphql.language.Value;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.search.PropertySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;

/**
 */
public class QueryConfig implements GraphQLQueryConfiguration {

	private Map<PropertyKey, SearchAttribute> attributes = new LinkedHashMap<>();
	private Set<PropertyKey> propertyKeys                = new LinkedHashSet<>();
	private PropertyKey sortKey                          = null;
	private int pageSize                                 = Integer.MAX_VALUE;
	private int page                                     = 1;
	private boolean sortDescending                       = false;

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return propertyKeys;
	}

	@Override
	public Predicate getPredicateForPropertyKey(final PropertyKey key) {
		return attributes.get(key);
	}

	public void addPropertyKey(final PropertyKey key) {
		propertyKeys.add(key);
	}

	public void setPage(final int page) {
		this.page = page;
	}

	public int getPage() {
		return page;
	}

	public void setPageSize(final int pageSize) {
		this.pageSize = pageSize;
	}

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

	public void handleTypeArguments(final Class type, final List<Argument> arguments) {

		// parse arguments
		for (final Argument argument : arguments) {

			final String name = argument.getName();
			final Value value = argument.getValue();

			switch (name) {

				case "_page":
					this.page = getIntegerValue(value, 1);
					break;

				case "_pageSize":
					this.pageSize = getIntegerValue(value, Integer.MAX_VALUE);
					break;

				case "_sort":
					this.sortKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, getStringValue(value, "name"));
					break;

				case "_desc":
					this.sortDescending = getBooleanValue(value, false);
					break;

				default:

					//
					break;
			}
		}
	}

	public void handleFieldArguments(final Class type, final Field parentField, final Field field) {

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
					searchTuples.add(new SearchTuple(castValue(key, value), true));
					break;

				case "_contains":
					searchTuples.add(new SearchTuple(castValue(key, value), false));
					break;

				case "_conj":
					occurrence = getOccurrence(value);
					break;

				default:
					//
					break;
			}
		}

		// only add field if a value was set
		final int valueCount = searchTuples.size();
		if (valueCount > 0) {

			if (valueCount == 1) {

				final SearchTuple tuple = searchTuples.get(0);

				// single value, no group
				attributes.put(parentKey, new PropertySearchAttribute(key, tuple.value, occurrence, tuple.exact));

			} else {

				// multiple values
				final SearchAttributeGroup group = new SearchAttributeGroup(Occurrence.REQUIRED);

				for (final SearchTuple tuple : searchTuples) {

					group.add(new PropertySearchAttribute(key, tuple.value, occurrence, tuple.exact));
				}

				attributes.put(parentKey, group);
			}
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

	private Object castValue(final PropertyKey key, final Value value) {

		final String typeName = key.typeName();
		if (typeName != null) {

			switch (typeName) {

				case "String":
					return getStringValue(value, null);

				case "Integer":
					return getIntegerValue(value, -1);

				case "Boolean":
					return getBooleanValue(value, false);

				default:
					break;
			}
		}

		return null;
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

