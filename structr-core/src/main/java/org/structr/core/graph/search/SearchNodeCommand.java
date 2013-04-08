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

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.*;

import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;

import org.structr.common.geo.GeoHelper;
import org.structr.core.property.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.NodeServiceCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.geo.GeoCodingResult;

//~--- classes ----------------------------------------------------------------

/**
 * Search for nodes by their attributes.
 * <p>
 * The execute method takes four parameters:
 * <p>
 * <ol>
 * <li>{@see AbstractNode} top node: search only below this node
 *     <p>if null, search everywhere (top node = root node)
 * <li>boolean include deleted and hidden: if true, return deleted and hidden nodes as well
 * <li>boolean public only: if true, return only public nodes
 * <li>List<{@see TextualSearchAttribute}> search attributes: key/value pairs with search operator
 *    <p>if no TextualSearchAttribute is given, return any node matching the other
 *       search criteria
 * </ol>
 *
 * @author Axel Morgner
 */
public class SearchNodeCommand<T extends GraphObject> extends NodeServiceCommand {

	public static String IMPROBABLE_SEARCH_VALUE = "×¦÷þ·";
	private static final Logger logger           = Logger.getLogger(SearchNodeCommand.class.getName());
	
	private static final boolean INCLUDE_DELETED_AND_HIDDEN = true;
	private static final boolean PUBLIC_ONLY		= false;

	//~--- methods --------------------------------------------------------

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
		
		return search(includeDeletedAndHidden, publicOnly, searchAttrs, sortKey, sortDescending, pageSize, page, offsetId, null);
	}

	public Result<T> execute(final boolean includeDeletedAndHidden, final boolean publicOnly,
			      final List<SearchAttribute> searchAttrs, final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId,
			      final Integer sortType) throws FrameworkException {
		
		return search(includeDeletedAndHidden, publicOnly, searchAttrs, sortKey, sortDescending, pageSize, page, offsetId, sortType);
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
	private Result<T> search(final boolean includeDeletedAndHidden, final boolean publicOnly,
			      final List<SearchAttribute> searchAttrs, final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId,
			      final Integer sortType)
		throws FrameworkException {

		if (page == 0 || pageSize <= 0) {

			return Result.EMPTY_RESULT;
		}

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		NodeFactory nodeFactory      = new NodeFactory(securityContext, includeDeletedAndHidden, publicOnly, pageSize, page, offsetId);
		Result finalResult           = new Result(new ArrayList<AbstractNode>(), null, true, false);
		boolean allExactMatch        = true;
		final Index<Node> index;

		// boolean allFulltext = false;
		if (graphDb != null) {

			// At this point, all search attributes are ready
			// BooleanQuery query                             = new BooleanQuery();
			List<FilterSearchAttribute> filters            = new ArrayList<FilterSearchAttribute>();
			List<TextualSearchAttribute> textualAttributes = new ArrayList<TextualSearchAttribute>();
			StringBuilder queryString                      = new StringBuilder();
			DistanceSearchAttribute distanceSearch         = null;
			GeoCodingResult coords                         = null;
			Double dist                                    = null;

			for (SearchAttribute attr : searchAttrs) {

				if (attr instanceof RangeSearchAttribute) {

					handleRangeAttribute( (RangeSearchAttribute) attr, queryString);

				} else if (attr instanceof DistanceSearchAttribute) {

					distanceSearch = (DistanceSearchAttribute) attr;
					coords         = GeoHelper.geocode(distanceSearch);
					dist           = distanceSearch.getValue();

				} else if (attr instanceof SearchAttributeGroup) {

					SearchAttributeGroup attributeGroup = (SearchAttributeGroup) attr;

					handleAttributeGroup(attributeGroup, queryString, textualAttributes, allExactMatch);

				} else if (attr instanceof TextualSearchAttribute) {

					textualAttributes.add((TextualSearchAttribute) attr);

					// query.add(toQuery((TextualSearchAttribute) attr), translateToBooleanClauseOccur(attr.getSearchOperator()));
					queryString.append(toQueryString((TextualSearchAttribute) attr, StringUtils.isBlank(queryString.toString())));

					allExactMatch &= isExactMatch(((TextualSearchAttribute) attr).getValue());

				} else if (attr instanceof FilterSearchAttribute) {

					filters.add((FilterSearchAttribute) attr);
				}

			}

			// Check if all prerequisites are met
			if (distanceSearch == null && textualAttributes.size() < 1) {

				throw new UnsupportedArgumentError("At least one texutal search attribute or distance search have to be present in search attributes!");
			}

			Result intermediateResult;

			if (searchAttrs.isEmpty() && StringUtils.isBlank(queryString.toString())) {

//                              if (topNode != null) {
//
//                                      intermediateResult = topNode.getAllChildren();
//
//                              } else {
				intermediateResult = new Result(new ArrayList<AbstractNode>(), null, false, false);

//                              }
			} else {

				long t0 = System.nanoTime();

				logger.log(Level.FINEST, "Textual Query String: {0}", queryString);

				String query = queryString.toString();
				
				QueryContext queryContext = new QueryContext(query);
				IndexHits hits            = null;

				if (sortKey != null) {

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

						index = (LayerNodeIndex) arguments.get(NodeIndex.layer.name());

						synchronized (index) {

							hits = index.query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params);
						}

					}

				} else if ((textualAttributes.size() == 1) && textualAttributes.get(0).getKey().equals(AbstractNode.uuid.dbName())) {

					// Search for uuid only: Use UUID index
					index = (Index<Node>) arguments.get(NodeIndex.uuid.name());

					synchronized (index) {

						hits = index.get(AbstractNode.uuid.dbName(), decodeExactMatch(textualAttributes.get(0).getValue()));
					}
					
				} else if ( /* (textualAttributes.size() > 1) && */allExactMatch) {

//                                      } else if ((textualAttributes.size() > 1) && allExactMatch) {
					// Only exact machtes: Use keyword index
					index = (Index<Node>) arguments.get(NodeIndex.keyword.name());

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
					index = (Index<Node>) arguments.get(NodeIndex.fulltext.name());

					synchronized (index) {

						hits = index.query(queryContext);
					}
				}

				long t1 = System.nanoTime();

				logger.log(Level.FINE, "Querying index took {0} ns, size() says {1} results.", new Object[] { t1 - t0, (hits != null)
					? hits.size()
					: 0 });

//                              IndexHits hits = index.query(new QueryContext(query.toString()));//.sort("name"));
				intermediateResult = nodeFactory.createNodes(hits);

				if (hits != null) {
					hits.close();
				}

				long t2 = System.nanoTime();

				logger.log(Level.FINE, "Creating structr nodes took {0} ns, {1} nodes made.", new Object[] { t2 - t1, intermediateResult.getResults().size() });

			}

			List<? extends GraphObject> intermediateResultList = intermediateResult.getResults();
			long t2                                            = System.nanoTime();

			if (!filters.isEmpty()) {

				// Filter intermediate result
				for (GraphObject obj : intermediateResultList) {

					AbstractNode node = (AbstractNode) obj;

					for (FilterSearchAttribute attr : filters) {

						PropertyKey key    = attr.getKey();
						Object searchValue = attr.getValue();
						SearchOperator op  = attr.getSearchOperator();
						Object nodeValue   = node.getProperty(key);

						if (op.equals(SearchOperator.NOT)) {

							if ((nodeValue != null) && !(nodeValue.equals(decodeExactMatch(searchValue)))) {

								attr.addToResult(node);
							}

						} else {

							if ((nodeValue == null) && (searchValue == null)) {

								attr.addToResult(node);
							}

							if ((nodeValue != null) && nodeValue.equals(decodeExactMatch(searchValue))) {

								attr.addToResult(node);
							}

						}

					}

				}

				// now sum, intersect or substract all partly results
				for (FilterSearchAttribute attr : filters) {

					SearchOperator op                  = attr.getSearchOperator();
					List<? extends GraphObject> result = attr.getResult();

					if (op.equals(SearchOperator.AND)) {

						intermediateResult = new Result(ListUtils.intersection(intermediateResultList, result), null, true, false);
					} else if (op.equals(SearchOperator.OR)) {

						intermediateResult = new Result(ListUtils.sum(intermediateResultList, result), null, true, false);
					} else if (op.equals(SearchOperator.NOT)) {

						intermediateResult = new Result(ListUtils.subtract(intermediateResultList, result), null, true, false);
					}

				}
			}

			// eventually filter by distance from a given point
			if (coords != null) {}

			finalResult = intermediateResult;

			long t3 = System.nanoTime();

			logger.log(Level.FINE, "Filtering nodes took {0} ns. Result size now {1}.", new Object[] { t3 - t2, finalResult.getResults().size() });

//                      if (sortKey != null) {
//                              
//                              Collections.sort(finalResult.getResults(), new GraphObjectComparator(sortKey, sortDescending ? GraphObjectComparator.DESCENDING : GraphObjectComparator.ASCENDING));
//
//                              long t4 = System.nanoTime();
//                      
//                              logger.log(Level.FINE, "Sorting nodes took {0} ns.", new Object[] { t4 - t3 });
//                      
//                      }
		}

		return finalResult;

	}

	private String toQueryString(final TextualSearchAttribute singleAttribute, final boolean isFirst) {

		String queryString = "";
		PropertyKey key    = singleAttribute.getKey();
		String value       = singleAttribute.getValue();
		SearchOperator op  = singleAttribute.getSearchOperator();

		if (StringUtils.isBlank(value) || value.equals("\"\"")) {

			queryString = ((isFirst && !(op.equals(SearchOperator.NOT)))
				       ? ""
				       : " " + op + " ") + key.dbName() + ":\"" + IMPROBABLE_SEARCH_VALUE + "\"";

		} else {

			// NOT operator should always be applied
			queryString = ((isFirst && !(op.equals(SearchOperator.NOT)))
				       ? ""
				       : " " + op + " ") + expand(key.dbName(), value);
		}

		return queryString;

	}

	private String expand(final String key, final String value) {

		if (StringUtils.isBlank(key)) {

			return "";
		}

		String escapedKey  = Search.escapeForLucene(key);
		String stringValue = (String) value;

		if (StringUtils.isBlank(stringValue)) {

			return "";
		}

		// If value is not a single character and starts with operator (exact match, range query, or search operator word),
		// don't expand
		if ((stringValue.length() > 1) && (stringValue.startsWith("\"") && stringValue.endsWith("\"")) || (stringValue.startsWith("[") && stringValue.endsWith("]"))
			|| stringValue.startsWith("NOT") || stringValue.startsWith("AND") || stringValue.startsWith("OR")) {

			return " " + escapedKey + ":" + stringValue + " ";
		}

		StringBuilder result = new StringBuilder();

		result.append("( ");

		// Split string into words
		String[] words = StringUtils.split(stringValue, " ");

		// Expand key,word to ' (key:word* OR key:"word") '
		int i = 1;

		for (String word : words) {

			// Clean string
			// stringValue = Search.clean(stringValue);
			word = Search.escapeForLucene(word);

//                      // Treat ? special:
			// There's a bug in Lucene < 4: https://issues.apache.org/jira/browse/LUCENE-588
//                      if (word.equals("\\?")) {
//
//                              result += " ( " + key + ":\"" + word + "\")" + ((i < words.length)
//                                      ? " AND "
//                                      : " ) ");
//
//                      } else {
//                      String cleanWord = Search.clean(word);
//                      result += " (" + key + ":" + cleanWord + "* OR " + key + ":\"" + cleanWord + "\")" + (i<words.length ? " AND " : " ) ");
//                      result += " (" + key + ":" + word + "* OR " + key + ":\"" + word + "\")" + (i < words.length ? " AND " : " ) ");
			result.append(" (").append(escapedKey).append(":*").append(word).append("* OR ").append(escapedKey).append(":\"").append(word).append("\")").append((i < words.length)
				? " AND "
				: " ) ");

//                      }
			i++;
		}

		return result.toString();

	}

	private void handleAttributeGroup(final SearchAttributeGroup attributeGroup, StringBuilder queryString, List<TextualSearchAttribute> textualAttributes, boolean allExactMatch) {

		List<SearchAttribute> groupedAttributes = attributeGroup.getSearchAttributes();
		StringBuilder subQueryString            = new StringBuilder();

		if (!(groupedAttributes.isEmpty())) {

			// BooleanQuery subQuery = new BooleanQuery();
			String subQueryPrefix = (StringUtils.isBlank(queryString.toString())
						 ? ""
						 : attributeGroup.getSearchOperator()) + " ( ";

			for (SearchAttribute groupedAttr : groupedAttributes) {

				if (groupedAttr instanceof TextualSearchAttribute) {

					textualAttributes.add((TextualSearchAttribute) groupedAttr);

					// subQuery.add(toQuery((TextualSearchAttribute) groupedAttr), translateToBooleanClauseOccur(groupedAttr.getSearchOperator()));
					subQueryString.append(toQueryString((TextualSearchAttribute) groupedAttr, StringUtils.isBlank(subQueryString.toString())));

					allExactMatch &= isExactMatch(((TextualSearchAttribute) groupedAttr).getValue());

				} else if (groupedAttr instanceof RangeSearchAttribute) {

					handleRangeAttribute( (RangeSearchAttribute) groupedAttr, queryString);

				} else if (groupedAttr instanceof SearchAttributeGroup) {

					handleAttributeGroup((SearchAttributeGroup) groupedAttr, subQueryString, textualAttributes, allExactMatch);
				}
			}

			// query.add(subQuery, translateToBooleanClauseOccur(attributeGroup.getSearchOperator()));
			String subQuerySuffix = " ) ";

			// Add sub query only if not blank
			if (StringUtils.isNotBlank(subQueryString.toString())) {

				queryString.append(subQueryPrefix).append(subQueryString).append(subQuerySuffix);
			}
		}

	}
	
	private void handleRangeAttribute(RangeSearchAttribute rangeSearchAttribute, StringBuilder queryString) {
		
		queryString.append(" ");
		queryString.append(rangeSearchAttribute.getSearchOperator());
		queryString.append(" ");
		queryString.append(rangeSearchAttribute.getValue());
		
	}

	//~--- get methods ----------------------------------------------------

	private boolean isExactMatch(final String value) {

		if (value == null) {

			return false;
		}

		return value.startsWith("\"") && value.endsWith("\"");

	}

	private String decodeExactMatch(final String value) {

		return StringUtils.strip(value, "\"");

	}
	
	private Object decodeExactMatch(final Object value) {
		
		if (value instanceof String) {

			return decodeExactMatch((String) value);
			
		} else {
			return value;
		}

	}
	
}
