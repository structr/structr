/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.graph.search;

import org.structr.api.Predicate;
import org.structr.api.search.*;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.QueryGroup;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;

import java.util.*;

/**
 * Represents a group of search operators, to be used for queries with multiple textual search attributes grouped by parentheses.
 *
 *
 */
public class SearchAttributeGroup<T> extends SearchAttribute<T> implements QueryGroup<T>, GroupQuery {

	private static final Set<String> indexedWarningDisabled = Set.of(
		SchemaMethodTraitDefinition.SOURCE_PROPERTY,
		SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY,
		SchemaPropertyTraitDefinition.WRITE_FUNCTION_PROPERTY
	);

	private final List<SearchAttribute> searchItems = new LinkedList<>();
	private final SecurityContext securityContext;
	private final Operation operation;
	private final Query<T> query;

	public SearchAttributeGroup(final SecurityContext securityContext, final Query<T> parent, final Operation operation) {

		super(null, null);

		this.securityContext = securityContext;
		this.operation       = operation;
		this.query           = parent;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder(operation + "(");

		for (final Iterator<SearchAttribute> it = searchItems.iterator(); it.hasNext();) {

			final SearchAttribute item = it.next();

			buf.append(item.toString());

			if (it.hasNext()) {
				buf.append(", ");
			}
		}

		buf.append(")");

		return buf.toString();
	}

	public List<SearchAttribute> getSearchAttributes() {
		return searchItems;
	}

	public void add(final SearchAttribute searchAttribute) {
		searchItems.add(searchAttribute);
	}

	@Override
	public boolean isExactMatch() {

		boolean exactMatch = true;

		for (SearchAttribute attr : getSearchAttributes()) {

			exactMatch &= attr.isExactMatch();
		}

		return exactMatch;
	}

	@Override
	public boolean includeInResult(final GraphObject entity) {

		boolean includeInResult = true;

		for (SearchAttribute attr : getSearchAttributes()) {

			switch (operation) {

				case NOT:
					includeInResult &= !attr.includeInResult(entity);
					break;

				case AND:
					includeInResult &= attr.includeInResult(entity);
					break;

				case OR:
					// special behaviour for OR'ed predicates
					if (attr.includeInResult(entity)) {

						// we're in or mode, return
						// immediately
						return true;

					} else {

						// set result flag to false
						// and evaluate next search predicate
						includeInResult = false;
					}
					break;
			}
		}

		return includeInResult;
	}

	@Override
	public boolean isEmpty() {

		boolean isEmpty = searchItems.isEmpty();

		for (SearchAttribute attr : searchItems) {

			if (attr instanceof SearchAttributeGroup g) {

				isEmpty &= g.isEmpty();

			} else {

				return false;
			}
		}

		return isEmpty;
	}

	@Override
	public void setExactMatch(final boolean exact) {

		for (SearchAttribute attr : getSearchAttributes()) {

			attr.setExactMatch(exact);
		}
	}

	@Override
	public Class getQueryType() {
		return GroupQuery.class;
	}

	@Override
	public List<QueryPredicate> getQueryPredicates() {

		final List<QueryPredicate> predicates = new LinkedList<>();
		for (final SearchAttribute attr : searchItems) {

			predicates.add(attr);
		}

		return predicates;
	}

	// ----- interface QueryGroup<T> -----
	@Override
	public Operation getOperation() {
		return operation;
	}

	@Override
	public Query<T> getParent() {
		return query;
	}

	@Override
	public QueryGroup attributes(final List<SearchAttribute> attributes, final Operation operation) {

		final SearchAttributeGroup group = new SearchAttributeGroup(securityContext, this, operation);

		// add all search items
		for (final SearchAttribute attr : searchItems) {
			group.add(attr);
		}

		// add new attributes
		for (final SearchAttribute attr : attributes) {
			group.add(attr);
		}

		searchItems.clear();
		searchItems.add(group);

		return this;
	}

	@Override
	public QueryGroup<T> uuid(final String uuid) {

		query.doNotSort(true);

		return key(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), uuid);
	}

	@Override
	public QueryGroup<T> types(final Traits traits) {

		query.setTraits(traits);

		type(traits.getName());

		return this;
	}

	@Override
	public QueryGroup<T> type(final String type) {

		searchItems.add(new TypeSearchAttribute(type, true));
		return this;
	}

	@Override
	public QueryGroup<T> name(final String name) {
		return key(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);
	}

	@Override
	public QueryGroup<T> location(final double latitude, final double longitude, final double distance) {
		searchItems.add(new DistanceSearchAttribute(latitude, longitude, distance));
		return this;
	}

	@Override
	public QueryGroup<T> location(final String street, final String postalCode, final String city, final String country, final double distance) {
		return location(street, null, postalCode, city, null, country, distance);
	}

	@Override
	public QueryGroup<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance) {
		return location(street, null, postalCode, city, state, country, distance);
	}

	@Override
	public QueryGroup<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance) {
		searchItems.add(new DistanceSearchAttribute(street, house, postalCode, city, state, country, distance));
		return this;
	}

	@Override
	public <P> QueryGroup<T> key(final PropertyKey<P> key, final P value) {

		if (Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY).equals(key)) {
			query.doNotSort(false);
		}

		return key(key, value, true);
	}

	@Override
	public <P> QueryGroup<T> key(final PropertyKey<P> key, final P value, final boolean exact) {

		if (Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY).equals(key)) {

			query.doNotSort(false);
		}

		searchItems.add(key.getSearchAttribute(securityContext, value, exact, this));

		return this;
	}

	@Override
	public <P> QueryGroup<T> key(final PropertyMap attributes) {

		for (final Map.Entry<PropertyKey, Object> entry : attributes.entrySet()) {

			final PropertyKey key = entry.getKey();
			final Object value = entry.getValue();

			if (Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY).equals(key)) {

				query.doNotSort(false);
			}

			key(key, value);
		}

		return this;
	}

	@Override
	public QueryGroup<T> notBlank(final PropertyKey key) {

		searchItems.add(new NotBlankSearchAttribute(key));

		return this;
	}

	@Override
	public QueryGroup<T> blank(final PropertyKey key) {

		if (key.relatedType() != null) {

			// related nodes
			if (key.isCollection()) {

				searchItems.add(key.getSearchAttribute(securityContext, Collections.EMPTY_LIST, true, this));

			} else {

				searchItems.add(key.getSearchAttribute(securityContext, null, true, this));
			}

		} else {

			// everything else
			searchItems.add(new EmptySearchAttribute(key, null));
		}

		return this;
	}

	@Override
	public <P> QueryGroup<T> startsWith(final PropertyKey<P> key, final P prefix, final boolean caseSensitive) {

		searchItems.add(new ComparisonSearchAttribute(key, caseSensitive ? ComparisonQuery.Comparison.startsWith : ComparisonQuery.Comparison.caseInsensitiveStartsWith, prefix));

		return this;
	}

	@Override
	public <P> QueryGroup<T> endsWith(final PropertyKey<P> key, final P suffix, final boolean caseSensitive) {

		searchItems.add(new ComparisonSearchAttribute(key, caseSensitive ? ComparisonQuery.Comparison.endsWith : ComparisonQuery.Comparison.caseInsensitiveEndsWith, suffix));

		return this;
	}

	@Override
	public QueryGroup<T> matches(final PropertyKey<String> key, final String regex) {

		searchItems.add(new ComparisonSearchAttribute(key, ComparisonQuery.Comparison.matches, regex));

		return this;
	}

	@Override
	public <P> QueryGroup<T> range(final PropertyKey<P> key, final P rangeStart, final P rangeEnd) {

		return range(key, rangeStart, rangeEnd, true, true);
	}

	@Override
	public <P> QueryGroup<T> range(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd) {

		searchItems.add(new RangeSearchAttribute(key, rangeStart, rangeEnd, includeStart, includeEnd));

		return this;
	}

	@Override
	public <P> QueryGroup<T> fulltext(final PropertyKey<P> key, final String value) {
		searchItems.add(new FulltextSearchAttribute(key, value));
		return this;
	}

	// ----- interface Query<T> -----
	@Override
	public QueryContext getQueryContext() {
		return query.getQueryContext();
	}

	@Override
	public Query<T> isPing(boolean isPing) {
		return query.isPing(isPing);
	}

	@Override
	public ResultStream<T> getResultStream() throws FrameworkException {
		return query.getResultStream();
	}

	@Override
	public List<T> getAsList() throws FrameworkException {
		return query.getAsList();
	}

	@Override
	public T getFirst() throws FrameworkException {
		return query.getFirst();
	}

	@Override
	public Traits getTraits() {
		return query.getTraits();
	}

	@Override
	public Query<T> disableSorting() {
		return query.disableSorting();
	}

	@Override
	public Query<T> sort(final SortOrder sortOrder) {
		return query.sort(sortOrder);
	}

	@Override
	public Query<T> sort(final PropertyKey key, final boolean descending) {
		return query.sort(key, descending);
	}

	@Override
	public Query<T> comparator(final Comparator<T> comparator) {
		return query.comparator(comparator);
	}

	@Override
	public Query<T> pageSize(final int pageSize) {
		return query.pageSize(pageSize);
	}

	@Override
	public Query<T> page(final int page) {
		return query.page(page);
	}

	@Override
	public Query<T> publicOnly() {
		return query.publicOnly();
	}

	@Override
	public Query<T> includeHidden() {
		return query.includeHidden();
	}

	@Override
	public Query<T> publicOnly(final boolean publicOnly) {
		return query.publicOnly(publicOnly);
	}

	@Override
	public Query<T> includeHidden(final boolean includeHidden) {
		return query.includeHidden(includeHidden);
	}

	@Override
	public QueryGroup<T> and() {

		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(securityContext, this, Operation.AND);
		searchItems.add(group);

		return group;
	}

	@Override
	public QueryGroup<T> or() {

		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(securityContext, this, Operation.OR);
		searchItems.add(group);

		return group;
	}

	@Override
	public QueryGroup<T> not() {

		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(securityContext, this, Operation.NOT);
		searchItems.add(group);

		return group;
	}

	@Override
	public Predicate<GraphObject> toPredicate() {
		return query.toPredicate();
	}

	@Override
	public void doNotSort(final boolean doNotSort) {
		query.doNotSort(doNotSort);
	}

	@Override
	public void setTraits(final Traits traits) {
		query.setTraits(traits);
	}
}
