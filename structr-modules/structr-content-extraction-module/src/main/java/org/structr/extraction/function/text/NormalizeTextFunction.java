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
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.options.NormalizeOptions;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NormalizeTextFunction extends ContentExtractionFunction {

	private static final String UNICODE            = "unicode";
	private static final String CONTROL_CHARACTERS = "controlCharacters";
	private static final String HYPHENATION        = "hyphenation";
	private static final String WHITESPACE         = "whitespace";

	private static final Set<String> ACCEPTED = Set.of(UNICODE, CONTROL_CHARACTERS, HYPHENATION, WHITESPACE);

	@Override
	public String getName() {

		return NAMESPACE + ".normalizeText";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("text [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final String text                 = textArgument(sources, 0);
		final Map<String, Object> options = optionsArgument(sources, 1, ACCEPTED);
		final NormalizeOptions normalize  = new NormalizeOptions()
			.setUnicode(booleanOption(options, UNICODE, true))
			.setControlCharacters(booleanOption(options, CONTROL_CHARACTERS, true))
			.setHyphenation(booleanOption(options, HYPHENATION, true))
			.setWhitespace(booleanOption(options, WHITESPACE, true));

		return ContentExtraction.normalize(text, normalize);
	}

	@Override
	public String getShortDescription() {

		return "Cleans up extracted text: Unicode forms, control characters, hyphenation and whitespace.";
	}

	@Override
	public String getLongDescription() {

		return """
			Applies the cleanup steps that extracted text, especially from PDFs and scans, usually needs:
			- `unicode`: NFKC normalization, which replaces ligatures such as "ﬁ" and full-width letters with plain characters.
			- `controlCharacters`: removes control characters (except line breaks and tabs) and soft hyphens.
			- `hyphenation`: joins words hyphenated at a line break when the line continues in lower case ("extrac-\\ntion" becomes \
			"extraction", "Nord-\\nSüd" keeps its hyphen).
			- `whitespace`: collapses spaces and tabs, trims lines, and reduces several blank lines to one; paragraph breaks are kept.

			All steps are on by default and can be switched off in the options.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("text", "the text"), Parameter.optional("options", "an object to switch steps off, e.g. `{ hyphenation: false }`"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ documents.normalizeText('Content extrac-\\ntion   is   ﬁne.') }", "Returns \"Content extraction is fine.\""));
	}
}
