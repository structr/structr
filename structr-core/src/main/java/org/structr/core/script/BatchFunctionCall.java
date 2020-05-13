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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class BatchFunctionCall implements IdFunctionCall {

	private static final Logger logger = LoggerFactory.getLogger(BatchFunctionCall.class);

	private StructrScriptable scriptable = null;
	private ActionContext actionContext  = null;

	public BatchFunctionCall(final ActionContext actionContext, final StructrScriptable scriptable) {
		this.actionContext = actionContext;
		this.scriptable    = scriptable;
	}

	@Override
	public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {

		if (args.length < 1) {

			logger.warn("Invalid number of arguments for Structr.batch(): expected at least 1, got " + args.length + ". Usage: Structr.batch(function, [errorHandler, runInBackground]);");
			return null;
		}

		final Script mainCall     = toScript("function",     args, 0, false);
		final Script errorHandler = toScript("errorHandler", args, 1, true);
		final boolean background  = toBoolean(args, 2);
		final Thread workerThread = new Thread(() -> {

			Scripting.setupJavascriptContext();

			try {

				// register Structr scriptable
				scope.put("Structr", scope, scriptable);

				boolean runAgain = true;

				while (runAgain) {

					boolean hasError = false;

					try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

						final Object result = Scripting.evaluateJavascript(actionContext, null, new Snippet(mainCall));

						runAgain = (Boolean.TRUE.equals(result));

						tx.success();

					} catch (FrameworkException fex) {

						hasError = true;
						runAgain = false;

						if (errorHandler == null) {

							Function.logException(logger, fex, "Error in batch function: {}", new Object[] { fex.getMessage() });
						}
					}

					if (actionContext.hasError() || hasError) {

						try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

							final Object result = Scripting.evaluateJavascript(actionContext, null, new Snippet(errorHandler));

							runAgain = (Boolean.TRUE.equals(result));

							tx.success();

						} catch (FrameworkException fex) {

							Function.logException(logger, fex, "Error in batch error handler: {}", new Object[] { fex.getMessage() });
						}
					}
				}

			} finally {

				Scripting.destroyJavascriptContext();
			}
		});

		workerThread.start();

		// wait for batch processing to finish?
		if (!background) {

			try { workerThread.join(); } catch (Throwable t) { t.printStackTrace(); }
		}

		return null;
	}


	// ----- private methods -----
	private Script toScript(final String name, final Object[] args, final int index, final boolean optional) {

		if (index >= args.length) {
			return null;
		}

		final Object value = args[index];

		if (value instanceof Script) {

			return (Script)value;
		}

		if (!optional) {

			if (value == null) {

				logger.warn("Invalid argument {} for Structr.batch(): expected script, got null.");

			} else {

				logger.warn("Invalid argument {} for Structr.batch(): expected script, got {}", name, value.getClass());
			}
		}

		return null;
	}

	private boolean toBoolean(final Object[] args, final int index) {

		if (index >= args.length) {
			return false;
		}

		final Object value = args[index];

		if (value != null && value instanceof Boolean) {

			return ((Boolean)value);
		}

		return false;
	}
}
