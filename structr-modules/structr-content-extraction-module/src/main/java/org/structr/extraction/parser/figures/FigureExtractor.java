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

import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.structr.extraction.api.ContentExtractionException;
import org.structr.extraction.api.document.ExtractedFigureData;
import org.structr.extraction.api.document.FigureSource;
import org.structr.extraction.api.options.FigureOptions;
import org.structr.extraction.parser.figures.EmbeddedImageCollector.EmbeddedImage;
import org.structr.extraction.parser.tika.TikaParserSetup;
import org.xml.sax.SAXException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the figures of a document: in a PDF the regions of its pages where something is drawn, in
 * other documents their pictures, and an image file is a figure itself.
 */
public final class FigureExtractor {

	private FigureExtractor() {
	}

	/**
	 * The figures of a document in a local file, in reading order.
	 *
	 * @param path     the document
	 * @param fileName the file name, used as a hint for type detection; may be null
	 */
	public static List<ExtractedFigureData> extract(final Path path, final String fileName, final FigureOptions options) throws ContentExtractionException {

		try {

			final TikaParserSetup.Setup setup = TikaParserSetup.getSetup();
			final String mimeType             = detect(setup, path, fileName);

			if ("application/pdf".equals(mimeType)) {

				return PdfFigureDetector.detect(path, options);
			}

			if (mimeType.startsWith("image/")) {

				return imageFile(path, options);
			}

			return embeddedImages(setup, path, fileName, options);

		} catch (final org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException ex) {

			throw new ContentExtractionException("The document is encrypted and cannot be read.", ex);

		} catch (final IOException | TikaException | SAXException | LinkageError ex) {

			throw new ContentExtractionException("Unable to extract figures: " + ex.getMessage(), ex);
		}
	}

	// ----- private methods -----
	private static String detect(final TikaParserSetup.Setup setup, final Path path, final String fileName) throws IOException {

		final Metadata metadata = new Metadata();

		if (fileName != null && !fileName.isBlank()) {

			metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
		}

		try (final TikaInputStream stream = TikaInputStream.get(path)) {

			return setup.detector().detect(stream, metadata).getBaseType().toString();
		}
	}

	private static List<ExtractedFigureData> imageFile(final Path path, final FigureOptions options) throws IOException {

		final BufferedImage image = FigureImages.decode(Files.readAllBytes(path));
		if (image == null) {

			return List.of();
		}

		return List.of(figure(image, 0, FigureSource.FILE));
	}

	private static List<ExtractedFigureData> embeddedImages(final TikaParserSetup.Setup setup, final Path path, final String fileName, final FigureOptions options) throws IOException, TikaException, SAXException {

		final List<ExtractedFigureData> figures = new ArrayList<>();
		final Set<String> seen                  = new HashSet<>();

		for (final EmbeddedImage embedded : EmbeddedImageCollector.collect(setup, path, fileName)) {

			// the same picture used again (e.g. a logo in every slide) is one figure
			if (figures.size() >= options.getMaxFigures() || !seen.add(FigureImages.sha256(embedded.data()))) {

				continue;
			}

			final BufferedImage image = FigureImages.decode(embedded.data());
			if (image != null && image.getWidth() >= options.getMinFigureSize() && image.getHeight() >= options.getMinFigureSize()) {

				figures.add(figure(image, figures.size(), FigureSource.EMBEDDED));
			}
		}

		return figures;
	}

	private static ExtractedFigureData figure(final BufferedImage image, final int position, final FigureSource source) throws IOException {

		final byte[] png = FigureImages.png(image);

		return new ExtractedFigureData(null, position, null, null, source, null, image.getWidth(), image.getHeight(), FigureImages.MEDIA_TYPE, png, FigureImages.sha256(png), List.of(), "");
	}
}
