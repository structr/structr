/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.kernel.Traversal;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;
import org.structr.core.resource.NoResultsException;
import org.structr.core.resource.PathException;
import org.structr.core.resource.filter.Filter;

/**
 * Represents a bulk type match. A TypeConstraint will always result in a
 * list of elements when it is the last element in an URI. A TypeConstraint
 * that is not the first element in an URI will try to find a pre-defined
 * relationship between preceding and the node type (defined by
 * {@see AbstractNode#getRelationshipWith}) and follow that path.
 * 
 * @author Christian Morgner
 */
public class TypeConstraint extends ResourceConstraint<AbstractNode> {

	private static final Logger logger = Logger.getLogger(TypeConstraint.class.getName());

	private List<Filter> filters = new LinkedList<Filter>();
	private String type = null;
	
	@Override
	public boolean acceptUriPart(String part) {

		// todo: check if type exists etc.
		this.type = part;

		return true;
	}

	@Override
	public Result<AbstractNode> processParentResult(Result<AbstractNode> result, HttpServletRequest request) throws PathException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		User user = new SuperUser();
		AbstractNode topNode = null;
		boolean includeDeleted = false;
		boolean publicOnly = false;

		if(type != null) {

			searchAttributes.add(new TextualSearchAttribute("type", type, SearchOperator.OR));

			List<AbstractNode> results = (List<AbstractNode>)Services.command(SearchNodeCommand.class).execute(
				user,
				topNode,
				includeDeleted,
				publicOnly,
				searchAttributes
			);

			if(!results.isEmpty()) {

				return new Result(results);
			}
		}

		throw new NoResultsException();
	}

	@Override
	public boolean supportsMethod(String method) {
		return true;
	}

	@Override
	public boolean supportsNesting() {
		return true;
	}

	public void addFilter(Filter filter) {
		filters.add(filter);
	}

	private List<AbstractNode> getTraversalResults(Node node, RelationshipType relType, Direction direction) {

		// use traverser
		Iterable<Node> nodes = Traversal.description().breadthFirst().relationships(relType, direction).evaluator(

			new Evaluator() {

				@Override
				public Evaluation evaluate(Path path) {

					int len = path.length();
					if(len <= 1) {

						if(len == 0) {

							// do not include start node (which is the
							// index node in this case), but continue
							// traversal
							return Evaluation.EXCLUDE_AND_CONTINUE;

						} else {

							Node currentNode = path.endNode();
							boolean include = true;

							// apply filters
							for(Filter filter : filters) {
								include &= filter.includeInResultSet(currentNode);
							}

							if(include) {

								return Evaluation.INCLUDE_AND_CONTINUE;
							}
						}
					}

					return Evaluation.EXCLUDE_AND_PRUNE;
				}

			}

		).traverse(node).nodes();

		// collect results and convert nodes into splink nodes
		Command nodeFactory = Services.command(NodeFactoryCommand.class);
		List<AbstractNode> nodeList = new LinkedList<AbstractNode>();
		for(Node n : nodes) {
			nodeList.add((AbstractNode)nodeFactory.execute(n));
		}

		return nodeList;
	}
}
