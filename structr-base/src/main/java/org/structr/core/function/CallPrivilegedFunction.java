/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.schema.action.ActionContext;


public class CallPrivilegedFunction extends CallFunction {

	public static final String ERROR_MESSAGE_CALL_PRIVILEGED    = "Usage: ${call_privileged(key [, key, value]}. Example ${call_privileged('myEvent', 'key1', 'value1', 'key2', 'value2')}";
	public static final String ERROR_MESSAGE_CALL_PRIVILEGED_JS = "Usage: ${{Structr.call_privileged(key [, parameterMap]}}. Example ${{Structr.call_privileged('myEvent', {key1: 'value1', key2: 'value2'})}}";

	@Override
	public String getName() {
		return "call_privileged";
	}

	@Override
	public String getSignature() {
		return "functionName [, parameterMap ]";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CALL_PRIVILEGED_JS : ERROR_MESSAGE_CALL_PRIVILEGED);
	}

	@Override
	public String shortDescription() {
		return "Calls the given global schema method with a superuser context";
	}

	@Override
	public SecurityContext getSecurityContext(final ActionContext ctx) {

		final SecurityContext superuserSecurityContext = SecurityContext.getSuperUserInstance();
		superuserSecurityContext.setContextStore(ctx.getContextStore());

		return superuserSecurityContext;
	}
}
