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
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.TraitDefinition;

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

	// ----- builder methods -----
	Query<T> disableSorting();
	Query<T> sort(final SortOrder sortOrder);
	Query<T> sort(final PropertyKey<?> key, final boolean descending);
	default Query<T> sort(final PropertyKey<?> key) { return sort(key, false); }
	Query<T> comparator(final Comparator<T> comparator);
	Query<T> pageSize(final int pageSize);
	Query<T> page(final int page);
	Query<T> publicOnly();
	Query<T> includeHidden();
	Query<T> publicOnly(final boolean publicOnly);
	Query<T> includeHidden(final boolean includeHidden);
	Query<T> uuid(final String uuid);
	Query<T> andType(final TraitDefinition type);
	Query<T> orType(final TraitDefinition type);

	Query<T> andName(final String name);
	Query<T> orName(final String name);

	Query<T> location(final double latitude, final double longitude, final double distance);
	Query<T> location(final String street, final String postalCode, final String city, final String country, final double distance);
	Query<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance);
	Query<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance);

	default <P> Query<T> and(final String name, final P value) {

		final PropertyKey<P> key = StructrApp.getConfiguration().getPropertyKeyForJSONName(getTraits(), name, false);
		if (key != null) {

			return and(key, value);
		}

		throw new IllegalArgumentException("Invalid property key " + name + " for type " + getClass().getSimpleName());
	}

	<P> Query<T> and(final PropertyKey<P> key, final P value);
	<P> Query<T> and(final PropertyKey<P> key, final P value, final boolean exact);
	<P> Query<T> and(final PropertyKey<P> key, final P value, final boolean exact, final Occurrence occur);
	<P> Query<T> and(final PropertyMap attributes);
	Query<T> and();
	<P> Query<T> or(final PropertyKey<P> key, P value);
	<P> Query<T> or(final PropertyKey<P> key, P value, boolean exact);
	<P> Query<T> or(final PropertyMap attributes);
	Query<T> notBlank(final PropertyKey key);
	Query<T> blank(final PropertyKey key);
	<P> Query<T> startsWith(final PropertyKey<P> key, final P prefix, final boolean caseInsensitive);
	<P> Query<T> endsWith(final PropertyKey<P> key, final P suffix, final boolean caseInsensitive);
	<P> Query<T> matches(final PropertyKey<P> key, final String regex);

	<P> Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd);
	<P> Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd);
	<P> Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd);
	<P> Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd);

	Query<T> or();
	Query<T> not();

	Query<T> parent();
	Query<T> attributes(final List<SearchAttribute> attributes);

	Predicate<GraphObject> toPredicate();

	Occurrence getCurrentOccurrence();
}
