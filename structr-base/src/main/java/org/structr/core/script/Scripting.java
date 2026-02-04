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
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.core.script.polyglot.context.ContextHelper;
import org.structr.core.script.polyglot.util.JSFunctionTranspiler;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.parser.DatePropertyGenerator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Scripting {

	private static final Pattern ScriptEngineExpression             = Pattern.compile("^\\$\\{(\\w+)\\{(.*)\\}\\}$", Pattern.DOTALL);
	private static final Logger logger                              = LoggerFactory.getLogger(Scripting.class.getName());

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
		} else {

			source = expression.substring(2, expression.length() - 1);
		}

		final boolean isJavascript = "js".equals(engine);
		final boolean isScriptEngine = !isJavascript && StringUtils.isNotBlank(engine);

		actionContext.setJavaScriptContext(isJavascript);

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

				final EvaluationHints hints = new EvaluationHints();
				Object extractedValue       = Functions.evaluate(actionContext, entity, snippet, hints);
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

				final Value value = evaluatePolyglot(actionContext, engineName, context, entity, snippet);
				result = PolyglotWrapper.unwrap(actionContext, value);

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

		return result != null ? result : "";
	}


	public static Value evaluatePolyglot(final ActionContext actionContext, final String engineName, final Context context, final GraphObject entity, final Snippet snippet) throws FrameworkException {

		try {

			Source source = null;
			String code = snippet.getSource();

			if ("js".equals(engineName) && snippet.embed()) {

				code = JSFunctionTranspiler.transpileSource(snippet);
			}

			source = Source.newBuilder(engineName, code, snippet.getName()).mimeType(snippet.getMimeType()).build();

			try {
				if (source != null) {

					final Value result = context.eval(source);

					// Legacy print() support: Prefer explicitly printed output over actual result
					final String outputBuffer = actionContext.getOutput();
					if (outputBuffer != null && !outputBuffer.isEmpty()) {

						return Value.asValue(outputBuffer);
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
		boolean inComment              = false;
		boolean inSingleQuotes         = false;
		boolean inDoubleQuotes         = false;
		boolean inTemplate             = false;
		boolean hasSlash               = false;
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
					break;

				case '\'':
					if (inTemplate && !inDoubleQuotes && !hasBackslash && !inComment) {
						inSingleQuotes = !inSingleQuotes;
					}
					hasDollar = false;
					hasBackslash = false;
					break;

				case '\"':
					if (inTemplate && !inSingleQuotes && !hasBackslash && !inComment) {
						inDoubleQuotes = !inDoubleQuotes;
					}
					hasDollar = false;
					hasBackslash = false;
					break;

				case '$':
					if (!inComment) {
						hasDollar = true;
						hasBackslash = false;
					}
					break;

				case '{':
					if (!inTemplate && hasDollar && !inComment) {

						inTemplate = true;
						start = i-1;

					} else if (inTemplate && !inSingleQuotes && !inDoubleQuotes && !inComment) {
						level++;
					}

					hasDollar = false;
					hasBackslash = false;
					break;

				case '}':

					if (!inSingleQuotes && !inDoubleQuotes && inTemplate && !inComment && level-- == 0) {

						inTemplate = false;
						end = i+1;

						expressions.add(source.substring(start, end));

						level = 0;

					} else {

						//otherParts.add(buffer.toString());
						buffer.setLength(0);
					}
					hasDollar = false;
					hasBackslash = false;
					break;

				case '/':

					if (inTemplate && !inComment && !inSingleQuotes && !inDoubleQuotes) {

						if (hasSlash) {

							inComment = true;
							hasSlash  = false;

						} else {

							hasSlash = true;
						}
					}
					break;

				case '\r':
				case '\n':
					inComment = false;
					break;

				default:
					hasDollar = false;
					hasBackslash = false;
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

			final Stream<String> lines = Stream.concat(
				Stream.of(String.valueOf(throwable.getMessage())),
				Arrays.stream(throwable.getStackTrace())
						.takeWhile(ste -> !ste.getClassName().startsWith("org.graalvm"))
						.map(ste -> "\tat " + ste.toString())
			);

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
		logger.warn(exceptionPrefix.toString() + ": " + message);
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
