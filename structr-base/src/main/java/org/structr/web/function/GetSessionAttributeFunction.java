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
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.wrappers.HttpSessionWrapper;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;



public class GetSessionAttributeFunction extends UiAdvancedFunction {

	private int retryCount = 0;

	@Override
	public String getName() {
		return "get_session_attribute";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("key");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final HttpSession session = ctx.getSecurityContext().getSession();
			final HttpSessionWrapper sessionWrapper = new HttpSessionWrapper(ctx, session);

			if (session != null) {
				return PolyglotWrapper.unwrap(ctx, sessionWrapper.getMember(sources[0].toString()));
			} else {
				logger.warn("{}: No session available to get session attribute from! (this can happen in onStructrLogin/onStructrLogout)", getReplacement());
			}

			return null;

		} catch (IllegalStateException ex) {

			// retry
			if (retryCount < 3) {
				retryCount++;
				return apply(ctx, caller, sources);
			}
			throw ex;
		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${get_session_attribute(key)}. Example: ${get_session_attribute('do_no_track')}"),
			Usage.javaScript("Usage: ${{ $.getSessionAttribute(key)}}. Example: ${{ $.getSessionAttribute('do_not_track')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Retrieve a value for the given key from the user session.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}