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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class RandomUUIDFunction extends CoreFunction {

	@Override
	public String getName() {
		return "randomUuid";
	}

	@Override
	public List<Signature> getSignatures() {
		// empty signature, no parameters
		return Signature.forAllScriptingLanguages("");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {
		return NodeServiceCommand.getNextUuid();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${randomUuid()}."),
			Usage.javaScript("Usage: ${{ $.randomUuid() }}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a new random UUID (v4).";
	}

	@Override
	public String getLongDescription() {
		return """
			The output format depends on `application.uuid.allowedformats`:

			- `without_dashes` (default): 32 lowercase hex characters `[0-9a-f]{32}`

			- `with_dashes`: 36 lowercase hex characters including dashes `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`

			- `both`: Output format depends on `application.uuid.createcompact`:
			    - `true` (default): same as `without_dashes`
			    - `false`: same as `with_dashes`
			""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${random_uuid()}"),
				Example.javaScript("""
						${{
						    const newId = $.randomUuid();
						    return newId;
						}}
						""")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.System;
	}
}
