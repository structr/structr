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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.ScriptMethod;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FunctionInfoFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_FUNCTION_INFO_JS = "Usage: ${{ $.functionInfo([type, name]) }}. Example ${{ $.functionInfo() }}";

	public static final String DECLARING_TRAIT_KEY          = "declaringTrait";
	public static final String IS_USER_DEFINED_FUNCTION_KEY = "isUserDefinedFunction";

	@Override
	public String getName() {
		return "functionInfo";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("[type, name]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			if (sources.length == 0) {

				final AbstractMethod currentMethod = ctx.getCurrentMethod();
				if (currentMethod != null) {

					return getFunctionInfo(currentMethod);
				}

			} else if (sources.length == 2) {

				final String typeName     = sources[0].toString();
				final String functionName = sources[1].toString();

				if (Traits.exists(typeName)) {

					final Traits type           = Traits.of(typeName);
					final AbstractMethod method = Methods.resolveMethod(type, functionName);

					if (method != null) {

						return getFunctionInfo(method);
					}

					logParameterError(caller, sources, "Type " + typeName + " does not have a method named " + functionName + ". Source:", ctx.isJavaScriptContext());
					return usage(ctx.isJavaScriptContext());

				} else {

					logParameterError(caller, sources, "Type " + typeName + " does not exist. Source:", ctx.isJavaScriptContext());
					return usage(ctx.isJavaScriptContext());
				}

			} else {

				logParameterError(caller, sources, "Expected zero or two arguments but got " + sources.length + ". Source:", ctx.isJavaScriptContext());
				return usage(ctx.isJavaScriptContext());
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${functionInfo([type, name])}. Example ${functionInfo()}"),
			Usage.javaScript("Usage: ${{ $.functionInfo([type, name]) }}. Example ${{ $.functionInfo() }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns information about the currently running Structr method, or about the method defined in the given type and name.";
	}

	@Override
	public String getLongDescription() {
		return """
				The function returns an object with the following structure.

				| Key                   | Type    | Description                                                                                                                   |
				|-----------------------|---------|-------------------------------------------------------------------------------------------------------------------------------|
				| name                  | String  | name of the method                                                                                                            |
				| declaringTrait        | String  | name of the type the method is declared on (`null` if if `isUserDefinedFunction === true`)                                    |
				| isUserDefinedFunction | boolean | `true` if the method is not a type- or service class method, `false` otherwise                                                |
				| isStatic              | boolean | `true` if the method can be called statically, `false` if it can only be called in an object context                          |
				| isPrivate             | boolean | `true` if the method can only be called via scripting, `false` if it can be called via REST as well                           |
				| httpVerb              | String  | The HTTP verb this function can be called with (only present if `isPrivate === false`)                                        |
				| summary               | String  | summary as defined in OpenAPI (only present if summary is defined)                                                            |
				| description           | String  | description as defined in OpenAPI (only present if description is defined)                                                    |
				| parameters            | object  | key-value map of parameters as defined in OpenAPI (key = name, value = type) (only present if OpenAPI parameters are defined) |
				""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.optional("type", "type name"),
				Parameter.optional("name", "function name")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
				{
					let info = $.functionInfo();

					$.log(`[${info.declaringClass}][${info.name}] task started...`);

					// ...

					$.log(`[${info.declaringClass}][${info.name}] task finished...`);
				}
				""", "Add function information to log output")
		);
	}

	// ----- private methods -----
	private Map<String, Object> getFunctionInfo(final AbstractMethod method) {

		final Map<String, Object> info = new LinkedHashMap<>();

		info.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,        method.getName());
		info.put(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY,   method.isPrivate());
		info.put(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY,    method.isStatic());

		if (!method.isPrivate()) {
			info.put(SchemaMethodTraitDefinition.HTTP_VERB_PROPERTY, method.getHttpVerb());
		}

		if (method.getSummary() != null) {
			info.put(SchemaMethodTraitDefinition.SUMMARY_PROPERTY, method.getSummary());
		}

		if (method.getDescription() != null) {
			info.put(SchemaMethodTraitDefinition.DESCRIPTION_PROPERTY, method.getDescription());
		}

		if (!method.getParameters().isEmpty()) {
			info.put(SchemaMethodTraitDefinition.PARAMETERS_PROPERTY, method.getParameters());
		}

		if (method instanceof ScriptMethod sm) {
			if (sm.getDeclaringClass() != null) {
				info.put(FunctionInfoFunction.DECLARING_TRAIT_KEY, sm.getDeclaringClass());
				info.put(FunctionInfoFunction.IS_USER_DEFINED_FUNCTION_KEY, false);
			} else {
				info.put(FunctionInfoFunction.DECLARING_TRAIT_KEY, null);
				info.put(FunctionInfoFunction.IS_USER_DEFINED_FUNCTION_KEY, true);
			}
		}

		return info;
	}
}
