package org.structr.core.script;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.parser.Functions;
import org.structr.core.property.DateProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.parser.DatePropertyParser;

/**
 *
 * @author Christian Morgner
 */
public class Scripting {

	public static String replaceVariables(final ActionContext actionContext, final GraphObject entity, final Object rawValue) throws FrameworkException {

		if (rawValue == null) {

			return null;
		}

		String value;

		if (rawValue instanceof String) {

			value = (String) rawValue;

			if (!actionContext.returnRawValue()) {

				final Map<String, String> replacements = new LinkedHashMap<>();

				for (final String expression : extractScripts(value)) {

					final Object extractedValue = evaluate(actionContext, entity, expression);
					String partValue            = extractedValue != null ? formatToDefaultDateOrString(extractedValue) : "";

					if (partValue != null) {

						replacements.put(expression, partValue);

					} else {

						// If the whole expression should be replaced, and partValue is null
						// replace it by null to make it possible for HTML attributes to not be rendered
						// and avoid something like ... selected="" ... which is interpreted as selected==true by
						// all browsers
						if (!value.equals(expression)) {
							replacements.put(expression, "");
						}
					}
				}

				// apply replacements
				for (final Map.Entry<String, String> entry : replacements.entrySet()) {

					final String group = entry.getKey();
					final String replacement = entry.getValue();

					value = value.replace(group, replacement);
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
	 * Structr script, ${{}} means Javascript.
	 *
	 * @param securityContext the security context
	 * @param actionContext the action context
	 * @param entity the entity - may not be null because internal functions will fetch the security context from it
	 * @param expression the scripting expression
	 *
	 * @return
	 * @throws FrameworkException
	 */
	public static Object evaluate(final ActionContext actionContext, final GraphObject entity, final String expression) throws FrameworkException {

		final boolean isJavascript = expression.startsWith("${{") && expression.endsWith("}}");
		final int prefixOffset     = isJavascript ? 1 : 0;
		final String source        = expression.substring(2 + prefixOffset, expression.length() - (1 + prefixOffset));

		actionContext.setJavaScriptContext(isJavascript);

		if (isJavascript) {

			return evaluateJavascript(actionContext, entity, source);

		} else {

			return Functions.evaluate(actionContext, entity, source);
		}
	}

	private static Object evaluateJavascript(final ActionContext actionContext, final GraphObject entity, final String script) throws FrameworkException {

		final Context scriptingContext = Context.enter();

		try {

			// Set version to JavaScript1.2 so that we get object-literal style
			// printing instead of "[object Object]"
			scriptingContext.setLanguageVersion(Context.VERSION_1_2);

			// Initialize the standard objects (Object, Function, etc.)
			// This must be done before scripts can be executed.
			Scriptable scope = scriptingContext.initStandardObjects();

			final StructrScriptable scriptable = new StructrScriptable(actionContext, entity);
			scriptable.setParentScope(scope);

			// register Structr scriptable
			scope.put("Structr", scope, scriptable);

			// clear output buffer
			actionContext.clear();

			Object extractedValue = scriptingContext.evaluateString(scope, embedInFunction(script), "script source [" + ( !((NodeInterface)entity).getName().equals("") ? "\""+((NodeInterface)entity).getName()+"\":" : "" ) + entity.getUuid() + "], line ", 1, null);

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

		} catch (Throwable t) {

			t.printStackTrace();

		} finally {

			Context.exit();
		}

		return null;
	}

	private static String embedInFunction(final String source) {

		final StringBuilder buf = new StringBuilder();

		buf.append("function main() { ");
		buf.append(source);
		buf.append("\n}\n");
		buf.append("var _structrMainResult = main();");

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

}
