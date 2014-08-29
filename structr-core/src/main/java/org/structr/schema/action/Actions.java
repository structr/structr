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

		Create("onCreation","SecurityContext securityContext, ErrorBuffer errorBuffer", "securityContext, errorBuffer"), 
      Save("onModification","SecurityContext securityContext, ErrorBuffer errorBuffer", "securityContext, errorBuffer"), 
      Delete("onDeletion","SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties", "securityContext, errorBuffer, properties"), 
      Custom("", "", "");

		Type(final String method, final String signature, final String parameters) {
			this.method = method;
			this.signature = signature;
			this.parameters = parameters;
		}

		private String method = null;
		private String signature = null;
		private String parameters = null;

		public String getMethod() {
			return method;
		}

		public String getSignature() {
			return signature;
		}

		public String getParameters() {
			return parameters;
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
