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
package org.structr.core.api;

import org.structr.common.SecurityContext;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;

/**
 */
public class ScriptMethod extends AbstractMethod {

	private Parameters parameters  = null;
	private SchemaMethod method    = null;
	private String source          = null;

	public ScriptMethod(final SchemaMethod method) {

		super(method.getName(), method.getProperty(SchemaMethod.summary), method.getProperty(SchemaMethod.description));

		this.parameters = Parameters.fromSchemaMethod(method);
		this.source     = method.getProperty(SchemaMethod.source);
		this.method     = method;
	}

	@Override
	public String toString() {
		return method.getName() + "(" + parameters.toString() + ")";
	}

	@Override
	public boolean isPrivate() {
		return method.isPrivateMethod();
	}

	@Override
	public boolean isStatic() {
		return method.isStaticMethod();
	}

	@Override
	public Parameters getParameters() {
		return parameters;
	}

	@Override
	public String getFullMethodName() {

		final AbstractSchemaNode declaringClass = method.getProperty(SchemaMethod.schemaNode);
		if (declaringClass == null) {

			return "user-defined function " + method.getName();

		} else {

			return "method " + declaringClass.getName() + "." + method.getName();
		}
	}

	@Override
	public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

		final Arguments converted = checkAndConvertArguments(securityContext, arguments, false);

		try {

			// the next statement calls Arguments#toMap() which should
			return Actions.execute(securityContext, entity, "${" + source.trim() + "}", converted.toMap(), name, method.getUuid());

		} catch (AssertException e)   {
			throw new FrameworkException(e.getStatus(), e.getMessage());
		} catch (IllegalArgumentTypeException iatx) {
			throwIllegalArgumentExceptionForMapBasedArguments();
		}

		return null;
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
	*/
}
