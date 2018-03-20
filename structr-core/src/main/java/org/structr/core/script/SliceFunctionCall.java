/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.core.function.QueryFunction;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;


public class SliceFunctionCall implements IdFunctionCall {

	private static final Logger logger = LoggerFactory.getLogger(BatchFunctionCall.class);

	private SlicedScriptable scriptable = null;

	public SliceFunctionCall(final ActionContext actionContext, final GraphObject entity, final Context scriptingContext) {
		this.scriptable = new SlicedScriptable(actionContext, entity, scriptingContext);
	}

	@Override
	public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {

		if (args.length < 3) {

			logger.warn("Invalid number of arguments for Structr.slice(): expects 3, got " + args.length + ". Usage: Structr.slice(function, start, end);");
			return null;
		}

		final Script mainCall = toScript("function", args, 0);
		scriptable.setRangeStart(toInt(args, 1));
		scriptable.setRangeEnd(toInt(args, 2));

		Object result = null;

		Scripting.setupJavascriptContext();

		try {

			// register Structr scriptable
			scope.put("Structr", scope, scriptable);

			result = mainCall.exec(cx, scope);

		} finally {

			Scripting.destroyJavascriptContext();
		}

		return result;
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

			logger.warn("Invalid argument {} for Structr.batch(): expected script, got null.");

		} else {

			logger.warn("Invalid argument {} for Structr.batch(): expected script, got {}", name, value.getClass());
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

	// ----- private class -----
	private class SlicedScriptable extends StructrScriptable {

		private int start                    = -1;
		private int end                      = -1;

		public SlicedScriptable(ActionContext actionContext, GraphObject entity, Context scriptingContext) {
			super(actionContext, entity, scriptingContext);
		}

		@Override
		public Object get(final String name, Scriptable start) {

			// execute builtin function?
			final Function<Object, Object> function = Functions.get(CaseHelper.toUnderscore(name, false));

			if (function != null && function instanceof QueryFunction) {

				((QueryFunction)function).setRangeStart(this.start);
				((QueryFunction)function).setRangeEnd(this.end);

				return new IdFunctionObject(new FunctionWrapper(function), null, 0, 0);
			}

			return super.get(name, start);
		}

		public void setRangeStart (final int start) {
			this.start = start;
		}

		public void setRangeEnd (final int end) {
			this.end = end;
		}
	}
}
