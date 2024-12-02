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
package org.structr.web.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class RequestStorePutFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_REQUEST_STORE_PUT    = "Usage: ${request_store_put(key,value)}. Example: ${request_store_put(\"do_no_track\", true)}";
	public static final String ERROR_MESSAGE_REQUEST_STORE_PUT_JS = "Usage: ${{ $.request_store_put(key,value); }}. Example: ${{ $.request_store_put(\"do_not_track\", true); }}";


	@Override
	public String getName() {
		return "request_store_put";
	}

	@Override
	public String getSignature() {
		return "key, value";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {


			if (sources.length != 2) {
				throw ArgumentCountException.notEqual(sources.length, 2);
			}

			if (sources[0] == null) {
				throw new ArgumentNullException();
			}

			return ctx.getRequestStore().put(sources[0].toString(), sources[1]);
			
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
		return (inJavaScriptContext ? ERROR_MESSAGE_REQUEST_STORE_PUT_JS : ERROR_MESSAGE_REQUEST_STORE_PUT);
	}

	@Override
	public String shortDescription() {
		return "Stores a value in the request level store.";
	}
}
