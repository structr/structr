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
package org.structr.core.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.List;

/**
 *
 */
public class SetPrivilegedFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "setPrivileged";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("entity, parameterMap");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		synchronized (ctx) {

			final SecurityContext previousSecurityContext = ctx.getSecurityContext();
			ctx.setSecurityContext(SecurityContext.getSuperUserInstance());

			try {

				final SetFunction set = new SetFunction();
				set.apply(ctx, caller, sources);

			} finally {

				ctx.setSecurityContext(previousSecurityContext);
			}
		}

		return "";
	}


	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${setPrivileged(entity, propertyKey, value)}."),
			Usage.javaScript("Usage: ${{$.setPrivileged(entity, propertyKey, value)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets the given key/value pair(s) on the given entity with super-user privileges.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}



	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${ setPrivileged(page, 'accessCount', '2')}"),
				Example.javaScript("${{ $.setPrivileged($.page, 'accessCount', '2')} }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("entity", "URL to connect to"),
				Parameter.mandatory("map", "parameterMap (only JavaScript)"),
				Parameter.mandatory("key1", "key1 (only StructrScript)"),
				Parameter.mandatory("value1", "value for key1 (only StructrScript)"),
				Parameter.mandatory("key2", "key2 (only JavaScript)"),
				Parameter.mandatory("value2", "value for key1 (only StructrScript)"),
				Parameter.mandatory("keyN", "keyN (only JavaScript)"),
				Parameter.mandatory("valueN", "value for keyN (only StructrScript)")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.",
				"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."
		);
	}
}
