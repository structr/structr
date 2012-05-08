/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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
import org.structr.common.GeoHelper.Coordinates;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeFactory;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.NodeServiceCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * <b>Search for nodes by attributes</b>
 * <p>
 * The execute method takes four parameters:
 * <p>
 * <ol>
 * <li>{@see AbstractNode} top node: search only below this node
 *     <p>if null, search everywhere (top node = root node)
 * <li>boolean include deleted: if true, return deleted nodes as well
 * <li>boolean public only: if true, return only public nodes
 * <li>List<{@see TextualSearchAttribute}> search attributes: key/value pairs with search operator
 *    <p>if no TextualSearchAttribute is given, return any node matching the other
 *       search criteria
 * </ol>
 *
 * @author amorgner
 */
public class SearchNodeCommand extends NodeServiceCommand {

	private static String IMPROBABLE_SEARCH_VALUE = "xeHfc6OG30o3YQzX57_8____r-Wx-RW_70r84_71D-g--P9-3K";
	private static final Logger logger            = Logger.getLogger(SearchNodeCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		if ((parameters == null) || (parameters.length < 4)) {

			logger.log(Level.WARNING, "4 or more parameters are required for advanced search.");

			return Collections.emptyList();

		}

		AbstractNode topNode = null;

		if (parameters[0] instanceof AbstractNode) {

			topNode = (AbstractNode) parameters[0];

		}

		boolean includeDeleted = false;

		if (parameters[1] instanceof Boolean) {

			includeDeleted = (Boolean) parameters[1];

		}

		boolean publicOnly = false;

		if (parameters[2] instanceof Boolean) {

			publicOnly = (Boolean) parameters[2];

		}

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		if (parameters[3] instanceof List) {

			searchAttrs = (List<SearchAttribute>) parameters[3];

		}

		return search(securityContext, topNode, includeDeleted, publicOnly, searchAttrs);
	}

	/**
	 * Return a list of nodes which fit to all search criteria.
	 *
	 * @param securityContext       Search in this security context
	 * @param topNode               If set, return only search results below this top node (follows the HAS_CHILD relationship)
	 * @param includeDeleted        If true, include nodes marked as deleted or contained in a Trash node as well
	 * @param publicOnly            If true, don't include nodes which are not public
	 * @param searchAttrs           List with search attributes
	 * @return
	 */
	private List<AbstractNode> search(final SecurityContext securityContext, final AbstractNode topNode, final boolean includeDeleted, final boolean publicOnly,
					  final List<SearchAttribute> searchAttrs)
		throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		Index<Node> index;
		NodeFactory nodeFactory        = (NodeFactory) arguments.get("nodeFactory");
		List<AbstractNode> finalResult = new LinkedList<AbstractNode>();
		boolean allExactMatch          = true;

		// boolean allFulltext = false;
		if (graphDb != null) {

			// At this point, all search attributes are ready
			BooleanQuery query                             = new BooleanQuery();
			List<FilterSearchAttribute> filters            = new LinkedList<FilterSearchAttribute>();
			List<TextualSearchAttribute> textualAttributes = new LinkedList<TextualSearchAttribute>();
			StringBuilder textualQueryString               = new StringBuilder();
			DistanceSearchAttribute distanceSearch         = null;
			Coordinates coords                             = null;
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

			List<AbstractNode> intermediateResult;

			if (searchAttrs.isEmpty() && StringUtils.isBlank(textualQueryString.toString())) {

//                              if (topNode != null) {
//
//                                      intermediateResult = topNode.getAllChildren();
//
//                              } else {
				intermediateResult = new LinkedList<AbstractNode>();

//                              }
			} else {

				long t0 = System.currentTimeMillis();

				logger.log(Level.FINE, "Textual Query String: {0}", textualQueryString);

				QueryContext queryContext = new QueryContext(textualQueryString);
				IndexHits hits            = null;

				if ((textualAttributes.size() == 1) && textualAttributes.get(0).getKey().equals(AbstractNode.Key.uuid.name())) {

					// Search for uuid only: Use UUID index
					index = (Index<Node>) arguments.get(NodeIndex.uuid.name());
					hits  = index.get(AbstractNode.Key.uuid.name(), decodeExactMatch(textualAttributes.get(0).getValue()));
				} else if ((textualAttributes.size() > 1) && allExactMatch) {

					// Only exact machtes: Use keyword index
					index = (Index<Node>) arguments.get(NodeIndex.keyword.name());
					hits  = index.query(queryContext);
				} else if (distanceSearch != null) {

					if (coords != null) {

						Map<String, Object> params = new HashMap<String, Object>();

						params.put(LayerNodeIndex.POINT_PARAMETER, coords.toArray());
						params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, dist);

						index = (LayerNodeIndex) arguments.get(NodeIndex.layer.name());
						hits  = index.query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params);

					}

				} else {

					// Default: Mixed or fulltext-only search: Use fulltext index
					index = (Index<Node>) arguments.get(NodeIndex.fulltext.name());
					hits  = index.query(queryContext);
				}

				long t1 = System.currentTimeMillis();

				logger.log(Level.FINE, "Querying index took {0} ms, {1} results retrieved.", new Object[] { t1 - t0, (hits != null)
					? hits.size()
					: 0 });

//                              IndexHits hits = index.query(new QueryContext(query.toString()));//.sort("name"));
				intermediateResult = nodeFactory.createNodes(securityContext, hits, includeDeleted, publicOnly);

//                              hits.close();
				long t2 = System.currentTimeMillis();

				logger.log(Level.FINE, "Creating structr nodes took {0} ms, {1} nodes made.", new Object[] { t2 - t1, intermediateResult.size() });

			}

			long t2 = System.currentTimeMillis();

			if (!filters.isEmpty()) {

				// Filter intermediate result
				for (AbstractNode node : intermediateResult) {

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
					List<GraphObject> result = attr.getResult();

					if (op.equals(SearchOperator.AND)) {

						intermediateResult = ListUtils.intersection(intermediateResult, result);

					} else if (op.equals(SearchOperator.OR)) {

						intermediateResult = ListUtils.sum(intermediateResult, result);

					} else if (op.equals(SearchOperator.NOT)) {

						intermediateResult = ListUtils.subtract(intermediateResult, result);

					}

				}
			}

			// eventually filter by distance from a given point
			if (coords != null) {}

			finalResult.addAll(intermediateResult);

			long t3 = System.currentTimeMillis();

			logger.log(Level.FINE, "Filtering nodes took {0} ms. Result size now {1}.", new Object[] { t3 - t2, finalResult.size() });
		}

		long t4 = System.currentTimeMillis();

		// sort search results; defaults to name, (@see AbstractNode.compareTo())
		Collections.sort(finalResult);

		long t5 = System.currentTimeMillis();

		logger.log(Level.FINE, "Sorting nodes took {0} ms.", new Object[] { t5 - t4 });

		return finalResult;
	}

	private String toQueryString(final TextualSearchAttribute singleAttribute, final boolean isFirst) {

		String key        = singleAttribute.getKey();
		String value      = singleAttribute.getValue();
		SearchOperator op = singleAttribute.getSearchOperator();

		if (StringUtils.isBlank(value) || value.equals("\"\"")) {

			return " " + key + ":" + IMPROBABLE_SEARCH_VALUE + "";

		}

		// NOT operator should always be applied
		return ((isFirst &&!(op.equals(SearchOperator.NOT)))
			? ""
			: " " + op + " ") + expand(key, value);
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

		List<AbstractNode> notMatchingNodes = new LinkedList<AbstractNode>();

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
		return value.startsWith("\"") && value.endsWith("\"");
	}
}
