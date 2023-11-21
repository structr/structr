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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.rest.api.RESTCall;
import org.structr.schema.action.EvaluationHints;

/**
 */
public class ReflectiveMethod extends MethodSignature {

	private static final Logger logger = LoggerFactory.getLogger(ReflectiveMethod.class);

	private GraphObject entity = null;
	private Method method      = null;

	public ReflectiveMethod(final Method method, final GraphObject entity) {

		super(method.getName());

		this.method = method;
		this.entity = entity;
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(method.getModifiers());
	}

	@Override
	public MethodCall createCall(final RESTCall parameters) throws FrameworkException {
		return new ReflectiveMethodCall(null);
	}

	@Override
	public MethodCall createCall(final Map<String, Object> parameters) throws FrameworkException {
		// this method is called from Java code so the parameters do not need to be converted
		return new ReflectiveMethodCall(parameters);
	}

	@Override
	public MethodCall createCall(final Object[] arguments) throws FrameworkException {
		// this method is called from within the scripting engine so the parameters do not need to be converted
		return new ReflectiveMethodCall(null);
	}

	private class ReflectiveMethodCall implements MethodCall {

		private Map<String, Object> propertySet = null;

		public ReflectiveMethodCall(final Map<String, Object> propertySet) {
			this.propertySet = propertySet;
		}

		@Override
		public Object execute(final SecurityContext securityContext, final EvaluationHints hints) {

			final Object[] args = { securityContext };

			try {
				return method.invoke(entity, args);

			} catch (IllegalArgumentException ex) {

				throw new RuntimeException(new FrameworkException(422, "Tried to call method " + method.getName() + " with invalid parameters. SchemaMethods expect their parameters to be passed as an object."));

			} catch (IllegalAccessException ex) {

				logger.error("Unexpected exception while trying to get GraphObject member.", ex);

			} catch (InvocationTargetException ex) {

				if (ex.getTargetException() instanceof FrameworkException) {

					throw new RuntimeException(ex.getTargetException());

				} else if (ex.getTargetException() instanceof AssertException) {

					throw ((AssertException)ex.getTargetException());
				}

				logger.error("Unexpected exception while trying to get GraphObject member.", ex);
			}

			return null;
		}
	}
}
