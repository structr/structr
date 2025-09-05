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
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class OneFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_ONE = "Usage: ${one(number, oneValue, otherValue)}. Example: ${one(this.children.size, 'child', 'children')}";

	@Override
	public String getName() {
		return "one";
	}

	@Override
	public String getSignature() {
		return "number, oneValue, otherValue";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		assertArrayHasLengthAndAllElementsNotNull(sources, 3);

		final Integer value = this.parseInt(sources[0], 0);
		if (value == 1) {

			return sources[1];
		}

		return sources[2];
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ONE;
	}

	@Override
	public String shortDescription() {
		return "Checks if a number is equal to 1, returns the oneValue if yes, the otherValue if no.";
	}

}
