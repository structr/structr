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
import org.structr.docs.*;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class TypeInfoFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "typeInfo";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("type [, view]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String typeName = sources[0].toString();
			final String viewName = (sources.length == 2 ? sources[1].toString() : null);

			return SchemaHelper.getSchemaTypeInfo(ctx.getSecurityContext(), typeName, viewName);

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Documentable> getContextHints(String lastToken) {
		return getContextHintsForTypes(lastToken);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${typeInfo(type[, view])}."),
			Usage.javaScript("Usage: ${$.typeInfo(type[, view])}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the type information for the specified type.";
	}

	@Override
	public String getLongDescription() {
		return """
		If called with a view, all properties of that view are returned as a list. The items of the list are in the same 
		format as `property_info()` returns. This is identical to the result one would get from `/structr/rest/_schema/<type>/<view>`.
		If called without a view, the complete type information is returned as an object. 
		This is identical to the result one would get from `/structr/rest/_schema/<type>`.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${typeInfo('User', 'public')}"),
				Example.javaScript("${{ $.typeInfo('User', 'public') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("type", "schema type"),
				Parameter.optional("view", "view (default: `public`)")
				);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Schema;
	}
}
