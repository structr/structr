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
package org.structr.extraction.function.extract;

import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.document.EmbeddedDocument;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExtractEmbeddedFunction extends ContentExtractionFunction {

	public static final String OPTION_INCLUDE_TEXT = "includeText";

	private static final Set<String> ACCEPTED = Set.of(OPTION_MAX_DEPTH, OPTION_INCLUDE_TEXT, OPTION_OCR, OPTION_OCR_LANGUAGES, OPTION_MAX_CHARACTERS);

	@Override
	public String getName() {

		return NAMESPACE + ".extractEmbedded";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final File file                   = fileArgument(sources, 0);
		final Map<String, Object> options = optionsArgument(sources, 1, ACCEPTED);
		final boolean includeText         = booleanOption(options, OPTION_INCLUDE_TEXT, true);

		try {

			final List<EmbeddedDocument> documents = ContentExtraction.extractEmbedded(file, extractionOptions(options), includeText);

			return toGraphObjectMaps(documents.stream().map(EmbeddedDocument::toMap).toList());

		} catch (final ContentExtractionException ex) {

			throw extractionFailed(ex);
		}
	}

	@Override
	public String getShortDescription() {

		return "Lists the documents embedded in a file, such as e-mail attachments and archive entries, with their text.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns the documents embedded in a file (attachments of an e-mail, entries of a ZIP archive, documents embedded in an office file) as a \
			list of objects with `name`, `path` (the location inside the container, e.g. `/invoice.pdf`), `mimeType`, `depth` and `text`. Nested \
			containers are read recursively. The function does not create any files.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("file", "the container File"),
			Parameter.optional("options", """
				an object with any of: `maxDepth` (how deep nested containers are read, default 10), `includeText` (extract the text of each document, \
				default true), `ocr`, `ocrLanguages` and `maxCharacters` (per document)""")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.javaScript("""
			${{
			    let mail = $.first($.find('File', { name: 'order.eml' }));

			    // names of all PDF attachments
			    return $.documents.extractEmbedded(mail, { includeText: false })
			        .filter(d => d.mimeType === 'application/pdf')
			        .map(d => d.name);
			}}
			""", "PDF attachments of an e-mail"));
	}
}
