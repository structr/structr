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
import org.structr.extraction.api.document.ExtractedFigureData;
import org.structr.extraction.api.options.FigureOptions;
import org.structr.extraction.function.ContentExtractionFunction;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.File;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExtractFiguresFunction extends ContentExtractionFunction {

	public static final String OPTION_DPI                = "dpi";
	public static final String OPTION_MAX_PAGES          = "maxPages";
	public static final String OPTION_MAX_FIGURES        = "maxFigures";
	public static final String OPTION_MIN_FIGURE_SIZE    = "minFigureSize";
	public static final String OPTION_INCLUDE_TABLES     = "includeTables";
	public static final String OPTION_INCLUDE_IMAGE_DATA = "includeImageData";

	private static final Set<String> ACCEPTED = Set.of(OPTION_DPI, OPTION_MAX_PAGES, OPTION_MAX_FIGURES, OPTION_MIN_FIGURE_SIZE, OPTION_INCLUDE_TABLES, OPTION_INCLUDE_IMAGE_DATA);

	/** Rendering beyond this resolution only costs memory. */
	private static final int MAX_DPI = 600;

	@Override
	public String getName() {

		return NAMESPACE + ".extractFigures";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("file [, options ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final File file                   = fileArgument(sources, 0);
		final Map<String, Object> options = optionsArgument(sources, 1, ACCEPTED);
		final boolean includeImageData    = booleanOption(options, OPTION_INCLUDE_IMAGE_DATA, true);

		try {

			final List<ExtractedFigureData> figures = ContentExtraction.extractFigures(file, figureOptions(options));

			return toGraphObjectMaps(figures.stream().map(figure -> figure.toMap(includeImageData)).toList());

		} catch (final ContentExtractionException ex) {

			throw extractionFailed(ex);
		}
	}

	@Override
	public String getShortDescription() {

		return "Finds the figures of a file (charts, diagrams, tables with a caption, pictures) and returns them as images with their captions.";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns the figures of a file in reading order, as a list of objects with `page`, `position`, `label` (e.g. `Figure 3`), `caption`, \
			`source`, `bbox`, `width`, `height`, `imageHash` (SHA-256 of the image), `mentions`, `text` and `image`.

			In a PDF, a figure is a region of a page where something is drawn: an image, or a chart drawn as vector graphics. The text in it \
			(axis labels, legends, table cells) belongs to it, and a caption below it (for tables: above it) gives it its label. Each region is \
			rendered from the page as a PNG image (`source` is `region`, `bbox` is x, y, width and height in points from the top left corner of \
			the page). A caption whose figure cannot be told apart from the page, e.g. a table without rules, stands for the whole page \
			(`source` is `page`). `mentions` are the sentences of the text that refer to the label, e.g. "As Fig. 3 shows, ...".

			In other documents (Word, PowerPoint, e-mails, ...), the figures are the pictures in the document (`source` is `embedded`), without \
			caption. An image file is a figure itself (`source` is `file`).

			`image` is an object `{ data, mediaType }` with the PNG as base64, the form `llmPromptChat()` accepts as an attachment. `text` is \
			what the figure says in words: its caption and the text inside it.

			The function does not store anything. To store the figures of documents, check Extract Figures in the content extraction \
			configuration of their folder (or pass `extractFigures: true` to `documents.processContent()`): they are then kept as \
			`DocumentFigure`s, with their text in the searchable content of the document. A single figure is stored with \
			`$.create('DocumentFigure', figure)`, since the keys are the property names of `DocumentFigure`.""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("file", "the File"),
			Parameter.optional("options", """
				an object with any of: `dpi` (resolution of the rendered figures, default `contentextraction.figures.dpi`), `maxPages` (PDF pages \
				searched, default `contentextraction.figures.maxpages`), `maxFigures` (default `contentextraction.figures.maxfigures`), \
				`minFigureSize` (minimum width and height of a figure without caption, in points or pixels, default \
				`contentextraction.figures.minsize`), `includeTables` (count captioned tables as figures, default true) and `includeImageData` \
				(return the images, default true)""")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
				${{
				    let paper = $.first($.find('File', { name: 'paper.pdf' }));

				    // labels and captions only
				    return $.documents.extractFigures(paper, { includeImageData: false }).map(f => f.label + ': ' + f.caption);
				}}
				""", "The captions of the figures of a paper"),
			Example.javaScript("""
				${{
				    let paper  = $.first($.find('File', { name: 'paper.pdf' }));
				    let figure = $.documents.extractFigures(paper).find(f => f.label === 'Figure 2');
				    let config = $.first($.find('LLMProviderConfiguration', { name: 'default' }));
				    let chat   = $.llmStartChat(config, 'claude-sonnet-5');

				    return $.llmPromptChat(config, chat, 'What does this chart show? Caption: ' + figure.caption, figure.image);
				}}
				""", "Ask a vision model about a figure (Structr Enterprise)")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Charts in PDFs are found whether they are embedded pictures or vector graphics, since figures are rendered from the page.",
			"Figures without a caption are kept if they are at least `minFigureSize` wide and high and contain a picture or many drawn elements. Logos and decorations at the same place on several pages are left out."
		);
	}

	// ----- private methods -----
	private FigureOptions figureOptions(final Map<String, Object> options) throws FrameworkException {

		final FigureOptions result = FigureOptions.fromSettings();

		if (options.containsKey(OPTION_DPI)) {

			final int dpi = positiveIntOption(options, OPTION_DPI);
			if (dpi < 36 || dpi > MAX_DPI) {

				throw new FrameworkException(422, getDisplayName(false) + ": option '" + OPTION_DPI + "' must be between 36 and " + MAX_DPI + ", got " + dpi + ".");
			}

			result.setDpi(dpi);
		}

		if (options.containsKey(OPTION_MAX_PAGES)) {

			result.setMaxPages(positiveIntOption(options, OPTION_MAX_PAGES));
		}

		if (options.containsKey(OPTION_MAX_FIGURES)) {

			result.setMaxFigures(positiveIntOption(options, OPTION_MAX_FIGURES));
		}

		if (options.containsKey(OPTION_MIN_FIGURE_SIZE)) {

			result.setMinFigureSize(nonNegativeIntOption(options, OPTION_MIN_FIGURE_SIZE));
		}

		result.setIncludeTables(booleanOption(options, OPTION_INCLUDE_TABLES, true));

		return result;
	}
}
