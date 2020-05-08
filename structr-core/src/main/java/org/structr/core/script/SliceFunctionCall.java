/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.core.script;

import java.util.Arrays;
import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SliceFunctionCall implements IdFunctionCall {

	private static final Logger logger = LoggerFactory.getLogger(BatchFunctionCall.class);

	@Override
	public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {

		if (args.length < 3) {

			logger.warn("Invalid number of arguments for Structr.slice(): expects 3, got " + args.length + ". Usage: Structr.slice(function, start, end);");
			return null;
		}

		/**
		 * This function produces wrong results and errors if the bounds
		 * don't match the data, will be removed soon.
		 */



		final Script mainCall = toScript("function", args, 0);
		final int start       = toInt(args, 1);
		final int end         = toInt(args, 2);

		final Object result = mainCall.exec(cx, scope);

		if ( result instanceof NativeArray) {

			final NativeArray arr = (NativeArray)result;
			final Object[] data   = arr.toArray();

			return Arrays.copyOfRange(data, start, end);

		} else if (result instanceof List) {

			final List list = (List)result;
			return list.subList(start, end);

		} else if (result != null) {

			throw new RuntimeException("Unable to use splice() function on object of type " + result.getClass());

		} else {

			throw new RuntimeException("Unable to use splice() function on null object");
		}
	}

	// ----- private methods -----
	private Script toScript(final String name, final Object[] args, final int index) {

		if (index >= args.length) {
			return null;
		}

		final Object value = args[index];

		if (value instanceof Script) {

			return (Script)value;
		}

		if (value == null) {

			logger.warn("Invalid argument {} for Structr.slice(): expected script, got null.");

		} else {

			logger.warn("Invalid argument {} for Structr.slice(): expected script, got {}", name, value.getClass());
		}

		return null;
	}

	private int toInt(final Object[] args, final int index) {

		if (index >= args.length) {
			return -1;
		}

		final Object value = args[index];

		if (value != null) {

			if (value instanceof Integer) {

				return ((Integer)value);

			} else if (value instanceof Number) {

				return ((Number) value).intValue();

			}
		}

		return -1;
	}
}
