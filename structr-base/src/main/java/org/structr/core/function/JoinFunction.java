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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class JoinFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_JOIN = "Usage: ${join(collection, separator)}. Example: ${join(this.names, \",\")}";

	@Override
	public String getName() {
		return "join";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("list, separator");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof Iterable) {

				return StringUtils.join((Iterable)sources[0], sources[1].toString());

			} else if (sources[0].getClass().isArray()) {

				return StringUtils.join((Object[])sources[0], sources[1].toString());
			}

			return "";

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_JOIN;
	}

	@Override
	public String getShortDescription() {
		return "Joins all its parameters to a single string using the given separator";
	}
}
