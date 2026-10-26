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

import org.structr.api.DatabaseFeature;
import org.structr.common.AccessControllable;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionListener;
import org.structr.web.entity.User;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaNodeTraitDefinition;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.testng.AssertJUnit.*;

/**
 * Stored figures: DocumentFigure as an Image with the properties of a figure, and as a base type of the
 * data model.
 */
public class DocumentFigureTest extends StructrUiTest {

	@Test
	public void testFigureIsStoredFromTheResultOfExtractFigures() throws Exception {

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		final Object result = evaluate("""
			${{
			    let paper  = $.find('File', '%s');
			    let figure = $.documents.extractFigures(paper)[0];
			    let stored = $.create('DocumentFigure', figure);

			    stored.document = paper;

			    [ stored.label, stored.page, stored.position, stored.source, stored.bbox.length, stored.imageHash === figure.imageHash, stored.mentions.length,
			      stored.contentType, stored.isImage, stored.width === figure.width, stored.size > 0, paper.figures.length, paper.figures[0].id === stored.id ].join('|');
			}}
			""".formatted(paperId));

		assertEquals("Figure 1|1|0|region|4|true|1|image/png|true|true|true|1|true", result);
		assertEquals("A figure stored without a name is named after its label when the transaction commits", "Figure 1.png", evaluate("${{ $.find('DocumentFigure')[0].name; }}"));
	}

	@Test
	public void testFigureIsAFileOfItsOwn() throws Exception {

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		final Object stored = evaluate("""
			${{
			    let paper  = $.find('File', '%s');
			    let figure = $.documents.extractFigures(paper).find(f => f.label === 'Table 1');
			    let folder = $.find('Folder', { path: '/papers' })[0];
			    let stored = $.create('DocumentFigure', Object.assign({ name: 'paper - Table 1.png', parent: folder, document: paper }, figure));

			    [ stored.path, stored.label, stored.document.id === paper.id ].join('|');
			}}
			""".formatted(paperId));

		assertEquals("Everything of a figure map is taken, and the figure is a normal file", "/papers/paper - Table 1.png|Table 1|true", stored);

		evaluate("${{ $.delete($.find('File', '" + paperId + "')); }}");

		final Object left = evaluate("${{ let f = $.find('DocumentFigure')[0]; [ $.find('DocumentFigure').length, f.document ].join('|'); }}");

		assertEquals("Deleting the document keeps the figures", "1|", left);
	}

	@Test
	public void testFigureIsABaseTypeOfTheDataModel() throws Exception {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "PaperFigure"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), new String[] { ContentExtractionTraits.DOCUMENT_FIGURE })
			);

			tx.success();
		}

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		final Object result = evaluate("""
			${{
			    let paper  = $.find('File', '%s');
			    let stored = $.create('PaperFigure', $.documents.extractFigures(paper)[1]);

			    stored.document = paper;

			    [ stored.type, stored.label, stored.isImage, $.find('DocumentFigure').length, $.find('Image').length, paper.figures[0].type ].join('|');
			}}
			""".formatted(paperId));

		assertEquals("PaperFigure|Figure 2|true|1|1|PaperFigure", result);
	}

	@Test
	public void testImageMustBeAnImage() throws Exception {

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.create('DocumentFigure', { label: 'Figure 1', image: { data: '%%%' } }); }}", "test");
			fail("Invalid base64 is refused");

		} catch (final FrameworkException ex) {

			assertEquals(422, ex.getStatus());
		}
	}

	// ----- automatic extraction -----
	private static final String WITH_FIGURES = "{ wait: true, ocr: 'off', splitter: 'page', extractFigures: true }";

	@Test
	public void testFiguresAreStoredWhenADocumentIsExtracted() throws Exception {

		createFulltextIndex();

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		assertEquals("done", evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), " + WITH_FIGURES + ")[0].status; }}"));

		final Object result = evaluate("""
			${{
			    let paper   = $.find('File', '%s');
			    let figures = $.sort(paper.figures, 'position');
			    let chart   = figures[0];

			    [ figures.length, figures.map(f => f.label).join(','), chart.type, chart.parent.path, chart.name, chart.isImage, chart.contentType, chart.page,
			      chart.text, chart.content.text === chart.text, chart.content.file.id === paper.id, chart.content.pageStart,
			      $.documents.getExtractedText(paper).includes('Figure 1: Accuracy') ].join('|');
			}}
			""".formatted(paperId));

		assertEquals("The caption, then the text in the chart: its axis label and the labels of the bars",
			"3|Figure 1,Figure 2,Table 1|DocumentFigure|/papers/paper.pdf figures|paper.pdf - Figure 1.png|true|image/png|1|"
			+ "Figure 1: Accuracy of the three models on the benchmark. Higher is better.\nAccuracy\nA B C|true|true|1|true", result);

		final Object found = evaluate("${{ $.documents.fulltextSearch('Accuracy', { limit: 20 }).filter(h => h.figure).map(h => h.figure.label).sort().join(','); }}");

		assertEquals("The text of the figures is searchable as content of the document", "Figure 1,Table 1", found);
	}

	@Test
	public void testTheTextOfAFigureCanBeReplaced() throws Exception {

		createFulltextIndex();

		final String paperId          = createFile("/papers", "paper.pdf", Fixtures.paperPdf());
		final List<String> events     = new CopyOnWriteArrayList<>();
		final ContentExtractionListener listener = new ContentExtractionListener() {

			@Override
			public void onContentAdded(final String fileUuid, final List<String> nodeUuids) {

				events.add(fileUuid + ":" + nodeUuids.size());
			}
		};

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), " + WITH_FIGURES + "); }}");

		ContentExtraction.addListener(listener);

		try {

			// e.g. what a vision model said about the chart
			evaluate("${{ let chart = $.find('File', '" + paperId + "').figures.find(f => f.label === 'Figure 1'); chart.text = 'Bar chart: model C reaches the highest accuracy, like a zeppelin above the others.'; }}");

		} finally {

			ContentExtraction.removeListener(listener);
		}

		assertEquals("The listener is told once, after the commit", List.of(paperId + ":1"), events);

		final Object result = evaluate("""
			${{
			    let chart = $.find('File', '%s').figures.find(f => f.label === 'Figure 1');
			    let hits  = $.documents.fulltextSearch('zeppelin');

			    [ chart.content.text.startsWith('Bar chart'), hits.length, hits[0].figure.id === chart.id, hits[0].pageStart ].join('|');
			}}
			""".formatted(paperId));

		assertEquals("The chunk follows the text of the figure", "true|1|true|1", result);

		final Object ids    = evaluate("${{ $.sort($.find('File', '" + paperId + "').figures, 'position').map(f => f.id).join(','); }}");

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), { wait: true, force: true, ocr: 'off', splitter: 'page', extractFigures: true }); }}");

		final Object again = evaluate("${{ $.sort($.find('File', '" + paperId + "').figures, 'position').map(f => f.id).join(','); }}");
		final Object text  = evaluate("${{ $.find('File', '" + paperId + "').figures.find(f => f.label === 'Figure 1').text; }}");

		assertEquals("Extracting again keeps the figures", ids, again);
		assertTrue("and the text written by the application: " + text, text.toString().startsWith("Bar chart"));
		assertEquals("one chunk per figure, not more", 3, ((Number) evaluate("${{ $.find('ExtractedContentNode').filter(c => c.figure).length; }}")).intValue());
	}

	@Test
	public void testFiguresGetThePermissionsOfTheDocument() throws Exception {

		final NodeInterface owner  = createUser("author");
		final NodeInterface reader = createUser("reader");

		final String paperId;

		try (final Tx tx = app.tx()) {

			final Folder folder      = FileHelper.createFolderPath(securityContext, "/shared").as(Folder.class);
			final NodeInterface file = FileHelper.createFile(securityContext, new ByteArrayInputStream(Fixtures.paperPdf()), null, StructrTraits.FILE, "paper.pdf", folder);

			file.setProperty(file.getTraits().key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), owner);
			file.setProperty(file.getTraits().key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			file.as(AccessControllable.class).grant(Permission.read, reader.as(Principal.class));

			paperId = file.getUuid();
			tx.success();
		}

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), " + WITH_FIGURES + "); }}");

		try (final Tx tx = app.tx()) {

			final SecurityContext readerContext = SecurityContext.getInstance(reader.as(User.class), AccessMode.Backend);
			final List<NodeInterface> figures   = app.nodeQuery(ContentExtractionTraits.DOCUMENT_FIGURE).getAsList();

			assertEquals(3, figures.size());

			for (final NodeInterface figure : figures) {

				final NodeInterface figureOwner = figure.getProperty(figure.getTraits().key(NodeInterfaceTraitDefinition.OWNER_PROPERTY));

				assertEquals(owner.getUuid(), figureOwner.getUuid());
				assertTrue(figure.isVisibleToAuthenticatedUsers());
				assertTrue("a grant of the document is a grant of its figures", figure.isGranted(Permission.read, readerContext));
			}

			assertEquals("The reader sees the folder and the figures", 3, StructrApp.getInstance(readerContext).nodeQuery(ContentExtractionTraits.DOCUMENT_FIGURE).getAsList().size());
			assertNotNull(StructrApp.getInstance(readerContext).nodeQuery(StructrTraits.FOLDER).name("paper.pdf figures").getFirst());

			tx.success();
		}

		evaluate("${{ $.delete($.find('File', '" + paperId + "')); }}");

		assertEquals("Deleting the document keeps its figures, without their chunks", "3|0", evaluate("${{ [ $.find('DocumentFigure').length, $.find('ExtractedContentNode').length ].join('|'); }}"));
	}

	@Test
	public void testFiguresAreStoredAsATypeOfTheDataModel() throws Exception {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "PaperFigure"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), new String[] { ContentExtractionTraits.DOCUMENT_FIGURE })
			);

			tx.success();
		}

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), { wait: true, ocr: 'off', extractFigures: true, figureType: 'PaperFigure' }); }}");

		assertEquals("PaperFigure|3", evaluate("${{ let f = $.find('File', '" + paperId + "').figures; [ $.unwind(f.map(x => x.type)).filter((v, i, a) => a.indexOf(v) === i).join(','), f.length ].join('|'); }}"));

		try {

			evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), { wait: true, force: true, extractFigures: true, figureType: 'Image' }); }}");
			fail("Only a type that inherits DocumentFigure");

		} catch (final FrameworkException ex) {

			assertEquals(422, ex.getStatus());
			assertTrue(ex.getMessage(), ex.getMessage().contains("figureType"));
		}
	}

	@Test
	public void testFiguresTheDocumentNoLongerHasAreDeleted() throws Exception {

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), " + WITH_FIGURES + "); }}");

		try (final Tx tx = app.tx()) {

			FileHelper.writeToFile(app.getNodeById(StructrTraits.FILE, paperId).as(File.class), Fixtures.pdf(List.of("A new version without any figures.")));
			tx.success();
		}

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), { wait: true, force: true, ocr: 'off', extractFigures: true }); }}");

		assertEquals("0|0|0", evaluate("${{ [ $.find('DocumentFigure').length, $.find('ExtractedContentNode').filter(c => c.figure).length, $.documents.extractContentInfo($.find('File', '" + paperId + "')).figureCount ].join('|'); }}"));
	}

	@Test
	public void testThePictureOfAFigureIsReadWithOcr() throws Exception {

		if (!ContentExtraction.getOcrInfo().available()) {

			throw new SkipException("tesseract is not installed, OCR cannot be tested");
		}

		createFulltextIndex();

		// a picture with words in it and no caption: the PDF itself has no text for it
		final String reportId = createFile("/reports", "report.pdf", Fixtures.pdfWithImage());

		evaluate("${{ $.documents.processContent($.find('File', '" + reportId + "'), { wait: true, ocr: 'auto', extractFigures: true }); }}");

		final Object result = evaluate("""
			${{
			    let figure = $.find('File', '%s').figures[0];
			    let hits   = $.documents.fulltextSearch('%s').filter(h => h.figure);

			    [ figure.source, figure.label, figure.text.toUpperCase().includes('%s'), hits.length, hits.length > 0 && hits[0].figure.id === figure.id ].join('|');
			}}
			""".formatted(reportId, Fixtures.OCR_WORD.toLowerCase(), Fixtures.OCR_WORD));

		assertEquals("What OCR reads in the picture is the text of the figure, and searchable", "region||true|1|true", result);
	}

	@Test
	public void testWithoutOcrAPictureHasNoText() throws Exception {

		final String reportId = createFile("/reports", "report.pdf", Fixtures.pdfWithImage());

		evaluate("${{ $.documents.processContent($.find('File', '" + reportId + "'), { wait: true, ocr: 'off', extractFigures: true }); }}");

		assertEquals("The figure is stored, but without text there is no chunk", "1||0",
			evaluate("${{ let f = $.find('File', '" + reportId + "').figures; [ f.length, f[0].text, $.find('ExtractedContentNode').filter(c => c.figure).length ].join('|'); }}"));
	}

	@Test
	public void testNoFiguresWithoutTheOption() throws Exception {

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), { wait: true, ocr: 'off' }); }}");

		assertEquals(0, ((Number) evaluate("${{ $.find('DocumentFigure').length; }}")).intValue());
	}

	@Test
	public void testAFigureIsNotExtractedItself() throws Exception {

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), " + WITH_FIGURES + "); }}");

		final Object result = evaluate("${{ let f = $.find('DocumentFigure')[0]; let r = $.documents.processContent(f, " + WITH_FIGURES + ")[0]; [ r.status, $.find('File', '" + paperId + "').figures.length, $.find('DocumentFigure').length ].join('|'); }}");

		assertEquals("skipped|3|3", result);
	}

	// ----- context -----
	@Test
	public void testContextOfAFigure() throws Exception {

		createFulltextIndex();

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		evaluate("${{ $.documents.processContent($.find('File', '" + paperId + "'), { wait: true, ocr: 'off', splitter: 'page' }); }}");

		final Object result = evaluate("""
			${{
			    let paper   = $.find('File', '%s');
			    let context = $.documents.getFigureContext(paper, $.documents.extractFigures(paper, { includeImageData: false })[0]);

			    [ context.extracted, context.page, context.label, context.mentions.join(' / '), context.pageText.includes('The remaining text of the page'),
			      context.related.length > 0, context.related[0].page, context.text.startsWith('Document: paper.pdf\\nFigure on page 1: Figure 1: Accuracy'),
			      context.text.includes('Text of page 1:'), context.tokenCount > 0 ].join('|');
			}}
			""".formatted(paperId));

		assertEquals("Caption and mentions, the text of the page, and a passage of another page with words of the caption",
			"true|1|Figure 1|As Figure 1 shows, model C is the most accurate.|true|true|2|true|true|true", result);

		final Object small = evaluate("${{ let p = $.find('File', '" + paperId + "'); $.documents.getFigureContext(p, $.documents.extractFigures(p)[0], { maxTokens: 30, related: 0 }).tokenCount; }}");

		assertTrue("The budget is kept: " + small, ((Number) small).intValue() <= 30);

		final Object stored = evaluate("""
			${{
			    let paper  = $.find('File', '%s');
			    let figure = $.create('DocumentFigure', $.documents.extractFigures(paper)[2]);

			    figure.document = paper;

			    let context = $.documents.getFigureContext(paper, figure);

			    [ context.label, context.page, context.mentions.length ].join('|');
			}}
			""".formatted(paperId));

		assertEquals("A stored figure works as well", "Table 1|2|1", stored);
	}

	@Test
	public void testContextOfAFigureOfADocumentNotExtracted() throws Exception {

		final String paperId = createFile("/papers", "paper.pdf", Fixtures.paperPdf());

		final Object result = evaluate("""
			${{
			    let paper   = $.find('File', '%s');
			    let context = $.documents.getFigureContext(paper, { label: 'Figure 1', caption: 'Figure 1: Accuracy of the models.', page: 1, mentions: [] });

			    [ context.extracted, context.pageText, context.related.length, context.text ].join('|');
			}}
			""".formatted(paperId));

		assertEquals("false||0|Document: paper.pdf\nFigure on page 1: Figure 1: Accuracy of the models.", result);
	}

	// ----- private methods -----
	private NodeInterface createUser(final String name) throws Exception {

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name));

			tx.success();

			return user;
		}
	}

	private void createFulltextIndex() throws Exception {

		// keyword search needs Neo4j's fulltext index; the in-memory driver has none
		if (!app.getDatabaseService().supportsFeature(DatabaseFeature.FulltextIndexing)) {

			throw new SkipException("Keyword search needs a fulltext index, skipping test with " + Settings.DatabaseDriver.getValue());
		}

		// Structr's index management is off in tests, so the fulltext index is created here
		try (final Tx tx = app.tx()) {

			app.query("CREATE FULLTEXT INDEX " + ContentExtractionTraits.CONTENT_FULLTEXT_INDEX + " IF NOT EXISTS FOR (n:ExtractedContentNode) ON EACH [n.embeddedPath, n.text]", Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			app.query("CALL db.awaitIndexes(60)", Map.of());
			tx.success();
		}
	}

	private String createFile(final String folderPath, final String name, final byte[] data) throws Exception {

		try (final Tx tx = app.tx()) {

			final Folder folder      = FileHelper.createFolderPath(securityContext, folderPath).as(Folder.class);
			final NodeInterface file = FileHelper.createFile(securityContext, new ByteArrayInputStream(data), null, StructrTraits.FILE, name, folder);

			tx.success();

			return file.getUuid();
		}
	}

	private Object evaluate(final String script) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(new ActionContext(securityContext), null, script.strip(), "test");

			tx.success();

			return result;
		}
	}
}
