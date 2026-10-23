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
import org.structr.docs.Usage;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class CountTokensFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".countTokens";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("text [, encoding ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final String text     = textArgument(sources, 0);
		final String encoding = sources.length > 1 ? textArgument(sources, 1) : ContentExtractionSettings.TokenEncoding.getValue();

		try {

			return ContentExtraction.countTokens(text, encoding);

		} catch (final IllegalArgumentException ex) {

			throw new FrameworkException(422, getName() + "(): " + ex.getMessage());
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${ documents.countTokens(text [, encoding ]) }"), Usage.javaScript("Usage: ${{ $.documents.countTokens(text [, encoding ]); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Counts the tokens of a text, as a language or embedding model sees it.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns the number of tokens of a text in one of OpenAI's tiktoken encodings. The count is exact for OpenAI models (`cl100k_base` for \
			GPT-4 and the text-embedding-3 models, `o200k_base` for GPT-4o and later) and a close approximation for other models, whose tokenizers \
			differ. Useful for checking that a prompt or a chunk fits a model's limit.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("text", "the text"), Parameter.optional("encoding", "the encoding, defaults to the setting `contentextraction.split.tokenencoding`"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ documents.countTokens('Hello world') }", "Returns 2"));
	}
}
