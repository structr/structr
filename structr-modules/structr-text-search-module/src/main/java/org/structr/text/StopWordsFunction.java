/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.common.fulltext.ContentAnalyzer;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class StopWordsFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE    = "Usage: ${stop_words(language)}. Example: ${stop_words(\"de\")}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.stopWords(language)}}. Example: ${{Structr.stopWords(\"de\")}}";

	@Override
	public String getName() {
		return "stop_words";
	}

	@Override
	public String getSignature() {
		return "language";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final ContentAnalyzer contentAnalyzer = StructrApp.getInstance(ctx.getSecurityContext()).getContentAnalyzer();
			if (contentAnalyzer != null) {

				return contentAnalyzer.getStopWords(sources[0].toString());
			}

			return "";

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String shortDescription() {
		return "Returns a list of words (for the given language) which can be ignored for NLP purposes";
	}

	@Override
	public String getRequiredModule() {
		return "text-search";
	}
}
