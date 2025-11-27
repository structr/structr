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
package org.structr.core.api;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NumericalMethodInputParsingException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.core.GraphObject;
import org.structr.core.api.Arguments.Argument;
import org.structr.core.script.Scripting;
import org.structr.core.script.Snippet;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.StructrBinding;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.core.script.polyglot.context.ContextHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.parser.DatePropertyGenerator;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;


/**
 *
 */
public abstract class AbstractMethod {

	protected String description = null;
	protected String summary     = null;
	protected String name        = null;

	public AbstractMethod(final String name, final String summary, final String description) {

		this.description = description;
		this.summary     = summary;
		this.name        = name;
	}

	public abstract boolean isStatic();
	public abstract boolean isPrivate();
	public abstract Snippet getSnippet();
	public abstract String getHttpVerb();
	public abstract Parameters getParameters();
	public abstract String getFullMethodName();
	public abstract Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException;

	public String getName() {
		return name;
	}

	public String getSummary() {
		return summary;
	}

	public String getDescription() {
		return description;
	}

	public ProxyExecutable getProxyExecutable(final ActionContext actionContext, final GraphObject entity) {

		return (arguments) -> {

			try {
				final Snippet snippet = getSnippet();

				if (snippet != null && !snippet.getSource().isEmpty()) {

					final String engineName = snippet.getEngineName();

					if (!engineName.isEmpty()) {

						// Important: getContext is called with allowEntityOverride=false to prevent entity automatically being overridden in the case of an existent context
						final Context context                 = ContextFactory.getContext(engineName, actionContext, entity, false);
						final SecurityContext securityContext = actionContext.getSecurityContext();
						final Value bindings = context.getBindings(engineName).getMember("Structr");
						final StructrBinding binding = bindings.asProxyObject();
						final GraphObject previousEntity = binding.getEntity();
						final ActionContext previousContext = binding.getActionContext();
						final Value previousMethodParameters = binding.getMethodParameters();
						final Map<String, Object> tmp = securityContext.getContextStore().getTemporaryParameters();
						Locale effectiveLocale = actionContext.getLocale();

						ActionContext inner = null;
						try {

							final Arguments args      = NamedArguments.fromValues(actionContext, arguments);
							final Arguments converted = checkAndConvertArguments(securityContext, args, true);
							inner = new ActionContext(securityContext, converted.toMap());
							inner.setLocale(effectiveLocale);

							inner.setScriptingContexts(actionContext.getScriptingContexts());

							if (arguments.length == 1) {
								binding.setMethodParameters(arguments[0]);
							}

							binding.setEntity(entity);
							binding.setActionContext(inner);

							// store current AbstractMethod object in ActionContext
							inner.setCurrentMethod(this);

							// Context reference count handling
							ContextHelper.incrementReferenceCount(context);
							context.enter();

							final Value result = Scripting.evaluatePolyglot(inner, engineName, context, entity, snippet);

							// Context reference count handling
							context.leave();
							ContextHelper.decrementReferenceCount(context);

							if (ContextHelper.getReferenceCount(context) <= 0) {

								context.close();
								actionContext.putScriptingContext(engineName, null);
							}

							effectiveLocale = inner.getLocale();

							return result;

						} catch (IllegalArgumentTypeException iaex) {

							iaex.printStackTrace();

							throwIllegalArgumentExceptionForMapBasedArguments();

						} finally {

							// pass on error tokens
							if (inner != null && inner.hasError()) {
								for (ErrorToken token : inner.getErrorBuffer().getErrorTokens()) {
									actionContext.getErrorBuffer().add(token);
								}
							}

							// restore state before this method call
							binding.setEntity(previousEntity);
							binding.setActionContext(previousContext);
							binding.setMethodParameters(previousMethodParameters);
							securityContext.getContextStore().setTemporaryParameters(tmp);
							// take over inner locale, in case it changed
							actionContext.setLocale(effectiveLocale);
						}
					}
				}

				// fallback => normal scripting
				final Arguments converted = PolyglotWrapper.unwrapExecutableArguments(actionContext, this, arguments);
				return PolyglotWrapper.wrap(actionContext, this.execute(actionContext.getSecurityContext(), entity, converted, new EvaluationHints()));

			} catch (FrameworkException ex) {
				throw new RuntimeException(ex);
			}
		};
	}

	public boolean shouldReturnRawResult() {

		return false;
	}

	// ----- protected methods -----
	protected Arguments checkAndConvertArguments(final SecurityContext securityContext, final Arguments arguments, final boolean ensureArgumentsArePresent) throws FrameworkException, IllegalArgumentTypeException {

		final Parameters parameters = getParameters();
		if (parameters.isEmpty()) {

			// don't convert anything if the method defines no formal parameters
			return arguments;
		}

		final NamedArguments converted = new NamedArguments();
		int index                      = 0;

		// The below block tries to convert method arguments according to declared parameters (if present).
		// We go over all arguments, check if there is a corresponding parameter, and convert the value if yes.
		for (final Argument arg : arguments.getAll()) {

			// named argument?
			String parameterName        = arg.getName();
			final Object parameterValue = arg.getValue();

			if (parameterName == null) {
				parameterName = parameters.getNameByIndex(index);
			}

			final String type = parameters.getTypeByNameOrIndex(parameterName, index);
			if (type != null) {

				converted.add(parameterName, convertValue(parameterName, parameterValue, type));
			}

			index++;
		}

		if (ensureArgumentsArePresent) {

			int pos = 0;

			// go over all parameters and ensure that there is a value for each parameter
			for (final Entry<String, String> entry : parameters.entrySet()) {

				final String parameterName = entry.getKey();
				final String parameterType = entry.getValue();

				final Object argument = converted.get(pos++);
				if (argument == null) {

					switch (parameterType) {

						case "Map":
							converted.add(parameterName, new LinkedHashMap());
							break;

						case "SecurityContext":
							converted.add(parameterName, securityContext);
							break;
					}

				} else {

					// check type of argument, but only if it's not a SecurityContext,
					// because otherwise we need to make sure that a SuperUserSecurityContext
					// is also a valid type, but we only have string types, and no
					// inheritance, etc..

					if (!SecurityContext.class.getSimpleName().equals(parameterType)) {

						// fixme: we cannot do this check here because we don't have actual type / inheritance info..
						/*
						if (!parameterType.equals(argument.getClass().getSimpleName())) {

							throwIllegalArgumentExceptionForUnnamedArguments(parameters, converted);
						}
						*/
					}
				}

			}
		}


		return converted;
	}

	/*
	protected Arguments checkAndConvertArguments(final SecurityContext securityContext, final Arguments arguments, final boolean ensureArgumentsArePresent) throws FrameworkException {

		final Parameters parameters = getParameters();
		if (parameters.isEmpty()) {

			// don't convert anything if the method defines no formal parameters
			return arguments;
		}

		final Arguments converted = new Arguments();
		int index                 = 0;

		for (final Entry<String, String> entry : parameters.entrySet()) {

			final String name = entry.getKey();
			final String type = entry.getValue();

			// special handling for SecurityContext
			if (SecurityContext.class.getSimpleName().equals(type)) {

				converted.add(securityContext);

			} else if (name != null) {

				Object value = arguments.get(name);
				if (value != null) {

					converted.add(name, convertValue(value, type));

				} else {

					// fetch value by index, assuming order of arguments is correct
					value = arguments.get(index++);
					if (value != null) {

						converted.add(name, convertValue(value, type));

					} else {

						// make sure that a java.reflect.Method gets all arguments
						if (ensureArgumentsArePresent) {

							// we need to pass an empty map
							if ("Map".equals(type)) {

								converted.add(new LinkedHashMap<>());

							} else {

								converted.add(null);
							}
						}
					}
				}

			} else {

				// fixme: this might be wrong..
				throwIllegalArgumentExceptionForMapBasedArguments();
			}
		}

		return converted;
	}
	*/

	protected Object convertValue(final String parameterName, final Object input, final String targetType) throws FrameworkException {

		if ("Date".equals(targetType) && input instanceof String stringInput) {

			final Date date = DatePropertyGenerator.parseISO8601DateString(stringInput);
			if (date != null) {

				return date;
			}

			throw new FrameworkException(
				422,
				"Cannot parse input for ‛" + parameterName + "‛ in " + getFullMethodName(),
				new SemanticErrorToken(
					null,
					null,
					"invalid_date_format"
				).withValue(input).with("method", getName()).with("parameter", parameterName)
			);
		}

		if ("long".equals(targetType) || "Long".equals(targetType)) {

			if (input instanceof Number number) {

				return number.longValue();
			}

			if (input instanceof String string) {

				try {

					return Long.valueOf(string);

				} catch (NumberFormatException nex) {

					throw new NumericalMethodInputParsingException(getFullMethodName(), getName(), parameterName, input);
				}
			}
		}

		if ("int".equals(targetType) || "Integer".equals(targetType)) {

			if (input instanceof Number number) {

				return number.intValue();
			}

			if (input instanceof String string) {


				try {

					return Integer.valueOf(string);

				} catch (NumberFormatException nex) {

					throw new NumericalMethodInputParsingException(getFullMethodName(), getName(), parameterName, input);
				}
			}
		}

		if ("double".equals(targetType) || "Double".equals(targetType)) {

			if (input instanceof Number number) {

				return number.doubleValue();
			}

			if (input instanceof String string) {


				try {

					return Double.valueOf(string);

				} catch (NumberFormatException nex) {

					throw new NumericalMethodInputParsingException(getFullMethodName(), getName(), parameterName, input);
				}
			}
		}

		if ("float".equals(targetType) || "Float".equals(targetType)) {

			if (input instanceof Number number) {

				return number.floatValue();
			}

			if (input instanceof String string) {


				try {

					return Float.valueOf(string);

				} catch (NumberFormatException nex) {

					throw new NumericalMethodInputParsingException(getFullMethodName(), getName(), parameterName, input);
				}
			}
		}

		return input;
	}

	protected void throwIllegalArgumentExceptionForMapBasedArguments() throws FrameworkException {
		throw new FrameworkException(422, "Tried to call " + getFullMethodName() + " with illegal arguments. To fix this error, you can either specify method parameters, or call the method with a single argument of type object, e.g. { \"name\": \"example\" }.");
	}

	protected void throwIllegalArgumentExceptionForUnnamedArguments(final Parameters parameters, final Arguments arguments) throws FrameworkException {
		throw new FrameworkException(422, "Tried to call " + getFullMethodName() + " with illegal arguments. Expected: " + parameters.formatForErrorMessage() + ", actual: " + arguments.formatForErrorMessage());
	}
}
