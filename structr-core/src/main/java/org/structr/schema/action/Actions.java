package org.structr.schema.action;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class Actions {

	public enum Type {

		Create("onCreation"), Save("onModification"), Delete("onDeletion");

		Type(final String method) {
			this.method = method;
		}

		private String method = null;

		public String getMethod() {
			return method;
		}
	}

	// ----- public static methods -----
	public static boolean execute(final SecurityContext securityContext, final AbstractNode entity, final String source) throws FrameworkException {

		// ignore result for now
		entity.replaceVariables(securityContext, new ActionContext(), source);

		// false means SUCCESS!
		return false;
	}
}
