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
package org.structr.test.extraction;

import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.document.ExtractedFigureData;
import org.structr.extraction.api.document.FigureSource;
import org.structr.extraction.api.options.FigureOptions;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Figures of documents, without a database.
 */
public class FigureExtractionTest {

	@Test
	public void testFiguresOfAPaper() throws Exception {

		final List<ExtractedFigureData> figures = extract(Fixtures.paperPdf(), "paper.pdf", new FigureOptions());

		assertEquals("Vector chart, embedded chart and table; the logo on every page is left out", List.of("Figure 1", "Figure 2", "Table 1"), figures.stream().map(ExtractedFigureData::label).toList());
		assertEquals(List.of(1, 2, 2), figures.stream().map(ExtractedFigureData::page).toList());
		assertEquals(List.of(0, 1, 2), figures.stream().map(ExtractedFigureData::position).toList());

		for (final ExtractedFigureData figure : figures) {

			assertEquals(FigureSource.REGION, figure.source());
			assertEquals("image/png", figure.mediaType());

			final BufferedImage image = ImageIO.read(new ByteArrayInputStream(figure.image()));

			assertEquals(figure.width(), image.getWidth());
			assertEquals(figure.height(), image.getHeight());
			assertEquals(64, figure.checksum().length());
		}

		final ExtractedFigureData chart = figures.get(0);

		assertEquals("The caption is joined across its lines", "Figure 1: Accuracy of the three models on the benchmark. Higher is better.", chart.caption());

		// the bars and axes are at x 100 to 400 and y 172 to 372 (from the top), the axis label "Accuracy" left
		// of them, and the labels of the bars below them down to y 382
		assertBoxContains(chart.bbox(), 62, 172, 400, 381);
		assertTrue("The caption is not part of the figure", chart.bbox()[1] + chart.bbox()[3] < 400);
		assertEquals("Rendered at 150 dpi", Math.round((chart.bbox()[2] + 8) * 150 / 72.0), chart.width(), 2);

		assertEquals("Only the sentence about Figure 1, not the one about Figure 10", List.of("As Figure 1 shows, model C is the most accurate."), chart.mentions());

		final ExtractedFigureData picture = figures.get(1);

		assertEquals("Fig. 2. Training loss over time.", picture.caption());
		assertBoxContains(picture.bbox(), 100, 142, 400, 342);
		assertEquals(List.of("Fig. 2 shows the training loss, which drops quickly."), picture.mentions());

		final ExtractedFigureData table = figures.get(2);

		assertEquals("Table 1: Results on the test set.", table.caption());

		// from the top rule (y 442 from the top) to the bottom rule (y 512), with all rows
		assertBoxContains(table.bbox(), 100, 442, 400, 512);
		assertTrue("The caption above the table is not part of it", table.bbox()[1] > 432);
		assertEquals(List.of("The results in Table 1 confirm this."), table.mentions());
	}

	@Test
	public void testTablesCanBeLeftOut() throws Exception {

		final List<ExtractedFigureData> figures = extract(Fixtures.paperPdf(), "paper.pdf", new FigureOptions().setIncludeTables(false));

		assertEquals(List.of("Figure 1", "Figure 2"), figures.stream().map(ExtractedFigureData::label).toList());
	}

	@Test
	public void testMaxFigures() throws Exception {

		final List<ExtractedFigureData> figures = extract(Fixtures.paperPdf(), "paper.pdf", new FigureOptions().setMaxFigures(1));

		assertEquals(List.of("Figure 1"), figures.stream().map(ExtractedFigureData::label).toList());
	}

	@Test
	public void testRenderedFiguresDoNotChange() throws Exception {

		final List<ExtractedFigureData> first  = extract(Fixtures.paperPdf(), "paper.pdf", new FigureOptions());
		final List<ExtractedFigureData> second = extract(Fixtures.paperPdf(), "paper.pdf", new FigureOptions());

		assertEquals("The same document renders to the same images, so stored figures can be kept", first.stream().map(ExtractedFigureData::checksum).toList(), second.stream().map(ExtractedFigureData::checksum).toList());
	}

	@Test
	public void testTextOnlyPdfHasNoFigures() throws Exception {

		assertEquals(List.of(), extract(Fixtures.pdf(List.of("Only text on this page.", "And on this one.")), "text.pdf", new FigureOptions()));
	}

	@Test
	public void testPicturesOfAWordDocument() throws Exception {

		final List<ExtractedFigureData> figures = extract(Fixtures.docxWithPictures(), "report.docx", new FigureOptions());

		assertEquals("The chart once, the icon is too small", 1, figures.size());
		assertEquals(FigureSource.EMBEDDED, figures.getFirst().source());
		assertEquals(600, figures.getFirst().width());
		assertEquals(400, figures.getFirst().height());
		assertNull(figures.getFirst().page());
		assertNull(figures.getFirst().label());
	}

	@Test
	public void testAnImageIsAFigure() throws Exception {

		final List<ExtractedFigureData> figures = extract(Fixtures.blankPng(), "scan.png", new FigureOptions());

		assertEquals(1, figures.size());
		assertEquals(FigureSource.FILE, figures.getFirst().source());
	}

	@Test
	public void testFigureMapHasTheImageForAnLlm() throws Exception {

		final ExtractedFigureData figure = extract(Fixtures.paperPdf(), "paper.pdf", new FigureOptions()).getFirst();
		final java.util.Map<String, Object> map = figure.toMap(true);
		final java.util.Map<?, ?> image         = (java.util.Map<?, ?>) map.get("image");

		assertEquals("image/png", image.get("mediaType"));
		assertTrue(((String) image.get("data")).length() > 100);
		assertNull(figure.toMap(false).get("image"));
	}

	// ----- private methods -----
	private static List<ExtractedFigureData> extract(final byte[] data, final String name, final FigureOptions options) throws Exception {

		final Path path = Files.createTempFile("figure-test-", name);

		try {

			Files.write(path, data);

			return ContentExtraction.extractFigures(path, name, options);

		} finally {

			Files.deleteIfExists(path);
		}
	}

	private static void assertBoxContains(final double[] bbox, final double x0, final double y0, final double x1, final double y1) {

		final String box = "[" + bbox[0] + ", " + bbox[1] + ", " + (bbox[0] + bbox[2]) + ", " + (bbox[1] + bbox[3]) + "]";

		assertTrue("Box " + box + " starts left of " + x0, bbox[0] <= x0 + 1);
		assertTrue("Box " + box + " starts above " + y0, bbox[1] <= y0 + 1);
		assertTrue("Box " + box + " ends right of " + x1, bbox[0] + bbox[2] >= x1 - 1);
		assertTrue("Box " + box + " ends below " + y1, bbox[1] + bbox[3] >= y1 - 1);
	}
}
