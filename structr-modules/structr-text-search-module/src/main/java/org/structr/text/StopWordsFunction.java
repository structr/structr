/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.text;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.List;
import java.util.Set;

public class StopWordsFunction extends Function<Object, Object> {

	@Override
	public String getName() {
		return "stopWords";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("language");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final TextSearchModule module = (TextSearchModule)StructrApp.getConfiguration().getModules().get("text-search");
			if (module != null) {

				return module.getStopWords(sources[0].toString());
			}

			return Set.of();

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${stopWords(language)}. Example: ${stopWords(\"de\")}"),
			Usage.javaScript("Usage: ${{Structr.stopWords(language)}}. Example: ${{Structr.stopWords(\"de\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a list of words (for the given language) which can be ignored for NLP purposes.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public String getRequiredModule() {
		return "text-search";
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.String;
	}
}
