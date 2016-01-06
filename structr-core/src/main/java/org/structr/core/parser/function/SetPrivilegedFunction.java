package org.structr.core.parser.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SetPrivilegedFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SET_PRIVILEGED    = "Usage: ${set_privileged(entity, propertyKey, value)}. Example: ${set_privileged(this, \"email\", lower(this.email))}";
	public static final String ERROR_MESSAGE_SET_PRIVILEGED_JS = "Usage: ${{Structr.setPrvileged(entity, propertyKey, value)}}. Example: ${{Structr.setPrivileged(Structr.this, \"email\", lower(Structr.this.email))}}";

	@Override
	public String getName() {
		return "set_privileged()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		synchronized (entity) {

			final SecurityContext previousSecurityContext = entity.getSecurityContext();
			entity.setSecurityContext(SecurityContext.getSuperUserInstance());

			try {

				final SetFunction set = new SetFunction();
				set.apply(ctx, entity, sources);

			} finally {

				entity.setSecurityContext(previousSecurityContext);
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SET_PRIVILEGED_JS : ERROR_MESSAGE_SET_PRIVILEGED);
	}

	@Override
	public String shortDescription() {
		return "Sets the given key/value pair on the given entity with super-user privileges";
	}

}
