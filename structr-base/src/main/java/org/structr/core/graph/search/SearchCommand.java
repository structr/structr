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
package org.structr.core.graph.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.index.Index;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.*;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;
import org.structr.schema.ConfigurationProvider;

import java.util.*;
import org.structr.api.search.ComparisonQuery;
import org.structr.schema.SchemaService;

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

	static {

		baseTypes.add(RelationshipInterface.class.getSimpleName());
		baseTypes.add(AbstractRelationship.class.getSimpleName());
		baseTypes.add(NodeInterface.class.getSimpleName());
		baseTypes.add(AbstractNode.class.getSimpleName());
	}

	private final SearchAttributeGroup rootGroup = new SearchAttributeGroup(Occurrence.REQUIRED);
	private SortOrder sortOrder                  = new DefaultSortOrder();
	private QueryContext queryContext            = new QueryContext();
	private SearchAttributeGroup currentGroup    = rootGroup;
	private Comparator comparator                = null;
	private boolean publicOnly                   = false;
	private boolean includeHidden                = true;
	private boolean prefetchingEnabled           = true;
	private boolean doNotSort                    = false;
	private Class type                           = null;
	private int pageSize                         = Integer.MAX_VALUE;
	private int page                             = 1;

	public abstract Factory<S, T> getFactory(final SecurityContext securityContext, final boolean includeHidden, final boolean publicOnly, final int pageSize, final int page);
	public abstract boolean isRelationshipSearch();
	public abstract Index<S> getIndex();

	private ResultStream<T> doSearch(final String description) throws FrameworkException {

		if (page == 0 || pageSize <= 0) {

			return PagingIterable.EMPTY_ITERABLE;
		}

		// automatic prefetching
		if (prefetchingEnabled && type != null) {

			final Set<Class> schemaTypes = Set.of(SchemaNode.class, SchemaMethod.class, SchemaProperty.class, SchemaView.class);
			if (schemaTypes.contains(type)) {

				SchemaService.prefetchSchemaNodes(TransactionCommand.getCurrentTransaction());
			}
		}

		final Factory<S, T> factory  = getFactory(securityContext, includeHidden, publicOnly, pageSize, page);
		final Principal user         = securityContext.getUser(false);
		final SearchConfig config    = new SearchConfig();

		if (user == null) {

			if (isRelationshipSearch()) {

				rootGroup.add(new RelationshipVisibilitySearchAttribute());
			}

		} else {

			if (user.isAdmin()) {

				queryContext.setIsSuperuser(user.isAdmin());

				if (queryContext.isSliced()) {

					page     = queryContext.getPage();
					pageSize = queryContext.getPageSize();
				}
			}
		}

		// special handling of deleted and hidden flags
		if (!includeHidden && !isRelationshipSearch()) {

			rootGroup.add(new PropertySearchAttribute(NodeInterface.hidden,  true, Occurrence.FORBIDDEN, true));
		}

		// At this point, all search attributes are ready
		final List<SourceSearchAttribute> sources    = new ArrayList<>();
		Iterable indexHits                           = null;

		// resolve search attribute groups
		handleSearchAttributeGroup(config, rootGroup, sources);

		// only do "normal" query if no other sources are present
		// use filters to filter sources otherwise
		if (!config.hasSpatialSource && !sources.isEmpty()) {

			indexHits = new LinkedList<>();

		} else {

			// apply sorting
			if (!sortOrder.isEmpty() && !doNotSort) {

				rootGroup.setSortOrder(sortOrder);
			}

			final Index<S> index = getIndex();
			if (index != null) {

				// paging needs to be done AFTER instantiating all nodes
				if (config.hasEmptySearchFields || comparator != null) {
					factory.disablePaging();
				}

				// do query
				indexHits = Iterables.map(factory, index.query(getQueryContext(), rootGroup, pageSize, page));

				if (comparator != null) {

					// pull results into memory
					final List<T> rawResult = Iterables.toList(indexHits);

					// sort result
					Collections.sort(rawResult, comparator);

					// return paging iterable
					return new PagingIterable(description, rawResult, pageSize, page, queryContext.getSkipped());
				}
			}
		}

		//if (indexHits != null && (config.hasEmptySearchFields || config.hasGraphSources || config.hasSpatialSource || config.hasRelationshipVisibilitySearch)) {
		if (indexHits != null && (config.hasGraphSources || config.hasSpatialSource || config.hasRelationshipVisibilitySearch)) {

			// sorted result set
			final Set<T> intermediateResultSet = new LinkedHashSet<>(Iterables.toList(indexHits));
			final List<T> finalResult          = new ArrayList<>();

			// We need to find out whether there was a source for any of the possible sets that we want to merge.
			// If there was only a single source, the final result is the result of that source. If there are
			// multiple sources, the result is the intersection of all the sources, depending on the occur flag.

			if (config.hasGraphSources) {

				// merge sources according to their occur flag
				final Set<T> mergedSources = mergeSources(sources);

				if (config.hasSpatialSource) {

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
			if (!sortOrder.isEmpty()) {

				Collections.sort(finalResult, sortOrder);
			}

			return new PagingIterable(description, finalResult, pageSize, page, queryContext.getSkipped());

		} else {

			if (!sortOrder.isEmpty()) {

				final List<T> finalResult = new LinkedList<>(Iterables.toList(indexHits));
				Collections.sort(finalResult, sortOrder);
				return new PagingIterable(description, finalResult, pageSize, page, queryContext.getSkipped());
			}
			// no filtering
			return new PagingIterable(description, indexHits, pageSize, page, queryContext.getSkipped());
		}
	}

	private void handleSearchAttributeGroup(final SearchConfig config, final SearchAttributeGroup group, final List<SourceSearchAttribute> sources) throws FrameworkException {

		// check for optional-only queries
		// (some query types seem to allow no MUST occurs)
		for (final Iterator<SearchAttribute> it = group.getSearchAttributes().iterator(); it.hasNext();) {

			final SearchAttribute attr = it.next();

			if (attr instanceof SearchAttributeGroup) {

				handleSearchAttributeGroup(config, (SearchAttributeGroup)attr, sources);
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

				config.hasSpatialSource = true;
			}

			// store source attributes for later use
			if (attr instanceof SourceSearchAttribute) {

				sources.add((SourceSearchAttribute)attr);

				config.hasGraphSources = true;
			}

			if (attr instanceof GraphSearchAttribute) {
				config.hasGraphSources = true;
			}

			if (attr instanceof EmptySearchAttribute) {
				config.hasEmptySearchFields = true;
			}

			if (attr instanceof RelationshipVisibilitySearchAttribute) {
				config.hasRelationshipVisibilitySearch = true;
			}
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
		return doSearch("getResultStream" + getQueryDescription());
	}

	@Override
	public List<T> getAsList() throws FrameworkException {
		return Iterables.toList(doSearch("getAsList" + getQueryDescription()));
	}

	@Override
	public T getFirst() throws FrameworkException {

		for (final T result : doSearch("getFirst" + getQueryDescription())) {

			return result;
		}

		return null;
	}

	// ----- builder methods -----
	@Override
	public org.structr.core.app.Query<T> disableSorting() {
		this.doNotSort = true;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> sort(final SortOrder sortOrder) {

		if (sortOrder != null) {

			this.doNotSort = false;
			this.sortOrder = sortOrder;
		}

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> sort(final PropertyKey sortKey, final boolean sortDescending) {

		this.doNotSort  = false;

		if (sortOrder instanceof DefaultSortOrder) {
			((DefaultSortOrder)sortOrder).addElement(sortKey, sortDescending);
		}

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

		return and(key, value, exact, Occurrence.REQUIRED);
	}

	@Override
	public <P> org.structr.core.app.Query<T> and(final PropertyKey<P> key, final P value, final boolean exact, final Occurrence occur) {

		if (GraphObject.id.equals(key)) {

			this.doNotSort = false;
		}

		assertPropertyIsIndexed(key);

		currentGroup.getSearchAttributes().add(key.getSearchAttribute(securityContext, occur, value, exact, this));

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

		if (key.relatedType() != null) {

			// related nodes
			if (key.isCollection()) {

				currentGroup.getSearchAttributes().add(key.getSearchAttribute(securityContext, Occurrence.EXACT, Collections.EMPTY_LIST, true, this));

			} else {

				currentGroup.getSearchAttributes().add(key.getSearchAttribute(securityContext, Occurrence.EXACT, null, true, this));
			}

		} else {

			// everything else
			currentGroup.getSearchAttributes().add(new EmptySearchAttribute(key, null));
		}

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> startsWith(final PropertyKey<P> key, final P prefix, final boolean caseSensitive) {

		currentGroup.getSearchAttributes().add(new ComparisonSearchAttribute(key, caseSensitive ? ComparisonQuery.Operation.startsWith : ComparisonQuery.Operation.caseInsensitiveStartsWith, prefix, Occurrence.EXACT));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> endsWith(final PropertyKey<P> key, final P suffix, final boolean caseSensitive) {

		currentGroup.getSearchAttributes().add(new ComparisonSearchAttribute(key, caseSensitive ? ComparisonQuery.Operation.endsWith : ComparisonQuery.Operation.caseInsensitiveEndsWith, suffix, Occurrence.EXACT));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd) {

		return andRange(key, rangeStart, rangeEnd, true, true);
	}

	@Override
	public <P> org.structr.core.app.Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd) {

		currentGroup.getSearchAttributes().add(new RangeSearchAttribute(key, rangeStart, rangeEnd, Occurrence.REQUIRED, includeStart, includeEnd));

		assertPropertyIsIndexed(key);

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd) {

		return orRange(key, rangeStart, rangeEnd, true, true);
	}

	@Override
	public <P> org.structr.core.app.Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd, final boolean includeStart, final boolean includeEnd) {

		currentGroup.getSearchAttributes().add(new RangeSearchAttribute(key, rangeStart, rangeEnd, Occurrence.OPTIONAL, includeStart, includeEnd));

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
	public SearchAttributeGroup getRootAttributeGroup() {
		return rootGroup;
	}

	@Override
	public Occurrence getCurrentOccurrence() {
		return currentGroup.getOccurrence();
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

	@Override
	public void overrideFetchSize(final int fetchSizeForThisRequest) {
		queryContext.overrideFetchSize(fetchSizeForThisRequest);
	}

	public org.structr.core.app.Query<T> disablePrefetching() {
		prefetchingEnabled = false;
		return this;
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

	public static void prefetch(final Class type, final String uuid) {

		final Set<String> keys    = new LinkedHashSet<>();
		final Set<String> types1  = new LinkedHashSet<>();
		final Set<String> types2  = new LinkedHashSet<>();

		final Set<PropertyKey> all = StructrApp.getConfiguration().getPropertySet(type, PropertyView.All);

		for (final PropertyKey<?> key : all) {

			if (key instanceof RelationProperty relationProperty) {

				final Relation relation   = relationProperty.getRelation();
				final Direction direction = relation.getDirectionForType(type);
				final String name         = relation.name();

				for (final Class s : SearchCommand.typeAndAllSupertypes(relation.getSourceType())) {

					if (!s.isInterface()) {

						types1.add(s.getSimpleName());
					}
				}

				for (final Class s : SearchCommand.typeAndAllSupertypes(relation.getTargetType())) {

					if (!s.isInterface()) {

						types2.add(s.getSimpleName());
					}
				}

				switch (direction) {

					case INCOMING -> keys.add("all/INCOMING/" + name);
					case OUTGOING -> keys.add("all/OUTGOING/" + name);
				}
			}
		}

		// intersect
		types1.retainAll(types2);

		types1.remove(AbstractNode.class.getSimpleName());
		types1.remove(NodeInterface.class.getSimpleName());
		types1.remove(type.getSimpleName());

		if (!types1.isEmpty()) {

			final List<String> list      = Iterables.toList(types1);
			final String commonBaseClass = list.get(0);

			TransactionCommand.getCurrentTransaction().prefetch(commonBaseClass, null, keys, false);
		}
	}

	// ----- private methods ----
	private void assertPropertyIsIndexed(final PropertyKey key) {

		if (key != null && !key.isIndexed() && key instanceof AbstractPrimitiveProperty) {

			final Class declaringClass = key.getDeclaringClass();
			String className           = "";

			if (declaringClass != null) {

				className = declaringClass.getSimpleName() + ".";
			}

			// disable logging for keys we know are not indexed and cannot
			// easily be indexed because of the character limit of 4000..
			if (!indexedWarningDisabled.contains(key)) {

				logger.debug("Non-indexed property key {}{} is used in query. This can lead to performance problems in large databases.", className, key.jsonName());
			}
		}
	}

	private String getQueryDescription() {

		if (type != null) {

			return "(" + type.getSimpleName() + ")";
		}

		return null;
	}

	// ----- nested classes -----
	private class AndPredicate implements Predicate<GraphObject> {

		final List<Predicate<GraphObject>> predicates = new ArrayList<>();

		public AndPredicate(final List<SearchAttribute> searchAttributes) {

			for (final SearchAttribute attr : searchAttributes) {

				if (attr instanceof SearchAttributeGroup) {

					final SearchAttributeGroup group            = (SearchAttributeGroup)attr;
					final List<SearchAttribute> groupAttributes = group.getSearchAttributes();

					if (groupAttributes.isEmpty()) {

						// empty group? check occur (might be a "NOT")


					} else {

						for (final SearchAttribute groupAttr : group.getSearchAttributes()) {

							// ignore type search attributes as the nodes will
							// already have the correct type when arriving here
							if (groupAttr instanceof TypeSearchAttribute) {
								continue;
							}

							predicates.add(attr);
						}
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


	private class SearchConfig {

		public boolean hasGraphSources                 = false;
		public boolean hasSpatialSource                = false;
		public boolean hasEmptySearchFields            = false;
		public boolean hasRelationshipVisibilitySearch = false;
	}
}
