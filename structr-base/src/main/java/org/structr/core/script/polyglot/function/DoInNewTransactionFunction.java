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
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.core.script.polyglot.context.ContextHelper;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.Arrays;
import java.util.List;

public class DoInNewTransactionFunction extends BuiltinFunctionHint implements ProxyExecutable {

	private static final Logger logger = LoggerFactory.getLogger(DoInNewTransactionFunction.class.getName());
	private final ActionContext actionContext;
	private final GraphObject entity;
	public static final String THREAD_NAME_PREFIX = "NewTransactionHandler_";

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
					boolean isInterrupted = false;

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

									if (ex.getCause() != null && ex.getCause() instanceof InterruptedException) {

										logger.warn("Thread was interrupted - breaking out of doInNewTransaction()");

									} else if (unwrappedArgs.length < 2 || !(unwrappedArgs[1] instanceof PolyglotWrapper.FunctionWrapper)) {

										// Log if no error handler is given
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

								isInterrupted = Thread.currentThread().isInterrupted();

							} while (!isInterrupted && result != null && result.equals(true));

						} finally {

							innerContext.leave();

							if (ContextHelper.getReferenceCount(innerContext) <= 0) {

								innerContext.close();
								actionContext.removeScriptingContextByValue(innerContext);
							}
						}
					}
				}, THREAD_NAME_PREFIX + NodeServiceCommand.getNextUuid());

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
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("function", "lambda function to execute"),
			Parameter.optional("errorHandler", "error handler that receives the error / exception as an argument")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
			${{
				// Iterate over all users in packets of 10 and do stuff
				let pageSize = 10;
				let pageNo	= 1;

				$.doInNewTransaction(function() {

					// in Structr versions lower than 4.0 replace "$.predicate.page" with "$.page"
					let nodes = $.find('User', $.predicate.page(pageNo, pageSize));

					// do compute-heavy stuff here..

					pageNo++;

					return (nodes.length > 0);
					
				}, function() {
					$.log('Error occurred in batch function. Stopping.');
					return false;
				});
			}}
			""", "Regular working example"),

			Example.javaScript("""
			${{
				// create a new group
				let group = $.getOrCreate('Group', 'name', 'ExampleGroup');

				$.doInNewTransaction(() => {
					let page = $.create('Page', 'name', 'ExamplePage');

					// this is where the error occurs - the Group node is not yet committed to the graph and when this context is closed a relationship between the group and the page is created - which can not work because only the page is committed to the graph
					$.grant(group, page, 'read');
				});
			}}
			""", "Example where the inner transaction tries to create a relationship to a node which has not yet been committed and thus the whole code fails"),

			Example.javaScript("""
			${{
				// create a new group
				let groupId;

				$.doInNewTransaction(() => {
					let group = $.getOrCreate('Group', 'name', 'ExampleGroup');

					// save the group id to be able to fetch it later
					groupId = group.id;

					// after this context, the group is committed to the graph and relationships to it can later be created
				});

				$.doInNewTransaction(() => {
					let page = $.create('Page', 'name', 'ExamplePage' });

					/* fetch previously created group */
					let group = $.find('Group', groupId);

					// this works now
					$.grant(group, page, 'read');
				});
			}}
			""", "Example to fix the problems of example (2)"),

			Example.javaScript("""
			${{
				$.doInNewTransaction(() => {
					let myString = 'the following variable is undefined ' + M;
				}, (ex) => {
					// depending on the exception type different methods will be available
					$.log(ex.getClass().getSimpleName());
					$.log(ex.getMessage());
				});
			}
			""", "Example where an error occurs and is handled (v4.0+)")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.JavaScript);
	}

	@Override
	public String getShortDescription() {
		return "Runs the given function in a new transaction context.";
	}

	@Override
	public String getLongDescription() {
		return """
		This function allows you to detach long-running functions from the current transaction context (which is bound to the request), or execute large database operations in batches. Useful in situations where large numbers of nodes are created, modified or deleted.

		This function is only available in JavaScript and takes a worker function as its first parameter and an optional error handler function as its second parameter.
		
		**If the worker function returns `true`, it is run again.** If it returns anything else it is not run again.
		
		If an exception occurs in the worker function, the error handler function (if present) is called. If the error handler returns `true`, the worker function is called again. If the error handler function returns anything else (or an exception occurs) the worker function is not run again.

		When the `errorHandler` function is called, it receives the error / exception that was raised in the worker function. Depending on the error type, different methods are available on that object. Syntax errors will yield a `PolyglotException` (see https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/PolyglotException.html), other error types will yield different exception object.
		""";
	}

	@Override
	public List<Signature> getSignatures() {

		return List.of(
			Signature.of("workerFunction [, errorHandlerFunction]", Language.JavaScript)
		);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.doInNewTransaction(function) }}. Example: ${{ $.doInNewTransaction(() => log($.me))}")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"WARNING: This function is a prime candidate for an endless loop - be careful what your return condition is!",
			"WARNING: You have to be aware of the transaction context. Anything from the outermost transaction is not yet committed to the graph and thus can not be used to connect to in an inner transaction. The outer transaction context is only committed after the method is finished without a rollback. See example (2) for code which will result in an error and example (3) for a solution",

		"See also `schedule()`."
		);
	}
}
