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
import org.structr.core.function.SearchFunction;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.Collection;
import java.util.List;

public class FindAnyFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "find.any";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return "predicate.any";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final Object value = sources[0];

			if (value instanceof Collection collection) {
				return new AnyPredicate(collection);
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
			Usage.javaScript("Usage: ${{ $.predicate.equals(key, value). Example: ${{ $.find('Group', $.predicate.and($.predicate.equals('name', 'Test'))) }}"),
			Usage.structrScript("Usage: ${equals(key, value). Example: ${find('Group', and(equals('name', 'Test')))}")
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
		return Signature.forAllScriptingLanguages("value");
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Predicate;
	}
}
