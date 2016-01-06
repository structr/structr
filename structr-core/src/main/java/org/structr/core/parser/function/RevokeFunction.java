package org.structr.core.parser.function;

import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class RevokeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_REVOKE    = "Usage: ${revoke(principal, node, permissions)}. Example: ${revoke(me, this, 'write, delete'))}";
	public static final String ERROR_MESSAGE_REVOKE_JS = "Usage: ${{Structr.revoke(principal, node, permissions)}}. Example: ${{Structr.revoke(Structr.('me'), Structr.this, 'write, delete'))}}";

	@Override
	public String getName() {
		return "revoke()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 3)) {

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

									node.revoke(permission, principal);

								} else {

									return "Error: unknown permission " + trimmedPart;
								}
							}
						}

						return "";

					} else {

						return "Error: third argument is not a string.";
					}

				} else {

					return "Error: second argument is not a node.";
				}

			} else {

				return "Error: first argument is not of type Principal.";
			}

		} else {

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_REVOKE_JS : ERROR_MESSAGE_REVOKE);
	}

	@Override
	public String shortDescription() {
		return "Revokes the given permissions on the given entity from a user";
	}

}
