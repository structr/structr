/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.impl.lucene.QueryContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.NodeServiceCommand;
import org.structr.core.node.StructrNodeFactory;

/**
 * <b>Search for nodes by attributes</b>
 * <p>
 * The execute method takes an arbitraty list of parameters, but the first
 * four parameters are
 * <p>
 * <ol>
 * <li>{@see User} user: return nodes only if readable for the user
 *     <p>if null, don't filter by user
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

    private static final Logger logger = Logger.getLogger(SearchNodeCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {
        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        Index<Node> index = (Index<Node>) arguments.get("index");
        StructrNodeFactory nodeFactory = (StructrNodeFactory) arguments.get("nodeFactory");

        // Using TreeSet: No duplicates and no need to sort
        Set<AbstractNode> result = new TreeSet<AbstractNode>();

        if (graphDb != null) {

            if (parameters == null || parameters.length != 5) {
                logger.log(Level.WARNING, "Exactly 5 parameters are required for advanced search.");
                return Collections.emptyList();
            }

            User user = null;
            if (parameters[0] instanceof User) {
                user = (User) parameters[0];
            }

            // FIXME: filtering by top node is experimental
            AbstractNode topNode = null;
            if (parameters[1] instanceof AbstractNode) {
                topNode = (AbstractNode) parameters[1];
            }

            boolean includeDeleted = false;
            if (parameters[2] instanceof Boolean) {
                includeDeleted = (Boolean) parameters[2];
            }

            boolean publicOnly = false;
            if (parameters[3] instanceof Boolean) {
                publicOnly = (Boolean) parameters[3];
            }

            List<SearchAttribute> searchAttrs = new ArrayList<SearchAttribute>();
            if (parameters[4] instanceof List) {
                searchAttrs = (List<SearchAttribute>) parameters[4];
            }

            for (int i = 4; i < parameters.length; i++) {

                Object o = parameters[i];
                if (o instanceof SearchAttribute) {
                    searchAttrs.add((SearchAttribute) o);
                }

            }

            // At this point, all search attributes are ready

            BooleanQuery query = new BooleanQuery();

            List<BooleanSearchAttribute> booleanAttributes = new LinkedList<BooleanSearchAttribute>();

            String textualQueryString = "";

            for (SearchAttribute attr : searchAttrs) {

                if (attr instanceof SearchAttributeGroup) {

                    SearchAttributeGroup attributeGroup = (SearchAttributeGroup) attr;
                    List<SearchAttribute> groupedAttributes = attributeGroup.getSearchAttributes();

                    String subQueryString = "";

                    if (!(groupedAttributes.isEmpty())) {

                        BooleanQuery subQuery = new BooleanQuery();

                        String subQueryPrefix = (StringUtils.isBlank(textualQueryString) ? "" : attributeGroup.getSearchOperator()) + " ( ";

                        for (SearchAttribute groupedAttr : groupedAttributes) {

                            if (groupedAttr instanceof TextualSearchAttribute) {

                                subQuery.add(toQuery((TextualSearchAttribute) groupedAttr), translateToBooleanClauseOccur(groupedAttr.getSearchOperator()));
                                subQueryString += toQueryString((TextualSearchAttribute) groupedAttr, StringUtils.isBlank(subQueryString));

                            }
                        }
                        query.add(subQuery, translateToBooleanClauseOccur(attributeGroup.getSearchOperator()));
                        String subQuerySuffix = " ) ";

                        // Add sub query only if not blank
                        if (StringUtils.isNotBlank(subQueryString)) {
                            textualQueryString += subQueryPrefix + subQueryString + subQuerySuffix;
                        }

                    }

                } else if (attr instanceof TextualSearchAttribute) {

                    query.add(toQuery((TextualSearchAttribute) attr), translateToBooleanClauseOccur(attr.getSearchOperator()));
                    textualQueryString += toQueryString((TextualSearchAttribute) attr, StringUtils.isBlank(textualQueryString));

                } else if (attr instanceof BooleanSearchAttribute) {

                    booleanAttributes.add((BooleanSearchAttribute) attr);

                }

            }

            List<AbstractNode> intermediateResult;
            if (searchAttrs.isEmpty() || StringUtils.isBlank(textualQueryString)) {

                if (topNode != null) {
                    intermediateResult = topNode.getAllChildren(user);
                } else {
                    intermediateResult = new LinkedList<AbstractNode>();

                }
            } else {

                long t0 = System.currentTimeMillis();
                logger.log(Level.FINE, "Textual Query String: {0}", textualQueryString);

                IndexHits hits = index.query(new QueryContext(textualQueryString));//.sort("name"));

                long t1 = System.currentTimeMillis();
                logger.log(Level.FINE, "Querying index took {0} ms, {1} results retrieved.", new Object[]{t1 - t0, hits.size()});


//            IndexHits hits = index.query(new QueryContext(query.toString()));//.sort("name"));
                intermediateResult = nodeFactory.createNodes(hits, user, includeDeleted, publicOnly);

                long t2 = System.currentTimeMillis();
                logger.log(Level.FINE, "Creating structr nodes took {0} ms, {1} nodes made.", new Object[]{t2 - t1, intermediateResult.size()});
            }

            long t2 = System.currentTimeMillis();

            if (booleanAttributes.isEmpty()) {
                result.addAll(intermediateResult);
            } else {

                // Filter intermediate result
                for (AbstractNode node : intermediateResult) {

                    for (BooleanSearchAttribute attr : booleanAttributes) {

                        String key = attr.getKey();
                        Boolean value = attr.getValue();
                        SearchOperator op = attr.getSearchOperator();

                        Object nodeValue = node.getProperty(key);

                        if (op.equals(SearchOperator.OR)) {
                            result.add(node);
                        } else if (op.equals(SearchOperator.AND)) {

                            if (nodeValue == null && value == null) {
                                result.add(node);
                            }

                            if (nodeValue != null && nodeValue.equals(value)) {
                                result.add(node);
                            }

                        } else if (op.equals(SearchOperator.NOT)) {

                            if (nodeValue != null && !(nodeValue.equals(value))) {
                                result.add(node);
                            }

                        }
//
//
//
//                    Iterable<Node> nodes = null;
//                    try {
//
//                        String stringValue = null;
//                        boolean isString = false;
//                        if (value instanceof String) {
//                            isString = true;
//                            stringValue = (String) value;
//
//                            if (TextualSearchAttribute.WILDCARD.equals(Search.unquoteExactMatch(stringValue))) {
//                                value = Search.unquoteExactMatch(stringValue);
//                            }
//                        }
//
//                        // if more than one character with leading wildcard, remove wildcard
//                        if (stringValue != null && stringValue.length() > 1 && stringValue.startsWith(TextualSearchAttribute.WILDCARD)) {
//                            stringValue = stringValue.substring(1);
//                        }
//
//                        boolean indexHits = false;
//                        boolean wildcardHits = false;
//
//                        if (StringUtils.isNotBlank(key) && (value != null && (!isString || StringUtils.isNotBlank(stringValue)))) {
//
//                            // catch wildcard
//                            if (TextualSearchAttribute.WILDCARD.equals(value)) {
//                                nodes = graphDb.getAllNodes();
//                                wildcardHits = (nodes != null && nodes.iterator().hasNext());
//                            } else {
//
//                                if (isString) {
//                                    nodes = index.query(attr.getKey(), stringValue);
//                                } else {
//                                    nodes = index.query(attr.getKey(), attr.getValue());
//                                }
//                                indexHits = (nodes != null && nodes.iterator().hasNext());
//                            }
//
//                        }
//
//                        // if search operator is AND, stop search on first empty single result
//                        if (op.equals(SearchOperator.AND) && !indexHits && !wildcardHits) {
//                            return Collections.emptyList();
//                        }
//
//                    } catch (Throwable t) {
//                        logger.log(Level.WARNING, "Search error", t);
//                    }
//
//                    List<AbstractNode> singleResult = nodeFactory.createNodes(nodes);
//
//                    if (op.equals(SearchOperator.OR)) {
//
//                        // OR operator: add single result to intermediate result list
//                        intermediateResult = ListUtils.sum(intermediateResult, singleResult);
//
//                    } else if (op.equals(SearchOperator.AND)) {
//
//                        // If no intermediate result is given, start with the first single result
//                        // Note: We can safely assume an empty intermediate result because
//                        // in AND mode, search stops after the first empty single result set
//                        if (intermediateResult.isEmpty()) {
//                            intermediateResult = singleResult;
//                        }
//
//                        // AND operator: intersect single result with intermediate result
//                        List<AbstractNode> intersectionResult = ListUtils.intersection(intermediateResult, singleResult);
//                        intermediateResult = intersectionResult;
//
//                    } else if (op.equals(SearchOperator.AND_NOT)) {
//
//                        // If no intermediate result is given, start with the first single result
//                        // Note: We can safely assume an empty intermediate result because
//                        // in AND mode, search stops after the first empty single result set
//                        if (intermediateResult.isEmpty()) {
//                            intermediateResult = singleResult;
//                        }
//                        // AND_NOT operator: intersect single result with intermediate result
//                        List<AbstractNode> intersectionResult = ListUtils.subtract(intermediateResult, singleResult);
//                        intermediateResult = intersectionResult;
//                    }


                    }
                }
            }
            long t3 = System.currentTimeMillis();
            logger.log(Level.FINE, "Filtering nodes took {0} ms. Result size now {1}.", new Object[]{t3 - t2, result.size()});

            //result = new ArrayList(intermediateResult);
        }

//        long t4 = System.currentTimeMillis();
//
//        // sort search results; defaults to name, (@see AbstractNode.compareTo())
//        Collections.sort(result);
//
//        long t5 = System.currentTimeMillis();
//        logger.log(Level.INFO, "Sorting nodes took {0} ms.", new Object[]{t5 - t4});

        return new LinkedList<AbstractNode>(result);

    }

    private String toQueryString(final TextualSearchAttribute singleAttribute, final boolean isFirst) {

        String key = singleAttribute.getKey();
        String value = singleAttribute.getValue();
        SearchOperator op = singleAttribute.getSearchOperator();

        if (StringUtils.isBlank(value)) {
            return "";
        }

        // NOT operator should always be applied
        return (isFirst && !(op.equals(SearchOperator.NOT)) ? "" : " " + op + " ") + expand(key, value);

    }

    private Query toQuery(final TextualSearchAttribute singleAttribute) {

        String key = singleAttribute.getKey();
        Object value = singleAttribute.getValue();
//            SearchOperator op = singleAttribute.getSearchOperator();

        if (key == null || value == null) {
            return null;
        }

        boolean valueIsNoStringAndNotNull = (value != null && !(value instanceof String));
        boolean valueIsNoBlankString = (value != null && value instanceof String && StringUtils.isNotBlank((String) value));

        if (StringUtils.isNotBlank(key) && (valueIsNoStringAndNotNull || valueIsNoBlankString)) {
            return new TermQuery(new Term(key, value.toString()));
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
        if (stringValue == null || stringValue.startsWith("\"") || stringValue.startsWith("[") || stringValue.startsWith("NOT") || stringValue.startsWith("AND") || stringValue.startsWith("OR")) {
            return " " + key + ":" + value + " ";
        }

        String result = "( ";

        // Split string into words
        String[] words = StringUtils.split(stringValue, " ");

        // Expand key,word to ' (key:word* OR key:"word") '

        int i=1;
        for (String word : words) {

            String cleanWord = Search.normalize(word);

            result += " (" + key + ":" + cleanWord + "* OR " + key + ":\"" + cleanWord + "\")" + (i<words.length ? " AND " : " ) ");
            i++;

        }

        return result;
    }
}
