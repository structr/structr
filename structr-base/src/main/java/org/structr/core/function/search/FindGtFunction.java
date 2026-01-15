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
package org.structr.core.function.search;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindGtFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "find.gt";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return "predicate.gt";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null || sources.length > 1) {

				throw new IllegalArgumentException();
			}

			return new RangePredicate(sources[0], null, false, false);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.gt(value) }}. Example: ${{ $.find('User', { age: $.predicate.gt(42) }); }}"),
			Usage.structrScript("Usage: ${gt(value)}. Example: ${find('User', 'age', gt(42))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a gt predicate that can be used in find() function calls.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value");
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
