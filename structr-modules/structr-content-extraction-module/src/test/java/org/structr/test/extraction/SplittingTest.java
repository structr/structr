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
import org.structr.extraction.api.chunk.Chunk;
import org.structr.extraction.api.document.DetectedLanguage;
import org.structr.extraction.api.document.ExtractedDocument;
import org.structr.extraction.api.options.ExtractionOptions;
import org.structr.extraction.api.options.NormalizeOptions;
import org.structr.extraction.api.options.OcrMode;
import org.structr.extraction.api.options.SizeUnit;
import org.structr.extraction.api.options.SplitOptions;
import org.structr.extraction.api.options.SplitterType;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Splitting, token counting, normalization and language detection, without a database.
 */
public class SplittingTest {

	private static final String PARAGRAPH_A = "Graph databases store data as nodes and relationships. Queries follow the relationships directly.";
	private static final String PARAGRAPH_B = "Structr builds on a graph database. Its schema defines types, properties and relationships.";
	private static final String PARAGRAPH_C = "Content extraction turns files into text. The text is split into chunks for retrieval.";
	private static final String TEXT        = PARAGRAPH_A + "\n\n" + PARAGRAPH_B + "\n\n" + PARAGRAPH_C;

	// ----- recursive -----
	@Test
	public void testRecursiveKeepsParagraphsWhole() {

		final List<Chunk> chunks = ContentExtraction.split(TEXT, characters(SplitterType.RECURSIVE, 120, 0));

		assertEquals(List.of(PARAGRAPH_A, PARAGRAPH_B, PARAGRAPH_C), chunks.stream().map(Chunk::text).toList());
		assertChunksPointIntoText(TEXT, chunks);
	}

	@Test
	public void testRecursiveRespectsSizeAndOverlap() {

		// words as the smallest pieces, so whole pieces fit into the overlap
		final String words       = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda my ny xi omikron pi rho sigma tau ypsilon";
		final List<Chunk> chunks = ContentExtraction.split(words, characters(SplitterType.RECURSIVE, 40, 15));

		assertTrue("Several chunks expected: " + chunks.size(), chunks.size() > 3);

		for (int i = 0; i < chunks.size(); i++) {

			assertTrue("Chunk too large: " + chunks.get(i).text(), chunks.get(i).charCount() <= 40);
			assertEquals(i, chunks.get(i).position());
		}

		assertTrue("Consecutive chunks overlap", chunks.stream().skip(1).allMatch(c -> c.startOffset() < chunks.get(c.position() - 1).endOffset()));
		assertChunksPointIntoText(words, chunks);
	}

	@Test
	public void testOverlapIsMadeOfWholePieces() {

		// sentences are larger than the overlap, so they are not repeated (LangChain semantics)
		final List<Chunk> chunks = ContentExtraction.split(TEXT, characters(SplitterType.RECURSIVE, 60, 20));

		assertTrue(chunks.stream().allMatch(c -> c.charCount() <= 60));
		assertTrue(chunks.stream().skip(1).allMatch(c -> c.startOffset() >= chunks.get(c.position() - 1).endOffset()));
	}

	@Test
	public void testRecursiveCutsTextWithoutSeparators() {

		final String word        = "x".repeat(1000);
		final List<Chunk> chunks = ContentExtraction.split(word, characters(SplitterType.RECURSIVE, 100, 0));

		assertEquals(10, chunks.size());
		assertTrue(chunks.stream().allMatch(c -> c.charCount() == 100));
	}

	@Test
	public void testRecursiveWithCustomSeparators() {

		final List<Chunk> chunks = ContentExtraction.split("a;b;c;d", characters(SplitterType.RECURSIVE, 4, 0).setSeparators(List.of(";")));

		assertEquals(List.of("a;b;", "c;d"), chunks.stream().map(Chunk::text).toList());
	}

	@Test
	public void testTokenSizes() {

		final SplitOptions options = new SplitOptions().setSplitter(SplitterType.RECURSIVE).setChunkSize(20).setChunkOverlap(5).setSizeUnit(SizeUnit.TOKENS);
		final List<Chunk> chunks   = ContentExtraction.split(TEXT, options);

		for (final Chunk chunk : chunks) {

			// pieces are measured separately and added up, which can be off by a token at piece boundaries
			assertTrue("Chunk of " + chunk.tokenCount() + " tokens: " + chunk.text(), chunk.tokenCount() <= 21);
		}

		assertChunksPointIntoText(TEXT, chunks);
	}

	// ----- sentence -----
	@Test
	public void testSentenceSplitterKeepsSentencesWhole() {

		final List<Chunk> chunks = ContentExtraction.split(TEXT, characters(SplitterType.SENTENCE, 100, 0));

		for (final Chunk chunk : chunks) {

			assertTrue("Chunk ends with a sentence: '" + chunk.text() + "'", chunk.text().endsWith("."));
		}

		assertChunksPointIntoText(TEXT, chunks);
	}

	// ----- token -----
	@Test
	public void testTokenWindows() {

		final String text          = "Grüße aus Köln 🚀 und noch mehr Text mit Umlauten: äöü ß. ".repeat(10);
		final SplitOptions options = new SplitOptions().setSplitter(SplitterType.TOKEN).setChunkSize(30).setChunkOverlap(10);
		final List<Chunk> chunks   = ContentExtraction.split(text, options);
		final int total            = ContentExtraction.countTokens(text, "cl100k_base");

		assertTrue(chunks.size() > 2);
		// a window boundary inside a multi-token character (the emoji) moves to its start, which can add its extra tokens
		assertTrue("Windows are about the chunk size: " + chunks.stream().map(Chunk::tokenCount).toList(), chunks.stream().allMatch(c -> c.tokenCount() <= 33));
		assertTrue("Windows overlap", chunks.get(1).startOffset() < chunks.get(0).endOffset());
		assertTrue("Windows cover the whole text", total <= chunks.stream().mapToInt(Chunk::tokenCount).sum());

		for (int i = 1; i < chunks.size(); i++) {

			assertTrue("No text between windows is lost", chunks.get(i).startOffset() <= chunks.get(i - 1).endOffset());
		}

		// offsets refer to the text, even with characters that span several tokens
		for (final Chunk chunk : chunks) {

			assertEquals(text.substring(chunk.startOffset(), chunk.endOffset()).strip(), chunk.text());
		}
	}

	// ----- structure -----
	@Test
	public void testStructureFollowsSections() throws Exception {

		final ExtractedDocument document = html();
		final List<Chunk> chunks         = ContentExtraction.split(document, tokens(SplitterType.STRUCTURE, 1000, 0));

		assertEquals("One chunk per section of level 1 or 2: " + chunks.stream().map(Chunk::text).toList(), 4, chunks.size());
		assertTrue(chunks.get(0).text().startsWith("Installation"));
		assertTrue("A heading stays with its content", chunks.get(1).text().startsWith("Requirements"));
		assertTrue(chunks.get(1).text().contains("Java | 25"));
		assertEquals(List.of("Installation", "Requirements"), chunks.get(1).headingPath());
		assertTrue(chunks.get(3).text().startsWith("Configuration"));
		assertEquals(List.of("Configuration"), chunks.get(3).headingPath());
		assertChunksPointIntoText(document.getText(), chunks);
	}

	@Test
	public void testStructureSplitsLargeBlocksAndNeverEndsWithHeading() throws Exception {

		final ExtractedDocument document = html();
		final List<Chunk> chunks         = ContentExtraction.split(document, tokens(SplitterType.STRUCTURE, 12, 0));
		final List<String> headings      = List.of("Installation", "Requirements", "Download", "Configuration");

		for (final Chunk chunk : chunks) {

			assertTrue("Chunk of " + chunk.tokenCount() + " tokens: " + chunk.text(), chunk.tokenCount() <= 13);
			assertFalse("A chunk must not be just a heading: " + chunk.text(), headings.contains(chunk.text()));
		}

		assertChunksPointIntoText(document.getText(), chunks);
	}

	@Test
	public void testIncludeHeadingPath() throws Exception {

		final List<Chunk> chunks = ContentExtraction.split(html(), tokens(SplitterType.STRUCTURE, 12, 0).setIncludeHeadingPath(true));
		final Chunk table        = chunks.stream().filter(c -> c.text().contains("Neo4j | 2026.08")).findFirst().orElseThrow();

		assertTrue("Section path in front of the chunk: " + table.text(), table.text().startsWith("Installation > Requirements\n\n"));
		assertEquals(table.charCount(), table.text().length());
	}

	@Test
	public void testStructureOnMarkdownText() {

		final String markdown    = new String(Fixtures.markdown(), java.nio.charset.StandardCharsets.UTF_8);
		final List<Chunk> chunks = ContentExtraction.split(markdown, tokens(SplitterType.STRUCTURE, 1000, 0));

		assertEquals(3, chunks.size());
		assertEquals(List.of("Handbuch"), chunks.get(0).headingPath());
		assertEquals(List.of("Handbuch", "Voraussetzungen"), chunks.get(1).headingPath());
		assertFalse("Markdown markup is not part of the chunks", chunks.get(1).text().contains("##"));
	}

	// ----- page -----
	@Test
	public void testPageSplitter() throws Exception {

		final ExtractedDocument document = ContentExtraction.extract(new ByteArrayInputStream(Fixtures.pdf(List.of("Text on the first page.", "Text on the second page."))), "two.pdf", new ExtractionOptions().setOcrMode(OcrMode.OFF));
		final List<Chunk> chunks         = ContentExtraction.split(document, tokens(SplitterType.PAGE, 1000, 0));

		assertEquals(2, chunks.size());
		assertEquals(Integer.valueOf(1), chunks.get(0).pageStart());
		assertEquals(Integer.valueOf(1), chunks.get(0).pageEnd());
		assertEquals(Integer.valueOf(2), chunks.get(1).pageStart());
	}

	@Test
	public void testChunksCarryPagesWithOtherSplitters() throws Exception {

		final ExtractedDocument document = ContentExtraction.extract(new ByteArrayInputStream(Fixtures.pdf(List.of("Text on the first page.", "Text on the second page."))), "two.pdf", new ExtractionOptions().setOcrMode(OcrMode.OFF));
		final List<Chunk> chunks         = ContentExtraction.split(document, tokens(SplitterType.RECURSIVE, 1000, 0));

		assertEquals(1, chunks.size());
		assertEquals(Integer.valueOf(1), chunks.getFirst().pageStart());
		assertEquals(Integer.valueOf(2), chunks.getFirst().pageEnd());
	}

	// ----- general -----
	@Test
	public void testContentHash() {

		final List<Chunk> first  = ContentExtraction.split(TEXT, characters(SplitterType.RECURSIVE, 120, 0));
		final List<Chunk> second = ContentExtraction.split(PARAGRAPH_A + "\n\n" + "Something else entirely.", characters(SplitterType.RECURSIVE, 120, 0));

		assertEquals(64, first.getFirst().contentHash().length());
		assertEquals("Same text, same hash", first.get(0).contentHash(), second.get(0).contentHash());
		assertFalse(first.get(1).contentHash().equals(second.get(1).contentHash()));
	}

	@Test
	public void testInvalidOptions() {

		assertThrows(() -> ContentExtraction.split(TEXT, characters(SplitterType.RECURSIVE, 100, 100)), "chunkOverlap");
		assertThrows(() -> ContentExtraction.split(TEXT, characters(SplitterType.RECURSIVE, 0, 0)), "chunkSize");
		assertThrows(() -> ContentExtraction.split(TEXT, characters(SplitterType.RECURSIVE, 100, 0).setTokenEncoding("gpt-17")), "Unknown token encoding");
	}

	@Test
	public void testEmptyText() {

		assertTrue(ContentExtraction.split("", characters(SplitterType.RECURSIVE, 100, 0)).isEmpty());
		assertTrue(ContentExtraction.split("  \n\n ", characters(SplitterType.STRUCTURE, 100, 0)).isEmpty());
		assertTrue(ContentExtraction.split("", tokens(SplitterType.TOKEN, 100, 0)).isEmpty());
	}

	// ----- tokens -----
	@Test
	public void testCountTokens() {

		assertEquals(2, ContentExtraction.countTokens("Hello world", "cl100k_base"));
		assertEquals("Special tokens are counted as text", ContentExtraction.countTokens("<|endoftext|>", "cl100k_base") > 1, true);
	}

	@Test
	public void testTruncateTokens() {

		assertEquals("Short text.", ContentExtraction.truncateTokens("Short text.", 100, "cl100k_base"));
		assertEquals("Cut after the sentence that fits", PARAGRAPH_A.substring(0, PARAGRAPH_A.indexOf('.') + 1), ContentExtraction.truncateTokens(PARAGRAPH_A, 13, "cl100k_base"));

		final String cut = ContentExtraction.truncateTokens(PARAGRAPH_A, 5, "cl100k_base");

		assertTrue("Cut at a word: '" + cut + "'", PARAGRAPH_A.startsWith(cut) && PARAGRAPH_A.charAt(cut.length()) == ' ');
		assertTrue(ContentExtraction.countTokens(cut, "cl100k_base") <= 5);
	}

	// ----- normalization -----
	@Test
	public void testNormalize() {

		final String input = "Content extrac-\ntion is ­\u0007 ﬁne.\r\n\r\n\r\n\r\nNord-\nSüd   and\tmore  ";

		assertEquals("Content extraction is fine.\n\nNord-\nSüd and more", ContentExtraction.normalize(input, new NormalizeOptions()));
		assertTrue("Steps can be switched off", ContentExtraction.normalize(input, new NormalizeOptions().setUnicode(false)).contains("ﬁne"));
		assertTrue(ContentExtraction.normalize(input, new NormalizeOptions().setHyphenation(false)).contains("extrac-\ntion"));
	}

	// ----- language -----
	@Test
	public void testDetectLanguage() {

		final DetectedLanguage german = ContentExtraction.detectLanguage("Dies ist ein deutscher Satz über Graphdatenbanken und ihre Abfragen.");

		assertEquals("de", german.language());
		assertTrue(german.confidence() > 0.5);
		assertNull("Too short to tell", ContentExtraction.detectLanguage("Hallo").language());
	}

	// ----- private methods -----
	private static ExtractedDocument html() throws Exception {

		return ContentExtraction.extract(new ByteArrayInputStream(Fixtures.html()), "guide.html", new ExtractionOptions().setOcrMode(OcrMode.OFF));
	}

	private static SplitOptions characters(final SplitterType splitter, final int size, final int overlap) {

		return new SplitOptions().setSplitter(splitter).setChunkSize(size).setChunkOverlap(overlap).setSizeUnit(SizeUnit.CHARACTERS);
	}

	private static SplitOptions tokens(final SplitterType splitter, final int size, final int overlap) {

		return new SplitOptions().setSplitter(splitter).setChunkSize(size).setChunkOverlap(overlap).setSizeUnit(SizeUnit.TOKENS);
	}

	/**
	 * The offsets of every chunk select its text from the source, and chunks follow the source order.
	 */
	private static void assertChunksPointIntoText(final String text, final List<Chunk> chunks) {

		int previousStart = -1;

		for (final Chunk chunk : chunks) {

			final String fromOffsets = text.substring(chunk.startOffset(), chunk.endOffset());

			assertTrue("Chunk text must end with the text at its offsets: '" + chunk.text() + "' vs '" + fromOffsets + "'", chunk.text().endsWith(fromOffsets));
			assertTrue("Chunks follow the source order", chunk.startOffset() > previousStart);

			previousStart = chunk.startOffset();
		}
	}

	private static void assertThrows(final Runnable action, final String expectedMessagePart) {

		try {

			action.run();

		} catch (final IllegalArgumentException ex) {

			assertTrue(ex.getMessage(), ex.getMessage().contains(expectedMessagePart));

			return;
		}

		fail("Expected an IllegalArgumentException containing '" + expectedMessagePart + "'");
	}
}
