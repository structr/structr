/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
package org.structr.schema.action;

import java.util.Collections;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.Scripting;

/**
 *
 * @author Christian Morgner
 */
public class Actions {

	public enum Type {

		Create("onCreation","SecurityContext securityContext, ErrorBuffer errorBuffer", "securityContext, errorBuffer"),
		Save("onModification", "SecurityContext securityContext, ErrorBuffer errorBuffer", "securityContext, errorBuffer"),
		Delete("onDeletion", "SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties", "securityContext, errorBuffer, properties"),
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
	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source) throws FrameworkException {
		return execute(securityContext, entity, source, Collections.EMPTY_MAP);
	}

	public static Object execute(final SecurityContext securityContext, final GraphObject entity, final String source, final Map<String, Object> parameters) throws FrameworkException {

		final ActionContext context = new ActionContext(parameters);
		final Object result         = Scripting.evaluate(securityContext, context, entity, source);

		// check for errors raised by scripting
		if (context.hasError()) {
			throw new FrameworkException(422, context.getErrorBuffer());
		}

		return result;
	}
}
