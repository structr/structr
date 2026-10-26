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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.testng.SkipException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Test documents, generated in code so the tests show what each document contains.
 */
public final class Fixtures {

	/** Rendered into images for the OCR tests; a plain word Tesseract reads reliably. */
	public static final String OCR_WORD = "STRUCTURE";

	private Fixtures() {
	}

	public static byte[] html() {

		return """
			<html>
			<head><title>Installation Guide</title></head>
			<body>
			<h1>Installation</h1>
			<p>Structr needs a Java runtime.
			   This line belongs to the same paragraph.</p>
			<h2>Requirements</h2>
			<ul>
			  <li>Java 25</li>
			  <li>Neo4j
			    <ul><li>Version 5 or later</li></ul>
			  </li>
			</ul>
			<table>
			  <tr><th>Component</th><th>Version</th></tr>
			  <tr><td>Java</td><td>25</td></tr>
			  <tr><td>Neo4j</td><td>2026.08</td></tr>
			</table>
			<blockquote><p>Read the release notes first.</p></blockquote>
			<pre>bin/start
			bin/stop</pre>
			<h2>Download</h2>
			<p>See the <a href="https://structr.com/download">download page</a> for packages.</p>
			<h1>Configuration</h1>
			<p>The configuration lives in structr.conf.</p>
			</body>
			</html>
			""".getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] markdown() {

		return """
			# Handbuch

			Dieses Handbuch beschreibt die Einrichtung des Systems
			und die ersten Schritte.

			## Voraussetzungen

			- Java 25
			- Neo4j
			  - Version 5 oder neuer

			| Komponente | Version |
			|------------|---------|
			| Java       | 25      |

			> Bitte zuerst die Hinweise lesen.

			```
			bin/start
			```

			## Betrieb

			Der Server startet mit dem Startskript.
			""".getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] text() {

		return """
			First paragraph of a plain text file, which has no other structure.
			It continues on a second line.

			Second paragraph after a blank line.
			""".getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] docx() throws IOException {

		try (final XWPFDocument document = new XWPFDocument(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			document.getProperties().getCoreProperties().setTitle("Quarterly Report");
			document.getProperties().getCoreProperties().setCreator("Jane Doe");

			document.createParagraph().createRun().setText("Revenue grew in all regions.");

			final XWPFTable table = document.createTable(2, 2);

			table.getRow(0).getCell(0).setText("Region");
			table.getRow(0).getCell(1).setText("Revenue");
			table.getRow(1).getCell(0).setText("North");
			table.getRow(1).getCell(1).setText("1200");

			document.createParagraph().createRun().setText("Costs remained stable.");
			document.write(out);

			return out.toByteArray();
		}
	}

	/**
	 * A presentation with one text box per slide.
	 */
	public static byte[] pptx(final List<String> slides) throws IOException {

		try (final XMLSlideShow show = new XMLSlideShow(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			for (final String text : slides) {

				final XSLFSlide slide     = show.createSlide();
				final XSLFTextBox textBox = slide.createTextBox();

				textBox.setAnchor(new Rectangle(50, 50, 600, 100));
				textBox.setText(text);
			}

			show.write(out);

			return out.toByteArray();
		}
	}

	public static byte[] xlsx() throws IOException {

		try (final XSSFWorkbook workbook = new XSSFWorkbook(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			final XSSFSheet sheet = workbook.createSheet("Prices");
			final XSSFRow header  = sheet.createRow(0);
			final XSSFRow first   = sheet.createRow(1);
			final XSSFRow second  = sheet.createRow(2);

			header.createCell(0).setCellValue("Product");
			header.createCell(1).setCellValue("Price");
			first.createCell(0).setCellValue("Basic");
			first.createCell(1).setCellValue("10");
			second.createCell(0).setCellValue("Pro");
			second.createCell(1).setCellValue("25");

			workbook.write(out);

			return out.toByteArray();
		}
	}

	/**
	 * A PDF with real text, one line per page.
	 */
	public static byte[] pdf(final List<String> pages) throws IOException {

		try (final PDDocument document = new PDDocument(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			final PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

			for (final String text : pages) {

				final PDPage page = new PDPage();

				document.addPage(page);

				try (final PDPageContentStream content = new PDPageContentStream(document, page)) {

					content.beginText();
					content.setFont(font, 14);
					content.newLineAtOffset(72, 700);
					content.showText(text);
					content.endText();
				}
			}

			document.save(out);

			return out.toByteArray();
		}
	}

	/**
	 * A PDF page with a line of text and a watermark drawn diagonally across it.
	 */
	public static byte[] pdfWithWatermark() throws IOException {

		try (final PDDocument document = new PDDocument(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			final PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			final PDPage page      = new PDPage();

			document.addPage(page);

			try (final PDPageContentStream content = new PDPageContentStream(document, page)) {

				content.beginText();
				content.setFont(font, 14);
				content.newLineAtOffset(72, 700);
				content.showText("The regulation applies to all departments.");
				content.endText();

				content.beginText();
				content.setFont(font, 40);
				content.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(45), 100, 150));
				content.showText("This printout is not updated");
				content.endText();
			}

			document.save(out);

			return out.toByteArray();
		}
	}

	/**
	 * A PDF whose only page is an image of text, like a scan: there is no extractable text without OCR.
	 */
	public static byte[] scannedPdf() throws IOException {

		try (final PDDocument document = new PDDocument(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			final PDPage page           = new PDPage();
			final PDImageXObject image  = LosslessFactory.createFromImage(document, textImage(OCR_WORD + " SCAN"));

			document.addPage(page);

			try (final PDPageContentStream content = new PDPageContentStream(document, page)) {

				content.drawImage(image, 50, 500, 500, 80);
			}

			document.save(out);

			return out.toByteArray();
		}
	}

	/**
	 * A PDF page with a line of real text and an image of text below it, as in a report with a figure.
	 */
	public static byte[] pdfWithImage() throws IOException {

		try (final PDDocument document = new PDDocument(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			final PDPage page           = new PDPage();
			final PDImageXObject image  = LosslessFactory.createFromImage(document, textImage(OCR_WORD + " FIGURE"));

			document.addPage(page);

			try (final PDPageContentStream content = new PDPageContentStream(document, page)) {

				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
				content.newLineAtOffset(72, 700);
				content.showText("The figure below shows the result of the quarter.");
				content.endText();
				content.drawImage(image, 50, 500, 500, 80);
			}

			document.save(out);

			return out.toByteArray();
		}
	}

	/**
	 * A paper of three pages with a logo in the same place on every page.
	 *
	 * Page 1: a bar chart drawn as vector graphics (axes, bars, axis labels), "Figure 1: ..." below it, and
	 * text that refers to Figure 1 and to Figure 10. Page 2: a chart as an embedded picture with "Fig. 2.
	 * ..." below it, and a table with rules, "Table 1: ..." above it. Page 3: text only.
	 */
	public static byte[] paperPdf() throws IOException {

		try (final PDDocument document = new PDDocument(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			final PDType1Font font     = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			final PDImageXObject logo  = LosslessFactory.createFromImage(document, square(new Color(200, 30, 30)));
			final PDImageXObject chart = LosslessFactory.createFromImage(document, lineChart());

			for (int i = 0; i < 3; i++) {

				document.addPage(new PDPage(PDRectangle.LETTER));
			}

			try (final PDPageContentStream content = new PDPageContentStream(document, document.getPage(0))) {

				content.drawImage(logo, 500, 700, 80, 80);
				text(content, font, 14, 72, 720, "A Study of Model Accuracy");
				text(content, font, 10, 72, 690, "We compare three models on a benchmark. As Figure 1 shows, model C is the most accurate.");
				text(content, font, 10, 72, 676, "Figure 10 in the appendix lists all runs.");

				// axes
				content.moveTo(100, 620);
				content.lineTo(100, 420);
				content.lineTo(400, 420);
				content.stroke();

				// bars
				content.setNonStrokingColor(new Color(90, 90, 160));
				content.addRect(130, 420, 60, 80);
				content.addRect(220, 420, 60, 120);
				content.addRect(310, 420, 60, 170);
				content.fill();
				content.setNonStrokingColor(Color.BLACK);

				// tick marks
				for (int y = 470; y <= 620; y += 50) {

					content.moveTo(96, y);
					content.lineTo(100, y);
					content.stroke();
				}

				text(content, font, 9, 155, 410, "A");
				text(content, font, 9, 245, 410, "B");
				text(content, font, 9, 335, 410, "C");
				text(content, font, 9, 62, 520, "Accuracy");

				text(content, font, 9, 100, 385, "Figure 1: Accuracy of the three models on the benchmark.");
				text(content, font, 9, 100, 374, "Higher is better.");
				text(content, font, 10, 72, 330, "The remaining text of the page continues here.");
			}

			try (final PDPageContentStream content = new PDPageContentStream(document, document.getPage(1))) {

				content.drawImage(logo, 500, 700, 80, 80);
				text(content, font, 10, 72, 720, "Fig. 2 shows the training loss, which drops quickly.");

				content.drawImage(chart, 100, 450, 300, 200);
				text(content, font, 9, 100, 432, "Fig. 2. Training loss over time.");

				text(content, font, 9, 100, 362, "Table 1: Results on the test set.");

				for (final int y : new int[] { 350, 330, 280 }) {

					content.moveTo(100, y);
					content.lineTo(400, y);
					content.stroke();
				}

				text(content, font, 9, 110, 337, "Model");
				text(content, font, 9, 300, 337, "Accuracy");

				final String[][] rows = { { "A", "0.81" }, { "B", "0.86" }, { "C", "0.92" } };

				for (int i = 0; i < rows.length; i++) {

					text(content, font, 9, 110, 317 - i * 14, rows[i][0]);
					text(content, font, 9, 300, 317 - i * 14, rows[i][1]);
				}

				text(content, font, 10, 72, 250, "The results in Table 1 confirm this.");
			}

			try (final PDPageContentStream content = new PDPageContentStream(document, document.getPage(2))) {

				content.drawImage(logo, 500, 700, 80, 80);
				text(content, font, 14, 72, 720, "Appendix");
				text(content, font, 10, 72, 690, "This page has no figures.");
			}

			document.save(out);

			return out.toByteArray();
		}
	}

	/**
	 * A Word document with a chart picture used twice and a tiny icon.
	 */
	public static byte[] docxWithPictures() throws IOException {

		try (final XWPFDocument document = new XWPFDocument(); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			final byte[] chart = pngOf(lineChart());
			final byte[] icon  = pngOf(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB));

			document.createParagraph().createRun().setText("The chart shows the training loss.");

			try {

				document.createParagraph().createRun().addPicture(new ByteArrayInputStream(chart), Document.PICTURE_TYPE_PNG, "chart.png", Units.toEMU(300), Units.toEMU(200));
				document.createParagraph().createRun().addPicture(new ByteArrayInputStream(icon), Document.PICTURE_TYPE_PNG, "icon.png", Units.toEMU(16), Units.toEMU(16));
				document.createParagraph().createRun().addPicture(new ByteArrayInputStream(chart), Document.PICTURE_TYPE_PNG, "chart-again.png", Units.toEMU(300), Units.toEMU(200));

			} catch (final InvalidFormatException ex) {

				throw new IOException(ex);
			}

			document.write(out);

			return out.toByteArray();
		}
	}

	public static byte[] png() throws IOException {

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			ImageIO.write(textImage(OCR_WORD + " PROBE"), "png", out);

			return out.toByteArray();
		}
	}

	/**
	 * A PNG without text, drawn without fonts, for tests that only need an image.
	 */
	public static byte[] blankPng() throws IOException {

		final BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = image.createGraphics();

		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		graphics.dispose();

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			ImageIO.write(image, "png", out);

			return out.toByteArray();
		}
	}

	/**
	 * An e-mail with a plain text body and a text file attached.
	 */
	public static byte[] eml() {

		final String attachment = Base64.getMimeEncoder().encodeToString("Attached delivery note for order 4711.".getBytes(StandardCharsets.UTF_8));

		return ("""
			From: shop@example.com
			To: customer@example.com
			Subject: Your order
			MIME-Version: 1.0
			Content-Type: multipart/mixed; boundary="BOUNDARY"

			--BOUNDARY
			Content-Type: text/plain; charset=UTF-8

			Thank you for your order.
			--BOUNDARY
			Content-Type: text/plain; charset=UTF-8; name="delivery.txt"
			Content-Disposition: attachment; filename="delivery.txt"
			Content-Transfer-Encoding: base64

			""" + attachment + """

			--BOUNDARY--
			""").replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * A ZIP archive with a text file, a text file in a directory, and a nested ZIP containing another text file.
	 */
	public static byte[] zip() throws IOException {

		final byte[] inner = zipOf("nested.txt", "Text inside the nested archive.");

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final ZipOutputStream zip = new ZipOutputStream(out)) {

			zip.putNextEntry(new ZipEntry("readme.txt"));
			zip.write("Text of the readme entry.".getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			zip.putNextEntry(new ZipEntry("docs/guide.txt"));
			zip.write("Text of the second entry.".getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			zip.putNextEntry(new ZipEntry("inner.zip"));
			zip.write(inner);
			zip.closeEntry();

			zip.finish();

			return out.toByteArray();
		}
	}

	// ----- private methods -----
	private static void text(final PDPageContentStream content, final PDType1Font font, final float size, final float x, final float y, final String text) throws IOException {

		content.beginText();
		content.setFont(font, size);
		content.newLineAtOffset(x, y);
		content.showText(text);
		content.endText();
	}

	/**
	 * A line chart drawn without fonts: axes and a falling line.
	 */
	private static BufferedImage lineChart() {

		final BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = image.createGraphics();

		try {

			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			graphics.setColor(Color.BLACK);
			graphics.setStroke(new BasicStroke(3));
			graphics.drawLine(50, 20, 50, 350);
			graphics.drawLine(50, 350, 580, 350);
			graphics.setColor(new Color(30, 120, 30));
			graphics.drawPolyline(new int[] { 60, 150, 250, 350, 450, 570 }, new int[] { 40, 180, 250, 290, 310, 320 }, 6);

		} finally {

			graphics.dispose();
		}

		return image;
	}

	private static BufferedImage square(final Color color) {

		final BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = image.createGraphics();

		try {

			graphics.setColor(color);
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		} finally {

			graphics.dispose();
		}

		return image;
	}

	private static byte[] pngOf(final BufferedImage image) throws IOException {

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			ImageIO.write(image, "png", out);

			return out.toByteArray();
		}
	}

	private static byte[] zipOf(final String name, final String content) throws IOException {

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final ZipOutputStream zip = new ZipOutputStream(out)) {

			zip.putNextEntry(new ZipEntry(name));
			zip.write(content.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
			zip.finish();

			return out.toByteArray();
		}
	}

	/**
	 * An image of the given text, for the OCR tests. Drawing text needs fonts (fontconfig and at least one
	 * font on Linux); without them the OCR tests are skipped, like without Tesseract.
	 */
	private static BufferedImage textImage(final String text) {

		final BufferedImage image = new BufferedImage(1400, 220, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = image.createGraphics();

		try {

			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			graphics.setColor(Color.BLACK);
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 96));
			graphics.drawString(text, 40, 150);

		} catch (final LinkageError | AWTError ex) {

			throw new SkipException("No fonts available to draw the OCR test image (install fontconfig and a font such as DejaVu Sans): " + ex);

		} finally {

			graphics.dispose();
		}

		return image;
	}
}
