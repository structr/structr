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
package org.structr.web.function;

import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class AddHeaderFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "add_header";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("name, value");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length == 2) {

			if (sources[0] != null) {

				final String name = sources[0].toString();

				if (sources[1] == null) {
					ctx.removeHeader(name);

				} else {

					final String value = sources[1].toString();

					ctx.addHeader(name, value);
				}
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${add_header(field, value)}. Example: ${add_header('X-User', 'johndoe')}"),
			Usage.javaScript("Usage: ${{ $.addHeader(field, value)}}. Example: ${{ $.addHeader('X-User', 'johndoe')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Adds the given header field and value to the next request";
	}
}
