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
package org.structr.core.script.polyglot.function;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.structr.autocomplete.BuiltinFunctionHint;
import org.structr.common.SecurityContext;
import org.structr.schema.action.ActionContext;

public class DoPrivilegedFunction extends BuiltinFunctionHint implements ProxyExecutable {

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


	@Override
	public String getName() {
		return "doPrivileged";
	}

	@Override
	public String shortDescription() {
		return """
**JavaScript-only**

Runs the given function in a privileged (superuser) context. This can be useful in scenarios where no security checks should run (e.g. bulk import, bulk deletion).

**Important**: Any node resource, which was loaded outside of the function scope, must be looked up again inside the function scope to prevent access problems.

Example:
```
${{
	let userToDelete = $.find('User', { name: 'user_to_delete' })[0];

	$.doPrivileged(() => {

		// look up user again to set correct access rights
		let user = $.find('User', userToDelete.id);

		// delete all projects owned by user
		$.delete($.find('Project', { projectOwner: user }));

		// delete user
		$.delete(user);
	});
}}
```
""";
	}

	@Override
	public String getSignature() {
		return "function";
	}
}
