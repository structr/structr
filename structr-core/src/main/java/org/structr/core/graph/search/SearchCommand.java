/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.index.Index;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryContext;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.GraphObjectComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.Factory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;

/**
 *
 *
 */
public abstract class SearchCommand<S extends PropertyContainer, T extends GraphObject> extends NodeServiceCommand implements org.structr.core.app.Query<T> {

	private static final Logger logger = LoggerFactory.getLogger(SearchCommand.class.getName());

	protected static final boolean INCLUDE_DELETED_AND_HIDDEN = true;
	protected static final boolean PUBLIC_ONLY		  = false;

	private static final Set<PropertyKey> indexedWarningDisabled    = new LinkedHashSet<>(Arrays.asList(SchemaMethod.source, SchemaProperty.readFunction, SchemaProperty.writeFunction));
	private static final Map<String, Set<String>> subtypeMapForType = new LinkedHashMap<>();
	private static final Set<String> baseTypes                      = new LinkedHashSet<>();

	public static final String LAT_LON_SEARCH_KEYWORD     = "latlon";
	public static final String LOCATION_SEARCH_KEYWORD    = "location";
	public static final String STATE_SEARCH_KEYWORD       = "state";
	public static final String HOUSE_SEARCH_KEYWORD       = "house";
	public static final String COUNTRY_SEARCH_KEYWORD     = "country";
	public static final String POSTAL_CODE_SEARCH_KEYWORD = "postalCode";
	public static final String DISTANCE_SEARCH_KEYWORD    = "distance";
	public static final String CITY_SEARCH_KEYWORD        = "city";
	public static final String STREET_SEARCH_KEYWORD      = "street";

	static {

		baseTypes.add(RelationshipInterface.class.getSimpleName());
		baseTypes.add(AbstractRelationship.class.getSimpleName());
		baseTypes.add(NodeInterface.class.getSimpleName());
		baseTypes.add(AbstractNode.class.getSimpleName());
	}

	private final SearchAttributeGroup rootGroup = new SearchAttributeGroup(Occurrence.REQUIRED);
	private SearchAttributeGroup currentGroup    = rootGroup;
	private Comparator<T> comparator             = null;
	private PropertyKey sortKey                  = null;
	private boolean publicOnly                   = false;
	private boolean includeHidden                = true;
	private boolean sortDescending               = false;
	private boolean doNotSort                    = false;
	private Class type                           = null;
	private int pageSize                         = Integer.MAX_VALUE;
	private int page                             = 1;
	private QueryContext queryContext            = new QueryContext();

	public abstract Factory<S, T> getFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page);
	public abstract boolean isRelationshipSearch();
	public abstract Index<S> getIndex();

	private ResultStream<T> doSearch() throws FrameworkException {

		if (page == 0 || pageSize <= 0) {

			return PagingIterable.EMPTY_ITERABLE;
		}

		final Factory<S, T> factory  = getFactory(securityContext, includeHidden, publicOnly, pageSize, page);
		boolean hasGraphSources      = false;
		boolean hasSpatialSource     = false;

		if (securityContext.getUser(false) == null && !isRelationshipSearch()) {

			rootGroup.add(new PropertySearchAttribute(GraphObject.visibleToPublicUsers, true, Occurrence.REQUIRED, true));

		} else if (securityContext.getUser(false) == null && isRelationshipSearch()) {

			rootGroup.add(new RelationshipVisibilitySearchAttribute());

		}

		// special handling of deleted and hidden flags
		if (!includeHidden && !isRelationshipSearch()) {

			rootGroup.add(new PropertySearchAttribute(NodeInterface.hidden,  true, Occurrence.FORBIDDEN, true));
		}

		// At this point, all search attributes are ready
		final List<SourceSearchAttribute> sources    = new ArrayList<>();
		boolean hasEmptySearchFields                 = false;
		boolean hasRelationshipVisibilitySearch      = false;
		Iterable indexHits                           = null;

		// check for optional-only queries
		// (some query types seem to allow no MUST occurs)
		for (final Iterator<SearchAttribute> it = rootGroup.getSearchAttributes().iterator(); it.hasNext();) {

			final SearchAttribute attr = it.next();

			if (attr instanceof SearchAttributeGroup) {

				// fixme: this needs to be done recursively, but how?
				for (final Iterator<SearchAttribute> groupIterator = ((SearchAttributeGroup)attr).getSearchAttributes().iterator(); groupIterator.hasNext();) {

					final SearchAttribute item = groupIterator.next();
					if (item instanceof SourceSearchAttribute) {

						sources.add((SourceSearchAttribute)item);

						// remove attribute from filter list
						groupIterator.remove();

						hasGraphSources = true;

					}

					if (item instanceof EmptySearchAttribute) {
						hasEmptySearchFields = true;
					}
				}
			}

			// check for distance search and initialize
			if (attr instanceof DistanceSearchAttribute) {

				final DistanceSearchAttribute distanceSearch = (DistanceSearchAttribute) attr;
				if (distanceSearch.needsGeocding()) {

					final GeoCodingResult coords = GeoHelper.geocode(distanceSearch);
					if (coords != null) {

						distanceSearch.setCoords(coords.toArray());
					}
				}

				hasSpatialSource = true;
			}

			// store source attributes for later use
			if (attr instanceof SourceSearchAttribute) {

				sources.add((SourceSearchAttribute)attr);

				hasGraphSources = true;
			}

			if (attr instanceof EmptySearchAttribute) {
				hasEmptySearchFields = true;
			}

			if (attr instanceof RelationshipVisibilitySearchAttribute) {
				hasRelationshipVisibilitySearch = true;
			}
		}

		// only do "normal" query if no other sources are present
		// use filters to filter sources otherwise
		if (!hasSpatialSource && !sources.isEmpty()) {

			indexHits = new LinkedList<>();

		} else {

			// apply sorting
			if (sortKey != null && !doNotSort) {

				rootGroup.setSortKey(sortKey);
				rootGroup.sortDescending(sortDescending);
			}

			final Index<S> index = getIndex();
			if (index != null) {

				// paging needs to be done AFTER instantiating all nodes
				if (hasEmptySearchFields || comparator != null) {
					factory.disablePaging();
				}

				// do query
				indexHits = Iterables.map(factory, index.query(getQueryContext(), rootGroup));
				//indexHits = new PagingIterable<>(Iterables.map(factory, index.query(getQueryContext(), rootGroup)), pageSize, page);

				if (comparator != null) {

					// pull results into memory
					final List<T> rawResult = Iterables.toList(indexHits);

					// sort result
					Collections.sort(rawResult, comparator);

					// return paging iterable
					return new PagingIterable(rawResult, pageSize, page);
				}
			}
		}

		if (indexHits != null && (hasEmptySearchFields || hasGraphSources || hasSpatialSource || hasRelationshipVisibilitySearch)) {

			// sorted result set
			final Set<T> intermediateResultSet = new LinkedHashSet<>(Iterables.toList(indexHits));
			final List<T> finalResult          = new ArrayList<>();

			// We need to find out whether there was a source for any of the possible sets that we want to merge.
			// If there was only a single source, the final result is the result of that source. If there are
			// multiple sources, the result is the intersection of all the sources, depending on the occur flag.

			if (hasGraphSources) {

				// merge sources according to their occur flag
				final Set<T> mergedSources = mergeSources(sources);

				if (hasSpatialSource) {

					// CHM 2014-02-24: preserve sorting of intermediate result, might be sorted by distance which we cannot reproduce easily
					intermediateResultSet.retainAll(mergedSources);

				} else {

					intermediateResultSet.addAll(mergedSources);
				}
			}

			// Filter intermediate result
			for (final T obj : intermediateResultSet) {

				boolean addToResult = true;

				// check all attributes before adding a node
				for (SearchAttribute attr : rootGroup.getSearchAttributes()) {

					// check all search attributes
					addToResult &= attr.includeInResult(obj);
				}

				if (addToResult) {

					finalResult.add(obj);
				}
			}

			// sort list
			Collections.sort(finalResult, new GraphObjectComparator(sortKey, sortDescending));

			return new PagingIterable(finalResult, pageSize, page);

			// return paged final result
			//return new Result(PagingHelper.subList(finalResult, pageSize, page), resultCount, true, false);

		} else {

			// no filtering
			return new PagingIterable(indexHits, pageSize, page);
		}
	}

	private Set<T> mergeSources(List<SourceSearchAttribute> sources) {

		final Set<T> mergedResult = new LinkedHashSet<>();
		boolean alreadyAdded      = false;

		for (final Iterator<SourceSearchAttribute> it = sources.iterator(); it.hasNext();) {

			SourceSearchAttribute attr = it.next();

			if (!alreadyAdded) {

				mergedResult.addAll(attr.getResult());
				alreadyAdded = true;

			} else {

				switch (attr.getOccurrence()) {

					case REQUIRED:

						mergedResult.retainAll(attr.getResult());
						break;

					case OPTIONAL:

						mergedResult.addAll(attr.getResult());
						break;

					case FORBIDDEN:
						mergedResult.removeAll(attr.getResult());
						break;
				}
			}
		}


		return mergedResult;
	}

	@Override
	public ResultStream<T> getResultStream() throws FrameworkException {
		return doSearch();
	}

	@Override
	public List<T> getAsList() throws FrameworkException {
		return Iterables.toList(doSearch());
	}

	@Override
	public T getFirst() throws FrameworkException {

		final List<T> result = getAsList();

		if (result.isEmpty()) {

			return null;
		}

		return result.get(0);
	}

	// ----- builder methods -----
	@Override
	public org.structr.core.app.Query<T> disableSorting() {
		this.doNotSort = true;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> sort(final PropertyKey key) {
		return sortAscending(key);
	}

	@Override
	public org.structr.core.app.Query<T> sortAscending(final PropertyKey key) {

		this.doNotSort      = false;
		this.sortDescending = false;
		this.sortKey        = key;

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> sortDescending(final PropertyKey key) {

		this.doNotSort      = false;
		this.sortDescending = true;
		this.sortKey        = key;

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> order(final boolean descending) {

		this.doNotSort      = false;
		this.sortDescending = descending;

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> comparator(final Comparator<T> comparator) {

		this.doNotSort  = false;
		this.comparator = comparator;

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> pageSize(final int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> page(final int page) {
		this.page = page;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> publicOnly() {
		this.publicOnly = true;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> publicOnly(final boolean publicOnly) {
		this.publicOnly = publicOnly;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> includeHidden() {
		this.includeHidden = true;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> includeHidden(final boolean includeHidden) {
		this.includeHidden = includeHidden;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> uuid(final String uuid) {

		doNotSort = true;

		return and(GraphObject.id, uuid);
	}

	@Override
	public org.structr.core.app.Query<T> andType(final Class type) {

		this.type = type;

		currentGroup.getSearchAttributes().add(new TypeSearchAttribute(type, Occurrence.REQUIRED, true));
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> orType(final Class type) {

		this.type = type;

		currentGroup.getSearchAttributes().add(new TypeSearchAttribute(type, Occurrence.OPTIONAL, true));
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> andTypes(final Class type) {

		this.type = type;

		andType(type);

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> orTypes(final Class type) {

		this.type = type;

		orType(type);

		return this;
	}

	@Override
	public Class getType() {
		return this.type;
	}

	@Override
	public org.structr.core.app.Query<T> andName(final String name) {
		return and(AbstractNode.name, name);
	}

	@Override
	public org.structr.core.app.Query<T> orName(final String name) {
		return or(AbstractNode.name, name);
	}

	@Override
	public org.structr.core.app.Query<T> location(final double latitude, final double longitude, final double distance) {
		currentGroup.getSearchAttributes().add(new DistanceSearchAttribute(latitude, longitude, distance, Occurrence.REQUIRED));
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> location(final String street, final String postalCode, final String city, final String country, final double distance) {
		return location(street, null, postalCode, city, null, country, distance);
	}

	@Override
	public org.structr.core.app.Query<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance) {
		return location(street, null, postalCode, city, state, country, distance);
	}

	@Override
	public org.structr.core.app.Query<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance) {
		currentGroup.getSearchAttributes().add(new DistanceSearchAttribute(street, house, postalCode, city, state, country, distance, Occurrence.REQUIRED));
		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> and(final PropertyKey<P> key, final P value) {

		if (GraphObject.id.equals(key)) {
			this.doNotSort = false;
		}

		return and(key, value, true);
	}

	@Override
	public <P> org.structr.core.app.Query<T> and(final PropertyKey<P> key, final P value, final boolean exact) {

		if (GraphObject.id.equals(key)) {

			this.doNotSort = false;
		}

		assertPropertyIsIndexed(key);

		currentGroup.getSearchAttributes().add(key.getSearchAttribute(securityContext, Occurrence.REQUIRED, value, exact, this));

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> and(final PropertyMap attributes) {

		for (final Map.Entry<PropertyKey, Object> entry : attributes.entrySet()) {

			final PropertyKey key = entry.getKey();
			final Object value = entry.getValue();

			if (GraphObject.id.equals(key)) {

				this.doNotSort = false;
			}

			and(key, value);
		}


		return this;
	}

	@Override
	public org.structr.core.app.Query<T> and() {

		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, Occurrence.REQUIRED);

		currentGroup.getSearchAttributes().add(group);
		currentGroup = group;

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> or(final PropertyKey<P> key, P value) {
		return or(key, value, true);
	}

	@Override
	public <P> org.structr.core.app.Query<T> or(final PropertyKey<P> key, P value, final boolean exact) {

		currentGroup.getSearchAttributes().add(key.getSearchAttribute(securityContext, Occurrence.OPTIONAL, value, exact, this));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> or(final PropertyMap attributes) {

		for (final Map.Entry<PropertyKey, Object> entry : attributes.entrySet()) {

			final PropertyKey key = entry.getKey();
			final Object value = entry.getValue();

			or(key, value);
		}


		return this;
	}

	@Override
	public org.structr.core.app.Query<T> notBlank(final PropertyKey key) {

		currentGroup.getSearchAttributes().add(new NotBlankSearchAttribute(key));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> blank(final PropertyKey key) {

		currentGroup.getSearchAttributes().add(new EmptySearchAttribute(key, null));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd) {

		currentGroup.getSearchAttributes().add(new RangeSearchAttribute(key, rangeStart, rangeEnd, Occurrence.REQUIRED, includeStart, includeEnd));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd) {

		currentGroup.getSearchAttributes().add(new RangeSearchAttribute(key, rangeStart, rangeEnd, Occurrence.REQUIRED, true, true));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd) {

		currentGroup.getSearchAttributes().add(new RangeSearchAttribute(key, rangeStart, rangeEnd, Occurrence.OPTIONAL, true, true));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> or() {

		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, Occurrence.OPTIONAL);
		currentGroup.getSearchAttributes().add(group);
		currentGroup = group;

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> not() {

		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, Occurrence.FORBIDDEN);
		currentGroup.getSearchAttributes().add(group);
		currentGroup = group;

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> parent() {

		// one level up
		final SearchAttributeGroup parent = currentGroup.getParent();
		if (parent != null) {

			currentGroup = parent;
		}

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> attributes(final List<SearchAttribute> attributes) {

		currentGroup.getSearchAttributes().addAll(attributes);
		return this;
	}

	@Override
	public Predicate<GraphObject> toPredicate() {
		return new AndPredicate(rootGroup.getSearchAttributes());
	}

	@Override
	public Iterator<T> iterator() {

		try {
			return getAsList().iterator();

		} catch (FrameworkException fex) {

			// there is no way to handle this elegantly with the
			// current Iterator<> interface, so we just have to
			// drop the exception here, which is ugly ugly ugly. :(
			logger.warn("", fex);
		}

		return null;
	}

	@Override
	public SearchAttributeGroup getRootAttributeGroup() {
		return rootGroup;
	}

	// ----- static methods -----
	public static synchronized void clearInheritanceMap() {
		subtypeMapForType.clear();
	}

	public static synchronized Set<String> getAllSubtypesAsStringSet(final String type) {

		Set<String> allSubtypes = subtypeMapForType.get(type);
		if (allSubtypes == null) {

			logger.debug("Subtype map cache miss.");

			allSubtypes = new LinkedHashSet<>();
			subtypeMapForType.put(type, allSubtypes);

			final ConfigurationProvider configuration                             = StructrApp.getConfiguration();
			final Map<String, Class<? extends NodeInterface>> nodeEntities        = configuration.getNodeEntities();
			final Map<String, Class<? extends RelationshipInterface>> relEntities = configuration.getRelationshipEntities();

			// add type first (this is neccesary because two class objects of the same dynamic type node are not equal
			// to each other and not assignable, if the schema node was modified in the meantime)
			allSubtypes.add(type);

			// scan all node entities for subtypes
			for (final Map.Entry<String, Class<? extends NodeInterface>> entity : nodeEntities.entrySet()) {

				final Class entityType     = entity.getValue();
				final Set<Class> ancestors = typeAndAllSupertypes(entityType);

				for (final Class superClass : ancestors) {

					final String superClasSimpleName = superClass.getSimpleName();
					final String superClassFullName  = superClass.getName();

					if ((superClassFullName.startsWith("org.structr.") || superClassFullName.startsWith("com.structr.")) && superClasSimpleName.equals(type)) {

						allSubtypes.add(entityType.getSimpleName());
					}
				}
			}

			// scan all relationship entities for subtypes
			for (final Map.Entry<String, Class<? extends RelationshipInterface>> entity : relEntities.entrySet()) {

				final Class entityType     = entity.getValue();
				final Set<Class> ancestors = typeAndAllSupertypes(entityType);

				for (final Class superClass : ancestors) {

					final String superClasSimpleName = superClass.getSimpleName();
					final String superClassFullName  = superClass.getName();

					if ((superClassFullName.startsWith("org.structr.") || superClassFullName.startsWith("com.structr.")) && superClasSimpleName.equals(type)) {

						allSubtypes.add(entityType.getSimpleName());
					}
				}
			}

		} else {

			logger.debug("Subtype map cache hit.");
		}

		return Collections.unmodifiableSet(allSubtypes);
	}

	public static boolean isTypeAssignableFromOtherType (Class type, Class otherType) {

		return getAllSubtypesAsStringSet(type.getSimpleName()).contains(otherType.getSimpleName());

	}

	public static Set<Class> typeAndAllSupertypes(final Class type) {

		final ConfigurationProvider configuration = StructrApp.getConfiguration();
		final Set<Class> allSupertypes            = new LinkedHashSet<>();

		Class localType = type;

		while (localType != null && !localType.equals(Object.class)) {

			allSupertypes.add(localType);
			allSupertypes.addAll(configuration.getInterfacesForType(localType));

			localType = localType.getSuperclass();
		}

		// remove base types
		allSupertypes.removeAll(baseTypes);

		return allSupertypes;
	}

	@Override
	public void setQueryContext(final QueryContext queryContext) {
		this.queryContext = queryContext;
	}

	@Override
	public QueryContext getQueryContext() {
		return queryContext;
	}

	@Override
	public org.structr.core.app.Query<T> isPing(final boolean isPing) {

		getQueryContext().isPing(isPing);
		return this;
	}

	// ----- private methods ----
	private void assertPropertyIsIndexed(final PropertyKey key) {

		if (key != null && !key.isIndexed() && key instanceof AbstractPrimitiveProperty) {

			final Class declaringClass = key.getDeclaringClass();
			String className           = "";

			if (declaringClass != null) {

				className = declaringClass.getSimpleName() + ".";
			}

			// disable warning for keys we know are not indexed and cannot
			// easily be indexed because of the character limit of 4000..
			if (!indexedWarningDisabled.contains(key)) {

				logger.warn("Non-indexed property key {}{} is used in query. This can lead to performance problems in large databases.", className, key.jsonName());
			}
		}
	}

	// ----- nested classes -----
	private class AndPredicate implements Predicate<GraphObject> {

		final List<Predicate<GraphObject>> predicates = new ArrayList<>();

		public AndPredicate(final List<SearchAttribute> searchAttributes) {

			for (final SearchAttribute attr : searchAttributes) {

				if (attr instanceof SearchAttributeGroup) {

					for (final SearchAttribute groupAttr : ((SearchAttributeGroup)attr).getSearchAttributes()) {
						// ignore type search attributes as the nodes will
						// already have the correct type when arriving here
						if (groupAttr instanceof TypeSearchAttribute) {
							continue;
						}

						predicates.add(attr);
					}

				} else {

					// ignore type search attributes as the nodes will
					// already have the correct type when arriving here
					if (attr instanceof TypeSearchAttribute) {
						continue;
					}

					predicates.add(attr);
				}
			}
		}

		@Override
		public boolean accept(GraphObject obj) {

			boolean result = true;

			for (final Predicate<GraphObject> predicate : predicates) {

				result &= predicate.accept(obj);
			}

			return result;
		}
	}


}
