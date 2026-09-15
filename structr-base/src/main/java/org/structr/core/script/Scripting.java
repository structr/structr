/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.DateProperty;
import org.structr.core.script.polyglot.PendingThenables;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.core.script.polyglot.context.ContextHelper;
import org.structr.core.script.polyglot.filesystem.PolyglotFilesystem;
import org.structr.core.script.polyglot.util.JSFunctionTranspiler;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.parser.DatePropertyGenerator;
import org.structr.web.traits.wrappers.ScratchpadTraitWrapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Scripting {

	private static final Pattern ScriptEngineExpression             = Pattern.compile("^\\$\\{(\\w+)\\{(.*)\\}\\}$", Pattern.DOTALL);
	private static final Logger logger                              = LoggerFactory.getLogger(Scripting.class.getName());
	private static final String PENDING_PROMISE_MESSAGE             = "Attempt to unwrap pending promise";

	/**
	 * Calls the transpiled async wrapper, which {@link JSFunctionTranspiler} deliberately leaves uncalled.
	 *
	 * <p>Being a plain arrow rather than an async function is the point.
	 * {@code js.interop-complete-promises} completes the promise of an <em>async function</em> crossing
	 * to the host, throwing if it cannot, so calling the wrapper directly leaves the host no pending
	 * promise to hold and no opportunity to stop settling early. Called through a function that is not
	 * itself async, the same promise crosses still pending, as a non-embedded snippet's completion value
	 * does. Both dialects then settle through {@code PolyglotWrapper.unwrapThenable}.</p>
	 */
	private static final Source ASYNC_WRAPPER_TRAMPOLINE = Source.newBuilder("js", "((fn) => fn())", "structrAsyncWrapperCall")
		.mimeType("application/javascript+module")
		.buildLiteral();

	public static String replaceVariables(final ActionContext actionContext, final GraphObject entity, final Object rawValue) throws FrameworkException {

		return replaceVariables(actionContext, entity, rawValue, false, "script source");
	}

	public static String replaceVariables(final ActionContext actionContext, final GraphObject entity, final Object rawValue, final String methodName) throws FrameworkException {

		return replaceVariables(actionContext, entity, rawValue, false, methodName);
	}

	public static String replaceVariables(final ActionContext actionContext, final GraphObject entity, final Object rawValue, final boolean returnNullValueForEmptyResult, final String methodName) throws FrameworkException {

		if (rawValue == null) {

			return null;
		}

		// don't parse empty values
		if (StringUtils.isEmpty(rawValue.toString())) {

			return "";
		}

		boolean valueWasNull = true;
		String value;

		if (rawValue instanceof String) {

			value = (String) rawValue;

			// this is a very important check here, the ActionContext can be set to "raw" mode
			if (!actionContext.returnRawValue()) {

				final List<Tuple> replacements = new LinkedList<>();

				for (final String expression : extractScripts(value)) {

					try {

						final ScriptConfig scriptConfig = ScriptConfig.builder()
								.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
								.build();

						final Object extractedValue = evaluate(actionContext, entity, expression, methodName, 0, entity != null ? entity.getUuid() : null, scriptConfig);
						String partValue            = extractedValue != null ? formatToDefaultDateOrString(extractedValue) : "";

						// non-null value?
						valueWasNull &= extractedValue == null;

						if (partValue != null) {

							replacements.add(new Tuple(expression, partValue));

						} else {

							if (!value.equals(expression)) {

								replacements.add(new Tuple(expression, ""));
							}
						}

					} catch (UnlicensedScriptException ex) {

						ex.log(logger);
					}
				}

				// apply replacements
				for (final Tuple tuple : replacements) {

					// only replace a single occurrence at a time!
					value = StringUtils.replaceOnce(value, tuple.key, tuple.value);
				}
			}

		} else if (rawValue instanceof Boolean) {

			value = Boolean.toString((Boolean) rawValue);

		} else {

			value = rawValue.toString();
		}

		if (returnNullValueForEmptyResult && valueWasNull && StringUtils.isBlank(value)) {

			return null;
		}

		return value;
	}

	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String input, final String methodName) throws FrameworkException, UnlicensedScriptException {

		final ScriptConfig scriptConfig = ScriptConfig.builder()
				.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
				.build();

		return evaluate(actionContext, entity, input, methodName, null, scriptConfig);
	}

	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String input, final String methodName, final String codeSource) throws FrameworkException, UnlicensedScriptException {

		final ScriptConfig scriptConfig = ScriptConfig.builder()
				.wrapJsInMain(Settings.WrapJSInMainFunction.getValue(false))
				.build();

		return evaluate(actionContext, entity, input, methodName, 0, codeSource, scriptConfig);
	}

	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String input, final String methodName, final String codeSource, final ScriptConfig scriptConfig) throws FrameworkException, UnlicensedScriptException {

		return evaluate(actionContext, entity, input, methodName, 0, codeSource, scriptConfig);
	}

	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String input, final String methodName, final int startRow, final String codeSource, final ScriptConfig scriptConfig) throws FrameworkException, UnlicensedScriptException {

		final String expression = StringUtils.strip(input);
		if (expression.isEmpty()) {

			return null;
		}

		String source;
		String[] splitSnippet = splitSnippetIntoEngineAndScript(expression);
		final String engine   = splitSnippet[0];

		if (!engine.isEmpty()) {

			source = splitSnippet[1];
			actionContext.setScriptingEngine(ActionContext.ScriptingEngine.fromName(engine));

		} else {

			source = expression.substring(2, expression.length() - 1);
			actionContext.setScriptingEngine(ActionContext.ScriptingEngine.STRUCTR_SCRIPT);
		}

		final boolean isJavascript = "js".equals(engine);
		final boolean isScriptEngine = !isJavascript && StringUtils.isNotBlank(engine);

		// temporarily disable notifications for scripted actions

		boolean enableTransactionNotifications = false;
		final SecurityContext securityContext = actionContext.getSecurityContext();

		if (securityContext != null) {

			enableTransactionNotifications = securityContext.doTransactionNotifications();

			securityContext.setDoTransactionNotifications(false);
		}

		final Snippet snippet = new Snippet(methodName, source, scriptConfig.wrapJsInMain());
		snippet.setCodeSource(codeSource);
		snippet.setStartRow(startRow);

		if (isScriptEngine) {

			return PolyglotWrapper.unwrap(actionContext, evaluateScript(actionContext, entity, engine, snippet, scriptConfig));

		} else if (isJavascript) {

			snippet.setMimeType("application/javascript+module");
			snippet.setEngineName("js");
			final Object result = evaluateScript(actionContext, entity, "js", snippet, scriptConfig);

			if (enableTransactionNotifications && securityContext != null) {

				securityContext.setDoTransactionNotifications(true);
			}

			return PolyglotWrapper.unwrap(actionContext, result);

		} else {

			try {

				Object extractedValue       = Functions.evaluate(actionContext, entity, snippet);
				final String value          = extractedValue != null ? extractedValue.toString() : "";
				final String output         = actionContext.getOutput();

				if (StringUtils.isEmpty(value) && output != null && !output.isEmpty()) {

					extractedValue = output;
				}

				if (enableTransactionNotifications && securityContext != null) {

					securityContext.setDoTransactionNotifications(true);
				}

				/* disabled
				hints.checkForErrorsAndThrowException((message, row, column) -> {
					// report usage errors (missing keys etc.)
					reportError(actionContext.getSecurityContext(), entity, message, row, column, snippet);
				});
				*/

				return PolyglotWrapper.unwrap(actionContext, extractedValue);

			} catch (StructrScriptException t) {

				// This block reports syntax errors in StructrScript expressions
				// StructrScript evaluation should not throw exceptions

				reportError(actionContext.getSecurityContext(), entity, t.getMessage(), t.getRow(), t.getColumn(), snippet);
			}

			return null;
		}
	}

	public static Object evaluateScript(final ActionContext actionContext, final GraphObject entity, final String engineName, final Snippet snippet) throws FrameworkException {

		return evaluateScript(actionContext, entity, engineName, snippet, ScriptConfig.builder().build());
	}

	public static Object evaluateScript(final ActionContext actionContext, final GraphObject entity, final String engineName, final Snippet snippet, final ScriptConfig scriptConfig) throws FrameworkException {

		// Clear output buffer
		actionContext.clear();
		Object result = null;
		final ContextFactory.LockedContext lockedContext = ContextFactory.getContext(engineName, actionContext, entity);

		lockedContext.getLock().lock();

		try {

			final Context context = lockedContext.getContext();

			ContextHelper.incrementReferenceCount(context);
			context.enter();

			try {

				// The host can only settle a promise from outside every evaluation, so host-driven settlement
				// is available to the outermost one alone. An open frame means an evaluation is already in
				// progress on this thread -- a script reaching this method again through $.evaluateScript,
				// a lifecycle method or rendering -- and this one has to complete its wrapper at its own
				// call boundary instead. Read before openFrame(), which is what would make it true.
				final AsyncCompletion asyncCompletion = PendingThenables.hasFrame()
					? AsyncCompletion.AT_BOUNDARY
					: AsyncCompletion.HOST_DRIVEN;

				// Deferred async settlements belong to this evaluation, so the frame is opened inside the lock
				// and the entered context: a drain has to happen on this thread, in this transaction, before
				// the context is left. unwrap() is inside the frame as well, because that is where a
				// thenable completion value is settled.
				PendingThenables.openFrame();

				// The scripting file system is shared by every context, so it learns whose evaluation this
				// is from the thread. It covers unwrap() as well as the evaluation itself, because a
				// host-driven drain settles promises - and can therefore finish an import() - out here.
				final ActionContext previousEvaluation = PolyglotFilesystem.bind(actionContext);

				try {

					final Value value = evaluatePolyglot(actionContext, engineName, context, entity, snippet, asyncCompletion);
					result = PolyglotWrapper.unwrap(actionContext, value);

				} finally {

					PolyglotFilesystem.unbind(previousEvaluation);
					PendingThenables.closeFrame();
				}

			} catch (final PolyglotWrapper.ThenableFailure tfx) {

				// unwrap() has no throws clause, so a promise it could not resolve -- rejected, or still
				// pending with nothing left that could settle it -- arrives wrapped in this marker.
				//
				// Each of the two failures it may carry has to leave by the same route the synchronous path
				// uses, or the reported status would depend on whether the snippet was wrapped. An
				// AssertException is not a FrameworkException, so it leaves unchecked for Actions.execute to
				// convert using the status $.assert was given.
				if (tfx.getReportedFailure() instanceof AssertException aex) {

					throw aex;
				}

				throw tfx.getFrameworkException();

			} finally {

				context.leave();
				ContextHelper.decrementReferenceCount(context);

				if (scriptConfig == null || !scriptConfig.keepContextOpen()) {

					if (ContextHelper.getReferenceCount(context) <= 0) {

						context.close();
						actionContext.putScriptingContext(engineName, null);
					}
				}
			}

		} finally {

			if (lockedContext.getLock().isHeldByCurrentThread()) {

				lockedContext.getLock().unlock();
			}
		}

		// Legacy print() support: Prefer explicitly printed output over actual result
		final String outputBuffer = actionContext.getOutput();
		if (outputBuffer != null && !outputBuffer.isEmpty()) {

			return outputBuffer;
		}

		return result;
	}

	/**
	 * Who settles the promise of an embedded snippet's async wrapper.
	 *
	 * <p>Determined by whether an evaluation is already in progress on this thread, not by the caller.</p>
	 *
	 * <ul>
	 * <li>{@link #HOST_DRIVEN} -- the outermost evaluation, and the only one where the host can settle
	 * anything: it owns the thread and can keep joining deferred calls until the script's promise
	 * resolves, which is what allows a race to stop at its winner.</li>
	 * <li>{@link #AT_BOUNDARY} -- an evaluation nested inside a running one: a script calling a schema
	 * method, or reaching {@link #evaluateScript} again through {@code $.evaluateScript}, a lifecycle
	 * method or rendering. An interop call made while an outer host-to-guest call is still on the stack
	 * does not get a job-queue drain of its own, so a promise the host registers reactions on there is
	 * never settled; the wrapper's promise must be completed at its own call boundary instead. A nested
	 * body that genuinely suspends consequently fails with <em>Attempt to unwrap pending promise</em>,
	 * so {@code await} is not usable in one.</li>
	 * </ul>
	 */
	public enum AsyncCompletion {

		HOST_DRIVEN, AT_BOUNDARY
	}

	/**
	 * Evaluates a snippet nested inside a running evaluation, completing any async wrapper at its own
	 * call boundary. The outermost evaluation goes through {@link #evaluateScript}.
	 */
	public static Value evaluatePolyglot(final ActionContext actionContext, final String engineName, final Context context, final GraphObject entity, final Snippet snippet) throws FrameworkException {

		return evaluatePolyglot(actionContext, engineName, context, entity, snippet, AsyncCompletion.AT_BOUNDARY);
	}

	public static Value evaluatePolyglot(final ActionContext actionContext, final String engineName, final Context context, final GraphObject entity, final Snippet snippet, final AsyncCompletion asyncCompletion) throws FrameworkException {

		// Also bound here, and not only in evaluateScript: a schema method reaches this method directly
		// through AbstractMethod, with an inner ActionContext that evaluateScript never sees.
		final ActionContext previousEvaluation = PolyglotFilesystem.bind(actionContext);

		try {

			Source source = null;
			String code = snippet.getSource();
			final boolean isAsyncWrapped = "js".equals(engineName) && snippet.embed();

			if (isAsyncWrapped) {

				code = JSFunctionTranspiler.transpileSource(snippet);
			}

			source = Source.newBuilder(engineName, code, snippet.getName()).mimeType(snippet.getMimeType()).build();

			try {

				if (source != null) {

					Value result = context.eval(source);

					// An embedded snippet is wrapped in an async arrow that JSFunctionTranspiler deliberately
					// leaves uncalled, so the call happens here. How it is called decides who settles it.
					if (isAsyncWrapped && result != null && result.canExecute()) {

						if (asyncCompletion == AsyncCompletion.HOST_DRIVEN) {

							// through a plain arrow, so the promise crosses still pending and the caller's
							// unwrap can settle it -- and stop as soon as the script itself has answered
							result = context.eval(ASYNC_WRAPPER_TRAMPOLINE).execute(result);

						} else {

							// js.interop-complete-promises drains the promise job queue at this boundary and
							// answers with the resolved value; a rejection arrives as the PolyglotException
							// handled below
							result = executeAsyncWrapper(result, snippet);
						}
					}

					// Legacy print() support: prefer explicitly printed output over the actual result.
					//
					// Only correct once the body has finished, which is why it comes after the call above,
					// and why the outermost evaluation does not apply it here at all. There the value may be
					// a promise that has not settled, with the body run no further than its first
					// suspension, so preferring the buffer would answer with whatever had been printed by
					// then and discard the promise carrying the rest of the script -- including every
					// print() after an await. evaluateScript applies the same preference once the value has
					// settled and the buffer is complete.
					//
					// A nested evaluation has no such gap: its wrapper was completed at the boundary above,
					// so the body has finished and the buffer is final here.
					if (asyncCompletion == AsyncCompletion.AT_BOUNDARY) {

						final String outputBuffer = actionContext.getOutput();
						if (outputBuffer != null && !outputBuffer.isEmpty()) {

							return Value.asValue(outputBuffer);
						}
					}

					return result;

				} else {

					return null;
				}

			} catch (PolyglotException ex) {

				if (ex.isHostException() && ex.asHostException() instanceof RuntimeException) {

					// Only report error, if exception is not an already logged AssertException
					if (ex.isHostException() && !(ex.asHostException() instanceof AlreadyLoggedAssertException)) {

						reportError(actionContext.getSecurityContext(), entity, ex, snippet);
					}

					// If exception is AssertException and has been logged above, rethrow as AlreadyLoggedAssertException
					if (ex.isHostException() && ex.asHostException() instanceof AssertException ae) {

						throw new AlreadyLoggedAssertException(ae);
					}

					// Unwrap FrameworkExceptions wrapped in RuntimeExceptions, if neccesary
					if (ex.asHostException().getCause() instanceof FrameworkException) {

						throw ex.asHostException().getCause();

					} else {

						throw ex.asHostException();
					}

				} else {

					reportError(actionContext.getSecurityContext(), entity, ex, snippet);
					throw new FrameworkException(422, "Server-side scripting error", ex);
				}
			}

		} catch (RuntimeException ex) {

			if (ex.getCause() instanceof FrameworkException) {

				throw (FrameworkException) ex.getCause();

			} else if (ex instanceof AssertException) {

				throw ex;

			} else {

				throw ex;
			}

		} catch (FrameworkException ex) {

			throw ex;

		} catch (Throwable ex) {

			throw new FrameworkException(422, "Server-side scripting error", ex);

		} finally {

			PolyglotFilesystem.unbind(previousEvaluation);
		}
	}

	/**
	 * Calls the async wrapper produced by {@link JSFunctionTranspiler} and answers its resolved value.
	 *
	 * <p>The one failure mode worth naming is a promise that never settles. GraalJS reports that as a
	 * bare {@code TypeError: Attempt to unwrap pending promise}, which says nothing about which script
	 * is at fault, so it is translated here. It is hard to reach on purpose -- there is no event loop
	 * and no timers in a Structr scripting context -- but a hand-built {@code new Promise(() => {})}
	 * gets there.</p>
	 */
	private static Value executeAsyncWrapper(final Value wrapper, final Snippet snippet) throws FrameworkException {

		try {

			return wrapper.execute();

		} catch (final PolyglotException ex) {

			if (!ex.isHostException() && ex.getMessage() != null && ex.getMessage().contains(PENDING_PROMISE_MESSAGE)) {

				throw new FrameworkException(422, "Server-side scripting error: " + snippet.getName()
					+ " returned a promise that never resolved. Every promise a script awaits has to be settled by the"
					+ " time the script ends; Structr scripting has no event loop, so nothing can settle it afterwards.");
			}

			throw ex;
		}
	}

	public static String[] splitSnippetIntoEngineAndScript(final String snippet) {

		final boolean isAutoScriptingEnv = !(snippet.startsWith("${") && snippet.endsWith("}"));
		final boolean isJavascript       = (snippet.startsWith("${{") && snippet.endsWith("}}")) || (isAutoScriptingEnv && (snippet.startsWith("{") && snippet.endsWith("}")));
		String engine = "";
		String script = "";

		if (isJavascript) {

			engine = "js";
			script = snippet.substring(isAutoScriptingEnv ? 1 : 3, snippet.length() - (isAutoScriptingEnv ? 1 : 2));

		} else {

			final Matcher matcher = ScriptEngineExpression.matcher(isAutoScriptingEnv ? String.format("${%s}", snippet) : snippet);
			if (matcher.matches()) {

				engine = matcher.group(1);
				script = matcher.group(2);
			}
		}

		logger.debug("Scripting engine {} requested.", engine);

		return new String[] { engine, script };
	}

	// ----- private methods -----

	// this is only public to be testable :(
	public static List<String> extractScripts(final String source) {

		final List<String> expressions = new LinkedList<>();
		final StringBuilder buffer     = new StringBuilder();
		final int length               = source.length();
		boolean inLineComment          = false;
		boolean inBlockComment         = false;
		boolean inSingleQuotes         = false;
		boolean inDoubleQuotes         = false;
		boolean inTemplateLiteral      = false;
		boolean inTemplate             = false;
		boolean hasSlash               = false;
		boolean hasStar                = false;
		boolean hasBackslash           = false;
		boolean hasDollar              = false;
		int level                      = 0;
		int start                      = 0;
		int end                        = 0;

		for (int i=0; i<length; i++) {

			final char c = source.charAt(i);

			buffer.append(c);

			switch (c) {

				case '\\':
					hasBackslash = true;
					hasSlash     = false;
					hasStar      = false;
					break;

				case '\'':

					if (inTemplate && !inDoubleQuotes && !inTemplateLiteral && !hasBackslash && !inLineComment && !inBlockComment) {

						inSingleQuotes = !inSingleQuotes;
					}

					hasDollar    = false;
					hasBackslash = false;
					hasSlash     = false;
					hasStar      = false;
					break;

				case '\"':

					if (inTemplate && !inSingleQuotes && !inTemplateLiteral && !hasBackslash && !inLineComment && !inBlockComment) {

						inDoubleQuotes = !inDoubleQuotes;
					}

					hasDollar    = false;
					hasBackslash = false;
					hasSlash     = false;
					hasStar      = false;
					break;

				case '`':

					if (inTemplate && !inSingleQuotes && !inDoubleQuotes && !hasBackslash && !inLineComment && !inBlockComment) {

						inTemplateLiteral = !inTemplateLiteral;
					}

					hasDollar    = false;
					hasBackslash = false;
					hasSlash     = false;
					hasStar      = false;
					break;

				case '$':

					if (!inLineComment && !inBlockComment) {

						hasDollar = true;
					}

					hasBackslash = false;
					hasSlash     = false;
					hasStar      = false;
					break;

				case '{':

					if (!inTemplate && hasDollar && !inLineComment && !inBlockComment) {

						inTemplate = true;
						start = i-1;

					} else if (inTemplate && !inSingleQuotes && !inDoubleQuotes && !inTemplateLiteral && !inLineComment && !inBlockComment) {

						level++;
					}

					hasDollar    = false;
					hasBackslash = false;
					hasSlash     = false;
					hasStar      = false;
					break;

				case '}':

					if (!inSingleQuotes && !inDoubleQuotes && !inTemplateLiteral && inTemplate && !inLineComment && !inBlockComment && level-- == 0) {

						inTemplate = false;
						end = i+1;

						expressions.add(source.substring(start, end));

						level = 0;

					} else {

						buffer.setLength(0);
					}

					hasDollar    = false;
					hasBackslash = false;
					hasSlash     = false;
					hasStar      = false;
					break;

				case '*': {
					if (inTemplate && !inSingleQuotes && !inDoubleQuotes && !inTemplateLiteral && !inLineComment) {

						if (!inBlockComment && hasSlash) {

							inBlockComment = true;
							hasStar = false;

						} else if (inBlockComment) {

							hasStar = true;

						} else {

							hasStar = false;
						}

					} else {

						hasStar = false;
					}

					hasSlash     = false;
					hasDollar    = false;
					hasBackslash = false;
					break;
				}

				case '/': {
					boolean keepSlash = false;

					if (inTemplate && !inSingleQuotes && !inDoubleQuotes && !inTemplateLiteral) {

						if (inBlockComment && hasStar) {

							inBlockComment = false;

						} else if (!inLineComment && !inBlockComment) {

							if (hasSlash) {

								inLineComment = true;

							} else {

								keepSlash = true;
							}
						}
					}

					hasSlash     = keepSlash;
					hasStar      = false;
					hasDollar    = false;
					hasBackslash = false;
					break;
				}

				case '\r':
				case '\n':
					inLineComment = false;
					hasSlash      = false;
					hasStar       = false;
					hasDollar     = false;
					hasBackslash  = false;
					break;

				default:
					hasDollar    = false;
					hasBackslash = false;
					hasSlash     = false;
					hasStar      = false;
					break;
			}
		}

		return expressions;
	}

	public static String formatToDefaultDateOrString(final Object value) {

		if (value == null) {

			return "null";

		} else if (value instanceof Date) {

			return DatePropertyGenerator.format((Date) value, DateProperty.getDefaultFormat());

		} else if (value instanceof Iterable) {

			return Iterables.toList((Iterable)value).toString();

		} else {

			return value.toString();

		}
	}

	public static String formatForLogging(final Object value) {

		if (value == null) {

			return "null";

		} else if (value instanceof Date) {

			return DatePropertyGenerator.format((Date) value, DateProperty.getDefaultFormat());

		} else if (value instanceof Iterable) {

			final StringBuilder buf = new StringBuilder();
			final Iterable iterable = (Iterable)value;

			buf.append("[");

			for (final Iterator it = iterable.iterator(); it.hasNext();) {

				buf.append(Scripting.formatToDefaultDateOrString(it.next()));

				if (it.hasNext()) {

					buf.append(", ");
				}
			}

			buf.append("]");

			return buf.toString();

		} else if (value instanceof GraphObject && !(value instanceof GraphObjectMap)) {

			final StringBuilder buf = new StringBuilder();
			final GraphObject obj   = (GraphObject)value;
			final String name       = obj.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

			buf.append(obj.getType());
			buf.append("(");

			if (StringUtils.isNotBlank(name)) {

				buf.append(name);
				buf.append(", ");
			}

			buf.append(obj.getUuid());
			buf.append(")");

			return buf.toString();

		} else if (value instanceof Throwable throwable) {

			final String mdcString        = MDC.get(ScratchpadTraitWrapper.MDC_SCRATCHPAD_TAG);
			final String scratchLogString = (mdcString == null) ? "" : mdcString + " ";
			Stream<String> lines = Stream.concat(Stream.of(throwable.toString()), Arrays.stream(throwable.getStackTrace())
						.takeWhile(ste -> !ste.getClassName().startsWith("org.graalvm"))
						.map(ste -> scratchLogString + "\tat " + ste.toString()));

			// attach causes recursively
			if (throwable.getCause() != null) {

				lines = Stream.concat(lines, Stream.of(scratchLogString + " Caused by: " + formatForLogging(throwable.getCause())));
			}

			return lines.collect(Collectors.joining(System.lineSeparator()));

		} else {

			return value.toString();

		}
	}

	private static void reportError(final SecurityContext securityContext, final GraphObject entity, final PolyglotException ex, final Snippet snippet) throws FrameworkException {

		final String message = ex.getMessage();
		int lineNumber       = 1;
		int columnNumber     = 1;
		int endLineNumber    = 1;
		int endColumnNumber  = 1;
		final SourceSection location = ex.getSourceLocation();

		if (location != null) {

			lineNumber      = location.getStartLine();
			columnNumber    = location.getStartColumn();
			endLineNumber   = location.getEndLine();
			endColumnNumber = location.getEndColumn();
		}

		boolean broadcastToAdminUI =  !(ex.isHostException() && (ex.asHostException() instanceof AssertException));

		reportError(securityContext, entity, message, lineNumber, columnNumber, endLineNumber, endColumnNumber, snippet, broadcastToAdminUI);
	}

	private static void reportError(final SecurityContext securityContext, final GraphObject entity, final String message, final int lineNumber, final int columnNumber, final Snippet snippet) throws FrameworkException {

		reportError(securityContext, entity, message, lineNumber, columnNumber, lineNumber, columnNumber, snippet, true);
	}

	private static void reportError(final SecurityContext securityContext, final GraphObject entity, final String message, final int lineNumber, final int columnNumber, final int endLineNumber, final int endColumnNumber, final Snippet snippet, final boolean broadcastToAdminUI) throws FrameworkException {

		final String entityName               = snippet.getName();
		final String entityDescription        = (StringUtils.isNotBlank(entityName) ? "\"" + entityName + "\":" : "" ) + snippet.getCodeSource();
		final Map<String, Object> messageData = new LinkedHashMap<>();
		final Map<String, Object> eventData   = new LinkedHashMap<>();
		final StringBuilder exceptionPrefix   = new StringBuilder();
		final String errorName                = "Scripting Error";

		eventData.putAll(
			Map.of(
				"errorName", errorName,
				"row", lineNumber + snippet.getStartRow(),
				"column", columnNumber,
				"endRow", endLineNumber + snippet.getStartRow(),
				"endColumn", endColumnNumber,
				"entity", entityDescription
			)
		);
		messageData.putAll(
			Map.of(
				"type", "SCRIPTING_ERROR",
				"row", lineNumber + snippet.getStartRow(),
				"column", columnNumber,
				"endRow", endLineNumber + snippet.getStartRow(),
				"endColumn", endColumnNumber
			)
		);

		putIfNotNull(eventData,   "message", message);
		putIfNotNull(messageData, "message", message);

		final String codeSourceId = snippet.getCodeSource();
		if (codeSourceId != null) {

			String nodeType = null;
			String nodeId = null;

			if (entity != null) {

				final String entityType = entity.getClass().getSimpleName();
				final String entityId = entity.getUuid();

				messageData.put("entityType", entityType);
				messageData.put("entityId", entityId);
				eventData.put("entityType", entityType);
				eventData.put("entityId", entityId);

				exceptionPrefix.append(entityType).append("[").append(entityId).append("]:");

			}

			final NodeInterface codeSource = StructrApp.getInstance().getNodeById(codeSourceId);
			if (codeSource != null) {

				nodeType = codeSource.getTraits().getName();
				nodeId = codeSource.getUuid();

				if (codeSource.is(StructrTraits.SCHEMA_METHOD) && codeSource.as(SchemaMethod.class).isStaticMethod()) {

					final AbstractSchemaNode node = codeSource.as(SchemaMethod.class).getSchemaNode();
					final String staticTypeName   = codeSource.getName();

					messageData.put("staticType",     staticTypeName);
					messageData.put("isStaticMethod", true);
					eventData.put("staticType",       staticTypeName);
					eventData.put("isStaticMethod",   true);

					exceptionPrefix.append(staticTypeName).append("[static]:");

				} else {

					if (entity == null) {

						// Only generate generic exception prefix, if none has been written for entity
						exceptionPrefix.append(nodeType).append("[").append(nodeId).append("]:");
					}
				}

			}

			eventData.put("type", nodeType);
			messageData.put("nodeType", nodeType);
			eventData.put("id", nodeId);
			messageData.put("nodeId", nodeId);
		}

		if (snippet.getName() != null) {

			eventData.put("name", snippet.getName());
			messageData.put("name", snippet.getName());
		}

		RuntimeEventLog.scripting(errorName, eventData);

		if (broadcastToAdminUI == true) {

			TransactionCommand.simpleBroadcastGenericMessage(messageData, Predicate.only(securityContext.getSessionId()));
		}

		exceptionPrefix.append(snippet.getName()).append(":").append(lineNumber).append(":").append(columnNumber);

		// log error but don't throw exception
		logger.error(exceptionPrefix.toString() + ": " + message);
	}

	private static void putIfNotNull(final Map<String, Object> map, final String key, final Object value) {

		if (value != null) {

			map.put(key, value);
		}
	}

	// ----- nested classes -----
	private static class Tuple {

		public String key = null;
		public String value = null;

		public Tuple(final String key, final String value) {

			this.key = key;
			this.value = value;
		}
	}
}
