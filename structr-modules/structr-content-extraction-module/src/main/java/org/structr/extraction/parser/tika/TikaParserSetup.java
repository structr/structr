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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaConfigException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.TikaPagedText;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParserDecorator;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.ocr.TesseractOCRParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.api.document.OcrInfo;
import org.structr.extraction.api.options.ExtractionOptions;
import org.structr.extraction.api.options.OcrMode;
import org.structr.extraction.parser.XhtmlStructureHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * The Tika setup of the module.
 *
 * Built from an explicit configuration instead of Tika's defaults, so it is clear which programs Tika may
 * start: the default parser includes {@code CompositeExternalParser}, which runs external tools such as
 * exiftool or ffmpeg when they are on the PATH, and {@code TesseractOCRParser}. The external parser is
 * excluded; Tesseract is the only external program allowed, configured from the module settings. The
 * setup is rebuilt when the tesseract or tessdata path setting changes.
 */
public final class TikaParserSetup {

	private static final Logger logger = LoggerFactory.getLogger(TikaParserSetup.class);

	/** Parsers that run external programs and are excluded from the setup. */
	public static final Set<String> EXCLUDED_PARSERS = Set.of("org.apache.tika.parser.external.CompositeExternalParser", "org.apache.tika.parser.external.ExternalParser");

	private static final Object lock      = new Object();
	private static volatile Setup current = null;

	private TikaParserSetup() {
	}

	/**
	 * The current setup, rebuilt if the Tesseract path settings changed since it was built.
	 */
	public static Setup getSetup() throws TikaConfigException {

		final String tesseractPath = StringUtils.trimToEmpty(ContentExtractionSettings.TesseractPath.getValue());
		final String tessdataPath  = StringUtils.trimToEmpty(ContentExtractionSettings.TessdataPath.getValue());
		final Setup setup          = current;

		if (setup != null && setup.tesseractPath().equals(tesseractPath) && setup.tessdataPath().equals(tessdataPath)) {

			return setup;
		}

		synchronized (lock) {

			if (current == null || !current.tesseractPath().equals(tesseractPath) || !current.tessdataPath().equals(tessdataPath)) {

				current = buildWithFallback(tesseractPath, tessdataPath);
			}

			return current;
		}
	}

	/**
	 * Visits every parser in the tree below the given one, including decorated and composite parsers.
	 */
	public static void walk(final Parser parser, final Consumer<Parser> visitor) {

		visitor.accept(parser);

		if (parser instanceof CompositeParser composite) {

			for (final Parser child : composite.getAllComponentParsers()) {

				walk(child, visitor);
			}

		} else if (parser instanceof ParserDecorator decorator) {

			walk(decorator.getWrappedParser(), visitor);
		}
	}

	// ----- private methods -----
	/**
	 * Tika rejects the whole configuration if a configured Tesseract path does not exist. A wrong path
	 * must cost OCR, not all extraction, so the setup is then built without Tesseract.
	 */
	private static Setup buildWithFallback(final String tesseractPath, final String tessdataPath) throws TikaConfigException {

		// Tika would try to run a missing binary and log the failure with a stack trace; a missing
		// Tesseract is a normal installation (it is only recommended), so it is checked here first
		if (!isTesseractInstalled(tesseractPath)) {

			return build(tesseractPath, tessdataPath, false);
		}

		try {

			return build(tesseractPath, tessdataPath, true);

		} catch (final TikaConfigException ex) {

			logger.warn("Content extraction: Tesseract is not usable with the configured paths, OCR is skipped: {}", ex.getMessage());

			return build(tesseractPath, tessdataPath, false);
		}
	}

	private static Setup build(final String tesseractPath, final String tessdataPath, final boolean withTesseract) throws TikaConfigException {

		// without preloading, TesseractOCRParser reports no languages at all
		final StringBuilder params = new StringBuilder("<param name=\"preloadLangs\" type=\"bool\">true</param>");

		if (!tesseractPath.isEmpty()) {

			params.append("<param name=\"tesseractPath\" type=\"string\">").append(escapeXml(tesseractPath)).append("</param>");
		}

		if (!tessdataPath.isEmpty()) {

			params.append("<param name=\"tessdataPath\" type=\"string\">").append(escapeXml(tessdataPath)).append("</param>");
		}

		final StringBuilder xml = new StringBuilder();

		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><properties><parsers>");
		xml.append("<parser class=\"org.apache.tika.parser.DefaultParser\">");
		xml.append("<parser-exclude class=\"org.apache.tika.parser.ocr.TesseractOCRParser\"/>");

		for (final String excluded : new TreeSet<>(EXCLUDED_PARSERS)) {

			xml.append("<parser-exclude class=\"").append(excluded).append("\"/>");
		}

		xml.append("</parser>");

		if (withTesseract) {

			xml.append("<parser class=\"org.apache.tika.parser.ocr.TesseractOCRParser\"><params>").append(params).append("</params></parser>");
		}

		xml.append("</parsers></properties>");

		try {

			final TikaConfig config         = new TikaConfig(new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)));
			final AutoDetectParser parser   = new AutoDetectParser(config);
			final List<TesseractOCRParser> tesseract = new ArrayList<>();

			walk(parser, p -> {

				if (p instanceof TesseractOCRParser ocrParser) {

					tesseract.add(ocrParser);
				}
			});

			final OcrInfo ocrInfo = ocrInfo(tesseract.isEmpty() ? null : tesseract.getFirst());

			logger.info("Content extraction: Tesseract OCR {}", ocrInfo.available() ? "available, languages " + ocrInfo.languages() : "not available, OCR is skipped");

			return new Setup(tesseractPath, tessdataPath, parser, config.getDetector(), ocrInfo);

		} catch (final Exception ex) {

			throw new TikaConfigException("Unable to build the Tika configuration for content extraction: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Whether the tesseract executable exists: in the configured directory if there is one, otherwise on
	 * the PATH, which is where Tika looks for it.
	 */
	private static boolean isTesseractInstalled(final String tesseractPath) {

		final String executable = SystemUtils.IS_OS_WINDOWS ? "tesseract.exe" : "tesseract";

		if (!tesseractPath.isEmpty()) {

			final boolean installed = Files.isExecutable(Path.of(tesseractPath, executable));
			if (!installed) {

				logger.warn("Content extraction: no {} executable in {} (setting {}), OCR is skipped", executable, tesseractPath, ContentExtractionSettings.TesseractPath.getKey());
			}

			return installed;
		}

		final String path = StringUtils.defaultString(System.getenv("PATH"));

		for (final String directory : path.split(File.pathSeparator)) {

			if (!directory.isBlank() && Files.isExecutable(Path.of(directory, executable))) {

				return true;
			}
		}

		logger.info("Content extraction: no {} executable on the PATH of this process; to use OCR, install Tesseract or set {} to its directory", executable, ContentExtractionSettings.TesseractPath.getKey());

		return false;
	}

	private static OcrInfo ocrInfo(final TesseractOCRParser parser) {

		if (parser == null) {

			return new OcrInfo(false, Set.of());
		}

		try {

			if (parser.hasTesseract()) {

				return new OcrInfo(true, parser.getLangs());
			}

		} catch (final TikaConfigException ex) {

			logger.warn("Content extraction: unable to check for Tesseract: {}", ex.getMessage());
		}

		return new OcrInfo(false, Set.of());
	}

	private static String escapeXml(final String value) {

		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/**
	 * A built Tika setup.
	 *
	 * @param tesseractPath the tesseract path setting it was built with
	 * @param tessdataPath  the tessdata path setting it was built with
	 * @param parser        the parser, with external tools excluded and Tesseract configured
	 * @param detector      the content type detector
	 * @param ocrInfo       whether OCR is available
	 */
	/**
	 * PPTX is read with Tika's own SAX-based extractor. The default one goes through POI's XSLFExtractor,
	 * whose package POI 5.4 does not export from its module (it exports the Word and Excel extractors
	 * only), so on the module path every PPTX fails with an IllegalAccessError. The SAX extractor marks
	 * slides the same way (div.slide-content); it does not report slide comments.
	 */
	public static OfficeParserConfig officeConfig() {

		final OfficeParserConfig config = new OfficeParserConfig();

		config.setUseSAXPptxExtractor(true);

		return config;
	}

	public record Setup(String tesseractPath, String tessdataPath, AutoDetectParser parser, Detector detector, OcrInfo ocrInfo) {

		/**
		 * A parse context for one extraction.
		 *
		 * @param options      the extraction options
		 * @param ocrLanguages the Tesseract languages to use, joined with "+", or null for no OCR at all
		 *                     (switched off for images and PDFs alike)
		 * @param embedded     whether embedded documents (attachments, archive entries, embedded office
		 *                     documents) are parsed; inline content such as an e-mail body always is
		 * @param pageCharacters the number of characters on the current page of the innermost document so far,
		 *                     to leave out inline images of PDF pages that OCR reads as a whole; may be null
		 */
		public ParseContext createContext(final ExtractionOptions options, final String ocrLanguages, final boolean embedded, final IntSupplier pageCharacters) {

			final ParseContext context         = new ParseContext();
			final TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
			final PDFParserConfig pdfConfig    = new PDFParserConfig();
			final boolean ocr                  = ocrLanguages != null;
			final boolean images               = ocr && options.isIncludeImages();

			ocrConfig.setSkipOcr(!ocr);
			pdfConfig.setExtractInlineImages(images);
			pdfConfig.setExtractUniqueInlineImagesOnly(true);

			// rotated text, e.g. a diagonal watermark, is read in its own direction; otherwise it is cut into fragments of a few letters
			pdfConfig.setDetectAngles(true);

			if (ocr) {

				ocrConfig.setLanguage(ocrLanguages);
				ocrConfig.setTimeoutSeconds(options.getOcrTimeoutSeconds());

				pdfConfig.setOcrStrategy(options.getOcrMode() == OcrMode.ALWAYS ? PDFParserConfig.OCR_STRATEGY.OCR_AND_TEXT_EXTRACTION : PDFParserConfig.OCR_STRATEGY.AUTO);
				pdfConfig.setOcrDPI(options.getOcrDpi());

			} else {

				pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
			}

			context.set(TesseractOCRConfig.class, ocrConfig);
			context.set(PDFParserConfig.class, pdfConfig);
			context.set(OfficeParserConfig.class, officeConfig());

			// The parser must always be in the context: without it, Tika parses embedded documents with
			// its default configuration, which includes the external tool parsers excluded here, and the
			// PDF parser finds no OCR parser.
			context.set(Parser.class, parser);

			context.set(EmbeddedDocumentExtractor.class, new EmbeddedDocuments(context, embedded, images, detector, pdfConfig, pageCharacters));

			return context;
		}
	}

	/**
	 * Decides which embedded resources are parsed: all of them, or, if embedded documents are not
	 * extracted, only inline content such as the body parts of an e-mail, which Tika also hands over as
	 * embedded resources.
	 *
	 * Images inside a document (inline images of a PDF, pictures in office documents, inline images of an
	 * e-mail) only have text if OCR reads them, so they are parsed only if images are included, and not if
	 * they are tiny (icons, bullets), or on a PDF page that OCR reads as a whole anyway, where their text
	 * would appear twice. An image that is a document of its own (an attachment, an archive entry) counts
	 * as an embedded document.
	 *
	 * Every embedded resource that is parsed is wrapped in an element that carries its name, see
	 * {@link XhtmlStructureHandler#EMBEDDED_DOCUMENT_CLASS}. Tika's own wrapper is not used: parsers ask
	 * for it only sometimes (not for e-mail attachments or images of a PDF), and it puts the name into
	 * the text as a heading.
	 */
	private static final class EmbeddedDocuments extends ParsingEmbeddedDocumentExtractor {

		private static final String XHTML         = "http://www.w3.org/1999/xhtml";
		private static final int MIN_IMAGE_SIDE   = 16;
		private static final int MIN_IMAGE_PIXELS = 64 * 64;

		private final boolean embedded;
		private final boolean images;
		private final Detector detector;
		private final PDFParserConfig pdfConfig;
		private final IntSupplier pageCharacters;

		EmbeddedDocuments(final ParseContext context, final boolean embedded, final boolean images, final Detector detector, final PDFParserConfig pdfConfig, final IntSupplier pageCharacters) {

			super(context);

			this.embedded       = embedded;
			this.images         = images;
			this.detector       = detector;
			this.pdfConfig      = pdfConfig;
			this.pageCharacters = pageCharacters;
		}

		@Override
		public boolean shouldParseEmbedded(final Metadata metadata) {

			final boolean inline = TikaCoreProperties.EmbeddedResourceType.INLINE.toString().equals(metadata.get(TikaCoreProperties.EMBEDDED_RESOURCE_TYPE));

			if (!embedded && !inline) {

				return false;
			}

			if (inline && isImage(metadata) && (!images || isTiny(metadata) || isReadByPageOcr(metadata))) {

				return false;
			}

			return super.shouldParseEmbedded(metadata);
		}

		@Override
		public void parseEmbedded(final InputStream stream, final ContentHandler handler, final Metadata metadata, final boolean outputHtml) throws SAXException, IOException {

			final AttributesImpl attributes = new AttributesImpl();
			final String name               = metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY);

			attributes.addAttribute("", "class", "class", "CDATA", XhtmlStructureHandler.EMBEDDED_DOCUMENT_CLASS);

			if (name != null && !name.isBlank()) {

				attributes.addAttribute("", XhtmlStructureHandler.EMBEDDED_NAME_ATTRIBUTE, XhtmlStructureHandler.EMBEDDED_NAME_ATTRIBUTE, "CDATA", name);
			}

			handler.startElement(XHTML, "div", "div", attributes);

			try {

				super.parseEmbedded(stream, handler, metadata, false);

			} finally {

				handler.endElement(XHTML, "div", "div");
			}
		}

		private boolean isImage(final Metadata metadata) {

			String type = metadata.get(Metadata.CONTENT_TYPE);

			if (type == null) {

				try {

					// detection by name only, the stream is not available here
					type = detector.detect(null, metadata).toString();

				} catch (final IOException ex) {

					return false;
				}
			}

			return type.startsWith("image/");
		}

		private static boolean isTiny(final Metadata metadata) {

			final Integer width  = metadata.getInt(Metadata.IMAGE_WIDTH);
			final Integer height = metadata.getInt(Metadata.IMAGE_LENGTH);

			return width != null && height != null && (width < MIN_IMAGE_SIDE || height < MIN_IMAGE_SIDE || width * height < MIN_IMAGE_PIXELS);
		}

		/**
		 * Whether the image is on a PDF page that OCR reads as a whole: every page with OCR mode always,
		 * otherwise a page without (enough) text of its own. The PDF parser hands over the images of a page
		 * after its text and before it decides about OCR, so the text of the page is known at this point.
		 */
		private boolean isReadByPageOcr(final Metadata metadata) {

			if (metadata.get(TikaPagedText.PAGE_NUMBER) == null || pageCharacters == null) {

				return false;
			}

			return switch (pdfConfig.getOcrStrategy()) {

				case OCR_AND_TEXT_EXTRACTION, OCR_ONLY -> true;
				case AUTO                              -> pageCharacters.getAsInt() <= pdfConfig.getOcrStrategyAuto().getTotalCharsPerPage();
				default                                -> false;
			};
		}
	}
}
