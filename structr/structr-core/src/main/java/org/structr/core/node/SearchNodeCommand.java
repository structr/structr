/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.structr.common.Search;
import org.structr.common.SearchOperator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

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
 * <li>boolean include hidden: if true, return hidden nodes as well
 * <li>boolean public only: if true, return only public nodes
 * <li>List<{@see SearchAttribute}> search attributes: key/value pairs with search operator
 *    <p>if no SearchAttribute is given, return any node matching the other
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

            // FIXME: filtering by top node is experimental
            AbstractNode topNode = null;
            if (parameters[0] instanceof AbstractNode) {
                topNode = (AbstractNode) parameters[0];
            }

            User user = null;
            if (parameters[1] instanceof User) {
                user = (User) parameters[1];
            }

            boolean includeHidden = false;
            if (parameters[2] instanceof Boolean) {
                includeHidden = (Boolean) parameters[2];
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


            if (searchAttrs.isEmpty()) {

                if (topNode != null) {
                    result = topNode.getAllChildren(user);
                } else {
                    result = new LinkedList<AbstractNode>();
                }

            } else {

                List<AbstractNode> intermediateResult;
                if (topNode != null) {
                    intermediateResult = topNode.getAllChildren(user);
                } else {
                    intermediateResult = new LinkedList<AbstractNode>();
                }

                for (SearchAttribute attr : searchAttrs) {

                    String key = attr.getKey();
                    Object value = attr.getValue();
                    SearchOperator op = attr.getSearchOperator();

                    Iterable<Node> nodes = null;
                    try {

                        String stringValue = null;
                        boolean isString = false;
                        if (value instanceof String) {
                            isString = true;
                            stringValue = (String) value;

                            if (SearchAttribute.WILDCARD.equals(Search.unquoteExactMatch(stringValue))) {
                                value = Search.unquoteExactMatch(stringValue);
                            }
                        }

                        // if more than one character with leading wildcard, remove wildcard
                        if (stringValue != null && stringValue.length() > 1 && stringValue.startsWith(SearchAttribute.WILDCARD)) {
                            stringValue = stringValue.substring(1);
                        }

                        boolean indexHits = false;
                        boolean wildcardHits = false;

                        if (StringUtils.isNotBlank(key) && (value != null && (!isString || StringUtils.isNotBlank(stringValue)))) {

                            // catch wildcard
                            if (SearchAttribute.WILDCARD.equals(value)) {
                                nodes = graphDb.getAllNodes();
                                wildcardHits = (nodes != null && nodes.iterator().hasNext());
                            } else {

                                if (isString) {
                                    nodes = index.query(attr.getKey(), stringValue);
                                } else {
                                    nodes = index.query(attr.getKey(), attr.getValue());
                                }
                                indexHits = (nodes != null && nodes.iterator().hasNext());
                            }

                        }

                        // if search operator is AND, stop search on first empty single result
                        if (op.equals(SearchOperator.AND) && !indexHits && !wildcardHits) {
                            return Collections.emptyList();
                        }

                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Search error", t);
                    }

                    List<AbstractNode> singleResult = nodeFactory.createNodes(nodes);

                    if (op.equals(SearchOperator.OR)) {

                        // OR operator: add single result to intermediate result list
                        intermediateResult = ListUtils.sum(intermediateResult, singleResult);

                    } else if (op.equals(SearchOperator.AND)) {

                        // If no intermediate result is given, start with the first single result
                        // Note: We can safely assume an empty intermediate result because
                        // in AND mode, search stops after the first empty single result set
                        if (intermediateResult.isEmpty()) {
                            intermediateResult = singleResult;
                        }

                        // AND operator: intersect single result with intermediate result
                        List<AbstractNode> intersectionResult = ListUtils.intersection(intermediateResult, singleResult);
                        intermediateResult = intersectionResult;

                    } else if (op.equals(SearchOperator.AND_NOT)) {

                        // If no intermediate result is given, start with the first single result
                        // Note: We can safely assume an empty intermediate result because
                        // in AND mode, search stops after the first empty single result set
                        if (intermediateResult.isEmpty()) {
                            intermediateResult = singleResult;
                        }
                        // AND_NOT operator: intersect single result with intermediate result
                        List<AbstractNode> intersectionResult = ListUtils.subtract(intermediateResult, singleResult);
                        intermediateResult = intersectionResult;
                    }


                }
                result = new ArrayList(intermediateResult);
            }

            // sort search results; defaults to name, (@see AbstractNode.compareTo())
            Collections.sort(result);

            return result;

        }
        return Collections.emptyList();
    }
}
