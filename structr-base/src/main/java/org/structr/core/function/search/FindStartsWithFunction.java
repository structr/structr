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

public class FindStartsWithFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "find.startsWith";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return "predicate.startsWith";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (sources.length == 2) {

				final String key      = sources[0].toString();
				final Object value    = sources[1];

				return new StartsWithPredicate(key, value);
			}

			if (sources.length == 1) {

				final Object value = sources[0];

				return new StartsWithPredicate(null, value);
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.startsWith(key, value) }}. Example: ${{ $.find('Group', $.predicate.and($.predicate.startsWith('name', 'Test'))) }}"),
			Usage.structrScript("Usage: ${startsWith(key, value). Example: ${find('Group', and(startsWith('name', 'Test')))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a query predicate that can be used with find() or search().";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("key, value");
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
