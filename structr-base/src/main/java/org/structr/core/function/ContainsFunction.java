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

import org.apache.commons.lang3.ArrayUtils;
import org.structr.api.util.Iterables;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ContainsFunction extends CoreFunction {

	@Override
	public String getName() {
		return "contains";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("stringOrList, wordOrObject");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			if (sources[0] instanceof String && sources[1] instanceof String) {

				final String source = sources[0].toString();
				final String part = sources[1].toString();

				return source.contains(part);

			} else if (sources[0] instanceof Iterable) {

				final Iterable iterable   = (Iterable)sources[0];
				final Set set             = Iterables.toSet(iterable);

				return set.contains(sources[1]);

			} else if (sources[0] instanceof Collection) {

				final Collection collection = (Collection)sources[0];
				return collection.contains(sources[1]);

			} else if (sources[0].getClass().isArray()) {

				return ArrayUtils.contains((Object[])sources[0], sources[1]);
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return false;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.contains(string, word)}} or ${{$.contains(collection, element)}}. Example: ${{$.contains($.this.name, \"the\")}} or ${{$.contains($.find('Page'), page)}}"),
			Usage.structrScript("Usage: ${contains(string, word)} or ${contains(collection, element)}. Example: ${contains(this.name, \"the\")} or ${contains(find('Page'), page)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns true if the given string or collection contains an element.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
