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
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

import java.util.Comparator;
import java.util.List;

/**
 *
 *
 * @param <T>
 */
public interface Query<T> {

	QueryContext getQueryContext();
	Query<T> isPing(final boolean isPing);

	ResultStream<T> getResultStream() throws FrameworkException;
	List<T> getAsList() throws FrameworkException;
	T getFirst() throws FrameworkException;
	Traits getTraits();

	// ----- builder methods -----
	Query<T> disableSorting();
	Query<T> sort(final SortOrder sortOrder);
	Query<T> sort(final PropertyKey key, final boolean descending);
	default Query<T> sort(final PropertyKey key) { return sort(key, false); }
	Query<T> comparator(final Comparator<T> comparator);
	Query<T> pageSize(final int pageSize);
	Query<T> page(final int page);
	Query<T> publicOnly();
	Query<T> includeHidden();
	Query<T> publicOnly(final boolean publicOnly);
	Query<T> includeHidden(final boolean includeHidden);

	QueryGroup<T> and();
	QueryGroup<T> or();
	QueryGroup<T> not();

	/*
	Query<T> uuid(final String uuid);
	Query<T> types(final Traits type);
	Query<T> type(final String type);
	Query<T> name(final String name);
	Query<T> location(final double latitude, final double longitude, final double distance);
	Query<T> location(final String street, final String postalCode, final String city, final String country, final double distance);
	Query<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance);
	Query<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance);
	<P> Query<T> key(final PropertyKey<P> key, final P value);
	<P> Query<T> key(final PropertyKey<P> key, final P value, final boolean exact);
	<P> Query<T> key(final PropertyMap attributes);
	Query<T> notBlank(final PropertyKey key);
	Query<T> blank(final PropertyKey key);
	<P> Query<T> startsWith(final PropertyKey<P> key, final P prefix, final boolean caseInsensitive);
	<P> Query<T> endsWith(final PropertyKey<P> key, final P suffix, final boolean caseInsensitive);
	<P> Query<T> matches(final PropertyKey<String> key, final String regex);
	<P> Query<T> range(final PropertyKey<P> key, final P rangeStart, final P rangeEnd);
	<P> Query<T> range(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd);

	Query<T> attributes(final List<SearchAttribute> attributes);
	*/

	Predicate<GraphObject> toPredicate();

	void doNotSort(final boolean doNotSort);
	void setTraits(final Traits traits);
}
