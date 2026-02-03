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
package org.structr.web.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class StripHtmlFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "stripHtml";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("html");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			return sources[0].toString().replaceAll("\\<.*?>", "");

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${stripHtml(html)}."),
			Usage.javaScript("Usage: ${{$.stripHtml(html)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Removes all HTML tags from the given source string and returns only the content.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${stripHtml('<h3><p>Clean me!</p></h3>')}"),
				Example.javaScript("${{ $.stripHtml('<h3><p>Clean me!</p></h3>') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("source", "HTML string for content extraction")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"Similar results can be produced by `strReplace(source, \"\\\\<[a-zA-Z].*?>\", \"\")`"
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.String;
	}
}
