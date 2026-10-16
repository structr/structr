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

import org.structr.common.error.FrameworkException;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * The script functions, called from JavaScript and StructrScript on File nodes.
 */
public class ExtractionFunctionsTest extends StructrUiTest {

	@Test
	public void testExtractContentFromJavaScript() throws Exception {

		createFile(Fixtures.html(), "text/html", "guide.html");

		final Object result = evaluate("""
			${{
			    let file    = $.first($.find('File', { name: 'guide.html' }));
			    let content = $.documents.extractContent(file, { includeBlocks: true, ocr: 'off' });

			    [
			        content.text.startsWith('Installation'),
			        content.markdown.includes('## Requirements'),
			        content.metadata.title,
			        content.blocks.filter(b => b.type === 'table')[0].rows[1][0],
			        content.blocks.find(b => b.text === 'Requirements').headingPath.join(' > ')
			    ].join('|');
			}}
			""");

		assertEquals("true|true|Installation Guide|Java|Installation > Requirements", result);
	}

	@Test
	public void testExtractContentFromStructrScript() throws Exception {

		createFile(Fixtures.text(), "text/plain", "notes.txt");

		final Object result = evaluate("${documents.extractContent(first(find('File', 'name', 'notes.txt'))).language}");

		assertEquals("en", result);
	}

	@Test
	public void testExtractMetadataTablesLinksAndEmbedded() throws Exception {

		createFile(Fixtures.docx(), "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "report.docx");
		createFile(Fixtures.html(), "text/html", "guide.html");
		createFile(Fixtures.eml(), "message/rfc822", "order.eml");

		final Object result = evaluate("""
			${{
			    let report = $.first($.find('File', { name: 'report.docx' }));
			    let guide  = $.first($.find('File', { name: 'guide.html' }));
			    let mail   = $.first($.find('File', { name: 'order.eml' }));

			    [
			        $.documents.extractMetadata(report).author,
			        $.documents.extractTables(report)[0].rows[1][1],
			        $.documents.extractLinks(guide)[0].url,
			        $.documents.extractEmbedded(mail).find(d => d.name === 'delivery.txt').text
			    ].join('|');
			}}
			""");

		assertEquals("Jane Doe|1200|https://structr.com/download|Attached delivery note for order 4711.", result);
	}

	@Test
	public void testUnknownOptionIsRejected() throws Exception {

		createFile(Fixtures.text(), "text/plain", "notes.txt");

		final FrameworkException ex = evaluateExpectingError("${{ $.documents.extractContent($.first($.find('File', { name: 'notes.txt' })), { includeBlock: true }); }}");

		assertEquals(422, ex.getStatus());
		assertTrue(ex.getMessage(), ex.getMessage().contains("unknown option 'includeBlock'"));
	}

	@Test
	public void testInvalidOcrModeIsRejected() throws Exception {

		createFile(Fixtures.text(), "text/plain", "notes.txt");

		final FrameworkException ex = evaluateExpectingError("${{ $.documents.extractContent($.first($.find('File', { name: 'notes.txt' })), { ocr: 'sometimes' }); }}");

		assertTrue(ex.getMessage(), ex.getMessage().contains("Unknown OCR mode 'sometimes'"));
	}

	@Test
	public void testNonFileArgumentIsRejected() throws Exception {

		final FrameworkException ex = evaluateExpectingError("${{ $.documents.extractContent('guide.html'); }}");

		assertEquals(422, ex.getStatus());
		assertTrue(ex.getMessage(), ex.getMessage().contains("expected a File as argument 1"));
	}

	@Test
	public void testSplitContentOfFileAndText() throws Exception {

		createFile(Fixtures.html(), "text/html", "guide.html");

		final Object result = evaluate("""
			${{
			    let file       = $.first($.find('File', { name: 'guide.html' }));
			    let fromFile   = $.documents.splitContent(file, { splitter: 'structure', chunkSize: 1000, chunkOverlap: 0, ocr: 'off' });
			    let fromText   = $.documents.splitContent('One. Two. Three.', { splitter: 'sentence', chunkSize: 6, chunkOverlap: 0, sizeUnit: 'characters' });

			    [
			        fromFile.length,
			        fromFile[1].headingPath.join(' > '),
			        fromFile[1].contentHash.length,
			        fromText.map(c => c.text).join('|')
			    ].join(';');
			}}
			""");

		assertEquals("4;Installation > Requirements;64;One.|Two.|Three.", result);
	}

	@Test
	public void testSplitContentFromStructrScript() throws Exception {

		final Object result = evaluate("${size(documents.splitContent('First sentence here. Second sentence here.', { splitter: 'sentence', chunkSize: 25, chunkOverlap: 0, sizeUnit: 'characters' }))}");

		assertEquals(2, ((Number) result).intValue());
	}

	@Test
	public void testSplitContentRejectsExtractionOptionsForText() throws Exception {

		final FrameworkException ex = evaluateExpectingError("${{ $.documents.splitContent('Some text', { ocr: 'off' }); }}");

		assertTrue(ex.getMessage(), ex.getMessage().contains("unknown option 'ocr'"));
	}

	@Test
	public void testSplitContentRejectsInvalidOverlap() throws Exception {

		final FrameworkException ex = evaluateExpectingError("${{ $.documents.splitContent('Some text', { chunkSize: 10, chunkOverlap: 10 }); }}");

		assertEquals(422, ex.getStatus());
		assertTrue(ex.getMessage(), ex.getMessage().contains("chunkOverlap must be at least 0 and smaller than chunkSize"));
	}

	@Test
	public void testTextFunctions() throws Exception {

		final Object result = evaluate("""
			${{
			    [
			        $.documents.countTokens('Hello world'),
			        $.documents.countTokens('Hello world', 'o200k_base'),
			        $.documents.truncateTokens('First sentence. Second sentence that is longer.', 4),
			        $.documents.normalizeText('extrac-\\ntion   ﬁne'),
			        $.documents.detectLanguage('Dies ist ein deutscher Satz über Graphdatenbanken und Abfragen.').language,
			        $.documents.stopWords('en').includes('the'),
			        $.documents.stopWords('xx').length
			    ].join('|');
			}}
			""");

		assertEquals("2|2|First sentence.|extraction fine|de|true|0", result);
		assertEquals("true", String.valueOf(evaluate("${contains(documents.stopWords('de'), 'und')}")));
	}

	@Test
	public void testUnknownEncodingIsRejected() throws Exception {

		final FrameworkException ex = evaluateExpectingError("${{ $.documents.countTokens('Hello', 'gpt-17'); }}");

		assertTrue(ex.getMessage(), ex.getMessage().contains("Unknown token encoding 'gpt-17'"));
	}

	@Test
	public void testFunctionsLiveInTheDocumentsNamespace() throws Exception {

		final Object result = evaluate("""
			${{
			    [
			        Object.keys($.documents).length,
			        Object.keys($.documents).includes('searchExtractedContent'),
			        typeof $.documents.splitContent,
			        'countTokens' in $.documents,
			        'noSuchFunction' in $.documents
			    ].join('|');
			}}
			""");

		assertEquals("17|true|function|true|false", result);
		assertEquals("2", String.valueOf(evaluate("${documents.countTokens('Hello world')}")));

		assertNull("Functions must not be registered outside the namespace", Functions.get("extractContent"));
		assertTrue(Functions.isNamespace("documents"));
	}

	// ----- private methods -----
	private void createFile(final byte[] data, final String contentType, final String name) throws Exception {

		try (final Tx tx = app.tx()) {

			final NodeInterface file = FileHelper.createFile(securityContext, data, contentType, StructrTraits.FILE, name, true);

			assertNotNull(file);

			tx.success();
		}
	}

	private Object evaluate(final String script) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(new ActionContext(securityContext), null, script.strip(), "test");

			tx.success();

			return result;
		}
	}

	private FrameworkException evaluateExpectingError(final String script) {

		try {

			evaluate(script);

		} catch (final FrameworkException ex) {

			return ex;
		}

		fail("Expected an error from: " + script);

		return null;
	}
}
