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
public class GrantFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GRANT    = "Usage: ${grant(principal, node, permissions)}. Example: ${grant(me, this, 'read, write, delete'))}";
	public static final String ERROR_MESSAGE_GRANT_JS = "Usage: ${{Structr.grant(principal, node, permissions)}}. Example: ${{Structr.grant(Structr.get('me'), Structr.this, 'read, write, delete'))}}";

	@Override
	public String getName() {
		return "grant()";
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

									node.grant(permission, principal);

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
		return (inJavaScriptContext ? ERROR_MESSAGE_GRANT_JS : ERROR_MESSAGE_GRANT);
	}

	@Override
	public String shortDescription() {
		return "Grants the given permissions on the given entity to a user";
	}

}
