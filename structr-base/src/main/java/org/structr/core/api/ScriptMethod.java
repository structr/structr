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
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.script.Scripting;
import org.structr.core.script.Snippet;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;

/**
 */
public class ScriptMethod extends AbstractMethod {

	private Parameters parameters               = null;
	private String source                       = null;
	private String uuid                         = null;
	private String name                         = null;
	private String fullName                     = null;
	private AbstractSchemaNode declaringClass   = null;
	private boolean isPrivateMethod             = false;
	private boolean isStaticMethod              = false;
	private boolean returnRawResult             = false;
	private boolean wrapJsInMain                = true;
	private String httpVerb                     = null;

	public ScriptMethod(final SchemaMethod method) {

		super(method.getName(), method.getSummary(), method.getDescription());

		this.parameters      = Parameters.fromSchemaMethod(method);
		this.source          = method.getSource();
		this.uuid            = method.getUuid();
		this.name            = method.getName();
		this.isPrivateMethod = method.isPrivateMethod();
		this.isStaticMethod  = method.isStaticMethod();
		this.returnRawResult = method.returnRawResult();
		this.httpVerb        = method.getHttpVerb();
		this.wrapJsInMain    = method.wrapJsInMain();

		final AbstractSchemaNode declaringClass = method.getSchemaNode();
		if (declaringClass == null) {

			fullName = "user-defined function ‛" + name + "‛";

		} else {

			this.declaringClass = declaringClass;
			fullName = "method ‛" + declaringClass.getName() + "." + name + "‛";
		}
	}

	public AbstractSchemaNode getDeclaringClass() {
		return this.declaringClass;
	}

	public String getRawSource() {
		return this.source;
	}

	@Override
	public String toString() {
		return name + "(" + parameters.toString() + "): " + getSnippet().getSource();
	}

	@Override
	public boolean isPrivate() {
		return isPrivateMethod;
	}

	@Override
	public boolean isStatic() {
		return isStaticMethod;
	}

	@Override
	public Snippet getSnippet() {

		Snippet snippet = null;

		if (source != null) {

			final String[] splitSource = Scripting.splitSnippetIntoEngineAndScript(source.trim());

			if ("js".equals(splitSource[0])) {

				snippet = new Snippet(name, splitSource[1], this.wrapJsInMain);
			} else {

				snippet = new Snippet(name, splitSource[1], false);
			}

			snippet.setEngineName(splitSource[0]);
		}

		return snippet;
	}

	@Override
	public String getHttpVerb() {
		return httpVerb;
	}

	@Override
	public Parameters getParameters() {
		return parameters;
	}

	@Override
	public String getFullMethodName() {
		return fullName;
	}

	@Override
	public boolean shouldReturnRawResult() {
		return this.returnRawResult;
	}

	@Override
	public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

		final Arguments converted = checkAndConvertArguments(securityContext, arguments, false);

		try {

			final ScriptConfig scriptConfig = ScriptConfig.builder()
					.wrapJsInMain(this.wrapJsInMain)
					.currentMethod(this)
					.build();

			return Actions.execute(securityContext, entity, "${" + source.trim() + "}", converted.toMap(), name, uuid, scriptConfig);

		} catch (AssertException e)   {
			throw new FrameworkException(e.getStatus(), e.getMessage());
		} catch (IllegalArgumentTypeException iatx) {
			throwIllegalArgumentExceptionForMapBasedArguments();
		}

		return null;
	}
}
