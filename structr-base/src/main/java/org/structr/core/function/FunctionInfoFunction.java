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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class FunctionInfoFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_FUNCTION_INFO = "Usage: ${function_info([type, name])}. Example ${function_info()}";
	public static final String ERROR_MESSAGE_FUNCTION_INFO_JS = "Usage: ${$.functionInfo([type, name])}. Example ${$.functionInfo()}";

	@Override
	public String getName() {
		return "function_info";
	}

	@Override
	public String getSignature() {
		return "[type, view]";
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
				final Class type          = SchemaHelper.getEntityClassForRawType(typeName);

				if (type != null) {

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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_FUNCTION_INFO_JS : ERROR_MESSAGE_FUNCTION_INFO);
	}

	@Override
	public String shortDescription() {
		return "Returns information about the currently running Structr method, OR about the method defined in the given type and name.";
	}

	// ----- private methods -----
	private Map<String, Object> getFunctionInfo(final AbstractMethod method) {

		final Map<String, Object> info = new LinkedHashMap<>();

		info.put("name",        method.getName());
		info.put("isPrivate",   method.isPrivate());
		info.put("isStatic",    method.isStatic());
		info.put("httpVerb",    method.getHttpVerb().name());

		if (method.getSummary() != null) {
			info.put("summary", method.getSummary());
		}

		if (method.getDescription() != null) {
			info.put("description", method.getDescription());
		}

		if (method.getParameters() != null) {
			info.put("parameters", method.getParameters());
		}

		return info;
	}
}
