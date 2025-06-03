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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class RequestStoreHasFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_REQUEST_STORE_HAS    = "Usage: ${request_store_has(key)}. Example: ${request_store_has(\"do_no_track\")}";
	public static final String ERROR_MESSAGE_REQUEST_STORE_HAS_JS = "Usage: ${{ $.request_store_has(key); }}. Example: ${{ $.request_store_has(\"do_not_track\"); }}";

	@Override
	public String getName() {
		return "request_store_has";
	}

	@Override
	public String getSignature() {
		return "key";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return ctx.getRequestStore().containsKey(sources[0].toString());
			
		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_REQUEST_STORE_HAS_JS : ERROR_MESSAGE_REQUEST_STORE_HAS);
	}

	@Override
	public String shortDescription() {
		return "Checks if a key is present in the request level store.";
	}
}
