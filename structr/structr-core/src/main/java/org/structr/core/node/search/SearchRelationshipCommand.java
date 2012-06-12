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

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.NodeService.RelationshipIndex;
import org.structr.core.node.NodeServiceCommand;
import org.structr.core.node.RelationshipFactory;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * <b>Search for relationships by attributes</b>
 *
 * @author amorgner
 */
public class SearchRelationshipCommand extends NodeServiceCommand {

	private static String IMPROBABLE_SEARCH_VALUE = "xeHfc6OG30o3YQzX57_8____r-Wx-RW_70r84_71D-g--P9-3K";
	private static final Logger logger            = Logger.getLogger(SearchRelationshipCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		if ((parameters == null) || (parameters.length < 1) || (parameters.length > 1)) {

			logger.log(Level.WARNING, "Exactly one parameter of type 'List<SearchAttribute>' is required for relationship search.");

			return Collections.emptyList();

		}

		List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

		if (parameters[0] instanceof List) {

			searchAttrs = (List<SearchAttribute>) parameters[0];

		}

		return search(securityContext, searchAttrs);
	}

	/**
	 * Return a list of relationships which fit to all search criteria.
	 *
	 * @param securityContext       Search in this security context
	 * @param searchAttrs           List with search attributes
	 * @return
	 */
	private List<AbstractRelationship> search(final SecurityContext securityContext, final List<SearchAttribute> searchAttrs) throws FrameworkException {

		GraphDatabaseService graphDb            = (GraphDatabaseService) arguments.get("graphDb");
		RelationshipFactory relationshipFactory = (RelationshipFactory) arguments.get("relationshipFactory");
		List<AbstractRelationship> finalResult  = new LinkedList<AbstractRelationship>();
		boolean allExactMatch                   = true;
		final Index<Relationship> index;

		// boolean allFulltext = false;
		if (graphDb != null) {

			// At this point, all search attributes are ready
			BooleanQuery query                             = new BooleanQuery();
			List<FilterSearchAttribute> filters            = new LinkedList<FilterSearchAttribute>();
			List<TextualSearchAttribute> textualAttributes = new LinkedList<TextualSearchAttribute>();
			StringBuilder textualQueryString               = new StringBuilder();

			for (SearchAttribute attr : searchAttrs) {

				if (attr instanceof SearchAttributeGroup) {

					SearchAttributeGroup attributeGroup     = (SearchAttributeGroup) attr;
					List<SearchAttribute> groupedAttributes = attributeGroup.getSearchAttributes();
					StringBuilder subQueryString            = new StringBuilder();

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

			List<AbstractRelationship> intermediateResult;

			if (searchAttrs.isEmpty() || StringUtils.isBlank(textualQueryString.toString())) {

				intermediateResult = new LinkedList<AbstractRelationship>();

			} else {

				long t0 = System.currentTimeMillis();

				logger.log(Level.FINE, "Textual Query String: {0}", textualQueryString);

				QueryContext queryContext = new QueryContext(textualQueryString);
				IndexHits hits;

				if ((textualAttributes.size() == 1) && textualAttributes.get(0).getKey().equals(AbstractRelationship.Key.uuid.name())) {

					// Search for uuid only: Use UUID index
					index = (Index<Relationship>) arguments.get(RelationshipIndex.rel_uuid.name());
					synchronized(index) {
						hits  = index.get(AbstractNode.Key.uuid.name(), decodeExactMatch(textualAttributes.get(0).getValue()));
					}
				} else if ((textualAttributes.size() > 1) && allExactMatch) {

					// Only exact machtes: Use keyword index
					index = (Index<Relationship>) arguments.get(RelationshipIndex.rel_keyword.name());
					synchronized(index) {
						hits  = index.query(queryContext);
					}
				} else {

					// Default: Mixed or fulltext-only search: Use fulltext index
					index = (Index<Relationship>) arguments.get(RelationshipIndex.rel_fulltext.name());
					synchronized(index) {
						hits  = index.query(queryContext);
					}
				}

				long t1 = System.currentTimeMillis();

				logger.log(Level.FINE, "Querying index took {0} ms, size() says {1} results.", new Object[] { t1 - t0, hits.size() });

//                              IndexHits hits = index.query(new QueryContext(query.toString()));//.sort("name"));
				intermediateResult = relationshipFactory.createRelationships(securityContext, hits);

				hits.close();
				long t2 = System.currentTimeMillis();

				logger.log(Level.FINE, "Creating structr relationships took {0} ms, {1} relationships made.", new Object[] { t2 - t1, intermediateResult.size() });

			}

			long t2 = System.currentTimeMillis();

			if (!filters.isEmpty()) {

				// Filter intermediate result
				for (AbstractRelationship rel : intermediateResult) {

					for (FilterSearchAttribute attr : filters) {

						String key         = attr.getKey();
						Object searchValue = attr.getValue();
						SearchOperator op  = attr.getSearchOperator();
						Object nodeValue   = rel.getProperty(key);

						if (op.equals(SearchOperator.NOT)) {

							if ((nodeValue != null) &&!(nodeValue.equals(searchValue))) {

								attr.addToResult(rel);

							}

						} else {

							if ((nodeValue == null) && (searchValue == null)) {

								attr.addToResult(rel);

							}

							if ((nodeValue != null) && nodeValue.equals(searchValue)) {

								attr.addToResult(rel);

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

			finalResult.addAll(intermediateResult);

			long t3 = System.currentTimeMillis();

			logger.log(Level.FINE, "Filtering nodes took {0} ms. Result size now {1}.", new Object[] { t3 - t2, finalResult.size() });
		}

		long t4 = System.currentTimeMillis();

		// sort search results; defaults to name, (@see AbstractNode.compareTo())
		Collections.sort(finalResult);

		long t5 = System.currentTimeMillis();

		logger.log(Level.FINE, "Sorting relationships took {0} ms.", new Object[] { t5 - t4 });

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

	private String expand(final String key, final Object value) {

		if (StringUtils.isBlank(key)) {

			return "";

		}

		String stringValue = null;

		if (value instanceof String) {

			stringValue = (String) value;

			if (StringUtils.isBlank(stringValue)) {

				return "";

			}

		}

		// If value is not a string or starts with operator (exact match, range query, or search operator word),
		// don't expand
		if ((stringValue == null) || stringValue.startsWith("\"") || stringValue.startsWith("[") || stringValue.startsWith("NOT") || stringValue.startsWith("AND")
			|| stringValue.startsWith("OR")) {

			return " " + key + ":" + value + " ";

		}

		String result = "( ";

		// Clean string
		stringValue = Search.clean(stringValue);

		// Split string into words
		String[] words = StringUtils.split(stringValue, " ");

		// Expand key,word to ' (key:word* OR key:"word") '
		int i = 1;

		for (String word : words) {

//                      String cleanWord = Search.clean(word);
//                      result += " (" + key + ":" + cleanWord + "* OR " + key + ":\"" + cleanWord + "\")" + (i<words.length ? " AND " : " ) ");
//                      result += " (" + key + ":" + word + "* OR " + key + ":\"" + word + "\")" + (i < words.length ? " AND " : " ) ");
			result += " (" + key + ":*" + word + "* OR " + key + ":\"" + word + "\")" + ((i < words.length)
				? " AND "
				: " ) ");

			i++;
		}

		return result;
	}

//
//      private List<AbstractNode> filterNotExactMatches(final List<AbstractNode> result, TextualSearchAttribute attr) {
//
//              List<AbstractNode> notMatchingNodes = new LinkedList<AbstractNode>();
//
//              // Filter not exact matches
//              for (AbstractNode node : result) {
//
//                      String value = attr.getValue();
//
//                      if ((value != null) && isExactMatch(value)) {
//
//                              String key          = attr.getKey();
//                              String nodeValue    = node.getStringProperty(key);
//                              String decodedValue = decodeExactMatch(value);
//
//                              if (!nodeValue.equals(decodedValue)) {
//
//                                      notMatchingNodes.add(node);
//
//                              }
//
//                      }
//
//              }
//
//              return ListUtils.subtract(result, notMatchingNodes);
//      }

	private String decodeExactMatch(final String value) {
		return StringUtils.strip(value, "\"");
	}

	//~--- get methods ----------------------------------------------------

	private boolean isExactMatch(final String value) {
		return value.startsWith("\"") && value.endsWith("\"");
	}
}
