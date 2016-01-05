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
public class IncomingFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_INCOMING    = "Usage: ${incoming(entity [, relType])}. Example: ${incoming(this, 'PARENT_OF')}";
	public static final String ERROR_MESSAGE_INCOMING_JS = "Usage: ${{Structr.incoming(entity [, relType])}}. Example: ${{Structr.incoming(Structr.this, 'PARENT_OF')}}";

	@Override
	public String getName() {
		return "incoming()";
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
						return factory.instantiate(node.getNode().getRelationships(Direction.INCOMING, DynamicRelationshipType.withName(relTypeName)));
					}

				} else {

					return factory.instantiate(node.getNode().getRelationships(Direction.INCOMING));
				}

			} else {

				return "Error: entity is not a node.";
			}
		}
		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_INCOMING_JS : ERROR_MESSAGE_INCOMING);
	}

	@Override
	public String shortDescription() {
		return "Returns the incoming relationships of the given entity";
	}
}
