/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.PathException;

/**
 * Represents a bulk type match. A TypeConstraint will always result in a
 * list of elements when it is the last element in an URI. A TypeConstraint
 * that is not the first element in an URI will try to find a pre-defined
 * relationship between preceding and the node type (defined by
 * {@see AbstractNode#getRelationshipWith}) and follow that path.
 * 
 * @author Christian Morgner
 */
public class TypeConstraint extends ResourceConstraint {

	private static final Logger logger = Logger.getLogger(TypeConstraint.class.getName());

	private String type = null;
	
	@Override
	public boolean acceptUriPart(String part) {

		// todo: check if type exists etc.
		this.setType(part);

		return true;
	}

	@Override
	public List<GraphObject> process(List<GraphObject> results, HttpServletRequest request) throws PathException {

		if(results == null) {

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
			User user = new SuperUser();
			AbstractNode topNode = null;
			boolean includeDeleted = false;
			boolean publicOnly = false;

			if(type != null) {

				searchAttributes.add(new TextualSearchAttribute("type", type, SearchOperator.OR));

				results = (List<GraphObject>)Services.command(securityContext, SearchNodeCommand.class).execute(
					topNode,
					includeDeleted,
					publicOnly,
					searchAttributes
				);

				if(!results.isEmpty()) {
					return results;
				}
			}

		} else {

			logger.log(Level.WARNING, "Received results from predecessor, this query is probably not optimized!");

			// TypeConstraint acts as a type filter here
			for(Iterator<GraphObject> it = results.iterator(); it.hasNext();) {
				if(!type.equalsIgnoreCase(it.next().getType())) {
					it.remove();
				}
			}

			return results;
		}

		throw new NoResultsException();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		
		this.type = type.toLowerCase();

		if(this.type.endsWith("ies")) {
			logger.log(Level.FINEST, "Replacing trailing 'ies' with 'y' for type {0}", type);
			this.type = this.type.substring(0, this.type.length() - 3).concat("y");
		} else
		if(this.type.endsWith("s")) {
			logger.log(Level.FINEST, "Removing trailing plural 's' from type {0}", type);
			this.type = this.type.substring(0, this.type.length() - 1);
		}
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) {

		ResourceConstraint combinedConstraint = null;

		if(next instanceof IdConstraint)	combinedConstraint = new TypedIdConstraint((IdConstraint)next, this); else
		if(next instanceof SearchConstraint)	combinedConstraint = new TypedSearchConstraint(this, ((SearchConstraint)next).getSearchString());

		return combinedConstraint;
	}
}


	/*
	private List<Filter> filters = new LinkedList<Filter>();

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

		// collect results and convert nodes into structr nodes
		Command nodeFactory = Services.command(securityContext, NodeFactoryCommand.class);
		List<AbstractNode> nodeList = new LinkedList<AbstractNode>();
		for(Node n : nodes) {
			nodeList.add((AbstractNode)nodeFactory.execute(n));
		}

		return nodeList;
	}
	*/
