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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.SchemaMethod;
import org.structr.rest.api.RESTCall;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;

/**
 */
public class ScriptMethod extends MethodSignature {

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

	@Override
	public MethodCall createCall(final RESTCall parameters) throws FrameworkException {
		return new ScriptMethodCall(null);
	}

	@Override
	public MethodCall createCall(final Map<String, Object> parameters) throws FrameworkException {
		// this method is called from Java code so the parameters do not need to be converted
		return new ScriptMethodCall(parameters);
	}

	@Override
	public MethodCall createCall(final Object[] arguments) throws FrameworkException {
		return new ScriptMethodCall(null);
	}

	private class ScriptMethodCall implements MethodCall {

		private Map<String, Object> propertySet = null;

		public ScriptMethodCall(final Map<String, Object> propertySet) {
			this.propertySet = propertySet;
		}

		@Override
		public Object execute(final SecurityContext securityContext, final EvaluationHints hints) throws FrameworkException {

			final String methodName = method.getName();
			final String codeSource = method.getUuid();
			final String source     = method.getProperty("source");

			return Actions.execute(securityContext, entity, "${" + source.trim() + "}", propertySet, methodName, codeSource);
		}
	}
}
