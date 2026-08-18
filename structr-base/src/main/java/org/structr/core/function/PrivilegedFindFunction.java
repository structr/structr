/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class PrivilegedFindFunction extends AbstractQueryFunction {

	@Override
	public String getName() {

		return "findPrivileged";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("type, options...");
	}

	@Override
	public String getNamespaceIdentifier() {

		return "find";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		return applyInternal(ctx, securityContext, caller, sources, true);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("Usage: ${{$.findPrivileged(type, map)}. Example: ${{$.findPrivileged(\"User\", { eMail: 'tester@test.com' }); }}"),
			Usage.structrScript("Usage: ${findPrivileged(type, key, value)}. Example: ${findPrivileged(\"User\", \"email\", \"tester@test.com\"}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Executes a `find()` operation with elevated privileges.";
	}

	@Override
	public String getLongDescription() {

		return "You can use this function to query data from an anonymous context or when a users privileges need to be escalated. See documentation of `find()` for more details.";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("type", "type to return (includes inherited types"),
			Parameter.optional("predicates", "list of predicates"),
			Parameter.optional("uuid", "uuid, makes the function return **a single object**")
		);
	}

	@Override
	public List<Example> getExamples() {

		return super.getExamples();
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"It is recommended to use `find()` instead of `findPrivileged()` whenever possible, as improper use of `findPrivileged()` can result in the exposure of sensitive data.",
			"In a StructrScript environment parameters are passed as pairs of 'key1', 'value1'.",
			"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."
		);
	}
}