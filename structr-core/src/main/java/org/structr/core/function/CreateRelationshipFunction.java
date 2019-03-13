/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class CreateRelationshipFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CREATE_RELATIONSHIP    = "Usage: ${create_relationship(from, to, relType)}. Example: ${create_relationship(me, user, 'FOLLOWS')} (Relationshiptype has to exist)";
	public static final String ERROR_MESSAGE_CREATE_RELATIONSHIP_JS = "Usage: ${{Structr.create_relationship(from, to, relType)}}. Example: ${{Structr.create_relationship(Structr.get('me'), user, 'FOLLOWS')}} (Relationshiptype has to exist)";

	@Override
	public String getName() {
		return "create_relationship";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			final Object source = sources[0];
			final Object target = sources[1];
			final String relType = (String)sources[2];

			AbstractNode sourceNode = null;
			AbstractNode targetNode = null;

			if (source instanceof AbstractNode && target instanceof AbstractNode) {

				sourceNode = (AbstractNode)source;
				targetNode = (AbstractNode)target;

			} else {

				logger.warn("Error: entities are not nodes. Parameters: {}", getParametersAsString(sources));
				return "Error: entities are not nodes.";
			}

			final Class relClass = StructrApp.getConfiguration().getRelationClassForCombinedType(sourceNode.getType(), relType, targetNode.getType());

			if (relClass != null) {

				return StructrApp.getInstance(sourceNode.getSecurityContext()).create(sourceNode, targetNode, relClass);

			} else {

				logger.warn("Error: Unknown relationship type. Parameters: {}", getParametersAsString(sources));
				return "Error: Unknown relationship type";
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CREATE_RELATIONSHIP_JS : ERROR_MESSAGE_CREATE_RELATIONSHIP);
	}

	@Override
	public String shortDescription() {
		return "Creates a relationship of the given type between two entities";
	}
}
