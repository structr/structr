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
package org.structr.extraction.parser.tika;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.PDF;
import org.apache.tika.metadata.PagedText;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.RecursiveParserWrapper;
import org.apache.tika.sax.BasicContentHandlerFactory;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.RecursiveParserWrapperHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.document.DetectedLanguage;
import org.structr.extraction.api.document.DocumentMetadata;
import org.structr.extraction.api.document.EmbeddedDocument;
import org.structr.extraction.api.document.ExtractedDocument;
import org.structr.extraction.api.document.ExtractedLink;
import org.structr.extraction.api.document.OcrInfo;
import org.structr.extraction.api.document.OcrSkipReason;
import org.structr.extraction.api.options.ExtractionOptions;
import org.structr.extraction.api.options.OcrMode;
import org.structr.extraction.parser.DocumentTextRenderer;
import org.structr.extraction.parser.ParsedBlock;
import org.structr.extraction.parser.XhtmlStructureHandler;
import org.structr.extraction.parser.markdown.MarkdownBlockParser;
import org.structr.extraction.text.TextLanguageDetector;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs Tika on a document and turns the result into the module's document model.
 */
public final class TikaDocumentExtractor {

	private static final Logger logger = LoggerFactory.getLogger(TikaDocumentExtractor.class);

	private static final String TESSERACT_PARSER = "org.apache.tika.parser.ocr.TesseractOCRParser";
	private static final Set<String> MARKDOWN    = Set.of("text/markdown", "text/x-markdown", "text/x-web-markdown");

	private TikaDocumentExtractor() {
	}

	public static OcrInfo getOcrInfo() throws ContentExtractionException {

		return setup().ocrInfo();
	}

	/**
	 * Extracts text, structure, links and metadata of a document.
	 *
	 * Formats that need random access (PDF, ZIP, Office) make Tika spool the stream to a temporary file
	 * first; use {@link #extract(Path, String, ExtractionOptions)} for content that is in a file already.
	 */
	public static ExtractedDocument extract(final InputStream input, final String fileName, final ExtractionOptions options) throws ContentExtractionException {

		try (final TikaInputStream stream = TikaInputStream.get(input)) {

			return extract(stream, fileName, options);

		} catch (final IOException ex) {

			throw new ContentExtractionException("Unable to extract content: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Extracts text, structure, links and metadata of a document in a local file, which Tika reads in
	 * place instead of copying it.
	 */
	public static ExtractedDocument extract(final Path path, final String fileName, final ExtractionOptions options) throws ContentExtractionException {

		try (final TikaInputStream stream = TikaInputStream.get(path)) {

			return extract(stream, fileName, options);

		} catch (final IOException ex) {

			throw new ContentExtractionException("Unable to extract content: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Reads the metadata of a document. The document is parsed, but its text is discarded and no OCR runs.
	 */
	public static DocumentMetadata extractMetadata(final InputStream input, final String fileName) throws ContentExtractionException {

		final TikaParserSetup.Setup setup = setup();

		try (final TikaInputStream stream = TikaInputStream.get(input)) {

			final Metadata metadata         = newMetadata(fileName);
			final ExtractionOptions options = ExtractionOptions.fromSettings().setOcrMode(OcrMode.OFF);

			setup.parser().parse(stream, new DefaultHandler(), metadata, setup.createContext(options, null, false));

			return toDocumentMetadata(metadata, DetectedLanguage.UNKNOWN, new ParseOutcome(0, false, null, false));

		} catch (final EncryptedDocumentException ex) {

			throw new ContentExtractionException("The document is encrypted and cannot be read.", ex);

		} catch (final IOException | TikaException | SAXException | LinkageError ex) {

			throw new ContentExtractionException("Unable to read metadata: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Lists the documents embedded in a container (mail attachments, archive entries, embedded office
	 * documents), recursively up to the maximum depth of the options.
	 */
	public static List<EmbeddedDocument> extractEmbedded(final InputStream input, final String fileName, final ExtractionOptions options, final boolean includeText) throws ContentExtractionException {

		final TikaParserSetup.Setup setup = setup();

		try (final TikaInputStream stream = TikaInputStream.get(input)) {

			final Metadata metadata                   = newMetadata(fileName);
			final String mimeType                     = detect(setup, stream, metadata);
			final OcrDecision ocr                     = includeText ? decideOcr(setup, options, mimeType, stream) : OcrDecision.disabled(null);
			final BasicContentHandlerFactory.HANDLER_TYPE type = includeText ? BasicContentHandlerFactory.HANDLER_TYPE.TEXT : BasicContentHandlerFactory.HANDLER_TYPE.IGNORE;
			final RecursiveParserWrapperHandler handler = new RecursiveParserWrapperHandler(new BasicContentHandlerFactory(type, includeText ? options.getMaxCharacters() : -1), -1);
			final RecursiveParserWrapper wrapper      = new RecursiveParserWrapper(setup.parser());
			final List<EmbeddedDocument> result       = new ArrayList<>();

			wrapper.parse(stream, handler, metadata, setup.createContext(options, ocr.languages(), true));

			// the first entry of the metadata list is the container itself
			for (final Metadata embedded : handler.getMetadataList().stream().skip(1).toList()) {

				final Integer depth = embedded.getInt(TikaCoreProperties.EMBEDDED_DEPTH);
				if (depth != null && depth > options.getMaxEmbeddedDepth()) {

					continue;
				}

				final String path    = firstNonNull(embedded.get(TikaCoreProperties.FINAL_EMBEDDED_RESOURCE_PATH), embedded.get(TikaCoreProperties.EMBEDDED_RESOURCE_PATH));
				final String content = includeText ? embedded.get(TikaCoreProperties.TIKA_CONTENT) : null;

				result.add(new EmbeddedDocument(embedded.get(TikaCoreProperties.RESOURCE_NAME_KEY), path, embedded.get(Metadata.CONTENT_TYPE), depth != null ? depth : 1, content != null ? content.strip() : null));
			}

			return result;

		} catch (final EncryptedDocumentException ex) {

			throw new ContentExtractionException("The document is encrypted and cannot be read.", ex);

		} catch (final IOException | TikaException | SAXException | LinkageError ex) {

			throw new ContentExtractionException("Unable to read embedded documents: " + ex.getMessage(), ex);
		}
	}

	// ----- private methods -----
	private static ExtractedDocument extract(final TikaInputStream stream, final String fileName, final ExtractionOptions options) throws ContentExtractionException {

		final TikaParserSetup.Setup setup = setup();

		try {

			final Metadata metadata = newMetadata(fileName);
			final String mimeType   = detect(setup, stream, metadata);
			final OcrDecision ocr   = decideOcr(setup, options, mimeType, stream);

			final List<ParsedBlock> rawBlocks;
			final List<ExtractedLink> links;
			final int pagesSeen;
			final boolean truncated;

			if (MARKDOWN.contains(mimeType)) {

				final BodyContentHandler body       = new BodyContentHandler(-1);
				final MarkdownBlockParser markdown  = new MarkdownBlockParser(options.getMaxCharacters());

				setup.parser().parse(stream, body, metadata, setup.createContext(options, null, false));

				rawBlocks = markdown.parse(body.toString());
				links     = List.of();
				pagesSeen = 0;
				truncated = markdown.isTruncated();

			} else {

				final XhtmlStructureHandler handler = new XhtmlStructureHandler(options.getMaxCharacters());

				setup.parser().parse(stream, handler, metadata, setup.createContext(options, ocr.languages(), options.isIncludeEmbedded()));

				rawBlocks = handler.getBlocks();
				links     = handler.getLinks();
				pagesSeen = handler.getPageCount();
				truncated = handler.isTruncated();
			}

			final DocumentTextRenderer renderer   = new DocumentTextRenderer(rawBlocks);
			final String text                 = renderer.getText();
			final DetectedLanguage language   = TextLanguageDetector.detect(text);
			final boolean ocrApplied          = ocr.enabled() && ocrApplied(metadata, text);

			if (metadata.get(Metadata.CONTENT_TYPE) == null) {

				metadata.set(Metadata.CONTENT_TYPE, mimeType);
			}

			final DocumentMetadata documentMetadata = toDocumentMetadata(metadata, language, new ParseOutcome(pagesSeen, ocrApplied, ocr.skippedReason(), truncated));

			return new ExtractedDocument(documentMetadata, renderer.getBlocks(), links, text, renderer.getMarkdown());

		} catch (final EncryptedDocumentException ex) {

			throw new ContentExtractionException("The document is encrypted and cannot be extracted.", ex);

		} catch (final IOException | TikaException | SAXException | LinkageError ex) {

			// a LinkageError is a parser that cannot load or access a class it needs (e.g. a package a
			// module does not export); it fails this document, not the job that processes many of them
			throw new ContentExtractionException("Unable to extract content: " + ex.getMessage(), ex);
		}
	}

	private static TikaParserSetup.Setup setup() throws ContentExtractionException {

		try {

			return TikaParserSetup.getSetup();

		} catch (final TikaException ex) {

			throw new ContentExtractionException(ex.getMessage(), ex);
		}
	}

	private static Metadata newMetadata(final String fileName) {

		final Metadata metadata = new Metadata();

		if (fileName != null && !fileName.isBlank()) {

			metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
		}

		return metadata;
	}

	private static String detect(final TikaParserSetup.Setup setup, final TikaInputStream stream, final Metadata metadata) throws IOException {

		final MediaType type = setup.detector().detect(stream, metadata);

		return type.getBaseType().toString();
	}

	/**
	 * Whether OCR may run for this document, with which languages, and if not, why.
	 */
	private static OcrDecision decideOcr(final TikaParserSetup.Setup setup, final ExtractionOptions options, final String mimeType, final TikaInputStream stream) {

		if (options.getOcrMode() == OcrMode.OFF) {

			return OcrDecision.disabled(OcrSkipReason.MODE_OFF);
		}

		final OcrInfo info = setup.ocrInfo();
		if (!info.available()) {

			return OcrDecision.disabled(OcrSkipReason.TESSERACT_NOT_INSTALLED);
		}

		final List<String> requested = Arrays.stream(options.getOcrLanguages().split("\\+")).map(String::trim).filter(s -> !s.isEmpty()).toList();
		final List<String> installed = requested.stream().filter(info.languages()::contains).toList();

		if (installed.isEmpty()) {

			return OcrDecision.disabled(OcrSkipReason.LANGUAGES_NOT_INSTALLED);
		}

		if (installed.size() < requested.size()) {

			logger.info("Content extraction: OCR languages {} are not installed and are left out (installed: {})", requested.stream().filter(l -> !installed.contains(l)).toList(), info.languages());
		}

		if ("application/pdf".equals(mimeType) && pageCount(stream) > options.getOcrMaxPages()) {

			return OcrDecision.disabled(OcrSkipReason.MAX_PAGES_EXCEEDED);
		}

		return new OcrDecision(String.join("+", installed), null);
	}

	/**
	 * Number of pages of a PDF, read with PDFBox before parsing so a long document can be kept from OCR
	 * entirely; Tika has no per-document page limit for OCR. Returns 0 if the PDF cannot be opened (e.g.
	 * encrypted), leaving the decision to the parser.
	 */
	private static int pageCount(final TikaInputStream stream) {

		try (final PDDocument document = Loader.loadPDF(stream.getFile())) {

			return document.getNumberOfPages();

		} catch (final IOException ex) {

			return 0;
		}
	}

	/**
	 * Whether OCR contributed text: Tesseract parsed (part of) the document, or the PDF parser OCR'd pages.
	 */
	private static boolean ocrApplied(final Metadata metadata, final String text) {

		final Integer ocrPages = metadata.getInt(PDF.OCR_PAGE_COUNT);
		if (ocrPages != null && ocrPages > 0) {

			return true;
		}

		final List<String> parsedBy = new ArrayList<>();

		parsedBy.addAll(Arrays.asList(metadata.getValues(TikaCoreProperties.TIKA_PARSED_BY)));
		parsedBy.addAll(Arrays.asList(metadata.getValues(TikaCoreProperties.TIKA_PARSED_BY_FULL_SET)));

		return parsedBy.contains(TESSERACT_PARSER) && !text.isBlank();
	}

	private static DocumentMetadata toDocumentMetadata(final Metadata metadata, final DetectedLanguage detected, final ParseOutcome outcome) {

		final Map<String, List<String>> raw = new LinkedHashMap<>();

		for (final String name : metadata.names()) {

			raw.put(name, List.of(metadata.getValues(name)));
		}

		Integer pageCount = metadata.getInt(PagedText.N_PAGES);
		if (pageCount == null && outcome.pagesSeen() > 0) {

			pageCount = outcome.pagesSeen();
		}

		String language     = detected.language();
		Double confidence   = language != null ? detected.confidence() : null;

		if (language == null) {

			language = metadata.get(TikaCoreProperties.LANGUAGE);
		}

		return new DocumentMetadata(
			metadata.get(Metadata.CONTENT_TYPE),
			metadata.get(TikaCoreProperties.TITLE),
			metadata.get(TikaCoreProperties.CREATOR),
			language,
			confidence,
			pageCount,
			metadata.getDate(TikaCoreProperties.CREATED),
			metadata.getDate(TikaCoreProperties.MODIFIED),
			outcome.ocrApplied(),
			outcome.ocrSkippedReason(),
			outcome.truncated(),
			raw
		);
	}

	private static String firstNonNull(final String first, final String second) {

		return first != null ? first : second;
	}

	/**
	 * Whether OCR may run: the Tesseract languages to use, or null with the reason OCR is skipped.
	 */
	private record OcrDecision(String languages, OcrSkipReason skippedReason) {

		static OcrDecision disabled(final OcrSkipReason reason) {

			return new OcrDecision(null, reason);
		}

		boolean enabled() {

			return languages != null;
		}
	}

	/**
	 * What parsing found out besides the content: pages seen, and how OCR and the character limit went.
	 */
	private record ParseOutcome(int pagesSeen, boolean ocrApplied, OcrSkipReason ocrSkippedReason, boolean truncated) {
	}
}
