package org.structr.core.parser.function;

import org.neo4j.graphdb.Node;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeFactory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class InstantiateFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_INSTANTIATE = "Usage: ${instantiate(node)}. Example: ${instantiate(result.node)}";

	@Override
	public String getName() {
		return "instantiate()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			return new NodeFactory<>(ctx.getSecurityContext()).instantiate((Node)sources[0]);
		}
		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_INSTANTIATE;
	}

	@Override
	public String shortDescription() {
		return "Instantiates the given Neo4j nodes into Structr nodes";
	}

}
