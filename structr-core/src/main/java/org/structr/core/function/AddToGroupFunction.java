/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class AddToGroupFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE    = "Usage: ${add_to_group(group, principal)}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.addToGroup(group, principal);}}";

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {


		try {

			if (!arrayHasLengthAndAllElementsNotNull(sources, 2)) {

				return "";
			}

			if (!(sources[0] instanceof Group)) {

				logger.warn("Error: first argument is not a Group. Parameters: {}", getParametersAsString(sources));
				return "Error: first argument is not a Group.";
			}

			if (!(sources[1] instanceof Principal)) {

				logger.warn("Error: second argument is not a Principal. Parameters: {}", getParametersAsString(sources));
				return "Error: second argument is not a Principal.";
			}

			final Group group    = (Group)sources[0];
			final Principal user = (Principal)sources[1];

			group.addMember(user);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {

		if (inJavaScriptContext) {
			return ERROR_MESSAGE_JS;
		}

		return ERROR_MESSAGE;
	}

	@Override
	public String shortDescription() {
		return "Adds a user to a group.";
	}

	@Override
	public String getName() {
		return "addToGroup()";
	}
}
