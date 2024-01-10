/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.api.graph.Direction;
import org.structr.api.graph.RelationshipType;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.schema.action.ActionContext;

public class OutgoingFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_OUTGOING    = "Usage: ${outgoing(entity [, relType])}. Example: ${outgoing(this, 'PARENT_OF')}";
	public static final String ERROR_MESSAGE_OUTGOING_JS = "Usage: ${{Structr.outgoing(entity [, relType])}}. Example: ${{outgoing(Structr.this, 'PARENT_OF')}}";

	@Override
	public String getName() {
		return "outgoing";
	}

	@Override
	public String getSignature() {
		return "entity [, relType ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final RelationshipFactory factory = new RelationshipFactory(ctx.getSecurityContext());
			final Object source = sources[0];

			if (source instanceof NodeInterface) {

				final NodeInterface node = (NodeInterface)source;
				if (sources.length > 1) {

					final Object relType = sources[1];
					if (relType != null && relType instanceof String) {

						final String relTypeName = (String)relType;
						return factory.bulkInstantiate(node.getNode().getRelationships(Direction.OUTGOING, RelationshipType.forName(relTypeName)));
					}

				} else {

					return factory.bulkInstantiate(node.getNode().getRelationships(Direction.OUTGOING));
				}

			} else {

				logger.warn("Error: entity is not a node. Parameters: {}", getParametersAsString(sources));
				return "Error: entity is not a node.";
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
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
