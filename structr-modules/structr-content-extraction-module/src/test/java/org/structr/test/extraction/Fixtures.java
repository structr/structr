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
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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

	public static byte[] png() throws IOException {

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			ImageIO.write(textImage(OCR_WORD + " PROBE"), "png", out);

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
	 * A ZIP archive with a text file and a nested ZIP containing another text file.
	 */
	public static byte[] zip() throws IOException {

		final byte[] inner = zipOf("nested.txt", "Text inside the nested archive.");

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final ZipOutputStream zip = new ZipOutputStream(out)) {

			zip.putNextEntry(new ZipEntry("readme.txt"));
			zip.write("Text of the readme entry.".getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			zip.putNextEntry(new ZipEntry("inner.zip"));
			zip.write(inner);
			zip.closeEntry();

			zip.finish();

			return out.toByteArray();
		}
	}

	// ----- private methods -----
	private static byte[] zipOf(final String name, final String content) throws IOException {

		try (final ByteArrayOutputStream out = new ByteArrayOutputStream(); final ZipOutputStream zip = new ZipOutputStream(out)) {

			zip.putNextEntry(new ZipEntry(name));
			zip.write(content.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
			zip.finish();

			return out.toByteArray();
		}
	}

	private static BufferedImage textImage(final String text) {

		final BufferedImage image = new BufferedImage(1400, 220, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = image.createGraphics();

		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		graphics.setColor(Color.BLACK);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 96));
		graphics.drawString(text, 40, 150);
		graphics.dispose();

		return image;
	}
}
