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
package org.structr.core.script.polyglot.function;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.autocomplete.BuiltinFunctionHint;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.core.script.polyglot.context.ContextHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.Arrays;

public class DoInNewTransactionFunction extends BuiltinFunctionHint implements ProxyExecutable {

	private static final Logger logger = LoggerFactory.getLogger(DoInNewTransactionFunction.class.getName());
	private final ActionContext actionContext;
	private final GraphObject entity;

	public DoInNewTransactionFunction(final ActionContext actionContext, final GraphObject entity) {

		this.actionContext = actionContext;
		this.entity = entity;
	}

	@Override
	public Object execute(final Value... arguments) {

		if (arguments != null && arguments.length > 0) {

			Object[] unwrappedArgs = Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray();
			Context context = null;

			try {

				context = ContextFactory.getContext("js", actionContext, entity);

				context.leave();

				final Thread workerThread = new Thread(() -> {

					// Execute main batch function
					Object result = null;
					Throwable exception = null;

					if (unwrappedArgs[0] instanceof PolyglotWrapper.FunctionWrapper) {

						Context innerContext = null;
						try {
							innerContext = ContextFactory.getContext("js", actionContext, entity);
						} catch (FrameworkException ex) {
							logger.error("Could not retrieve context in DoInNewTransactionFunction worker.", ex);
							return;
						}

						try {

							innerContext.enter();

							// Execute batch function until it returns anything but true
							do {
								ContextHelper.incrementReferenceCount(innerContext);
								boolean hasError = false;

								try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

									result = PolyglotWrapper.unwrap(actionContext, ((PolyglotWrapper.FunctionWrapper) unwrappedArgs[0]).execute());
									tx.success();

								} catch (Throwable ex) {

									hasError = true;
									exception = ex;

									// Log if no error handler is given
									if (unwrappedArgs.length < 2 || !(unwrappedArgs[1] instanceof PolyglotWrapper.FunctionWrapper)) {

										Function.logException(logger, ex, "Error in doInNewTransaction(): {}", new Object[]{ ex.toString() });
									}
								}

								if (actionContext.hasError() || hasError) {

									if (unwrappedArgs.length >= 2 && unwrappedArgs[1] instanceof PolyglotWrapper.FunctionWrapper) {

										// Execute error handler
										try (final Tx tx = StructrApp.getInstance(actionContext.getSecurityContext()).tx()) {

											result = PolyglotWrapper.unwrap(actionContext, ((PolyglotWrapper.FunctionWrapper) unwrappedArgs[1]).execute(Value.asValue(exception)));
											tx.success();

											// Error has been handled, clear error buffer.
											actionContext.getErrorBuffer().setStatus(0);
											actionContext.getErrorBuffer().getErrorTokens().clear();
										} catch (Throwable ex) {

											Function.logException(logger, ex, "Error in transaction error handler: {}", new Object[]{ex.getMessage()});
										}

									}
								}

								ContextHelper.decrementReferenceCount(innerContext);
							} while (result != null && result.equals(true));

						} finally {

							innerContext.leave();

							if (ContextHelper.getReferenceCount(innerContext) <= 0) {

								innerContext.close();
								actionContext.removeScriptingContextByValue(innerContext);
							}
						}
					}


				});

				workerThread.start();

				try {

					workerThread.join();

				} catch (Throwable t) {}

			} catch (FrameworkException ex) {

				logger.error("Exception in DoInNewTransactionFunction.", ex);
			} finally {

				if (context != null) {

					context.enter();
				}
			}
		}

		return null;
	}

	@Override
	public String getName() {
		return "doInNewTransaction";
	}

	@Override
	public String shortDescription() {
		return """
**JavaScript-only**

Removes the given function form the current transaction context and allows batching of a given expression, i.e. if the expression contains a long-running function (for example the deletion of all nodes of a given type).
Useful in situations where large numbers of nodes are created, modified or deleted.

The paging/batching must be done manually.

Example:
```
${{
    /* Iterate over all users in packets of 10 and do stuff */
    let pageSize = 10;
    let pageNo    = 1;

    $.doInNewTransaction(() => {
        let nodes = $.find('User', $.predicate.page(pageNo, pageSize));

        // compute-heavy stuff
        // ...

        pageNo++;

        return (nodes.length > 0);

    }, (exception) => {

        $.log('Error occurred in batch function. Stopping.');
        return false;
    });
}}
```
""";
	}

	@Override
	public String getSignature() {
		return "workerFunction [, errorHandlerFunction]";
	}
}
