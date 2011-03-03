/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.structr.common.SearchOperator;
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
 * <li>{@see AbstractNode} top node: search only below this node
 *     <p>if null, search everywhere (top node = root node)
 * <li>{@see User} user: return nodes only if readable for the user
 *     <p>if null, don't filter by user
 * <li>boolean include deleted: if true, return deleted nodes as well
 * <li>boolean public only: if true, return only public nodes
 * <li>List<{@see SingleSearchAttribute}> search attributes: key/value pairs with search operator
 *    <p>if no SingleSearchAttribute is given, return any node matching the other
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

        List<AbstractNode> result = Collections.emptyList();

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



            String queryString = "";

            int a = 0;
            for (SearchAttribute attr : searchAttrs) {

                if (attr instanceof SearchAttributeGroup) {

                    SearchAttributeGroup attributeGroup = (SearchAttributeGroup) attr;
                    List<SearchAttribute> groupedAttributes = attributeGroup.getSearchAttributes();

                    String subQueryString = "";

                    if (!(groupedAttributes.isEmpty())) {

                        BooleanQuery subQuery = new BooleanQuery();

                        String subQueryPrefix = (a > 0 ? " " + attributeGroup.getSearchOperator() : "") + " ( ";

                        int b = 0;
                        for (SearchAttribute groupedAttr : groupedAttributes) {

                            subQuery.add(toQuery((SingleSearchAttribute) groupedAttr), translateToBooleanClauseOccur(groupedAttr.getSearchOperator()));
                            subQueryString += toQueryString((SingleSearchAttribute) groupedAttr, b > 0);
                            b++;
                        }
                        query.add(subQuery, translateToBooleanClauseOccur(attributeGroup.getSearchOperator()));
                        String subQuerySuffix = " ) ";

                        // Add sub query only if not blank
                        if (StringUtils.isNotBlank(subQueryString)) {
                            queryString += subQueryPrefix + subQueryString + subQuerySuffix;
                        }

                    }
                    a++;

                } else if (attr instanceof SingleSearchAttribute) {

                    query.add(toQuery((SingleSearchAttribute) attr), translateToBooleanClauseOccur(attr.getSearchOperator()));
                    queryString += toQueryString((SingleSearchAttribute) attr, a > 0);
                    a++;

                }

            }

            if (searchAttrs.isEmpty() || StringUtils.isBlank(queryString)) {

                if (topNode != null) {
                    result = topNode.getAllChildren(user);
                    Collections.sort(result);
                    return result;

                } else {

                    return Collections.emptyList();

                }
            }

            long t0 = System.currentTimeMillis();
            logger.log(Level.INFO, "{0}", queryString);

            IndexHits hits = index.query(new QueryContext(queryString));//.sort("name"));

            long t1 = System.currentTimeMillis();
            logger.log(Level.INFO, "Querying index took {0} ms, {1} results retrieved.", new Object[]{t1 - t0, hits.size()});


//            IndexHits hits = index.query(new QueryContext(query.toString()));//.sort("name"));
            result = nodeFactory.createNodes(hits, includeDeleted);

            long t2 = System.currentTimeMillis();
            logger.log(Level.INFO, "Creating structr nodes took {0} ms, {1} nodes made.", new Object[]{t2 - t1, result.size()});



//
//                List<AbstractNode> intermediateResult;
//                if (topNode != null) {
//                    intermediateResult = topNode.getAllChildren(user);
//                } else {
//                    intermediateResult = new LinkedList<AbstractNode>();
//                }
//
//                for (SingleSearchAttribute attr : searchAttrs) {
//
//                    String key = attr.getKey();
//                    Object value = attr.getValue();
//                    SearchOperator op = attr.getSearchOperator();
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
//                            if (SingleSearchAttribute.WILDCARD.equals(Search.unquoteExactMatch(stringValue))) {
//                                value = Search.unquoteExactMatch(stringValue);
//                            }
//                        }
//
//                        // if more than one character with leading wildcard, remove wildcard
//                        if (stringValue != null && stringValue.length() > 1 && stringValue.startsWith(SingleSearchAttribute.WILDCARD)) {
//                            stringValue = stringValue.substring(1);
//                        }
//
//                        boolean indexHits = false;
//                        boolean wildcardHits = false;
//
//                        if (StringUtils.isNotBlank(key) && (value != null && (!isString || StringUtils.isNotBlank(stringValue)))) {
//
//                            // catch wildcard
//                            if (SingleSearchAttribute.WILDCARD.equals(value)) {
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
//
//
//                }
//                result = new ArrayList(intermediateResult);
        }

        // sort search results; defaults to name, (@see AbstractNode.compareTo())
        Collections.sort(result);
        return result;

    }

    private String toQueryString(final SingleSearchAttribute singleAttribute, final boolean isFirst) {

        String key = singleAttribute.getKey();
        Object value = singleAttribute.getValue();
        SearchOperator op = singleAttribute.getSearchOperator();

        boolean valueIsNoStringAndNotNull = (value != null && !(value instanceof String));
        boolean valueIsNoBlankString = (value != null && value instanceof String && StringUtils.isNotBlank((String) value));

        if (StringUtils.isNotBlank(key) && (valueIsNoStringAndNotNull || valueIsNoBlankString)) {

            return (isFirst ? " " + op + " " : "") + expand(key, value.toString());

        }

        return "";
    }

    private Query toQuery(final SingleSearchAttribute singleAttribute) {

        String key = singleAttribute.getKey();
        Object value = singleAttribute.getValue();
        SearchOperator op = singleAttribute.getSearchOperator();

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

    private String expand(final String key, final String value) {

        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return "";
        }

        // If value starts with operator (exact match, range query, or search operator word),
        // don't expand
        if (value.startsWith("\"") || value.startsWith("[") || value.startsWith("NOT") || value.startsWith("AND") || value.startsWith("OR")) {
            return " " + key + ":" + value + " ";
        }

        String result = "( ";

        // Split string into words
        String[] words = StringUtils.split(value, " ");

        // Expand key,word to ' (key:word* OR key:"word") '

        for (String word : words) {

            result += " (" + key + ":" + word + "* OR " + key + ":\"" + word + "\") OR ";

        }

        result += key + ":\"" + value + "\" ) ";

        return result;
    }
}
