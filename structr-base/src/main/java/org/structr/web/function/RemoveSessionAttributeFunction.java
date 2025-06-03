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
package org.structr.web.function;


import jakarta.servlet.http.HttpSession;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.script.polyglot.wrappers.HttpSessionWrapper;
import org.structr.schema.action.ActionContext;



public class RemoveSessionAttributeFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_REMOVE_SESSION_ATTRIBUTE    = "Usage: ${remove_session_attribute(key)}. Example: ${remove_session_attribute(\"do_no_track\")}";
	public static final String ERROR_MESSAGE_REMOVE_SESSION_ATTRIBUTE_JS = "Usage: ${{Structr.remove_session_attribute(key)}}. Example: ${{Structr.remove_session_attribute(\"do_not_track\")}}";

	@Override
	public String getName() {
		return "remove_session_attribute";
	}

	@Override
	public String getSignature() {
		return "key";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final HttpSession session = ctx.getSecurityContext().getSession();
			final HttpSessionWrapper sessionWrapper = new HttpSessionWrapper(ctx, session);

			if (session != null) {

				sessionWrapper.removeMember(sources[0].toString());
			} else {
				logger.warn("{}: No session available to remvoe session attribute! (this can happen in onStructrLogin/onStructrLogout)", getReplacement());
			}

			return "";

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_REMOVE_SESSION_ATTRIBUTE_JS : ERROR_MESSAGE_REMOVE_SESSION_ATTRIBUTE);
	}

	@Override
	public String shortDescription() {
		return "Remove key/value pair from the user session.";
	}
}