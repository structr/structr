/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.lang.StringUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.renjin.script.RenjinScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.function.Functions;
import org.structr.core.property.DateProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.parser.DatePropertyParser;

/**
 *
 *
 */
public class Scripting {

	private static final Logger logger                  = LoggerFactory.getLogger(Scripting.class.getName());
	private static final Pattern ScriptEngineExpression = Pattern.compile("^\\$\\{(\\w+)\\{(.*)\\}\\}$", Pattern.DOTALL);

	public static String replaceVariables(final ActionContext actionContext, final GraphObject entity, final Object rawValue) throws FrameworkException {

		if (rawValue == null) {

			return null;
		}

		String value;

		if (rawValue instanceof String) {

			value = (String) rawValue;

			if (!actionContext.returnRawValue()) {

				final List<Tuple> replacements = new LinkedList<>();

				for (final String expression : extractScripts(value)) {

					final Object extractedValue = evaluate(actionContext, entity, expression);
					String partValue            = extractedValue != null ? formatToDefaultDateOrString(extractedValue) : "";

					if (partValue != null) {

						replacements.add(new Tuple(expression, partValue));

					} else {

						// If the whole expression should be replaced, and partValue is null
						// replace it by null to make it possible for HTML attributes to not be rendered
						// and avoid something like ... selected="" ... which is interpreted as selected==true by
						// all browsers
						if (!value.equals(expression)) {
							replacements.add(new Tuple(expression, ""));
						}
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

		// return literal null
		if (Functions.NULL_STRING.equals(value)) {
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
	 * @param expression the scripting expression
	 *
	 * @return
	 * @throws FrameworkException
	 */
	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String expression) throws FrameworkException {

		boolean isJavascript   = expression.startsWith("${{") && expression.endsWith("}}");
		final int prefixOffset = isJavascript ? 1 : 0;
		String source          = expression.substring(2 + prefixOffset, expression.length() - (1 + prefixOffset));

		String engine = "";
		boolean isScriptEngine = false;

		if (!isJavascript) {

			final Matcher matcher = ScriptEngineExpression.matcher(expression);
			if (matcher.matches()) {

				engine = matcher.group(1);
				source = matcher.group(2);

				logger.info("Scripting engine {} requested.", engine);

				isJavascript   = StringUtils.isBlank(engine) || "JavaScript".equals(engine);
				isScriptEngine = !isJavascript && StringUtils.isNotBlank(engine);
			}
		}

		actionContext.setJavaScriptContext(isJavascript);

		if (isScriptEngine) {

			return evaluateScript(actionContext, entity, engine, source);

		} else if (isJavascript) {

			return evaluateJavascript(actionContext, entity, source);

		} else {

			Object extractedValue = Functions.evaluate(actionContext, entity, source);
			final String value    = extractedValue != null ? extractedValue.toString() : "";
			final String output   = actionContext.getOutput();

			if (StringUtils.isEmpty(value) && output != null && !output.isEmpty()) {
				extractedValue = output;
			}

			return extractedValue;
		}
	}

	private static Object evaluateScript(final ActionContext actionContext, final GraphObject entity, final String engineName, final String script) throws FrameworkException {

		final ScriptEngineManager manager = new ScriptEngineManager();
		final ScriptEngine engine = manager.getEngineByName(engineName);

		if (engine == null) {
			throw new RuntimeException(engineName + " script engine could not be initialized. Check class path.");
		}

		final ScriptContext scriptContext = engine.getContext();
		final Bindings bindings           = new StructrScriptBindings(actionContext, entity);

		if (!(engine instanceof RenjinScriptEngine)) {
			scriptContext.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
		}

		StringWriter output = new StringWriter();
		scriptContext.setWriter(output);

		try {

			engine.eval(script);

			Object extractedValue = output.toString();

			return extractedValue;

		} catch (final ScriptException e) {

			e.printStackTrace();

			logger.error("Error while processing " + engineName + " script: {}", new Object[]{script, e});
		}

		return null;

	}

	private static Object evaluateJavascript(final ActionContext actionContext, final GraphObject entity, final String script) throws FrameworkException {

		final String entityName        = entity != null ? entity.getProperty(AbstractNode.name) : null;
		final String entityDescription = entity != null ? ( StringUtils.isNotBlank(entityName) ? "\"" + entityName + "\":" : "" ) + entity.getUuid() : "anonymous";
		final Context scriptingContext = new ContextFactory().enterContext();

		try {

			// Set version to JavaScript1.2 so that we get object-literal style
			// printing instead of "[object Object]"
			scriptingContext.setLanguageVersion(Context.VERSION_1_2);

			// Initialize the standard objects (Object, Function, etc.)
			// This must be done before scripts can be executed.
			Scriptable scope = scriptingContext.initStandardObjects();

			// set optimization level to interpreter mode to avoid
			// class loading / PermGen space bug in Rhino
			//scriptingContext.setOptimizationLevel(-1);

			final StructrScriptable scriptable = new StructrScriptable(actionContext, entity, scriptingContext);
			scriptable.setParentScope(scope);

			// register Structr scriptable
			scope.put("Structr", scope, scriptable);

			// clear output buffer
			actionContext.clear();

			Object extractedValue = scriptingContext.evaluateString(scope, embedInFunction(actionContext, script), "script source [" + entityDescription + "], line ", 1, null);

			if (scriptable.hasException()) {
				throw scriptable.getException();
			}

			// prioritize written output over result returned from method
			final String output = actionContext.getOutput();
			if (output != null && !output.isEmpty()) {
				extractedValue = output;
			}

			if (extractedValue == null || extractedValue == Undefined.instance) {
				extractedValue = scriptable.unwrap(scope.get("_structrMainResult", scope));
			}

			if (extractedValue == null || extractedValue == Undefined.instance) {
				extractedValue = "";
			}

			return extractedValue;

		} catch (final FrameworkException fex) {

			// just throw the FrameworkException so we dont lose the information contained
			throw fex;

		} catch (final Throwable t) {

			// if any other kind of Throwable is encountered throw a new FrameworkException and be done with it
			logger.warn("", t);
			throw new FrameworkException(422, t.getMessage());

		} finally {

			Context.exit();
		}

	}

	private static String embedInFunction(final ActionContext actionContext, final String source) {

		final StringBuilder buf = new StringBuilder();

		buf.append("function main() { ");
		buf.append(source);
		buf.append("\n}\n");
		buf.append("\n\nvar _structrMainResult = main();");

		return buf.toString();
	}

	// this is only public to be testable :(
	public static List<String> extractScripts(final String source) {

		final List<String> expressions = new LinkedList<>();
		final int length               = source.length();
		boolean inSingleQuotes         = false;
		boolean inDoubleQuotes         = false;
		boolean inTemplate             = false;
		boolean hasDollar              = false;
		int level                      = 0;
		int start                      = 0;
		int end                        = 0;

		for (int i=0; i<length; i++) {

			final char c = source.charAt(i);

			switch (c) {

				case '\'':
					if (inTemplate) {
						inSingleQuotes = !inSingleQuotes;
					}
					hasDollar = false;
					break;

				case '\"':
					if (inTemplate) {
						inDoubleQuotes = !inDoubleQuotes;
					}
					hasDollar = false;
					break;

				case '$':
					hasDollar = true;
					break;

				case '{':
					if (!inTemplate && hasDollar) {

						inTemplate = true;
						start = i-1;

					} else if (inTemplate && !inSingleQuotes && !inDoubleQuotes) {
						level++;
					}

					hasDollar = false;
					break;

				case '}':

					if (!inSingleQuotes && !inDoubleQuotes && inTemplate && level-- == 0) {

						inTemplate = false;
						end = i+1;

						expressions.add(source.substring(start, end));

						level = 0;
					}
					hasDollar = false;
					break;

				default:
					hasDollar = false;
					break;
			}
		}

		return expressions;
	}

	private static String formatToDefaultDateOrString(final Object value) {

		if (value instanceof Date) {

			return DatePropertyParser.format((Date) value, DateProperty.getDefaultFormat());

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
}
