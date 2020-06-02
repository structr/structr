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

import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.function.Functions;
import org.structr.core.property.DateProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.parser.DatePropertyParser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Scripting {

	private static final Logger logger                       = LoggerFactory.getLogger(Scripting.class.getName());
	private static final Pattern ScriptEngineExpression      = Pattern.compile("^\\$\\{(\\w+)\\{(.*)\\}\\}$", Pattern.DOTALL);
	//private static final Map<String, Script> compiledScripts = Collections.synchronizedMap(new LRUMap<>(10000));

	public static String replaceVariables(final ActionContext actionContext, final GraphObject entity, final Object rawValue) throws FrameworkException {
		return replaceVariables(actionContext, entity, rawValue, false);
	}

	public static String replaceVariables(final ActionContext actionContext, final GraphObject entity, final Object rawValue, final boolean returnNullValueForEmptyResult) throws FrameworkException {

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

						final Object extractedValue = evaluate(actionContext, entity, expression, "script source");
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

	/**
	 * Evaluate the given script according to the parsing conventions: ${} will try to evaluate
	 * Structr script, ${{}} means Javascript, ${ENGINE{}} means calling a script interpreter for ENGINE
	 *
	 * @param actionContext the action context
	 * @param entity the entity - may not be null because internal functions will fetch the security context from it
	 * @param input the scripting input
	 * @param methodName the name of the method for error logging
	 *
	 * @return
	 * @throws FrameworkException
	 * @throws UnlicensedScriptException
	 */
	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String input, final String methodName) throws FrameworkException, UnlicensedScriptException {

		final String expression = StringUtils.strip(input);
		boolean isJavascript    = expression.startsWith("${{") && expression.endsWith("}}");
		final int prefixOffset  = isJavascript ? 1 : 0;
		String source           = expression.substring(2 + prefixOffset, expression.length() - (1 + prefixOffset));

		if (source.length() <= 0) {
			return null;
		}

		String engine = "";
		boolean isScriptEngine = false;

		if (!isJavascript) {

			final Matcher matcher = ScriptEngineExpression.matcher(expression);
			if (matcher.matches()) {

				engine = matcher.group(1);
				source = matcher.group(2);

				logger.debug("Scripting engine {} requested.", engine);

				isJavascript   = StringUtils.isBlank(engine) || "JavaScript".equals(engine);
				isScriptEngine = !isJavascript && StringUtils.isNotBlank(engine);
			}
		}

		actionContext.setJavaScriptContext(isJavascript);

		// temporarily disable notifications for scripted actions

		boolean enableTransactionNotifactions = false;

		final SecurityContext securityContext = actionContext.getSecurityContext();
		if (securityContext != null) {

			enableTransactionNotifactions = securityContext.doTransactionNotifications();

			securityContext.setDoTransactionNotifications(false);
		}

		if (isScriptEngine) {

			return evaluateScript(actionContext, entity, engine, source);

		} else if (isJavascript) {

			final Object result = evaluateJavascript(actionContext, entity, new Snippet(methodName, source));

			if (enableTransactionNotifactions && securityContext != null) {
				securityContext.setDoTransactionNotifications(true);
			}

			return result;

		} else {

			Object extractedValue = Functions.evaluate(actionContext, entity, source);
			final String value    = extractedValue != null ? extractedValue.toString() : "";
			final String output   = actionContext.getOutput();

			if (StringUtils.isEmpty(value) && output != null && !output.isEmpty()) {
				extractedValue = output;
			}

			if (enableTransactionNotifactions && securityContext != null) {
				securityContext.setDoTransactionNotifications(true);
			}

			return extractedValue;
		}
	}

	public static Object evaluateJavascript(final ActionContext actionContext, final GraphObject entity, final Snippet snippet) throws FrameworkException {

		final String entityType        = entity != null ? (entity.getClass().getSimpleName() + ".") : "";
		final String entityName        = entity != null ? entity.getProperty(AbstractNode.name) : null;
		final String entityDescription = entity != null ? ( StringUtils.isNotBlank(entityName) ? "\"" + entityName + "\":" : "" ) + entity.getUuid() : "anonymous";

		final Context context = Context.newBuilder("js")
				.allowPolyglotAccess(StructrPolyglotAccessProvider.getPolyglotAccessConfig())
				.allowHostAccess(StructrPolyglotAccessProvider.getHostAccessConfig())
				// TODO: Add config switch to toggle Host Class Lookup
				//.allowHostClassLookup(s -> true)
				.allowExperimentalOptions(true)
				.option("js.experimental-foreign-object-prototype", "true")
				.option("js.nashorn-compat", "true")
				.build();

		final StructrPolyglotBinding structrBinding = new StructrPolyglotBinding(actionContext, entity);

		context.getBindings("js").putMember("Structr", structrBinding);
		context.getBindings("js").putMember("$", structrBinding);

		try {
			Object result = StructrPolyglotWrapper.unwrap(context.eval("js", embedInFunction(snippet.getSource())));

			return result;
		} catch (Exception ex) {

			throw new FrameworkException(422, ex.getMessage());
		}

	}

	private static String getExceptionMessage (final ActionContext actionContext) {

		final StringBuilder sb = new StringBuilder("Exception in Scripting context");

		if (Settings.LogJSExcpetionRequest.getValue()) {

			sb.append(" (");

			final String requestInfo = actionContext.getRequestInfoForVerboseJavaScriptExceptionLog();

			if (requestInfo != null) {

				sb.append(requestInfo);

			} else {

				sb.append("no request information available for this scripting error");
			}

			sb.append(")");
		}

		return sb.toString();
	}

	// ----- private methods -----
	private static Object evaluateScript(final ActionContext actionContext, final GraphObject entity, final String engineName, final String script) throws FrameworkException {

		try {

			final Context context = Context.newBuilder()
					.allowPolyglotAccess(StructrPolyglotAccessProvider.getPolyglotAccessConfig())
					.allowHostAccess(StructrPolyglotAccessProvider.getHostAccessConfig())
					.build();

			final StructrPolyglotBinding structrBinding = new StructrPolyglotBinding(actionContext, entity);

			context.getBindings(engineName).putMember("Structr", structrBinding);
			context.getBindings(engineName).putMember("$", structrBinding);


			final StringBuilder wrappedScript = new StringBuilder();


			switch (engineName) {
				case "R":
					wrappedScript.append("main <- function() {");
					wrappedScript.append(script);
					wrappedScript.append("}\n");
					wrappedScript.append("\n\nmain()");
					break;
				case "python":
					// Prepend tabs
					final String tabPrependedScript = Arrays.stream(script.trim().split("\n")).map(line -> "	" + line).collect(Collectors.joining("\n"));
					wrappedScript.append("def main():\n");
					wrappedScript.append(tabPrependedScript);
					wrappedScript.append("\n");

					context.eval(engineName, wrappedScript.toString());

					return StructrPolyglotWrapper.unwrap(context.getBindings(engineName).getMember("main").execute());
			}


				Object result = StructrPolyglotWrapper.unwrap(context.eval(engineName, wrappedScript.toString()));

				return result;
		} catch (PolyglotException ex) {

			throw new FrameworkException(422, ex.getMessage());
		} catch (Throwable ex) {

			throw new FrameworkException(422, ex.getMessage());
		}

	}

	private static String embedInFunction(final String source) {

		final StringBuilder buf = new StringBuilder();

		buf.append("function main() { ");
		buf.append(source);
		buf.append("\n}\n");
		buf.append("\n\nmain();");

		return buf.toString();
	}

	// this is only public to be testable :(
	public static List<String> extractScripts(final String source) {

		final List<String> otherParts  = new LinkedList<>();
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

						otherParts.add(buffer.toString());
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

		if (value instanceof Date) {

			return DatePropertyParser.format((Date) value, DateProperty.getDefaultFormat());

		} else if (value instanceof Iterable) {

			return Iterables.toList((Iterable)value).toString();

		} else {

			return value.toString();

		}
	}

	public static String formatForLogging(final Object value) {

		if (value instanceof Date) {

			return DatePropertyParser.format((Date) value, DateProperty.getDefaultFormat());

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

		} else if (value instanceof GraphObject) {

			final StringBuilder buf = new StringBuilder();
			final GraphObject obj   = (GraphObject)value;
			final String name       = obj.getProperty(AbstractNode.name);

			buf.append(obj.getType());
			buf.append("(");

			if (StringUtils.isNotBlank(name)) {

				buf.append(name);
				buf.append(", ");
			}

			buf.append(obj.getUuid());
			buf.append(")");

			return buf.toString();

		} else {

			return value.toString();

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

	public static void main(final String[] args) {

		extractScripts("blah${'blah'}test");
	}

}
