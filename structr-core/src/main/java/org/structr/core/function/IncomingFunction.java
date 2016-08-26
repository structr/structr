/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.function;

import java.util.logging.Level;
import org.structr.api.graph.Direction;
import org.structr.api.graph.RelationshipType;
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

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

			final RelationshipFactory factory = new RelationshipFactory(entity != null ? entity.getSecurityContext() : ctx.getSecurityContext());
			final Object source = sources[0];

			if (source instanceof NodeInterface) {

				final NodeInterface node = (NodeInterface)source;
				if (sources.length > 1) {

					final Object relType = sources[1];
					if (relType != null && relType instanceof String) {

						final String relTypeName = (String)relType;
						return factory.bulkInstantiate(node.getNode().getRelationships(Direction.INCOMING, RelationshipType.forName(relTypeName)));
					}

				} else {

					return factory.bulkInstantiate(node.getNode().getRelationships(Direction.INCOMING));
				}

			} else {

				logger.log(Level.WARNING, "Error: entity is not a node. Parameters: {0}", getParametersAsString(sources));
				return "Error: entity is not a node.";

			}

		} else {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

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
