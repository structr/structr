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
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Set;

public class StopWordsFunction extends ContentExtractionFunction {

	@Override
	public String getName() {

		return NAMESPACE + ".stopWords";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("language");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		// returned as a list, since scripts handle arrays better than sets
		final Set<String> words = ContentExtraction.stopWords(textArgument(sources, 0));

		return List.copyOf(words);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${ documents.stopWords(language) }"), Usage.javaScript("Usage: ${{ $.documents.stopWords(language); }}"));
	}

	@Override
	public String getShortDescription() {

		return "Returns the stop words of a language.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns the stop words of a language as a list of lower-case words: frequent words such as "the", "and" or "is" that carry little \
			meaning on their own and are usually left out when building keyword lists or tag clouds. Lists exist for about 30 languages \
			(ar, bg, br, ca, cs/cz, da, de, el, en, es, fa, fi, fr, hi, hu, id, it, ja, lv, nl, no, pl, pt, ro, ru, sk, sv, tr, zh); for any \
			other language the result is empty.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("language", "a two-letter language code, e.g. `en` or `de`; longer tags such as `en-US` use their first two letters"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.javaScript("""
			${{
			    let stopWords = $.documents.stopWords('en');

			    return 'the quick brown fox'.split(' ').filter(word => !stopWords.includes(word));
			}}
			""", "Removes the English stop words from a text"));
	}

	@Override
	public List<String> getNotes() {

		return List.of("Replaces the function `stopWords()` of the removed text-search module.");
	}
}
