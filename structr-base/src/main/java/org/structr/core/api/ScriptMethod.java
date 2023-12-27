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

	private Parameters parameters  = null;
	private GraphObject entity     = null;
	private SchemaMethod method    = null;
	private String source          = null;

	public ScriptMethod(final SchemaMethod method, final GraphObject entity) {

		super(method.getName(), method.getProperty(SchemaMethod.summary), method.getProperty(SchemaMethod.description));

		this.parameters = Parameters.fromSchemaMethod(method);
		this.source     = method.getProperty(SchemaMethod.source);
		this.method     = method;
		this.entity     = entity;
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
	public Object execute(final SecurityContext securityContext, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

		checkAndConvertArguments(arguments);

		try {

			return Actions.execute(securityContext, entity, "${" + source.trim() + "}", arguments.toMap(), name, method.getUuid());

		} catch (AssertException e)   {
			throw new FrameworkException(e.getStatus(), e.getMessage());
		}
	}
}
