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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

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
		 */
		public ParseContext createContext(final ExtractionOptions options, final String ocrLanguages, final boolean embedded) {

			final ParseContext context         = new ParseContext();
			final TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
			final PDFParserConfig pdfConfig    = new PDFParserConfig();
			final boolean ocr                  = ocrLanguages != null;

			ocrConfig.setSkipOcr(!ocr);
			pdfConfig.setExtractInlineImages(false);

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

			if (!embedded) {

				context.set(EmbeddedDocumentExtractor.class, new InlineContentOnly(context));
			}

			return context;
		}
	}

	/**
	 * Parses inline content, such as the body parts of an e-mail, which Tika also hands over as embedded
	 * resources, and skips everything else.
	 */
	private static final class InlineContentOnly extends ParsingEmbeddedDocumentExtractor {

		InlineContentOnly(final ParseContext context) {

			super(context);
		}

		@Override
		public boolean shouldParseEmbedded(final Metadata metadata) {

			final String type = metadata.get(TikaCoreProperties.EMBEDDED_RESOURCE_TYPE);

			return TikaCoreProperties.EmbeddedResourceType.INLINE.toString().equals(type) && super.shouldParseEmbedded(metadata);
		}
	}
}
