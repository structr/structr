/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.structr.autocomplete.AbstractHint;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class RetrieveFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_RETRIEVE    = "Usage: ${retrieve(key)}. Example: ${retrieve('tmpUser')}";
	public static final String ERROR_MESSAGE_RETRIEVE_JS = "Usage: ${{Structr.retrieve(key)}}. Example: ${{Structr.retrieve('tmpUser')}}";

	@Override
	public String getName() {
		return "retrieve";
	}

	@Override
	public String getSignature() {
		return "key";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				return ctx.retrieve(sources[0].toString());
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_RETRIEVE_JS : ERROR_MESSAGE_RETRIEVE);
	}

	@Override
	public String shortDescription() {
		return "Returns the value associated with the given key from the temporary store";
	}

	@Override
	public List<AbstractHint> getContextHints(final String lastToken) {

		// this might be the place where information about the execution context
		// of a function etc. can be used, but not yet.
		return null;
	}
}
