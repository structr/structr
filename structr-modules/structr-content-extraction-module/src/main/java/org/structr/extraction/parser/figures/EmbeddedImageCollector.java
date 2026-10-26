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
package org.structr.extraction.parser.figures;

import org.apache.tika.detect.Detector;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.microsoft.OfficeParserConfig;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.exception.TikaException;
import org.structr.extraction.parser.tika.TikaParserSetup;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects the pictures of a document that Tika hands over as inline resources: pictures in office
 * documents, inline images of e-mails and HTML. Attachments and archive entries are documents of their
 * own and are left out.
 */
final class EmbeddedImageCollector implements EmbeddedDocumentExtractor {

	/** Larger images are not read. */
	private static final int MAX_BYTES = 32 * 1024 * 1024;

	/** A picture as it is stored in the document. */
	record EmbeddedImage(String name, byte[] data) {
	}

	private final List<EmbeddedImage> images = new ArrayList<>();
	private final Detector detector;

	private EmbeddedImageCollector(final Detector detector) {

		this.detector = detector;
	}

	static List<EmbeddedImage> collect(final TikaParserSetup.Setup setup, final Path path, final String fileName) throws IOException, TikaException, SAXException {

		final EmbeddedImageCollector collector = new EmbeddedImageCollector(setup.detector());
		final ParseContext context             = new ParseContext();
		final TesseractOCRConfig ocrConfig     = new TesseractOCRConfig();
		final Metadata metadata                = new Metadata();

		ocrConfig.setSkipOcr(true);

		context.set(TesseractOCRConfig.class, ocrConfig);
		context.set(OfficeParserConfig.class, TikaParserSetup.officeConfig());
		context.set(Parser.class, setup.parser());
		context.set(EmbeddedDocumentExtractor.class, collector);

		if (fileName != null && !fileName.isBlank()) {

			metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
		}

		try (final TikaInputStream stream = TikaInputStream.get(path)) {

			setup.parser().parse(stream, new DefaultHandler(), metadata, context);
		}

		return collector.images;
	}

	@Override
	public boolean shouldParseEmbedded(final Metadata metadata) {

		final boolean inline = TikaCoreProperties.EmbeddedResourceType.INLINE.toString().equals(metadata.get(TikaCoreProperties.EMBEDDED_RESOURCE_TYPE));

		return inline && isImage(metadata);
	}

	@Override
	public void parseEmbedded(final InputStream stream, final ContentHandler handler, final Metadata metadata, final boolean outputHtml) throws IOException {

		final byte[] data = stream.readNBytes(MAX_BYTES + 1);
		if (data.length > 0 && data.length <= MAX_BYTES) {

			images.add(new EmbeddedImage(metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY), data));
		}
	}

	// ----- private methods -----
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
}
