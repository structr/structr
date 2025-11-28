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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class StartsWithFunction extends CoreFunction {

	@Override
	public String getName() {
		return "startsWith";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("str, prefix");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources != null && sources.length == 2) {

				if (sources[0] != null && sources[0] instanceof Iterable) {

					final Iterable collection = (Iterable) sources[0];
					final Iterator it         = collection.iterator();

					return (it.hasNext() && it.next().equals(sources[1]));

				} else if (sources[0] != null && sources[0] instanceof Collection) {

					final Collection collection = (Collection) sources[0];
					return collection.size() > 0 && collection.iterator().next().equals(sources[1]);

				} else if (sources[0] != null && sources[0].getClass().isArray() && ((Object[]) sources[0]).length > 0) {

					return ((Object[]) sources[0])[0].equals(sources[1]);

				} else {

					final String searchString = String.valueOf(sources[0]);
					final String prefix       = String.valueOf(sources[1]);

					return StringUtils.startsWith(searchString, prefix);
				}

			} else {

				return usage(ctx.isJavaScriptContext());

			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.startsWith(string, prefix) }}. Example: ${{ $.startsWith(locale, \"de\") }}"),
			Usage.structrScript("Usage: ${startsWith(string, prefix)}. Example: ${startsWith(locale, \"de\")}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns true if the given string starts with the given prefix.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
