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

import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.entity.SuperUser;
import org.structr.schema.action.ActionContext;

import java.util.HashSet;
import java.util.Set;

public class GrantFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_GRANT    = "Usage: ${grant(principal, node, permissions)}. Example: ${grant(me, this, 'read, write, delete'))}";
	public static final String ERROR_MESSAGE_GRANT_JS = "Usage: ${{Structr.grant(principal, node, permissions)}}. Example: ${{Structr.grant(Structr.get('me'), Structr.this, 'read, write, delete'))}}";

	@Override
	public String getName() {
		return "grant";
	}

	@Override
	public String getSignature() {
		return "user, node, permissions";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			if (!(sources[0] instanceof PrincipalInterface)) {

				logParameterError(caller, sources, "Expected node of type Principal as first argument!", ctx.isJavaScriptContext());

			} else if (sources[0] instanceof SuperUser) {

				logParameterError(caller, sources, "Expected node of type Principal as first argument - unable to grant rights for the SuperUser!", ctx.isJavaScriptContext());

			} else if (!(sources[1] instanceof AbstractNode)) {

				logParameterError(caller, sources, "Expected node as second argument!", ctx.isJavaScriptContext());

			} else if (!(sources[2] instanceof String)) {

				logParameterError(caller, sources, "Expected string as third argument!", ctx.isJavaScriptContext());

			} else {

				final PrincipalInterface principal         = (PrincipalInterface)sources[0];
				final AbstractNode node           = (AbstractNode)sources[1];
				final Set<Permission> permissions = new HashSet();
				final String[] parts              = ((String)sources[2]).split("[,]+");

				for (final String part : parts) {

					final String trimmedPart = part.trim();
					if (trimmedPart.length() > 0) {

						final Permission permission = Permissions.valueOf(trimmedPart);
						if (permission != null) {

							permissions.add(permission);

						} else {

							logParameterError(caller, sources, "Unknown permission \"" + trimmedPart + "\"!", ctx.isJavaScriptContext());
							return "";
						}
					}
				}

				if (permissions.size() > 0) {
					node.grant(permissions, principal, ctx.getSecurityContext());
				}
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
		return (inJavaScriptContext ? ERROR_MESSAGE_GRANT_JS : ERROR_MESSAGE_GRANT);
	}

	@Override
	public String shortDescription() {
		return "Grants the given permissions on the given entity to a user";
	}
}
