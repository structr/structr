package org.structr.core.graph.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
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

	private static final Logger logger = Logger.getLogger(SearchCommand.class.getName());
	
	protected static final boolean INCLUDE_DELETED_AND_HIDDEN = true;
	protected static final boolean PUBLIC_ONLY		= false;

	public static String IMPROBABLE_SEARCH_VALUE = "×¦÷þ·";
	
	
	public abstract Factory<S, T> getFactory(final SecurityContext securityContext, final boolean includeDeletedAndHidden, final boolean publicOnly, final int pageSize, final int page, final String offsetId);
	public abstract Index<S> getFulltextIndex();
	public abstract Index<S> getKeywordIndex();
	public abstract LayerNodeIndex getSpatialIndex();
	

	// ----- public command methods -----
	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly, final SearchAttribute... attributes) throws FrameworkException {
		
		return execute(includeDeletedAndHidden, publicOnly, Arrays.asList(attributes));
	}

	public Result<T> execute(final SearchAttribute... attributes) throws FrameworkException {
		
		return execute(INCLUDE_DELETED_AND_HIDDEN, PUBLIC_ONLY, Arrays.asList(attributes));
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

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		Factory<S, T> factory        = getFactory(securityContext, includeDeletedAndHidden, publicOnly, pageSize, page, offsetId);
		Result finalResult           = new Result(new ArrayList<AbstractNode>(), null, true, false);
		BooleanQuery query           = new BooleanQuery();
		boolean optionalOnly         = false;
		boolean allExactMatch        = true;
		final Index<S> index;

		// boolean allFulltext = false;
		if (graphDb != null) {

			// At this point, all search attributes are ready
			List<FilterSearchAttribute> filters            = new ArrayList<FilterSearchAttribute>();
			StringBuilder queryString                      = new StringBuilder();
			DistanceSearchAttribute distanceSearch         = null;
			GeoCodingResult coords                         = null;
			Double dist                                    = null;

			// check for optional-only queries
			// (some query types seem to allow no MUST occurs)
			for (SearchAttribute attr : searchAttrs) {

				if (attr.forcesExclusivelyOptionalQueries()) {
					optionalOnly = true;
				}

				// check for distance search and initialize
				if (attr instanceof DistanceSearchAttribute) {
					
					distanceSearch = (DistanceSearchAttribute) attr;
					coords         = GeoHelper.geocode(distanceSearch);
					dist           = distanceSearch.getValue();
				}
				
				// store filter attribute for later use
				if (attr instanceof FilterSearchAttribute) {
					
					filters.add((FilterSearchAttribute)attr);
				}
			}

			// only build query if distance search is not active
			if (distanceSearch == null) {
				
				for (SearchAttribute attr : searchAttrs) {

					Query queryElement = attr.getQuery();
					if (queryElement != null) {

						query.add(queryElement, optionalOnly ? Occur.SHOULD : attr.getOccur());
					}

					allExactMatch &= attr.isExactMatch();
				}
			}
			
//			// Check if all prerequisites are met
//			if (distanceSearch == null && textualAttributes.size() < 1) {
//
//				throw new UnsupportedArgumentError("At least one texutal search attribute or distance search have to be present in search attributes!");
//			}

			Result intermediateResult;

			if (searchAttrs.isEmpty() && StringUtils.isBlank(queryString.toString())) {

				intermediateResult = new Result(new ArrayList<AbstractNode>(), null, false, false);

			} else {

				QueryContext queryContext = new QueryContext(query);
				IndexHits hits            = null;
				
				// logger.log(Level.INFO, "Query: {0}", query);
				
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

				/*
				} else if (uuidOnly) {

					// Search for uuid only: Use UUID index
					index = (Index<Node>) arguments.get(NodeIndex.uuid.name());

					synchronized (index) {

						hits = index.get(AbstractNode.uuid.dbName(), decodeExactMatch(textualAttributes.get(0).getValue()));
					}
				*/
					
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
					
				} else {

					// Default: Mixed or fulltext-only search: Use fulltext index
					index = getFulltextIndex(); // (Index<Node>) arguments.get(NodeService.NodeIndex.fulltext.name());

					synchronized (index) {

						hits = index.query(queryContext);
					}
				}

				intermediateResult = factory.instantiate(hits);

				if (hits != null) {
					hits.close();
				}
			}

			List<? extends GraphObject> intermediateResultList = intermediateResult.getResults();

			if (!filters.isEmpty()) {

				// Filter intermediate result
				for (GraphObject obj : intermediateResultList) {

					AbstractNode node = (AbstractNode) obj;

					for (FilterSearchAttribute attr : filters) {

						PropertyKey key    = attr.getKey();
						Object searchValue = attr.getValue();
						Occur occur        = attr.getOccur();
						Object nodeValue   = node.getProperty(key);

						if (occur.equals(Occur.MUST_NOT)) {

							if ((nodeValue != null) && !(nodeValue.equals(searchValue))) {

								attr.addToResult(node);
							}

						} else {

							if ((nodeValue == null) && (searchValue == null)) {

								attr.addToResult(node);
							}

							if ((nodeValue != null) && nodeValue.equals(searchValue)) {

								attr.addToResult(node);
							}

						}

					}

				}

				// now sum, intersect or substract all partly results
				for (FilterSearchAttribute attr : filters) {

					Occur occur                        = attr.getOccur();
					List<? extends GraphObject> result = attr.getResult();

					if (occur.equals(Occur.MUST)) {

						intermediateResult = new Result(ListUtils.intersection(intermediateResultList, result), null, true, false);
					} else if (occur.equals(Occur.SHOULD)) {

						intermediateResult = new Result(ListUtils.sum(intermediateResultList, result), null, true, false);
					} else if (occur.equals(Occur.MUST_NOT)) {

						intermediateResult = new Result(ListUtils.subtract(intermediateResultList, result), null, true, false);
					}

				}
			}
			
			finalResult = intermediateResult;
		}

		return finalResult;

	}
}
