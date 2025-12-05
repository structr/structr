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

public class ClearHeadersFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "clearHeaders";
	}

	@Override
	public List<Signature> getSignatures() {
		// empty signature, no parameters
		return Signature.forAllScriptingLanguages("");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources == null || sources.length == 0) {

			ctx.clearHeaders();
			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${clearHeaders()}. Example: ${clearHeaders()}"),
			Usage.javaScript("Usage: ${{ $.clearHeaders()}}. Example: ${{ $.clearHeaders()}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Clears headers for the next HTTP request.";
	}

	@Override
	public String getLongDescription() {
		return """
		Removes all headers previously set with `addHeader()` in the same request. This function is a helper for the HTTP request functions that make HTTP requests **from within the Structr Server**, triggered by a frontend control like a button etc.
		""";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This is important if multiple calls to the family of HTTP functions is made in the same request to clear the headers in between usages to prevent sending the wrong headers in subsequent requests."
		);
	}
}
