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
package org.structr.extraction;

import org.structr.api.config.BooleanSetting;
import org.structr.api.config.ChoiceSetting;
import org.structr.api.config.IntegerSetting;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.api.config.StringSetting;
import org.structr.extraction.api.options.OcrMode;
import org.structr.extraction.api.options.SizeUnit;

/**
 * Settings of the content extraction module.
 *
 * The split and OCR settings are only the fallback: content extraction configurations (on folders and
 * files) and per-call options override them.
 */
public final class ContentExtractionSettings {

	private static final String CATEGORY = "Content Extraction Module";

	// ----- automatic extraction -----
	public static final Setting<Boolean> AutoExtractionEnabled = new BooleanSetting(Settings.applicationGroup, CATEGORY, "contentextraction.autoextraction.enabled", true,
		"Whether files are extracted automatically on upload or when moved into a folder whose content extraction configuration enables it. Explicit processing via processContent() is not affected.");

	public static final Setting<String> ExtractionQueue = new StringSetting(Settings.applicationGroup, CATEGORY, "contentextraction.extraction.queue", "content",
		"Name of the job queue content extraction jobs run in. A queue of its own keeps long extractions from blocking the default queue.");

	public static final Setting<Integer> MaxFileSize = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.extraction.maxfilesize", 50,
		"Maximum size in MB of a file that is extracted. Larger files are skipped.");

	public static final Setting<Integer> MaxCharacters = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.extraction.maxcharacters", 10_000_000,
		"Maximum number of characters extracted from a single document. Text beyond the limit is dropped.");

	public static final Setting<Boolean> IncludeEmbedded = new BooleanSetting(Settings.applicationGroup, CATEGORY, "contentextraction.extraction.includeembedded", false,
		"Whether the contents of embedded documents (archive entries, mail attachments, embedded office documents) are extracted as well.");

	public static final Setting<Boolean> IncludeImages = new BooleanSetting(Settings.applicationGroup, CATEGORY, "contentextraction.extraction.includeimages", false,
		"Whether images inside documents (inline images of PDFs, pictures in office documents and e-mails) are read with OCR. Only applies if OCR is available. Images on PDF pages that OCR reads as a whole, and tiny images, are left out. Images that are attachments or archive entries are embedded documents, see contentextraction.extraction.includeembedded.");

	// ----- figures -----
	public static final Setting<Boolean> ExtractFigures = new BooleanSetting(Settings.applicationGroup, CATEGORY, "contentextraction.figures.enabled", false,
		"Whether processing a file also stores its figures (charts, diagrams, tables with a caption, pictures) as DocumentFigure images in a folder next to the file, with their text in the searchable content. Can be overridden by a content extraction configuration.");

	public static final Setting<Integer> FigureDpi = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.figures.dpi", 150,
		"Resolution the figures of PDF pages are rendered at.");

	public static final Setting<Integer> FigureMaxPages = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.figures.maxpages", 300,
		"Maximum number of PDF pages searched for figures per document. Further pages are left out.");

	public static final Setting<Integer> FigureMaxFigures = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.figures.maxfigures", 100,
		"Maximum number of figures found per document.");

	public static final Setting<Integer> FigureMinSize = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.figures.minsize", 72,
		"Minimum width and height of a figure without a caption, in points on a PDF page (72 points are one inch) and in pixels in an image. Figures with a caption are kept at any size.");

	// ----- splitting -----
	public static final Setting<String> Splitter = new ChoiceSetting(Settings.applicationGroup, CATEGORY, "contentextraction.split.splitter", "structure",
		Settings.getStringsAsSet("recursive", "structure", "sentence", "token", "page"),
		"Default splitter used to divide extracted content into chunks. Can be overridden by a content extraction configuration.");

	public static final Setting<Integer> ChunkSize = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.split.chunksize", 512,
		"Default maximum chunk size, in the unit given by contentextraction.split.sizeunit. Can be overridden by a content extraction configuration.");

	public static final Setting<Integer> ChunkOverlap = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.split.chunkoverlap", 64,
		"Default overlap between consecutive chunks, in the unit given by contentextraction.split.sizeunit. Can be overridden by a content extraction configuration.");

	public static final Setting<String> SizeUnit = new ChoiceSetting(Settings.applicationGroup, CATEGORY, "contentextraction.split.sizeunit", "tokens",
		Settings.getStringsAsSet("tokens", "characters"),
		"Unit of chunk size and overlap. Can be overridden by a content extraction configuration.");

	public static final Setting<String> TokenEncoding = new ChoiceSetting(Settings.applicationGroup, CATEGORY, "contentextraction.split.tokenencoding", "cl100k_base",
		Settings.getStringsAsSet("cl100k_base", "o200k_base", "p50k_base", "r50k_base"),
		"Tokenizer encoding used to count tokens. Counts are exact for OpenAI models and an approximation for other embedding models.");

	// ----- OCR -----
	public static final Setting<String> OcrMode = new ChoiceSetting(Settings.applicationGroup, CATEGORY, "contentextraction.ocr.mode", "auto",
		Settings.getStringsAsSet("auto", "always", "off"),
		"OCR mode if Tesseract is installed. auto: images, and PDF pages without extractable text. always: every image and page. off: no OCR. Can be overridden by a content extraction configuration.");

	public static final Setting<String> OcrLanguages = new StringSetting(Settings.applicationGroup, CATEGORY, "contentextraction.ocr.languages", "eng+deu",
		"Tesseract languages, joined with \"+\". Each language needs its installed language pack.");

	public static final Setting<String> TesseractPath = new StringSetting(Settings.applicationGroup, CATEGORY, "contentextraction.ocr.tesseractpath", "",
		"Directory containing the tesseract binary. Empty means tesseract is looked up on the PATH.");

	public static final Setting<String> TessdataPath = new StringSetting(Settings.applicationGroup, CATEGORY, "contentextraction.ocr.tessdatapath", "",
		"Directory containing the Tesseract language data. Empty means the Tesseract default.");

	public static final Setting<Integer> OcrTimeout = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.ocr.timeout", 120,
		"Timeout in seconds for a single Tesseract invocation (one image or one PDF page).");

	public static final Setting<Integer> OcrMaxPages = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.ocr.maxpages", 200,
		"Maximum number of PDF pages OCR'd per document. Further pages are extracted without OCR.");

	public static final Setting<Integer> OcrDpi = new IntegerSetting(Settings.applicationGroup, CATEGORY, "contentextraction.ocr.dpi", 300, "Resolution PDF pages are rendered at before OCR.");

	private ContentExtractionSettings() {
	}

	/**
	 * Triggers class initialization: each Setting registers itself in its constructor, so the settings
	 * only appear in the configuration once this class has been loaded.
	 */
	static void register() {
	}
}
