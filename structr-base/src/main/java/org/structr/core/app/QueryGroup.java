/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.api.search.Operation;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;

import java.util.List;

/**
 */
public interface QueryGroup<T> extends Query<T> {

	QueryGroup<T> uuid(final String uuid);
	QueryGroup<T> types(final Traits type);
	QueryGroup<T> type(final String type);
	QueryGroup<T> name(final String name);
	QueryGroup<T> location(final double latitude, final double longitude, final double distance);
	QueryGroup<T> location(final String street, final String postalCode, final String city, final String country, final double distance);
	QueryGroup<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance);
	QueryGroup<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance);
	<P> QueryGroup<T> key(final PropertyKey<P> key, final P value);
	<P> QueryGroup<T> key(final PropertyKey<P> key, final P value, final boolean exact);
	<P> QueryGroup<T> key(final PropertyMap attributes);
	QueryGroup<T> notBlank(final PropertyKey key);
	QueryGroup<T> blank(final PropertyKey key);
	<P> QueryGroup<T> startsWith(final PropertyKey<P> key, final P prefix, final boolean caseInsensitive);
	<P> QueryGroup<T> endsWith(final PropertyKey<P> key, final P suffix, final boolean caseInsensitive);
	QueryGroup<T> matches(final PropertyKey<String> key, final String regex);
	<P> QueryGroup<T> range(final PropertyKey<P> key, final P rangeStart, final P rangeEnd);
	<P> QueryGroup<T> range(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd);

	Query<T> attributes(final List<SearchAttribute> attributes, final Operation operation);

	void add(final SearchAttribute<T> attribute);

	Operation getOperation();
	Query<T> getParent();
}
