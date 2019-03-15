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

import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class GrantFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GRANT    = "Usage: ${grant(principal, node, permissions)}. Example: ${grant(me, this, 'read, write, delete'))}";
	public static final String ERROR_MESSAGE_GRANT_JS = "Usage: ${{Structr.grant(principal, node, permissions)}}. Example: ${{Structr.grant(Structr.get('me'), Structr.this, 'read, write, delete'))}}";

	@Override
	public String getName() {
		return "grant";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			if (sources[0] instanceof Principal) {

				final Principal principal = (Principal)sources[0];

				if (sources[1] instanceof AbstractNode) {

					final AbstractNode node = (AbstractNode)sources[1];

					if (sources[2] instanceof String) {

						final String[] parts = ((String)sources[2]).split("[,]+");
						for (final String part : parts) {

							final String trimmedPart = part.trim();
							if (trimmedPart.length() > 0) {

								final Permission permission = Permissions.valueOf(trimmedPart);
								if (permission != null) {

									node.grant(permission, principal, ctx.getSecurityContext());

								} else {

									logger.warn("Error: unknown permission \"{}\". Parameters: {}", new Object[] { trimmedPart, getParametersAsString(sources) });
									return "Error: unknown permission " + trimmedPart;
								}
							}
						}

						return "";

					} else {

						logger.warn("Error: third argument is not a string. Parameters: {}", getParametersAsString(sources));
						return "Error: third argument is not a string.";
					}

				} else {

					logger.warn("Error: second argument is not a node. Parameters: {}", getParametersAsString(sources));
					return "Error: second argument is not a node.";
				}

			} else {

				logger.warn("Error: first argument is not of type Principal. Parameters: {}", getParametersAsString(sources));
				return "Error: first argument is not of type Principal.";
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
		return (inJavaScriptContext ? ERROR_MESSAGE_GRANT_JS : ERROR_MESSAGE_GRANT);
	}

	@Override
	public String shortDescription() {
		return "Grants the given permissions on the given entity to a user";
	}
}
