/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.node.search;

import java.util.*;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;

import org.structr.common.GeoHelper;
import org.structr.common.GeoHelper.GeoCodingResult;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeFactory;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.NodeServiceCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.*;
import org.structr.common.PropertyKey;
import org.structr.core.Result;
import org.structr.core.UnsupportedArgumentError;

//~--- classes ----------------------------------------------------------------

/**
 * <b>Search for nodes by attributes</b>
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
 * @author amorgner
 */
public class SearchNodeCommand extends NodeServiceCommand {

	public static String IMPROBABLE_SEARCH_VALUE = "×¦÷þ·";
	
	private static final Logger logger            = Logger.getLogger(SearchNodeCommand.class.getName());

//	private static final Map<String, Integer> sortKeyTypeMap = new LinkedHashMap<String, Integer>();
	
//	static {
//		
//		sortKeyTypeMap.put("name",       SortField.STRING);
//		sortKeyTypeMap.put("created_at", SortField.LONG);
//	}
	
	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		if ((parameters == null) || (parameters.length < 4)) {

			logger.log(Level.WARNING, "4 or more parameters are required for advanced search.");

			return Collections.emptyList();

		}

		List<SearchAttribute> searchAttrs = new ArrayList<SearchAttribute>();
		AbstractNode topNode              = null;
		boolean includeDeletedAndHidden   = false;
		boolean publicOnly                = false;
		String sortKey                    = null;
		boolean sortDescending            = false;
		int pageSize                      = NodeFactory.DEFAULT_PAGE_SIZE;
		int page                          = NodeFactory.DEFAULT_PAGE;
		String offsetId                   = null;
		Integer sortType                  = null;
		
		
		switch (parameters.length) {
			
			case 10:
				if (parameters[9] instanceof Integer) {

					sortType = ((Integer) parameters[9]).intValue();

				}
				
			case 9:
				if (parameters[8] instanceof String) {

					offsetId = (String) parameters[8];

				}
				
			case 8:
				if (parameters[7] instanceof Integer) {

					page = ((Integer) parameters[7]).intValue();

				}
			
			case 7:
				if (parameters[6] instanceof Integer) {

					pageSize = ((Integer) parameters[6]).intValue();

				}
			
			case 6:
				if (parameters[5] instanceof Boolean) {

					sortDescending = (Boolean) parameters[5];

				}
				
			
			case 5:
				if(parameters[4] instanceof String) {
					
					sortKey = (String)parameters[4];
					
				} else if(parameters[4] instanceof PropertyKey) {
					sortKey = ((PropertyKey)parameters[4]).name();
				}
			
			case 4:
				if (parameters[0] instanceof AbstractNode) {
					topNode = (AbstractNode) parameters[0];

				}

				if (parameters[1] instanceof Boolean) {

					includeDeletedAndHidden = (Boolean) parameters[1];

				}


				if (parameters[2] instanceof Boolean) {

					publicOnly = (Boolean) parameters[2];

				}


				if (parameters[3] instanceof List) {

					searchAttrs = (List<SearchAttribute>) parameters[3];

				}
				
				break;
		}


		return search(securityContext, topNode, includeDeletedAndHidden, publicOnly, searchAttrs, sortKey, sortDescending, pageSize, page, offsetId, sortType);
	}

	/**
	 * Return a result with a list of nodes which fit to all search criteria.
	 *
	 * @param securityContext		Search in this security context
	 * @param topNode			If set, return only search results below this top node (follows the HAS_CHILD relationship)
	 * @param includeDeletedAndHidden       If true, include nodes marked as deleted or hidden
	 * @param publicOnly			If true, don't include nodes which are not public
	 * @param searchAttrs			List with search attributes
	 * @param sortKey			Key to sort results
	 * @param sortDescending		If true, sort results in descending order (higher values first)
	 * @param pageSize			Return a portion of the overall result of this size
	 * @param page				Return the page of the result set with this page size
	 * @param offsetId			If given, start pagination at the object with this UUID
	 * @param sortType			The entity type to sort the results (needed for lucene)
	 * @return
	 */
	private Result search(final SecurityContext securityContext, final AbstractNode topNode, final boolean includeDeletedAndHidden, final boolean publicOnly,
					  final List<SearchAttribute> searchAttrs, final String sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId, final Integer sortType)
		throws FrameworkException {

		if (page == 0 || pageSize <= 0) {
			return Result.EMPTY_RESULT;
		}
		
		GraphDatabaseService graphDb   = (GraphDatabaseService) arguments.get("graphDb");
		NodeFactory nodeFactory        = new NodeFactory(securityContext, includeDeletedAndHidden, publicOnly, pageSize, page, offsetId);
		Result finalResult	       = new Result(new ArrayList<AbstractNode>(), null, true, false);
		boolean allExactMatch          = true;
		final Index<Node> index;

		// boolean allFulltext = false;
		if (graphDb != null) {

			// At this point, all search attributes are ready
			BooleanQuery query                             = new BooleanQuery();
			List<FilterSearchAttribute> filters            = new ArrayList<FilterSearchAttribute>();
			List<TextualSearchAttribute> textualAttributes = new ArrayList<TextualSearchAttribute>();
			StringBuilder textualQueryString               = new StringBuilder();
			DistanceSearchAttribute distanceSearch         = null;
			GeoCodingResult coords                         = null;
			Double dist                                    = null;

			for (SearchAttribute attr : searchAttrs) {

				if (attr instanceof DistanceSearchAttribute) {

					distanceSearch = (DistanceSearchAttribute) attr;
					coords         = GeoHelper.geocode(distanceSearch.getKey());
					dist           = distanceSearch.getValue();

				} else if (attr instanceof SearchAttributeGroup) {

					SearchAttributeGroup attributeGroup     = (SearchAttributeGroup) attr;
					List<SearchAttribute> groupedAttributes = attributeGroup.getSearchAttributes();
					StringBuilder subQueryString                   = new StringBuilder();

					if (!(groupedAttributes.isEmpty())) {

						BooleanQuery subQuery = new BooleanQuery();
						String subQueryPrefix = (StringUtils.isBlank(textualQueryString.toString())
									 ? ""
									 : attributeGroup.getSearchOperator()) + " ( ";

						for (SearchAttribute groupedAttr : groupedAttributes) {

							// TODO: support other than textual search attributes
							if (groupedAttr instanceof TextualSearchAttribute) {

								textualAttributes.add((TextualSearchAttribute) groupedAttr);
								subQuery.add(toQuery((TextualSearchAttribute) groupedAttr), translateToBooleanClauseOccur(groupedAttr.getSearchOperator()));

								subQueryString.append(toQueryString((TextualSearchAttribute) groupedAttr, StringUtils.isBlank(subQueryString.toString())));
								allExactMatch  &= isExactMatch(((TextualSearchAttribute) groupedAttr).getValue());

							}
						}

						query.add(subQuery, translateToBooleanClauseOccur(attributeGroup.getSearchOperator()));

						String subQuerySuffix = " ) ";

						// Add sub query only if not blank
						if (StringUtils.isNotBlank(subQueryString.toString())) {

							textualQueryString.append(subQueryPrefix).append(subQueryString).append(subQuerySuffix);

						}

					}

				} else if (attr instanceof TextualSearchAttribute) {

					textualAttributes.add((TextualSearchAttribute) attr);
					query.add(toQuery((TextualSearchAttribute) attr), translateToBooleanClauseOccur(attr.getSearchOperator()));

					textualQueryString.append(toQueryString((TextualSearchAttribute) attr, StringUtils.isBlank(textualQueryString.toString())));
					allExactMatch      &= isExactMatch(((TextualSearchAttribute) attr).getValue());

				} else if (attr instanceof FilterSearchAttribute) {

					filters.add((FilterSearchAttribute) attr);

				}

			}

			// Check if all prerequisites are met
			if (distanceSearch == null && textualAttributes.size() < 1) {
				
				throw new UnsupportedArgumentError("At least one texutal search attribute or distance search have to be present in search attributes!");
				
			}
			
			Result intermediateResult;

			if (searchAttrs.isEmpty() && StringUtils.isBlank(textualQueryString.toString())) {

//                              if (topNode != null) {
//
//                                      intermediateResult = topNode.getAllChildren();
//
//                              } else {
				intermediateResult = new Result(new ArrayList<AbstractNode>(), null, false, false);

//                              }
			} else {

				long t0 = System.nanoTime();

				logger.log(Level.FINE, "Textual Query String: {0}", textualQueryString);

				QueryContext queryContext = new QueryContext(textualQueryString);
				IndexHits hits            = null;

				if (sortKey != null) {
					
					if (sortType != null) {
						
						queryContext.sort(new Sort(new SortField(sortKey, sortType, sortDescending)));
						
					} else {
						
						queryContext.sort(new Sort(new SortField(sortKey, Locale.getDefault(), sortDescending)));
					}
				}
				
				 if (distanceSearch != null) {

					if (coords != null) {

						Map<String, Object> params = new HashMap<String, Object>();

						params.put(LayerNodeIndex.POINT_PARAMETER, coords.toArray());
						params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, dist);
						
						index = (LayerNodeIndex) arguments.get(NodeIndex.layer.name());
						synchronized(index) {
							hits  = index.query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params);
						}

					}

				} else if ((textualAttributes.size() == 1) && textualAttributes.get(0).getKey().equals(AbstractNode.Key.uuid.name())) {

					// Search for uuid only: Use UUID index
					index = (Index<Node>) arguments.get(NodeIndex.uuid.name());
					synchronized(index) {
						hits  = index.get(AbstractNode.Key.uuid.name(), decodeExactMatch(textualAttributes.get(0).getValue()));
					}
					
					
				} else if (/*(textualAttributes.size() > 1) &&*/ allExactMatch) {
//				} else if ((textualAttributes.size() > 1) && allExactMatch) {

					// Only exact machtes: Use keyword index
					index = (Index<Node>) arguments.get(NodeIndex.keyword.name());
					synchronized(index) {
						hits  = index.query(queryContext);
					}
					
				} else {

					// Default: Mixed or fulltext-only search: Use fulltext index
					index = (Index<Node>) arguments.get(NodeIndex.fulltext.name());
					synchronized(index) {
						hits  = index.query(queryContext);
					}
				}

				long t1 = System.nanoTime();

				logger.log(Level.FINE, "Querying index took {0} ns, size() says {1} results.", new Object[] { t1 - t0, (hits != null)
					? hits.size()
					: 0 });

//                              IndexHits hits = index.query(new QueryContext(query.toString()));//.sort("name"));
				intermediateResult = nodeFactory.createNodes(hits);

				hits.close();
				long t2 = System.nanoTime();

				logger.log(Level.FINE, "Creating structr nodes took {0} ns, {1} nodes made.", new Object[] { t2 - t1, intermediateResult.getResults().size() });

			}

			List<? extends GraphObject> intermediateResultList = intermediateResult.getResults();

			
			long t2 = System.nanoTime();

			if (!filters.isEmpty()) {

				// Filter intermediate result
				for (GraphObject obj : intermediateResultList) {
					
					AbstractNode node = (AbstractNode) obj;

					for (FilterSearchAttribute attr : filters) {

						String key         = attr.getKey();
						Object searchValue = attr.getValue();
						SearchOperator op  = attr.getSearchOperator();
						Object nodeValue   = node.getProperty(key);

						if (op.equals(SearchOperator.NOT)) {

							if ((nodeValue != null) &&!(nodeValue.equals(searchValue))) {

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

					SearchOperator op        = attr.getSearchOperator();
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
			
//			if (sortKey != null) {
//				
//				Collections.sort(finalResult.getResults(), new GraphObjectComparator(sortKey, sortDescending ? GraphObjectComparator.DESCENDING : GraphObjectComparator.ASCENDING));
//
//				long t4 = System.nanoTime();
//			
//				logger.log(Level.FINE, "Sorting nodes took {0} ns.", new Object[] { t4 - t3 });
//			
//			}

		}

		return finalResult;
	}

	private String toQueryString(final TextualSearchAttribute singleAttribute, final boolean isFirst) {

		String queryString = "";
		
		String key        = singleAttribute.getKey();
		String value      = singleAttribute.getValue();
		SearchOperator op = singleAttribute.getSearchOperator();

		if (StringUtils.isBlank(value) || value.equals("\"\"")) {

			queryString = ((isFirst && !(op.equals(SearchOperator.NOT)))
			? ""
			: " " + op + " ") + key + ":\"" + IMPROBABLE_SEARCH_VALUE + "\"";

		} else {

			// NOT operator should always be applied
			queryString = ((isFirst && !(op.equals(SearchOperator.NOT)))
				? ""
				: " " + op + " ") + expand(key, value);
			
		}
		
		return queryString;
	}

	private Query toQuery(final TextualSearchAttribute singleAttribute) {

		String key   = singleAttribute.getKey();
		String value = singleAttribute.getValue();

//              SearchOperator op = singleAttribute.getSearchOperator();
		if ((key == null) || (value == null)) {

			return null;

		}

		if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {

			return new TermQuery(new Term(key, value));

		}

		return null;
	}

	private BooleanClause.Occur translateToBooleanClauseOccur(final SearchOperator searchOp) {

		if (searchOp.equals(SearchOperator.AND)) {

			return BooleanClause.Occur.MUST;

		} else if (searchOp.equals(SearchOperator.OR)) {

			return BooleanClause.Occur.SHOULD;

		} else if (searchOp.equals(SearchOperator.NOT)) {

			return BooleanClause.Occur.MUST_NOT;

		}

		// Default
		return BooleanClause.Occur.MUST;
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
			result.append(" (").append(escapedKey).append(":*").append(word).append("* OR ").append(escapedKey).append(":\"").append(word).append("\")").append((i < words.length) ? " AND ": " ) ");

//                      }
			i++;
		}

		return result.toString();
	}

	private List<AbstractNode> filterNotExactMatches(final List<AbstractNode> result, TextualSearchAttribute attr) {

		List<AbstractNode> notMatchingNodes = new ArrayList<AbstractNode>();

		// Filter not exact matches
		for (AbstractNode node : result) {

			String value = attr.getValue();

			if ((value != null) && isExactMatch(value)) {

				String key          = attr.getKey();
				String nodeValue    = node.getStringProperty(key);
				String decodedValue = decodeExactMatch(value);

				if (!nodeValue.equals(decodedValue)) {

					notMatchingNodes.add(node);

				}

			}

		}

		return ListUtils.subtract(result, notMatchingNodes);
	}

	private String decodeExactMatch(final String value) {
		return StringUtils.strip(value, "\"");
	}

	//~--- get methods ----------------------------------------------------

	private boolean isExactMatch(final String value) {
		if (value == null) return false;
		return value.startsWith("\"") && value.endsWith("\"");
	}
}
