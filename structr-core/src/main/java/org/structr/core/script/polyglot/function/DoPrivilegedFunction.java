/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.core.script.polyglot.function;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.structr.common.SecurityContext;
import org.structr.schema.action.ActionContext;

public class DoPrivilegedFunction implements ProxyExecutable {

	private final ActionContext actionContext;

	public DoPrivilegedFunction(final ActionContext actionContext) {

		this.actionContext = actionContext;
	}

	@Override
	public Object execute(Value... arguments) {
		Object result = null;

		if (arguments != null && arguments.length == 1) {

			if (arguments[0].canExecute()) {

				// Backup existing security context
				final SecurityContext securityContext = actionContext.getSecurityContext();

				// Copy context store from outer context
				final SecurityContext superUserSecurityContext = SecurityContext.getSuperUserInstance(securityContext.getRequest());
				superUserSecurityContext.setContextStore(securityContext.getContextStore());

				try {

					// Replace security context with super user context
					actionContext.setSecurityContext(superUserSecurityContext);

					result = arguments[0].execute();
				} finally {

					// Overwrite context store with possibly changed context store
					securityContext.setContextStore(superUserSecurityContext.getContextStore());

					// Restore saved security context
					actionContext.setSecurityContext(securityContext);
				}

			}
		}

		return result;
	}
}
