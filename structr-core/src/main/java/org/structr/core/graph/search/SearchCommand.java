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
package org.structr.core.graph.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.Predicate;
import org.neo4j.index.lucene.QueryContext;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PagingHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.Factory;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;

/**
 *
 *
 */
public abstract class SearchCommand<S extends PropertyContainer, T extends GraphObject> extends NodeServiceCommand implements org.structr.core.app.Query<T> {

	private static final Logger logger                        = Logger.getLogger(SearchCommand.class.getName());

	protected static final boolean INCLUDE_DELETED_AND_HIDDEN = true;
	protected static final boolean PUBLIC_ONLY		  = false;

	private static final Map<String, Set<String>> subtypeMapForType = new LinkedHashMap<>();
	private static final Set<Character> specialCharsExact           = new LinkedHashSet<>();
	private static final Set<Character> specialChars                = new LinkedHashSet<>();
	private static final Set<String> baseTypes                      = new LinkedHashSet<>();

	public static final String LOCATION_SEARCH_KEYWORD    = "location";
	public static final String STATE_SEARCH_KEYWORD       = "state";
	public static final String HOUSE_SEARCH_KEYWORD       = "house";
	public static final String COUNTRY_SEARCH_KEYWORD     = "country";
	public static final String POSTAL_CODE_SEARCH_KEYWORD = "postalCode";
	public static final String DISTANCE_SEARCH_KEYWORD    = "distance";
	public static final String CITY_SEARCH_KEYWORD        = "city";
	public static final String STREET_SEARCH_KEYWORD      = "street";

	static {

		specialChars.add('\\');
		specialChars.add('+');
		specialChars.add('-');
		specialChars.add('!');
		specialChars.add('(');
		specialChars.add(')');
		specialChars.add(':');
		specialChars.add('^');
		specialChars.add('[');
		specialChars.add(']');
		specialChars.add('"');
		specialChars.add('{');
		specialChars.add('}');
		specialChars.add('~');
		specialChars.add('*');
		specialChars.add('?');
		specialChars.add('|');
		specialChars.add('&');
		specialChars.add(';');
		specialCharsExact.add('"');
		specialCharsExact.add('\\');

		baseTypes.add(RelationshipInterface.class.getSimpleName());
		baseTypes.add(AbstractRelationship.class.getSimpleName());
		baseTypes.add(NodeInterface.class.getSimpleName());
		baseTypes.add(AbstractNode.class.getSimpleName());
	}

	private final SearchAttributeGroup rootGroup = new SearchAttributeGroup(BooleanClause.Occur.MUST);
	private SearchAttributeGroup currentGroup    = rootGroup;
	private PropertyKey sortKey                  = null;
	private boolean publicOnly                   = false;
	private boolean includeDeletedAndHidden      = false;
	private boolean sortDescending               = false;
	private boolean doNotSort                    = false;
	private boolean exactSearch                  = true;
	private String offsetId                      = null;
	private int pageSize                         = Integer.MAX_VALUE;
	private int page                             = 1;

	public abstract Factory<S, T> getFactory(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly, final int pageSize, final int page, final String offsetId);
	public abstract Index<S> getFulltextIndex();
	public abstract Index<S> getKeywordIndex();
	public abstract LayerNodeIndex getSpatialIndex();
	public abstract boolean isRelationshipSearch();

	private Result<T> doSearch() throws FrameworkException {

		if (page == 0 || pageSize <= 0) {

			return Result.EMPTY_RESULT;
		}

		Factory<S, T> factory        = getFactory(securityContext, includeDeletedAndHidden, publicOnly, pageSize, page, offsetId);
		boolean filterResults        = true;
		boolean hasGraphSources      = false;
		boolean hasSpatialSource     = false;

		if (securityContext.getUser(false) == null) {

			rootGroup.add(new PropertySearchAttribute(GraphObject.visibleToPublicUsers, true, BooleanClause.Occur.MUST, true));

		}

		// special handling of deleted and hidden flags
		if (!includeDeletedAndHidden && !isRelationshipSearch()) {

			rootGroup.add(new PropertySearchAttribute(NodeInterface.hidden,  true, BooleanClause.Occur.MUST_NOT, true));
			rootGroup.add(new PropertySearchAttribute(NodeInterface.deleted, true, BooleanClause.Occur.MUST_NOT, true));
		}

		// At this point, all search attributes are ready
		List<SourceSearchAttribute> sources    = new ArrayList<>();
		DistanceSearchAttribute distanceSearch = null;
		boolean hasEmptySearchFields           = false;
		Result intermediateResult              = null;
		GeoCodingResult coords                 = null;
		Double dist                            = null;

		// check for optional-only queries
		// (some query types seem to allow no MUST occurs)
		for (Iterator<SearchAttribute> it = rootGroup.getSearchAttributes().iterator(); it.hasNext();) {

			SearchAttribute attr = it.next();

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

				distanceSearch = (DistanceSearchAttribute) attr;
				coords         = GeoHelper.geocode(distanceSearch);
				dist           = distanceSearch.getValue();

				// remove attribute from filter list
				it.remove();

				hasSpatialSource = true;
			}

			// store source attributes for later use
			if (attr instanceof SourceSearchAttribute) {

				sources.add((SourceSearchAttribute)attr);

				// remove attribute from filter list
				it.remove();

				hasGraphSources = true;
			}

			if (attr instanceof EmptySearchAttribute) {
				hasEmptySearchFields = true;
			}
		}

		// only do "normal" query if no other sources are present
		// use filters to filter sources otherwise
		if (distanceSearch == null && !sources.isEmpty()) {

			intermediateResult = new Result(new ArrayList<>(), null, false, false);

		} else {

			BooleanQuery query    = new BooleanQuery();
			boolean allExactMatch = true;

			// build query
			for (SearchAttribute attr : rootGroup.getSearchAttributes()) {

				Query queryElement = attr.getQuery();
				if (queryElement != null) {

					query.add(queryElement, attr.getOccur());
				}

				allExactMatch &= attr.isExactMatch();
			}

			QueryContext queryContext = new QueryContext(query);

			if (sortKey != null && !doNotSort) {

				Integer sortType = sortKey.getSortType();
				if (sortType != null) {

					queryContext.sort(new Sort(new SortField(sortKey.dbName(), sortType, sortDescending)));

				} else {

					queryContext.sort(new Sort(new SortField(sortKey.dbName(), Locale.getDefault(), sortDescending)));
				}

			}

			if (distanceSearch != null) {

				if (coords != null) {

					Map<String, Object> params = new HashMap<>();

					params.put(LayerNodeIndex.POINT_PARAMETER, coords.toArray());
					params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, dist);

					LayerNodeIndex spatialIndex = this.getSpatialIndex();
					if (spatialIndex != null) {

						try (final IndexHits hits = spatialIndex.query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params)) {

							// instantiate spatial search results without paging,
							// as the results must be filtered by type anyway
							intermediateResult = new NodeFactory(securityContext).instantiate(hits);

						}
					}
				}

			} else if (allExactMatch) {

//				try (final IndexHits hits = synchronizedLuceneQuery(getKeywordIndex(), queryContext)) {
				try (final IndexHits hits = getKeywordIndex().query(queryContext)) {

					filterResults      = hasEmptySearchFields;
					intermediateResult = factory.instantiate(hits);

				} catch (NumberFormatException nfe) {

					logger.log(Level.SEVERE, "Could not sort results", nfe);
				}

			} else {

				// Default: Mixed or fulltext-only search: Use fulltext index
				try (final IndexHits hits = getFulltextIndex().query(queryContext)) {

					filterResults      = hasEmptySearchFields;
					intermediateResult = factory.instantiate(hits);

				} catch (NumberFormatException nfe) {

					logger.log(Level.SEVERE, "Could not sort results", nfe);
				}
			}
		}

		if (intermediateResult != null && filterResults) {

			// sorted result set
			Set<GraphObject> intermediateResultSet = new LinkedHashSet<>(intermediateResult.getResults());
			List<GraphObject> finalResult          = new ArrayList<>();
			int resultCount                        = 0;

			// We need to find out whether there was a source for any of the possible sets that we want to merge.
			// If there was only a single source, the final result is the result of that source. If there are
			// multiple sources, the result is the intersection of all the sources, depending on the occur flag.

			if (hasGraphSources) {

				// merge sources according to their occur flag
				final Set<GraphObject> mergedSources = mergeSources(sources);

				if (hasSpatialSource) {

					// CHM 2014-02-24: preserve sorting of intermediate result, might be sorted by distance which we cannot reproduce easily
					intermediateResultSet.retainAll(mergedSources);

				} else {

					intermediateResultSet.addAll(mergedSources);
				}
			}

			// Filter intermediate result
			for (GraphObject obj : intermediateResultSet) {

				boolean addToResult = true;

				// check all attributes before adding a node
				for (SearchAttribute attr : rootGroup.getSearchAttributes()) {

					// check all search attributes
					addToResult &= attr.includeInResult(obj);
				}

				if (addToResult) {

					finalResult.add(obj);
					resultCount++;
				}
			}

			// sort list
			Collections.sort(finalResult, new GraphObjectComparator(sortKey, sortDescending));

			// return paged final result
			return new Result(PagingHelper.subList(finalResult, pageSize, page, offsetId), resultCount, true, false);

		} else {

			// no filtering
			return intermediateResult;
		}
	}

	private Set<GraphObject> mergeSources(List<SourceSearchAttribute> sources) {

		Set<GraphObject> mergedResult = new LinkedHashSet<>();
		boolean alreadyAdded          = false;

		for (Iterator<SourceSearchAttribute> it = sources.iterator(); it.hasNext();) {

			SourceSearchAttribute attr = it.next();

			if (!alreadyAdded) {

				mergedResult.addAll(attr.getResult());
				alreadyAdded = true;

			} else {

				switch (attr.getOccur()) {

					case MUST:

						mergedResult.retainAll(attr.getResult());
						break;

					case SHOULD:

						mergedResult.addAll(attr.getResult());
						break;

					case MUST_NOT:
						mergedResult.removeAll(attr.getResult());
						break;
				}
			}
		}


		return mergedResult;
	}

	@Override
	public Result<T> getResult() throws FrameworkException {
		return doSearch();
	}

	@Override
	public List<T> getAsList() throws FrameworkException {
		return getResult().getResults();
	}

	@Override
	public T getFirst() throws FrameworkException {

		final Result<T> result = getResult();
		if (result.isEmpty()) {

			return null;
		}

		return result.get(0);
	}

	@Override
	public boolean isExactSearch() {
		return exactSearch;
	}

	// ----- builder methods -----
	@Override
	public org.structr.core.app.Query<T> sort(final PropertyKey key) {
		return sortAscending(key);
	}

	@Override
	public org.structr.core.app.Query<T> sortAscending(final PropertyKey key) {

		this.doNotSort      = false;
		this.sortDescending = false;
		this.sortKey        = key;

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> sortDescending(final PropertyKey key) {

		this.doNotSort      = false;
		this.sortDescending = true;
		this.sortKey        = key;

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> order(final boolean descending) {

		this.doNotSort      = false;
		this.sortDescending = descending;

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
	public org.structr.core.app.Query<T> exact(final boolean exact) {

		if (!exact) {

			for (final SearchAttribute attr : rootGroup.getSearchAttributes()) {

				attr.setExactMatch(false);

			}
		}

		this.exactSearch = exact;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> includeDeletedAndHidden() {
		this.includeDeletedAndHidden = true;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> includeDeletedAndHidden(final boolean includeDeletedAndHidden) {
		this.includeDeletedAndHidden = includeDeletedAndHidden;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> offsetId(final String offsetId) {
		this.offsetId = offsetId;
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> uuid(final String uuid) {

		doNotSort = true;

		return and(GraphObject.id, uuid);
	}

	@Override
	public org.structr.core.app.Query<T> andType(final Class type) {

		currentGroup.getSearchAttributes().add(new TypeSearchAttribute(type, BooleanClause.Occur.MUST, exactSearch));
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> orType(final Class type) {

		currentGroup.getSearchAttributes().add(new TypeSearchAttribute(type, BooleanClause.Occur.SHOULD, exactSearch));
		return this;
	}

	public org.structr.core.app.Query<T> andType(final String type) {

		currentGroup.getSearchAttributes().add(new TypeSearchAttribute(type, BooleanClause.Occur.MUST, exactSearch));
		return this;
	}

	public org.structr.core.app.Query<T> orType(final String type) {

		currentGroup.getSearchAttributes().add(new TypeSearchAttribute(type, BooleanClause.Occur.SHOULD, exactSearch));
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> andTypes(final Class type) {

		// create a new search group
		and();

		for (final String subtype : getAllSubtypesAsStringSet(type.getSimpleName())) {
			orType(subtype);
		}

		// exit search group
		parent();

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> orTypes(final Class type) {

		// create a new search group
		or();

		for (final String subtype : getAllSubtypesAsStringSet(type.getSimpleName())) {
			orType(subtype);
		}

		// exit search group
		parent();

		return this;
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
	public org.structr.core.app.Query<T> location(final String street, final String postalCode, final String city, final String country, final double distance) {
		return location(street, null, postalCode, city, null, country, distance);
	}

	@Override
	public org.structr.core.app.Query<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance) {
		return location(street, null, postalCode, city, state, country, distance);
	}

	@Override
	public org.structr.core.app.Query<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance) {
		currentGroup.getSearchAttributes().add(new DistanceSearchAttribute(street, house, postalCode, city, state, country, distance, BooleanClause.Occur.MUST));
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

		exact(exact);
		currentGroup.getSearchAttributes().add(key.getSearchAttribute(securityContext, BooleanClause.Occur.MUST, value, exact, this));

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
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, BooleanClause.Occur.MUST);

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

		exact(exact);
		currentGroup.getSearchAttributes().add(key.getSearchAttribute(securityContext, BooleanClause.Occur.SHOULD, value, exact, this));

		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> or(final PropertyMap attributes) {

		for (Map.Entry<PropertyKey, Object> entry : attributes.entrySet()) {

			final PropertyKey key = entry.getKey();
			final Object value = entry.getValue();

			or(key, value);
		}


		return this;
	}

	@Override
	public org.structr.core.app.Query<T> notBlank(final PropertyKey key) {

		currentGroup.getSearchAttributes().add(new NotBlankSearchAttribute(key));
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> blank(final PropertyKey key) {

		currentGroup.getSearchAttributes().add(new EmptySearchAttribute(key, null));
		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> andRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd) {

		currentGroup.getSearchAttributes().add(new RangeSearchAttribute(key, rangeStart, rangeEnd, BooleanClause.Occur.MUST));
		return this;
	}

	@Override
	public <P> org.structr.core.app.Query<T> orRange(final PropertyKey<P> key, final P rangeStart, final P rangeEnd) {

		currentGroup.getSearchAttributes().add(new RangeSearchAttribute(key, rangeStart, rangeEnd, BooleanClause.Occur.SHOULD));
		return this;
	}

	@Override
	public org.structr.core.app.Query<T> or() {

		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, BooleanClause.Occur.SHOULD);
		currentGroup.getSearchAttributes().add(group);
		currentGroup = group;

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> not() {

		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, BooleanClause.Occur.MUST_NOT);
		currentGroup.getSearchAttributes().add(group);
		currentGroup = group;

		return this;
	}

	@Override
	public org.structr.core.app.Query<T> parent() {

		// one level up
		SearchAttributeGroup parent = currentGroup.getParent();
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
			fex.printStackTrace();
		}

		return null;
	}

	@Override
	public SearchAttributeGroup getRootAttributeGroup() {
		return rootGroup;
	}

	// ----- static methods -----
	public static String escapeForLucene(String input) {

		StringBuilder output = new StringBuilder();

		for (int i = 0; i < input.length(); i++) {

			char c = input.charAt(i);

			if (specialChars.contains(c) || Character.isWhitespace(c)) {

				output.append('\\');
			}

			output.append(c);

		}

		return output.toString();

	}

	public static synchronized void clearInheritanceMap() {
		subtypeMapForType.clear();
	}

	public static synchronized Set<String> getAllSubtypesAsStringSet(final String type) {

		Set<String> allSubtypes = subtypeMapForType.get(type);
		if (allSubtypes == null) {

			allSubtypes = new LinkedHashSet<>();
			subtypeMapForType.put(type, allSubtypes);

			final ConfigurationProvider configuration                             = StructrApp.getConfiguration();
			final Map<String, Class<? extends NodeInterface>> nodeEntities        = configuration.getNodeEntities();
			final Map<String, Class<? extends RelationshipInterface>> relEntities = configuration.getRelationshipEntities();

			// add type first (this is neccesary because two class objects of the same dynamic type node are not equal
			// to each other and not assignable, if the schema node was modified in the meantime)
			allSubtypes.add(type);

			// scan all node entities for subtypes
			for (Map.Entry<String, Class<? extends NodeInterface>> entity : nodeEntities.entrySet()) {

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
			for (Map.Entry<String, Class<? extends RelationshipInterface>> entity : relEntities.entrySet()) {

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
		}

		return Collections.unmodifiableSet(allSubtypes);
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

			for (Predicate<GraphObject> predicate : predicates) {

				result &= predicate.accept(obj);
			}

			return result;
		}
	}
}
