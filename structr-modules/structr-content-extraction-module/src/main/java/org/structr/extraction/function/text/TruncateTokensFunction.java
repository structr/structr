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
package org.structr.extraction.function.text;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class TruncateTokensFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".truncateTokens";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("text, maxTokens [, encoding ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final String text     = textArgument(sources, 0);
		final String encoding = sources.length > 2 ? textArgument(sources, 2) : ContentExtractionSettings.TokenEncoding.getValue();

		if (sources.length < 2 || !(sources[1] instanceof Number number) || number.doubleValue() < 0 || number.doubleValue() != Math.floor(number.doubleValue())) {

			throw new FrameworkException(422, getDisplayName(false) + ": expected the maximum number of tokens, a whole number of 0 or more, as argument 2.");
		}

		try {

			return ContentExtraction.truncateTokens(text, number.intValue(), encoding);

		} catch (final IllegalArgumentException ex) {

			throw new FrameworkException(422, getDisplayName(false) + ": " + ex.getMessage());
		}
	}

	@Override
	public String getShortDescription() {

		return "Shortens a text to a maximum number of tokens, at a sentence or word boundary.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns the text unchanged if it has at most `maxTokens` tokens. Otherwise it is cut after the last sentence that fits, or, if that \
			would drop more than half of what fits, after the last word. Useful for fitting context into a prompt budget.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("text", "the text"), Parameter.mandatory("maxTokens", "the maximum number of tokens"),
			Parameter.optional("encoding", "the encoding, defaults to the setting `contentextraction.split.tokenencoding`"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.javaScript("${{ $.documents.truncateTokens($.documents.extractContent(file).text, 2000); }}", "The beginning of a document, at most 2000 tokens"));
	}
}
