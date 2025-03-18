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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.autocomplete.BuiltinFunctionHint;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;

public class DoAsFunction extends BuiltinFunctionHint implements ProxyExecutable {
    private static final Logger logger = LoggerFactory.getLogger(DoAsFunction.class);
    private final ActionContext actionContext;

    public DoAsFunction(final ActionContext actionContext) {

        this.actionContext = actionContext;
    }

    @Override
    public Object execute(Value... arguments) {

        Object[] parameters = Arrays.stream(arguments).map(a -> PolyglotWrapper.unwrap(actionContext, a)).toArray();

        if (parameters.length == 2) {

            final SecurityContext initialSecurityContext = actionContext.getSecurityContext();

            try {

                final Principal user = (Principal) parameters[0];
                final ProxyExecutable executable = (ProxyExecutable) parameters[1];

                final SecurityContext userContext = SecurityContext.getInstance(user, initialSecurityContext.getRequest(), AccessMode.Frontend);

                userContext.setContextStore(initialSecurityContext.getContextStore());
                actionContext.setSecurityContext(userContext);
                executable.execute();
                initialSecurityContext.setContextStore(userContext.getContextStore());

            } catch (ClassCastException ex) {

                throw new RuntimeException(new FrameworkException(422, "Invalid parameters for do_as function. Expected: Principal, Executable"));
            } finally {

                actionContext.setSecurityContext(initialSecurityContext);
            }

        } else {

            throw new RuntimeException(new FrameworkException(422, "Invalid parameter count for do_as function."));
        }

        return null;
    }

    @Override
    public String getName() {
        return "doAs";
    }

    @Override
    public String shortDescription() {
        return """
**JavaScript-only**

Runs the given function in the context of the given user.

**Important**: Any node resource, which was loaded outside of the function scope, must be looked up again inside the function scope to prevent access problems.

Example:
```
${{
    let user = $.find('User', { name: 'user_to_impersonate' })[0];

    $.doAs(user, () => {

        // code to be run as the given user
    });
}}
```
""";
    }

    @Override
    public String getSignature() {
        return "user, function";
    }
}
