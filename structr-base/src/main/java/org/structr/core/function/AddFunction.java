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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class AddFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_ADD = "Usage: ${add(values...)}. Example: ${add(1, 2, 3, this.children.size)}";

	@Override
	public String getName() {
		return "add";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("values...");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		double result = 0.0;

		if (sources != null) {

			for (Object i : sources) {

				if (i != null) {

					try {

						result += Double.parseDouble(i.toString());

					} catch (Throwable t) {

						logException(caller, t, sources);

						return t.getMessage();
					}
				}
			}
		}

		return result;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ADD;
	}

	@Override
	public String getShortDescription() {
		return "Returns the sum of the given arguments";
	}
}
