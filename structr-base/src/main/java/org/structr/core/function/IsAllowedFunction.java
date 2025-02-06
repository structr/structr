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

import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;

public class IsAllowedFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_IS_ALLOWED    = "Usage: ${is_allowed(principal, node, permissions)}. Example: ${is_allowed(me, this, 'write, delete'))}";
	public static final String ERROR_MESSAGE_IS_ALLOWED_JS = "Usage: ${{Structr.is_allowed(principal, node, permissions)}}. Example: ${{Structr.is_allowed(Structr.('me'), Structr.this, 'write, delete'))}}";

	@Override
	public String getName() {
		return "is_allowed";
	}

	@Override
	public String getSignature() {
		return "user, node, permissions";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			if (!(sources[0] instanceof NodeInterface n && n.is("Principal"))) {

				logParameterError(caller, sources, "Expected node of type Principal as first argument!", ctx.isJavaScriptContext());

			} else if (sources[0] instanceof SuperUser) {

				logParameterError(caller, sources, "Expected node of type Principal as first argument - unable to check rights for the SuperUser!", ctx.isJavaScriptContext());

			} else if (!(sources[1] instanceof NodeInterface)) {

				logParameterError(caller, sources, "Expected node as second argument!", ctx.isJavaScriptContext());

			} else if (!(sources[2] instanceof String)) {

				logParameterError(caller, sources, "Expected string as third argument!", ctx.isJavaScriptContext());

			} else {

				final Principal principal = ((NodeInterface)sources[0]).as(Principal.class);
				final NodeInterface node  = (NodeInterface) sources[1];
				final String[] parts      = ((String) sources[2]).split("[,]+");
				boolean allowed           = true;

				for (final String part : parts) {

					final String trimmedPart = part.trim();
					if (trimmedPart.length() > 0) {

						final Permission permission = Permissions.valueOf(trimmedPart);
						if (permission != null) {

							allowed &= node.isGranted(permission, SecurityContext.getInstance(principal, AccessMode.Backend));

						} else {

							logParameterError(caller, sources, "Unknown permission \"" + trimmedPart + "\"!", ctx.isJavaScriptContext());
							return false;
						}
					}
				}

				return allowed;
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
		}

		return false;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_IS_ALLOWED_JS : ERROR_MESSAGE_IS_ALLOWED);
	}

	@Override
	public String shortDescription() {
		return "Returns whether the principal has all of the permission(s) on the given node.";
	}
}
