/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;

public class RemoveFromGroupFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE    = "Usage: ${remove_from_group(group, principal)}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.removeFromGroup(group, principal);}}";

	@Override
	public String getName() {
		return "remove_from_group";
	}

	@Override
	public String getSignature() {
		return "group, user";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			if (!(sources[0] instanceof NodeInterface n1 && n1.is(StructrTraits.GROUP))) {

				logParameterError(caller, sources, "Expected node of type Group as first argument!", ctx.isJavaScriptContext());

			} else if (!(sources[1] instanceof NodeInterface n2 && n2.is(StructrTraits.PRINCIPAL))) {

				logParameterError(caller, sources, "Expected node of type Principal as second argument!", ctx.isJavaScriptContext());

			} else if ((sources[1] instanceof SuperUser)) {

				logParameterError(caller, sources, "Expected node of type Principal as second argument - SuperUser can not be member of a group!", ctx.isJavaScriptContext());

			} else {

				final NodeInterface group = (NodeInterface)sources[0];
				final NodeInterface user  = (NodeInterface)sources[1];

				group.as(Group.class).removeMember(ctx.getSecurityContext(), user.as(Principal.class));
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String shortDescription() {
		return "Removes the given user from the given group";
	}
}
