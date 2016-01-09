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
package org.structr.core.app;

import java.util.List;
import org.neo4j.helpers.Predicate;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 *
 * @param <T>
 */
public interface Query<T extends GraphObject> extends Iterable<T> {

	public Result<T> getResult() throws FrameworkException;
	public List<T> getAsList() throws FrameworkException;
	public T getFirst() throws FrameworkException;
	public boolean isExactSearch();

	// ----- builder methods -----
	public Query<T> sort(final PropertyKey key);
	public Query<T> sortAscending(final PropertyKey key);
	public Query<T> sortDescending(final PropertyKey key);
	public Query<T> order(final boolean descending);
	public Query<T> pageSize(final int pageSize);
	public Query<T> page(final int page);
	public Query<T> publicOnly();
	public Query<T> exact(final boolean exact);
	public Query<T> includeDeletedAndHidden();
	public Query<T> publicOnly(final boolean publicOnly);
	public Query<T> includeDeletedAndHidden(final boolean includeDeletedAndHidden);
	public Query<T> offsetId(final String offsetId);
	public Query<T> uuid(final String uuid);
	public Query<T> andType(final Class<T> type);
	public Query<T> orType(final Class<T> type);
	public Query<T> andTypes(final Class<T> type);
	public Query<T> orTypes(final Class<T> type);

	public Query<T> andName(final String name);
	public Query<T> orName(final String name);

	public Query<T> location(final String street, final String postalCode, final String city, final String country, final double distance);
	public Query<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance);
	public Query<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance);
	public <P> Query<T> and(final PropertyKey<P> key, final P value);
	public <P> Query<T> and(final PropertyKey<P> key, final P value, final boolean exact);
	public <P> Query<T> and(final PropertyMap attributes);
	public Query<T> and();
	public <P> Query<T> or(final PropertyKey<P> key, P value);
	public <P> Query<T> or(final PropertyKey<P> key, P value, boolean exact);
	public <P> Query<T> or(final PropertyMap attributes);
	public Query<T> notBlank(final PropertyKey key);
	public Query<T> blank(final PropertyKey key);

	public <P> Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd);
	public <P> Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd);

	public Query<T> or();
	public Query<T> not();

	public Query<T> parent();
	public Query<T> attributes(final List<SearchAttribute> attributes);

	public Predicate<GraphObject> toPredicate();
	public SearchAttributeGroup getRootAttributeGroup();
}
