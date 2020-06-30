/*
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
package org.structr.core.script.polyglot.function;

import com.oracle.truffle.api.object.DynamicObject;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class BatchFunction implements ProxyExecutable {
	private static final Logger logger = LoggerFactory.getLogger(BatchFunction.class.getName());
	private final ActionContext actionContext;

	public BatchFunction(final ActionContext actionContext) {

		this.actionContext = actionContext;
	}

	@Override
	public Object execute(Value... arguments) {

		boolean runInBackground = arguments != null && arguments.length >= 3 && arguments[2] != null && PolyglotWrapper.unwrap(actionContext, arguments[2]) instanceof Boolean ? (Boolean) PolyglotWrapper.unwrap(actionContext, arguments[2]) : false;

		final Thread workerThread = new Thread(() -> {

			if (arguments != null) {

				// Execute main batch function
				if (arguments[0].canExecute()) {

					Object result = null;

					// Execute batch function until it returns anything but true
					do {
						boolean hasError = false;

						try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

							result = PolyglotWrapper.unwrap(actionContext, arguments[0].execute());
							tx.success();
						} catch (FrameworkException ex) {

							hasError = true;
							// Log if no error handler is given
							if (arguments.length < 2 || !arguments[1].canExecute()) {

								Function.logException(logger, ex, "Error in batch function: {}", new Object[]{ex.getMessage()});
							}
						}

						if (actionContext.hasError() || hasError) {

							if (arguments.length >= 2 && arguments[1].canExecute()) {

								// Execute error handler
								try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

									result = PolyglotWrapper.unwrap(actionContext, arguments[1].execute());
									tx.success();
								} catch (FrameworkException ex) {

									Function.logException(logger, ex, "Error in batch error handler: {}", new Object[] { ex.getMessage() });
								}
							}
						}

					} while (result.equals(true));
				}
			}
		});

		workerThread.start();

		if (!runInBackground) {

			try { workerThread.join(); } catch (Throwable t) { t.printStackTrace(); }
		}

		return null;
	}
}
