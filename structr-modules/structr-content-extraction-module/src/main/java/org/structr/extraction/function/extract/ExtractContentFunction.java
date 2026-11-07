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
import org.structr.extraction.api.document.ExtractedDocument;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.extraction.function.ResultMaps;
import org.structr.web.entity.File;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExtractContentFunction extends ContentExtractionFunction {

	public static final String OPTION_INCLUDE_BLOCKS = "includeBlocks";

	private static final Set<String> ACCEPTED = Set.of(OPTION_OCR, OPTION_OCR_LANGUAGES, OPTION_MAX_CHARACTERS, OPTION_INCLUDE_EMBEDDED, OPTION_INCLUDE_IMAGES, OPTION_INCLUDE_BLOCKS);

	@Override
	public String getName() {

		return NAMESPACE + ".extractContent";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final File file                   = fileArgument(sources, 0);
		final Map<String, Object> options = optionsArgument(sources, 1, ACCEPTED);
		final boolean includeBlocks       = booleanOption(options, OPTION_INCLUDE_BLOCKS, false);

		try {

			final ExtractedDocument document = ContentExtraction.extract(file, extractionOptions(options));

			return ResultMaps.toGraphObjectMap(document.toMap(includeBlocks));

		} catch (final ContentExtractionException ex) {

			throw extractionFailed(ex);
		}
	}

	@Override
	public String getShortDescription() {

		return "Extracts the text, structure and metadata of a file.";
	}

	@Override
	public String getLongDescription() {

		return """
			Extracts the content of a file (PDF, Office and OpenDocument formats, HTML, Markdown, plain text, e-mail and many more) and returns an object with \
			`text` (plain text), `markdown` (the document as Markdown, with headings, lists and tables), `metadata`, `language` and `pageCount`. \
			With the option `includeBlocks`, the result also contains `blocks`: the structural units of the document (headings, paragraphs, list items, tables, \
			code blocks, quotes) with their page, the path of section headings they belong to, the embedded document (archive entry, attachment) \
			they are in, and their position in `text`.

			If Tesseract is installed, images and scanned PDF pages are read with OCR (see the setting `contentextraction.ocr.mode`). The function does not \
			change the file or create any objects.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("file", "the File to extract"),
			Parameter.optional("options", """
				an object with any of: `ocr` (`auto`, `always` or `off`, defaults to the setting `contentextraction.ocr.mode`), `ocrLanguages` (Tesseract \
				languages joined with `+`, e.g. `eng+deu`), `maxCharacters` (text beyond is dropped), `includeEmbedded` (also extract attachments and \
				archive entries), `includeImages` (also read images inside the document with OCR) and `includeBlocks` (add the block \
				list to the result)""")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${ documents.extractContent(first(find('File', 'name', 'manual.pdf'))).text }", "The plain text of a file"),
			Example.javaScript("""
				${{
				    let file = $.first($.find('File', { name: 'scan.pdf' }));

				    // read scanned pages with German and English language data
				    let content = $.documents.extractContent(file, { ocr: 'always', ocrLanguages: 'deu+eng', includeBlocks: true });

				    // the section headings of the document
				    return content.blocks.filter(b => b.type === 'heading').map(b => b.text);
				}}
				""", "Headings of a scanned document")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Files larger than the setting `contentextraction.extraction.maxfilesize` (in MB) are rejected.",
			"`metadata.ocrApplied` tells whether OCR contributed text; `metadata.ocrSkippedReason` tells why OCR did not run (`mode-off`, `tesseract-not-installed`, `ocr-languages-not-installed`, `maxpages-exceeded`)."
		);
	}
}
