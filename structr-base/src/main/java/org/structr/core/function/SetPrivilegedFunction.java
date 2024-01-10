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
package org.structr.core.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class SetPrivilegedFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_SET_PRIVILEGED    = "Usage: ${set_privileged(entity, propertyKey, value)}. Example: ${set_privileged(this, \"email\", lower(this.email))}";
	public static final String ERROR_MESSAGE_SET_PRIVILEGED_JS = "Usage: ${{Structr.setPrvileged(entity, propertyKey, value)}}. Example: ${{Structr.setPrivileged(Structr.this, \"email\", lower(Structr.this.email))}}";

	@Override
	public String getName() {
		return "set_privileged";
	}

	@Override
	public String getSignature() {
		return "entity, parameterMap";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		synchronized (ctx) {

			final SecurityContext previousSecurityContext = ctx.getSecurityContext();
			ctx.setSecurityContext(SecurityContext.getSuperUserInstance());

			try {

				final SetFunction set = new SetFunction();
				set.apply(ctx, caller, sources);

			} finally {

				ctx.setSecurityContext(previousSecurityContext);
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
		return "Sets the given key/value pair(s) on the given entity with super-user privileges";
	}

}
