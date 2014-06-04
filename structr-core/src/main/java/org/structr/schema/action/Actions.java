package org.structr.schema.action;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class Actions {

	public enum Type {

		Create("onCreation"), Save("onModification"), Delete("onDeletion"), Custom("");

		Type(final String method) {
			this.method = method;
		}

		private String method = null;

		public String getMethod() {
			return method;
		}
	}

	// ----- public static methods -----
	public static boolean execute(final SecurityContext securityContext, final GraphObject entity, final String source) throws FrameworkException {

		final ActionContext context = new ActionContext();

		// ignore result for now
		entity.replaceVariables(securityContext, context, source);

		// check for errors raised by scripting
		if (context.hasError()) {
			throw new FrameworkException(422, context.getErrorBuffer());
		}

		// false means SUCCESS!
		return false;
	}
}
