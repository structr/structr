package org.structr.core.parser.function;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class OutgoingFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_OUTGOING    = "Usage: ${outgoing(entity [, relType])}. Example: ${outgoing(this, 'PARENT_OF')}";
	public static final String ERROR_MESSAGE_OUTGOING_JS = "Usage: ${{Structr.outgoing(entity [, relType])}}. Example: ${{outgoing(Structr.this, 'PARENT_OF')}}";

	@Override
	public String getName() {
		return "outgoing()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final RelationshipFactory factory = new RelationshipFactory(entity != null ? entity.getSecurityContext() : ctx.getSecurityContext());
			final Object source = sources[0];

			if (source instanceof NodeInterface) {

				final NodeInterface node = (NodeInterface)source;
				if (sources.length > 1) {

					final Object relType = sources[1];
					if (relType != null && relType instanceof String) {

						final String relTypeName = (String)relType;
						return factory.instantiate(node.getNode().getRelationships(Direction.OUTGOING, DynamicRelationshipType.withName(relTypeName)));
					}

				} else {

					return factory.instantiate(node.getNode().getRelationships(Direction.OUTGOING));
				}

			} else {

				return "Error: entity is not a node.";
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_OUTGOING_JS : ERROR_MESSAGE_OUTGOING);
	}

	@Override
	public String shortDescription() {
		return "Returns the outgoing relationships of the given entity";
	}

}
