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

import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.SchemaMethod;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;

/**
 */
public class ScriptMethod extends AbstractMethod {

	private GraphObject entity  = null;
	private SchemaMethod method = null;

	public ScriptMethod(final SchemaMethod method, final GraphObject entity) {

		super(method.getName());

		this.method = method;
		this.entity = entity;
	}

	@Override
	public boolean isStatic() {
		return method.isStaticMethod();
	}

	/*
	@Override
	public MethodCall createCall() throws FrameworkException {

		final List<SchemaMethodParameter> declaredParameters = new LinkedList<>();
		final Map<String, Object> converted                  = new LinkedHashMap<>();

		// "Method xy in class/interface ABC cannot be applied to given types. Required: ..., found: ..."

		//FIXME: how can we implement method parameters here, via REST and via scripting?


		declaredParameters.addAll(Iterables.toList(method.getParameters()));

		Collections.sort(declaredParameters, (a, b) -> {
			return a.getProperty(SchemaMethodParameter.index).compareTo(b.getProperty(SchemaMethodParameter.index));
		});

		for (final SchemaMethodParameter param : declaredParameters) {

			final String parameterName = param.getName();
			final String inputValue    = restParameters.get(parameterName);
			final String type          = param.getParameterType();

			// handle type
			final Object convertedValue = inputValue;

			converted.put(parameterName, convertedValue);
		}

		return createCall(converted);
	}
	*/

	@Override
	public Object execute(final SecurityContext securityContext, final Map<String, Object> arguments, final EvaluationHints hints) throws FrameworkException {

		try {

			final String methodName = method.getName();
			final String codeSource = method.getUuid();
			final String source     = method.getProperty("source");

			return Actions.execute(securityContext, entity, "${" + source.trim() + "}", arguments, methodName, codeSource);

		} catch (AssertException e)   {
			throw new FrameworkException(e.getStatus(), e.getMessage());
		}
	}
}
