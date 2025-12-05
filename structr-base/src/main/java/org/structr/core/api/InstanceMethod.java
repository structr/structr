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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.Snippet;
import org.structr.schema.action.EvaluationHints;

import java.util.Map;

public abstract class InstanceMethod extends AbstractMethod {

	private final Parameters parameters;
	private final String declaringTrait;

	public abstract Object execute(final SecurityContext securityContext, final GraphObject entity, final Map<String, Object> parameters) throws FrameworkException;

	public InstanceMethod(final String declaringTrait, final String name) {

		super(name, null, null);

		this.declaringTrait = declaringTrait;
		this.parameters     = new Parameters();
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public boolean isPrivate() {
		return false;
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
		return "method ‛" + declaringTrait + "." + name + "‛";
	}

	@Override
	public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
		return execute(securityContext, entity, arguments.toMap());
	}
}
