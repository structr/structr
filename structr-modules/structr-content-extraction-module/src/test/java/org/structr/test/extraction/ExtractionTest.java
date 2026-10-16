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

import org.apache.tika.parser.Parser;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.document.DocumentBlock;
import org.structr.extraction.api.document.DocumentBlockType;
import org.structr.extraction.api.document.DocumentMetadata;
import org.structr.extraction.api.document.EmbeddedDocument;
import org.structr.extraction.api.document.ExtractedDocument;
import org.structr.extraction.api.document.ExtractedLink;
import org.structr.extraction.api.document.ExtractedTable;
import org.structr.extraction.api.document.OcrSkipReason;
import org.structr.extraction.api.options.ExtractionOptions;
import org.structr.extraction.api.options.OcrMode;
import org.structr.extraction.parser.tika.TikaDocumentExtractor;
import org.structr.extraction.parser.tika.TikaParserSetup;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Extraction of the supported formats, without a database.
 */
public class ExtractionTest {

	@Test
	public void testHtmlStructure() throws Exception {

		final ExtractedDocument document = extract(Fixtures.html(), "guide.html", options());
		final List<DocumentBlock> blocks         = document.getBlocks();
		final DocumentBlock installation = find(blocks, DocumentBlockType.HEADING, "Installation");

		assertEquals(1, installation.level());
		assertEquals(List.of("Installation"), installation.headingPath());

		final DocumentBlock paragraph = find(blocks, DocumentBlockType.PARAGRAPH, "Structr needs a Java runtime. This line belongs to the same paragraph.");
		assertEquals("Line breaks inside a paragraph are joined", List.of("Installation"), paragraph.headingPath());

		final DocumentBlock requirements = find(blocks, DocumentBlockType.HEADING, "Requirements");
		assertEquals(2, requirements.level());
		assertEquals(List.of("Installation", "Requirements"), requirements.headingPath());

		assertEquals(1, find(blocks, DocumentBlockType.LIST_ITEM, "Java 25").level());
		assertEquals("Nested list items keep their depth", 2, find(blocks, DocumentBlockType.LIST_ITEM, "Version 5 or later").level());

		final ExtractedTable table = document.getTables().getFirst();
		assertEquals(List.of(List.of("Component", "Version"), List.of("Java", "25"), List.of("Neo4j", "2026.08")), table.rows());
		assertEquals(List.of("Installation", "Requirements"), table.headingPath());

		find(blocks, DocumentBlockType.QUOTE, "Read the release notes first.");
		assertTrue("Code keeps its line breaks", find(blocks, DocumentBlockType.CODE, null).text().contains("bin/start\nbin/stop"));

		final DocumentBlock configuration = find(blocks, DocumentBlockType.HEADING, "Configuration");
		assertEquals("A new top-level heading closes the previous section", List.of("Configuration"), configuration.headingPath());
		assertEquals(List.of("Configuration"), find(blocks, DocumentBlockType.PARAGRAPH, "The configuration lives in structr.conf.").headingPath());

		final ExtractedLink link = document.getLinks().getFirst();
		assertEquals("https://structr.com/download", link.url());
		assertEquals("download page", link.text());

		final String markdown = document.getMarkdown();
		assertTrue(markdown, markdown.contains("# Installation"));
		assertTrue(markdown, markdown.contains("## Requirements"));
		assertTrue(markdown, markdown.contains("- Java 25\n- Neo4j\n  - Version 5 or later"));
		assertTrue(markdown, markdown.contains("| Component | Version |\n| --- | --- |\n| Java | 25 |"));
		assertTrue(markdown, markdown.contains("> Read the release notes first."));
		assertTrue(markdown, markdown.contains("```\nbin/start"));

		assertEquals("Installation Guide", document.getMetadata().title());
		assertNull("HTML has no pages", paragraph.page());
	}

	@Test
	public void testBlockOffsetsPointIntoText() throws Exception {

		final ExtractedDocument document = extract(Fixtures.html(), "guide.html", options());

		for (final DocumentBlock block : document.getBlocks()) {

			assertEquals("Offsets of " + block.type() + " block", block.text(), document.getText().substring(block.startOffset(), block.endOffset()));
		}
	}

	@Test
	public void testMarkdownStructure() throws Exception {

		final ExtractedDocument document = extract(Fixtures.markdown(), "handbuch.md", options());
		final List<DocumentBlock> blocks         = document.getBlocks();

		assertEquals(1, find(blocks, DocumentBlockType.HEADING, "Handbuch").level());
		assertEquals(List.of("Handbuch", "Voraussetzungen"), find(blocks, DocumentBlockType.HEADING, "Voraussetzungen").headingPath());
		find(blocks, DocumentBlockType.PARAGRAPH, "Dieses Handbuch beschreibt die Einrichtung des Systems und die ersten Schritte.");
		assertEquals(2, find(blocks, DocumentBlockType.LIST_ITEM, "Version 5 oder neuer").level());
		assertEquals(List.of(List.of("Komponente", "Version"), List.of("Java", "25")), document.getTables().getFirst().rows());
		find(blocks, DocumentBlockType.QUOTE, "Bitte zuerst die Hinweise lesen.");
		assertEquals("bin/start", find(blocks, DocumentBlockType.CODE, null).text());
		assertEquals(List.of("Handbuch", "Betrieb"), find(blocks, DocumentBlockType.PARAGRAPH, "Der Server startet mit dem Startskript.").headingPath());

		assertEquals("de", document.getMetadata().language());
	}

	@Test
	public void testPlainTextParagraphs() throws Exception {

		final ExtractedDocument document = extract(Fixtures.text(), "notes.txt", options());

		assertEquals(2, document.getBlocks().size());
		assertEquals("First paragraph of a plain text file, which has no other structure. It continues on a second line.", document.getBlocks().get(0).text());
		assertEquals("Second paragraph after a blank line.", document.getBlocks().get(1).text());
		assertEquals("en", document.getMetadata().language());
	}

	@Test
	public void testDocx() throws Exception {

		final ExtractedDocument document = extract(Fixtures.docx(), "report.docx", options());

		find(document.getBlocks(), DocumentBlockType.PARAGRAPH, "Revenue grew in all regions.");
		find(document.getBlocks(), DocumentBlockType.PARAGRAPH, "Costs remained stable.");
		assertEquals(List.of(List.of("Region", "Revenue"), List.of("North", "1200")), document.getTables().getFirst().rows());
		assertEquals("Quarterly Report", document.getMetadata().title());
		assertEquals("Jane Doe", document.getMetadata().author());
	}

	@Test
	public void testXlsxSheetIsTable() throws Exception {

		final ExtractedDocument document = extract(Fixtures.xlsx(), "prices.xlsx", options());
		final ExtractedTable table       = document.getTables().getFirst();

		assertEquals(List.of(List.of("Product", "Price"), List.of("Basic", "10"), List.of("Pro", "25")), table.rows());
		assertEquals("The sheet name is the heading of its table", List.of("Prices"), table.headingPath());
	}

	@Test
	public void testPdfPages() throws Exception {

		final ExtractedDocument document = extract(Fixtures.pdf(List.of("Text on the first page.", "Text on the second page.")), "two-pages.pdf", options());

		assertEquals(Integer.valueOf(2), document.getMetadata().pageCount());
		assertEquals(Integer.valueOf(1), find(document.getBlocks(), DocumentBlockType.PARAGRAPH, "Text on the first page.").page());
		assertEquals(Integer.valueOf(2), find(document.getBlocks(), DocumentBlockType.PARAGRAPH, "Text on the second page.").page());
	}

	@Test
	public void testPptxSlides() throws Exception {

		// read with Tika's SAX extractor (TikaParserSetup.officeConfig), since POI does not export the package
		// of its own PPTX extractor; that only fails on the module path, these tests run on the class path
		final ExtractedDocument document = extract(Fixtures.pptx(List.of("Text on the first slide.", "Text on the second slide.")), "slides.pptx", options());

		assertEquals(Integer.valueOf(2), document.getMetadata().pageCount());
		assertEquals(Integer.valueOf(1), find(document.getBlocks(), DocumentBlockType.PARAGRAPH, "Text on the first slide.").page());
		assertEquals(Integer.valueOf(2), find(document.getBlocks(), DocumentBlockType.PARAGRAPH, "Text on the second slide.").page());
	}

	@Test
	public void testFileIsReadInPlace() throws Exception {

		final byte[] pdf = Fixtures.pdf(List.of("Text on the first page.", "Text on the second page."));
		final Path copy  = Files.createTempFile("structr-extraction-test-", ".tmp");

		try {

			Files.write(copy, pdf);

			// the temporary copy has no meaningful name; detection uses the file name, like in processing
			final ExtractedDocument fromPath   = ContentExtraction.extract(copy, "two-pages.pdf", options());
			final ExtractedDocument fromStream = extract(pdf, "two-pages.pdf", options());

			assertEquals("application/pdf", fromPath.getMetadata().mimeType());
			assertEquals(fromStream.getText(), fromPath.getText());
			assertEquals(fromStream.getMetadata().pageCount(), fromPath.getMetadata().pageCount());
			assertTrue("The file is left in place", Files.exists(copy));

		} finally {

			Files.deleteIfExists(copy);
		}
	}

	@Test
	public void testMaxCharactersTruncates() throws Exception {

		final ExtractedDocument document = extract(Fixtures.text(), "notes.txt", options().setMaxCharacters(20));

		assertTrue(document.getMetadata().truncated());
		assertTrue("Text is cut at the limit: " + document.getText(), document.getText().length() <= 20);
	}

	@Test
	public void testIncludeEmbeddedExtractsAttachments() throws Exception {

		final ExtractedDocument without = extract(Fixtures.eml(), "order.eml", options());
		final ExtractedDocument with    = extract(Fixtures.eml(), "order.eml", options().setIncludeEmbedded(true));

		assertTrue(without.getText(), without.getText().contains("Thank you for your order."));
		assertFalse("Attachments are not extracted by default", without.getText().contains("delivery note"));
		assertTrue(with.getText(), with.getText().contains("Attached delivery note for order 4711."));
	}

	@Test
	public void testExtractEmbeddedListsAttachments() throws Exception {

		final List<EmbeddedDocument> documents = TikaDocumentExtractor.extractEmbedded(new ByteArrayInputStream(Fixtures.eml()), "order.eml", options(), true);
		final EmbeddedDocument attachment      = documents.stream().filter(d -> "delivery.txt".equals(d.name())).findFirst().orElse(null);

		assertNotNull("Attachment listed: " + documents, attachment);
		assertEquals("Attached delivery note for order 4711.", attachment.text());
		assertTrue(attachment.mimeType(), attachment.mimeType().startsWith("text/plain"));
	}

	@Test
	public void testExtractEmbeddedHonoursDepth() throws Exception {

		final List<EmbeddedDocument> all     = TikaDocumentExtractor.extractEmbedded(new ByteArrayInputStream(Fixtures.zip()), "archive.zip", options(), true);
		final List<EmbeddedDocument> shallow = TikaDocumentExtractor.extractEmbedded(new ByteArrayInputStream(Fixtures.zip()), "archive.zip", options().setMaxEmbeddedDepth(1), false);

		assertTrue("Nested entries are found: " + all, all.stream().anyMatch(d -> "nested.txt".equals(d.name()) && d.depth() == 2));
		assertTrue(shallow.stream().anyMatch(d -> "readme.txt".equals(d.name())));
		assertFalse("Entries below the maximum depth are left out: " + shallow, shallow.stream().anyMatch(d -> "nested.txt".equals(d.name())));
		assertNull("No text without includeText", shallow.getFirst().text());
	}

	@Test
	public void testMetadataOnly() throws Exception {

		final DocumentMetadata metadata = TikaDocumentExtractor.extractMetadata(new ByteArrayInputStream(Fixtures.docx()), "report.docx");

		assertEquals("Quarterly Report", metadata.title());
		assertEquals("Jane Doe", metadata.author());
		assertTrue(metadata.mimeType(), metadata.mimeType().contains("wordprocessingml"));
	}

	@Test
	public void testNoExternalToolParsers() throws Exception {

		final List<String> parsers = new ArrayList<>();

		TikaParserSetup.walk(TikaParserSetup.getSetup().parser(), (final Parser parser) -> parsers.add(parser.getClass().getName()));

		for (final String excluded : TikaParserSetup.EXCLUDED_PARSERS) {

			assertFalse(excluded + " must not be part of the parser tree", parsers.contains(excluded));
		}

		assertTrue("Tesseract is the one external program allowed, as a configured parser", parsers.contains("org.apache.tika.parser.ocr.TesseractOCRParser"));
	}

	// ----- OCR -----
	@Test
	public void testOcrReadsImages() throws Exception {

		requireTesseract();

		final ExtractedDocument document = extract(Fixtures.png(), "probe.png", options().setOcrMode(OcrMode.AUTO));

		assertTrue("OCR text: '" + document.getText() + "', " + ocrDiagnostics(document), document.getText().contains(Fixtures.OCR_WORD));
		assertTrue(document.getMetadata().ocrApplied());
		assertNull(document.getMetadata().ocrSkippedReason());
	}

	@Test
	public void testOcrOff() throws Exception {

		requireTesseract();

		final ExtractedDocument document = extract(Fixtures.png(), "probe.png", options().setOcrMode(OcrMode.OFF));

		assertFalse(document.getText().contains(Fixtures.OCR_WORD));
		assertFalse(document.getMetadata().ocrApplied());
		assertEquals(OcrSkipReason.MODE_OFF, document.getMetadata().ocrSkippedReason());
	}

	@Test
	public void testOcrReadsScannedPdf() throws Exception {

		requireTesseract();

		final ExtractedDocument document = extract(Fixtures.scannedPdf(), "scan.pdf", options().setOcrMode(OcrMode.AUTO));

		assertTrue("OCR text: " + document.getText(), document.getText().contains(Fixtures.OCR_WORD));
		assertTrue(document.getMetadata().ocrApplied());
		assertEquals("OCR text keeps its page", Integer.valueOf(1), document.getBlocks().getFirst().page());
	}

	@Test
	public void testAutoModeLeavesTextPdfAlone() throws Exception {

		requireTesseract();

		final ExtractedDocument document = extract(Fixtures.pdf(List.of("Born-digital text.")), "text.pdf", options().setOcrMode(OcrMode.AUTO));

		assertEquals("Born-digital text.", document.getText());
		assertFalse("A page with text is not OCR'd in auto mode", document.getMetadata().ocrApplied());
	}

	@Test
	public void testOcrMaxPages() throws Exception {

		requireTesseract();

		final ExtractedDocument document = extract(Fixtures.scannedPdf(), "scan.pdf", options().setOcrMode(OcrMode.AUTO).setOcrMaxPages(0));

		assertFalse(document.getText().contains(Fixtures.OCR_WORD));
		assertEquals(OcrSkipReason.MAX_PAGES_EXCEEDED, document.getMetadata().ocrSkippedReason());
	}

	@Test
	public void testOcrLanguagesNotInstalled() throws Exception {

		requireTesseract();

		final ExtractedDocument document = extract(Fixtures.png(), "probe.png", options().setOcrMode(OcrMode.AUTO).setOcrLanguages("xyz"));

		assertEquals(OcrSkipReason.LANGUAGES_NOT_INSTALLED, document.getMetadata().ocrSkippedReason());
	}

	@Test
	public void testMissingTesseractDegradesGracefully() throws Exception {

		final String previous = ContentExtractionSettings.TesseractPath.getValue();

		try {

			ContentExtractionSettings.TesseractPath.setValue("/nonexistent/tesseract/bin");

			final ExtractedDocument document = extract(Fixtures.png(), "probe.png", options().setOcrMode(OcrMode.AUTO));

			assertFalse(ContentExtraction.getOcrInfo().available());
			assertFalse(document.getMetadata().ocrApplied());
			assertEquals(OcrSkipReason.TESSERACT_NOT_INSTALLED, document.getMetadata().ocrSkippedReason());

		} finally {

			ContentExtractionSettings.TesseractPath.setValue(previous);
		}
	}

	// ----- private methods -----
	private static ExtractionOptions options() {

		return ExtractionOptions.fromSettings().setOcrLanguages("eng");
	}

	private static ExtractedDocument extract(final byte[] data, final String fileName, final ExtractionOptions options) throws Exception {

		return ContentExtraction.extract(new ByteArrayInputStream(data), fileName, options);
	}

	private static String ocrDiagnostics(final ExtractedDocument document) throws Exception {

		return "skipped: " + document.getMetadata().ocrSkippedReason() + ", parsed by: " + document.getMetadata().raw().get("X-TIKA:Parsed-By") + ", ocr: " + ContentExtraction.getOcrInfo();
	}

	private static void requireTesseract() throws Exception {

		if (!ContentExtraction.getOcrInfo().available()) {

			throw new SkipException("tesseract is not installed, OCR cannot be tested");
		}
	}

	/**
	 * The first block of the given type with the given text, or of the given type if text is null.
	 */
	private static DocumentBlock find(final List<DocumentBlock> blocks, final DocumentBlockType type, final String text) {

		for (final DocumentBlock block : blocks) {

			if (block.type() == type && (text == null || text.equals(block.text()))) {

				return block;
			}
		}

		fail("No " + type + " block" + (text != null ? " with text '" + text + "'" : "") + " in " + blocks.stream().map(b -> b.type() + ": " + b.text()).toList());

		return null;
	}
}
