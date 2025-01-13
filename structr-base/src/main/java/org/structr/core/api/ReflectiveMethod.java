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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.Snippet;
import org.structr.schema.action.EvaluationHints;

/**
 */
public class ReflectiveMethod extends AbstractMethod {

	private static final Logger logger = LoggerFactory.getLogger(ReflectiveMethod.class);

	private Parameters parameters = null;
	private Method method         = null;

	public ReflectiveMethod(final Method method) {

		super(method.getName(), null, null);

		this.parameters = Parameters.fromMethod(method);
		this.method     = method;
	}

	@Override
	public String toString() {
		return method.getName() + "(" + parameters.toString() + ")";
	}

	@Override
	public boolean isPrivate() {
		return Modifier.isPrivate(method.getModifiers());
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(method.getModifiers());
	}

	@Override
	public Snippet getSnippet() {
		return null;
	}

	@Override
	public String getHttpVerb() {
		return "POST";
	}

	@Override
	public Parameters getParameters() {
		return parameters;
	}

	@Override
	public String getFullMethodName() {
		return "method ‛" + method.getDeclaringClass().getSimpleName() + "." + method.getName() + "‛";
	}

	@Override
	public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

		final Arguments converted = checkAndConvertArguments(securityContext, arguments, true);

		try {

			return method.invoke(entity, converted.toArray());

		} catch (IllegalArgumentException ex) {

			throwIllegalArgumentExceptionForUnnamedArguments(parameters, converted);

		} catch (IllegalAccessException ex) {

			ex.printStackTrace();

			logger.error("Unexpected exception while trying to execute method " + getName() + ".", ex);

		} catch (InvocationTargetException ex) {

			ex.printStackTrace();

			if (ex.getTargetException() instanceof FrameworkException fex) {

				throw fex;

			} else if (ex.getTargetException() instanceof AssertException aex) {

				throw aex;
			}

			logger.error("Unexpected exception while trying to execute method " + getName() + ".", ex);
		}

		return null;
	}
}
