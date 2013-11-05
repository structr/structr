/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PagingHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Factory;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public abstract class SearchCommand<S extends PropertyContainer, T extends GraphObject> extends NodeServiceCommand {

	private static final Logger logger                        = Logger.getLogger(SearchCommand.class.getName());
	
	protected static final boolean INCLUDE_DELETED_AND_HIDDEN = true;
	protected static final boolean PUBLIC_ONLY		  = false;

	// the value that will be indexed for "empty" fields
	//public static final String EMPTY_FIELD_VALUE		= new String(new byte[] { 0 } );
	
	public abstract Factory<S, T> getFactory(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly, final int pageSize, final int page, final String offsetId);
	public abstract Index<S> getFulltextIndex();
	public abstract Index<S> getKeywordIndex();
	public abstract LayerNodeIndex getSpatialIndex();
	

	// ----- public command methods -----
	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly, final SearchAttribute... attributes) throws FrameworkException {
		
		List<SearchAttribute> attrs = new ArrayList();
		
		// DON'T use Arrays.asList here! The result would be a list without add() methods, {@see Arrays#AbstractList}
		for (SearchAttribute attr : attributes) {
			attrs.add(attr);
		}

		return execute(includeDeletedAndHidden, publicOnly, attrs);
	}

	public Result<T> execute(final SearchAttribute... attributes) throws FrameworkException {

		return execute(INCLUDE_DELETED_AND_HIDDEN, PUBLIC_ONLY, attributes);
	}

	public Result<T> execute(final List<SearchAttribute> searchAttrs) throws FrameworkException {
		
		return execute(INCLUDE_DELETED_AND_HIDDEN, PUBLIC_ONLY, searchAttrs);
	}

	public Result<T> execute(final boolean includeDeletedAndHidden, final List<SearchAttribute> searchAttrs) throws FrameworkException {
		
		return execute(includeDeletedAndHidden, false, searchAttrs);
	}

	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly,
			      final List<SearchAttribute> searchAttrs) throws FrameworkException {
		
		return execute(includeDeletedAndHidden, publicOnly, searchAttrs, null);
	}

	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly,
			      final List<SearchAttribute> searchAttrs, final PropertyKey sortKey) throws FrameworkException {
		
		return execute(includeDeletedAndHidden, publicOnly, searchAttrs, sortKey, false);
	}

	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly,
			      final List<SearchAttribute> searchAttrs, final PropertyKey sortKey, final boolean sortDescending) throws FrameworkException {
		
		return execute(includeDeletedAndHidden, publicOnly, searchAttrs, sortKey, sortDescending, NodeFactory.DEFAULT_PAGE_SIZE);
	}

	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly,
			      final List<SearchAttribute> searchAttrs, final PropertyKey sortKey, final boolean sortDescending, final int pageSize) throws FrameworkException {
		
		return execute(includeDeletedAndHidden, publicOnly, searchAttrs, sortKey, sortDescending, pageSize, NodeFactory.DEFAULT_PAGE);
	}

	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly,
			      final List<SearchAttribute> searchAttrs, final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page) throws FrameworkException {
		
		return execute(includeDeletedAndHidden, publicOnly, searchAttrs, sortKey, sortDescending, pageSize, page, null);
	}

	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly,
			      final List<SearchAttribute> searchAttrs, final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		return search(includeDeletedAndHidden, publicOnly, searchAttrs, sortKey, sortDescending, pageSize, page, offsetId);
	}

	/**
	 * Return a result with a list of nodes which fit to all search criteria.
	 *
	 * @param securityContext               Search in this security context
	 * @param topNode                       If set, return only search results below this top node (follows the HAS_CHILD relationship)
	 * @param includeDeletedAndHidden       If true, include nodes marked as deleted or hidden
	 * @param publicOnly                    If true, don't include nodes which are not public
	 * @param searchAttrs                   List with search attributes
	 * @param sortKey                       Key to sort results
	 * @param sortDescending                If true, sort results in descending order (higher values first)
	 * @param pageSize                      Return a portion of the overall result of this size
	 * @param page                          Return the page of the result set with this page size
	 * @param offsetId                      If given, start pagination at the object with this UUID
	 * @param sortType                      The entity type to sort the results (needed for lucene)
	 * @return
	 */
	protected Result<T> search(final boolean includeDeletedAndHidden, final boolean publicOnly, final List<SearchAttribute> searchAttrs, final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		if (page == 0 || pageSize <= 0) {

			return Result.EMPTY_RESULT;
		}

		Factory<S, T> factory        = getFactory(securityContext, includeDeletedAndHidden, publicOnly, pageSize, page, offsetId);
		boolean filterResults        = true;
		final Index<S> index;
		
		if (securityContext.getUser(false) == null) {
			
			searchAttrs.add(Search.andExactProperty(securityContext, GraphObject.visibleToPublicUsers, true));
			
		}

		// At this point, all search attributes are ready
		List<SourceSearchAttribute> sources    = new ArrayList<SourceSearchAttribute>();
		boolean hasEmptySearchFields           = false;
		DistanceSearchAttribute distanceSearch = null;
		GeoCodingResult coords                 = null;
		Double dist                            = null;


		/**
		 * In this method, we need to do the following:
		 * 
		 * if (DistanceSearchAttributes are present) {
		 *  
		 *     do distance search
		 *     filter by StringSearchAttributes
		 * 
		 * } else 
		 * if (SourceSearchAttributes are present) {
		 * 
		 *     collect data from SourceSearchAttributes
		 *     filter by StringSearchAttributes
		 * 
		 * } else
		 * if (no SourceSearchAttributes are present) {
		 * 
		 *     combine all StringSearchAttributes into one Lucene query
		 *     do search
		 *     return
		 * }
		 * 
		 * 
		 * 
		 */

		// check for optional-only queries
		// (some query types seem to allow no MUST occurs)
		for (Iterator<SearchAttribute> it = searchAttrs.iterator(); it.hasNext();) {

			SearchAttribute attr = it.next();

			// check for distance search and initialize
			if (attr instanceof DistanceSearchAttribute) {

				distanceSearch = (DistanceSearchAttribute) attr;
				coords         = GeoHelper.geocode(distanceSearch);
				dist           = distanceSearch.getValue();

				// remove attribute from filter list
				it.remove();
			}

			// store source attributes for later use
			if (attr instanceof SourceSearchAttribute) {

				sources.add((SourceSearchAttribute)attr);

				// remove attribute from filter list
				it.remove();
			}
			
			if (attr instanceof EmptySearchAttribute) {
				hasEmptySearchFields = true;
			}
		}

		Result intermediateResult;

		// only do "normal" query if no other sources are present
		// use filters to filter sources otherwise
		if (distanceSearch == null && !sources.isEmpty()) {

			intermediateResult = new Result(new ArrayList<AbstractNode>(), null, false, false);

		} else {

			BooleanQuery query    = new BooleanQuery();
			boolean allExactMatch = true;

			// build query
			for (SearchAttribute attr : searchAttrs) {

				Query queryElement = attr.getQuery();
				if (queryElement != null) {

					query.add(queryElement, attr.getOccur());
				}

				allExactMatch &= attr.isExactMatch();
			}

			QueryContext queryContext = new QueryContext(query);
			IndexHits hits            = null;

			if (sortKey != null) {

				Integer sortType = sortKey.getSortType();
				if (sortType != null) {

					queryContext.sort(new Sort(new SortField(sortKey.dbName(), sortType, sortDescending)));

				} else {

					queryContext.sort(new Sort(new SortField(sortKey.dbName(), Locale.getDefault(), sortDescending)));
				}

			}

			if (distanceSearch != null) {

				if (coords != null) {

					Map<String, Object> params = new HashMap<String, Object>();

					params.put(LayerNodeIndex.POINT_PARAMETER, coords.toArray());
					params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, dist);

					LayerNodeIndex spatialIndex = this.getSpatialIndex();
					if (spatialIndex != null) {

						synchronized (spatialIndex) {

							hits = spatialIndex.query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params);
						}
					}
				}
					
				// instantiate spatial search results without paging,
				// as the results must be filtered by type anyway
				intermediateResult = new NodeFactory(securityContext).instantiate(hits);

			} else if (allExactMatch) {

				index = getKeywordIndex();

				synchronized (index) {

					try {
						hits = index.query(queryContext);

					} catch (NumberFormatException nfe) {

						logger.log(Level.SEVERE, "Could not sort results", nfe);

						// retry without sorting
						queryContext.sort(null);
						hits = index.query(queryContext);

					}
				}

				// all luecene query, do not filter results
				filterResults = hasEmptySearchFields;
				intermediateResult = factory.instantiate(hits);

			} else {

				// Default: Mixed or fulltext-only search: Use fulltext index
				index = getFulltextIndex();

				synchronized (index) {

					try {
						hits = index.query(queryContext);

					} catch (NumberFormatException nfe) {

						logger.log(Level.SEVERE, "Could not sort results", nfe);

						// retry without sorting
						queryContext.sort(null);
						hits = index.query(queryContext);

					}
				}

				// all luecene query, do not filter results
				filterResults = hasEmptySearchFields;
				intermediateResult = factory.instantiate(hits);
			}

			if (hits != null) {
				hits.close();
			}
		}

		if (filterResults) {

			// sorted result set
			List<GraphObject> finalResult            = new LinkedList<GraphObject>();
			List<GraphObject> intermediateResultList = intermediateResult.getResults();
			int resultCount                          = 0;

			if (intermediateResultList.isEmpty()) {
				
				// merge sources according to their occur flag
				intermediateResultList.addAll(mergeSources(sources));
				
			} else if (!sources.isEmpty()) {
				
				// merge sources according to their occur flag
				intermediateResultList.retainAll(mergeSources(sources));
			}

			// Filter intermediate result
			for (GraphObject obj : intermediateResultList) {

				boolean addToResult = true;

				// check all attributes before adding a node
				for (SearchAttribute attr : searchAttrs) {

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
	
	private List<GraphObject> mergeSources(List<SourceSearchAttribute> sources) {
		
		LinkedList<GraphObject> mergedResult = new LinkedList<GraphObject>();
		boolean alreadyAdded                 = false;
		
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
}
