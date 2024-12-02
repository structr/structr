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
package org.structr.core.app;

import org.structr.api.Predicate;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Comparator;
import java.util.List;

/**
 *
 *
 * @param <T>
 */
public interface Query<T extends GraphObject> {

	public void setQueryContext(final QueryContext queryContext);
	public QueryContext getQueryContext();
	public Query<T> isPing(final boolean isPing);

	public ResultStream<T> getResultStream() throws FrameworkException;
	public List<T> getAsList() throws FrameworkException;
	public T getFirst() throws FrameworkException;

	// ----- builder methods -----
	public Query<T> disableSorting();
	public Query<T> sort(final SortOrder sortOrder);
	public Query<T> sort(final PropertyKey key, final boolean descending);
	default public Query<T> sort(final PropertyKey key) { return sort(key, false); }
	public Query<T> comparator(final Comparator<T> comparator);
	public Query<T> pageSize(final int pageSize);
	public Query<T> page(final int page);
	public Query<T> publicOnly();
	public Query<T> includeHidden();
	public Query<T> publicOnly(final boolean publicOnly);
	public Query<T> includeHidden(final boolean includeHidden);
	public Query<T> uuid(final String uuid);
	public Query<T> andType(final Class<T> type);
	public Query<T> orType(final Class<T> type);
	public Query<T> andTypes(final Class<T> type);
	public Query<T> orTypes(final Class<T> type);
	public Class getType();

	public Query<T> andName(final String name);
	public Query<T> orName(final String name);

	public Query<T> location(final double latitude, final double longitude, final double distance);
	public Query<T> location(final String street, final String postalCode, final String city, final String country, final double distance);
	public Query<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance);
	public Query<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance);

	default public <P> Query<T> and(final String name, final P value) {

		final PropertyKey<P> key = StructrApp.getConfiguration().getPropertyKeyForJSONName(getType(), name, false);
		if (key != null) {

			return and(key, value);
		}

		throw new IllegalArgumentException("Invalid property key " + name + " for type " + getClass().getSimpleName());
	}

	public <P> Query<T> and(final PropertyKey<P> key, final P value);
	public <P> Query<T> and(final PropertyKey<P> key, final P value, final boolean exact);
	public <P> Query<T> and(final PropertyKey<P> key, final P value, final boolean exact, final Occurrence occur);
	public <P> Query<T> and(final PropertyMap attributes);
	public Query<T> and();
	public <P> Query<T> or(final PropertyKey<P> key, P value);
	public <P> Query<T> or(final PropertyKey<P> key, P value, boolean exact);
	public <P> Query<T> or(final PropertyMap attributes);
	public Query<T> notBlank(final PropertyKey key);
	public Query<T> blank(final PropertyKey key);
	public <P> Query<T> startsWith(final PropertyKey<P> key, final P prefix, final boolean caseInsensitive);
	public <P> Query<T> endsWith(final PropertyKey<P> key, final P suffix, final boolean caseInsensitive);
	public <P> Query<T> matches(final PropertyKey<P> key, final String regex);

	public <P> Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd);
	public <P> Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd);
	public <P> Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd);
	public <P> Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd);

	public Query<T> or();
	public Query<T> not();

	public Query<T> parent();
	public Query<T> attributes(final List<SearchAttribute> attributes);

	public Predicate<GraphObject> toPredicate();
	public SearchAttributeGroup getRootAttributeGroup();

	void overrideFetchSize(final int fetchSizeForThisRequest);
	Query<T> disablePrefetching();

	public Occurrence getCurrentOccurrence();
}
