/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;

/**
 * Base class for parameters that can be defined by Method implementations.
 */
public class Parameters {

	private final List<Parameter> parameters = new LinkedList<>();

	public void put(final String name, final String type) {
		parameters.add(new Parameter(name, type));
	}

	public static Parameters fromMethod(final Method method) {

		final Parameters parameters = new Parameters();

		for (final java.lang.reflect.Parameter p : method.getParameters()) {

			parameters.put(p.getName(), p.getType().getSimpleName());
		}

		return parameters;
	}

	public static Parameters fromSchemaMethod(final SchemaMethod method) {

		final Parameters parameters = new Parameters();

		for (final SchemaMethodParameter p : method.getParameters()) {

			parameters.put(p.getName(), p.getParameterType());
		}

		return parameters;
	}

	private class Parameter {

		private String name = null;
		private String type = null;

		public Parameter(final String name, final String type) {

			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}
	}

	/*

		There are some instances of exported methods that must be called with non-map parameters.
		We need to support this, but we should warn about it.

		Example:

		type.addMethod("sendMessage")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("topic", String.class.getName())
			.addParameter("message", String.class.getName())
			.setSource("return " + MessageClient.class.getName() + ".sendMessage(this, topic, message, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);
	*/

	/*

	protected Map<String, Object> convertArguments(final Map<String, Object> restInput) throws FrameworkException {

		final Map<String, Object> convertedArguments = new LinkedHashMap<>();
		final Map<String, String> declaredParameters = method.getParameters();

		for (final String name : restInput.keySet()) {

			final String type  = declaredParameters.get(name);
			final Object input = restInput.get(name);

			convertedArguments.put(name, convert(input, type));
		}

		return convertedArguments;
	}

	private Object convert(final Object input, final String type) {

		// TODO: implement conversion...
		System.out.println("RESTMethodCallHandler: NOT converting " + input + " to " + type + ", implementation missing.");

		return input;
	}
	*/
}
