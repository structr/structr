package org.structr.core.script;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.parser.Functions;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class Scripting {

	private static final String scriptEngineName = StructrApp.getConfigurationValue("scripting.engine", "structr");

	public static String replaceVariables(final SecurityContext securityContext, final GraphObject entity, final ActionContext actionContext, final Object rawValue) throws FrameworkException {

		String value = null;

		if (rawValue == null) {

			return null;
		}

		if (rawValue instanceof String) {

			value = (String) rawValue;

			if (!actionContext.returnRawValue(securityContext)) {

				final StructrScriptable scriptable     = new StructrScriptable(securityContext, actionContext, entity);
				final ScriptEngine engine              = new ScriptEngineManager().getEngineByName(scriptEngineName);
				final Map<String, String> replacements = new LinkedHashMap<>();

				engine.put("Structr", scriptable);
				engine.put("_securityContext", securityContext);
				engine.put("_actionContext", actionContext);
				engine.put("_entity", entity);

				for (final String expression : extractScripts(value)) {

					String source         = expression.substring(2, expression.length() - 1);
					Object extractedValue = null;

					try {
						 //Functions.evaluate(securityContext, actionContext, entity, source);
						extractedValue = engine.eval(source);

					} catch (ScriptException spex) {

						final Throwable cause = spex.getCause();

						// extract wrapped FrameworkException
						if (cause instanceof FrameworkException) {
							throw (FrameworkException)cause;

						} else {

							throw new FrameworkException(500, spex.getMessage());
						}

					}

					if (extractedValue == null) {
						extractedValue = "";
					}

					String partValue = extractedValue.toString();
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

					} else if (!inSingleQuotes && !inDoubleQuotes) {
						level++;
					}

					hasDollar = false;
					break;

				case '}':

					if (!inSingleQuotes && !inDoubleQuotes && level-- == 0 && inTemplate) {

						inTemplate = false;
						end = i+1;

						expressions.add(source.substring(start, end));

						level = 0;
					}
					hasDollar = true;
					break;

				default:
					hasDollar = false;
					break;
			}
		}

		return expressions;
	}
}
