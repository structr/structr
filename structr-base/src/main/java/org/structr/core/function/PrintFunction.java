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

import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class PrintFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_PRINT    = "Usage: ${print(objects...)}. Example: ${print(this.name, \"test\")}";
	public static final String ERROR_MESSAGE_PRINT_JS = "Usage: ${{Structr.print(objects...)}}. Example: ${{Structr.print(Structr.get('this').name, \"test\")}}";

	@Override
	public String getName() {
		return "print";
	}

	@Override
	public String getSignature() {
		return "objects...";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			ctx.print(sources, caller);

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_PRINT_JS : ERROR_MESSAGE_PRINT);
	}

	@Override
	public String shortDescription() {
		return "Prints the given strings or objects to the output buffer";
	}
}
