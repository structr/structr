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

import io.restassured.RestAssured;
import org.structr.api.DatabaseService;
import org.structr.common.AccessControllable;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.ContentExtractionListener;
import org.structr.extraction.api.chunk.Chunk;
import org.structr.extraction.api.options.EffectiveContentOptions;
import org.structr.extraction.api.search.ContentSearchMode;
import org.structr.extraction.api.search.ContentSearchOptions;
import org.structr.extraction.api.search.FileSearchHit;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.traits.definitions.ContentExtractionConfigurationTraitDefinition;
import org.structr.extraction.traits.properties.ContentFileStateProperties;
import org.structr.schema.action.ActionContext;
import org.structr.storage.StorageProviderFactory;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.websocket.command.SearchNodesCommand;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.AssertJUnit.*;

/**
 * Stored content: processing, automatic extraction, content extraction configurations, permissions and
 * the listener API.
 */
public class ContentGraphTest extends StructrUiTest {

	private static final String PROCESS_NOW = "{ wait: true, ocr: 'off', splitter: 'structure', chunkSize: 1000, chunkOverlap: 0 }";

	// ----- processing -----
	@Test
	public void testProcessContentStoresChunksInReadingOrder() throws Exception {

		final String fileId = createFile("/docs", "guide.html", Fixtures.html());
		final Object result = evaluate("${{ $.documents.processContent($.find('File', '" + fileId + "'), " + PROCESS_NOW + ")[0].status; }}");

		assertEquals("done", result);

		try (final Tx tx = app.tx()) {

			final File file                         = file(fileId);
			final List<ExtractedContentNode> chunks = ContentExtraction.getExtractedContent(file);

			assertEquals("One chunk per section", 4, chunks.size());

			for (int i = 0; i < chunks.size(); i++) {

				assertEquals(i, chunks.get(i).getPosition());
				assertEquals(fileId, chunks.get(i).getFile().getUuid());
				assertEquals(i < chunks.size() - 1 ? chunks.get(i + 1).getUuid() : null, chunks.get(i).getNext() != null ? chunks.get(i).getNext().getUuid() : null);
			}

			assertEquals(List.of("Installation", "Requirements"), chunks.get(1).getHeadingPath());
			assertEquals("done", property(file, ContentFileStateProperties.STATUS));
			assertEquals(file.getChecksum(), property(file, ContentFileStateProperties.CHECKSUM));
			assertNotNull(property(file, ContentFileStateProperties.OPTIONS_HASH));
			assertTrue(ContentExtraction.getExtractedText(file).startsWith("Installation\n\nStructr needs a Java runtime."));

			tx.success();
		}
	}

	@Test
	public void testUnchangedFilesAreSkippedUnlessForced() throws Exception {

		final String fileId = createFile("/docs", "guide.html", Fixtures.html());
		final String call   = "${{ $.documents.processContent($.find('File', '" + fileId + "'), %s)[0].status; }}";

		assertEquals("done", evaluate(call.formatted(PROCESS_NOW)));
		assertEquals("unchanged", evaluate(call.formatted(PROCESS_NOW)));
		assertEquals("done", evaluate(call.formatted("{ wait: true, force: true, ocr: 'off', splitter: 'structure', chunkSize: 1000, chunkOverlap: 0 }")));
	}

	@Test
	public void testConfigurationOptionsApplyToSubtreeAndChangesReprocess() throws Exception {

		final String fileId = createFile("/manuals/de/install", "guide.html", Fixtures.html());

		configure("/manuals", Map.of("chunkSize", 1000, "chunkOverlap", 0, "splitter", "structure", "ocr", "off"));

		try (final Tx tx = app.tx()) {

			final EffectiveContentOptions options = ContentExtraction.resolveOptions(file(fileId), Map.of());

			assertEquals(1000, options.split().getChunkSize());
			assertEquals("config:/manuals", options.values().get("chunkSize").source());
			assertEquals("config:/manuals", options.values().get("ocr").source());
			assertEquals("setting", options.values().get("sizeUnit").source());

			tx.success();
		}

		final String process = "${{ $.documents.processContent($.find('Folder', { path: '/manuals' })[0], { wait: true })[0].status; }}";

		assertEquals("done", evaluate(process));
		assertEquals("unchanged", evaluate(process));
		assertEquals(4, chunkCount(fileId));

		// a configuration on a nearer folder changes the options, so the file is processed again
		configure("/manuals/de/install", Map.of("chunkSize", 40, "chunkOverlap", 0, "ocr", "off"));

		assertEquals("done", evaluate(process));
		assertTrue("Smaller chunks, more of them", chunkCount(fileId) > 4);
	}

	@Test
	public void testOverlapLargerThanSettingSizeIsReduced() throws Exception {

		final String fileId = createFile("/a/b", "notes.txt", Fixtures.text());

		// the chunk size comes from the setting (512), so the overlap does not fit
		configure("/a", Map.of("chunkOverlap", 600));

		try (final Tx tx = app.tx()) {

			final EffectiveContentOptions options = ContentExtraction.resolveOptions(file(fileId), Map.of());

			assertEquals(128, options.split().getChunkOverlap());
			assertEquals(1, options.warnings().size());

			tx.success();
		}
	}

	// ----- automatic extraction -----
	@Test
	public void testUploadIntoMarkedFolderTreeIsExtracted() throws Exception {

		createFolder("/knowledge/deep/deeper");
		createFolder("/knowledge/excluded");
		configure("/knowledge", Map.of());
		configure("/knowledge/excluded", Map.of("enabled", false));

		final String deep     = createFile("/knowledge/deep/deeper", "guide.html", Fixtures.html());
		final String excluded = createFile("/knowledge/excluded", "notes.txt", Fixtures.text());
		final String outside  = createFile("/elsewhere", "notes.txt", Fixtures.text());

		assertEquals("A file several levels below a marked folder is extracted", "done", awaitStatus(deep));

		// the same job queue has finished the marked file, so a job for the others would have run as well
		assertNull("A subfolder can opt out, status " + status(excluded) + " " + error(excluded), status(excluded));
		assertNull("Files outside marked folders are not extracted, status " + status(outside) + " " + error(outside), status(outside));
	}

	@Test
	public void testChunkedUploadIsExtractedOnlyWhenComplete() throws Exception {

		createFolder("/uploads");
		configure("/uploads", Map.of());

		final byte[] data = Fixtures.text();
		final String fileId;

		// like the Files UI: CREATE makes an empty file (File's AfterCreation then sets version 0), and
		// the content arrives in later transactions, one per chunk
		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "notes.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder("/uploads"))
			);

			fileId = file.getUuid();

			tx.success();
		}

		writeChunk(fileId, Arrays.copyOf(data, data.length / 2), false);

		// a job for the empty or half-written file would have run by now
		Thread.sleep(2000);

		assertNull("An incomplete upload must not be extracted, status " + status(fileId) + " " + error(fileId), status(fileId));

		writeChunk(fileId, data, true);

		assertEquals("The completed upload is extracted, error " + error(fileId), "done", awaitStatus(fileId));
	}

	@Test
	public void testFileOverridesFolder() throws Exception {

		createFolder("/marked");
		createFolder("/plain");
		configure("/marked", Map.of());

		final String optedIn  = createFileWithConfiguration("/plain", "in.txt", true);
		final String optedOut = createFileWithConfiguration("/marked", "out.txt", false);

		assertEquals("done", awaitStatus(optedIn));
		assertNull(status(optedOut));

		try (final Tx tx = app.tx()) {

			assertEquals("file", ContentExtraction.shouldExtract(file(optedIn)).source());
			assertEquals("file", ContentExtraction.shouldExtract(file(optedOut)).source());

			tx.success();
		}
	}

	@Test
	public void testMovingIntoMarkedFolderTriggersExtraction() throws Exception {

		final String file      = createFile("/inbox", "guide.html", Fixtures.html());
		final String subtree   = createFile("/inbox/project/specs", "notes.txt", Fixtures.text());

		createFolder("/archive");
		configure("/archive", Map.of());

		assertNull("Not marked yet", status(file));

		try (final Tx tx = app.tx()) {

			final Folder archive = folder("/archive");

			file(file).setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), archive);
			folder("/inbox/project").setProperty(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), archive);

			tx.success();
		}

		assertEquals("A moved file is extracted", "done", awaitStatus(file));
		assertEquals("Files in a moved folder are extracted", "done", awaitStatus(subtree));
	}

	// ----- permissions -----
	@Test
	public void testConfigurationsAreAdminOnly() throws Exception {

		final NodeInterface user          = createUser("tester", false);
		final SecurityContext userContext = SecurityContext.getInstance(user.as(User.class), AccessMode.Backend);
		final App userApp                 = StructrApp.getInstance(userContext);
		final Traits traits               = Traits.of(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION);

		final String folderId;

		try (final Tx tx = userApp.tx()) {

			folderId = FileHelper.createFolderPath(userContext, "/own").getUuid();

			tx.success();
		}

		try (final Tx tx = userApp.tx()) {

			userApp.create(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, new NodeAttribute<>(traits.key(ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY), userApp.getNodeById(folderId)));
			tx.success();

			fail("A non-admin must not configure content extraction, even on an own folder");

		} catch (final FrameworkException ex) {

			assertEquals(403, ex.getStatus());
		}

		final NodeInterface admin = createUser("boss", true);
		final App adminApp        = StructrApp.getInstance(SecurityContext.getInstance(admin.as(User.class), AccessMode.Backend));

		final String configurationId;

		try (final Tx tx = adminApp.tx()) {

			configurationId = adminApp.create(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, new NodeAttribute<>(traits.key(ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY), adminApp.getNodeById(folderId))).getUuid();
			tx.success();
		}

		try (final Tx tx = adminApp.tx()) {

			adminApp.getNodeById(configurationId).setProperty(traits.key("chunkSize"), 5);
			tx.success();

			fail("Chunk sizes below the minimum are rejected");

		} catch (final FrameworkException ex) {

			assertEquals(422, ex.getStatus());
		}

		try (final Tx tx = userApp.tx()) {

			final NodeInterface configuration = userApp.getNodeById(configurationId);

			assertNotNull("The owner of a folder can see its configuration", configuration);

			configuration.setProperty(traits.key("enabled"), false);
			tx.success();

			fail("A non-admin must not change a configuration");

		} catch (final FrameworkException ex) {

			assertEquals(403, ex.getStatus());
		}

		try (final Tx tx = userApp.tx()) {

			userApp.delete(userApp.getNodeById(folderId));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			assertNull("Deleting a folder deletes its configuration, also for a non-admin who may delete the folder", app.getNodeById(configurationId));
			tx.success();
		}
	}

	@Test
	public void testNearestConfigurationWinsCompletely() throws Exception {

		final String fileId = createFile("/outer/inner", "notes.txt", Fixtures.text());

		configure("/outer", Map.of("chunkSize", 100, "chunkOverlap", 0, "ocr", "off"));
		configure("/outer/inner", Map.of("chunkSize", 200));

		try (final Tx tx = app.tx()) {

			final EffectiveContentOptions options = ContentExtraction.resolveOptions(file(fileId), Map.of());

			assertEquals("config:/outer/inner", options.values().get("chunkSize").source());
			assertEquals("Options the nearest configuration leaves empty come from the settings, not from the outer one", "setting", options.values().get("ocr").source());
			assertEquals("/outer/inner", ContentExtraction.shouldExtract(file(fileId)).targetPath());

			tx.success();
		}

		configure("/outer/inner", Map.of("enabled", false));

		try (final Tx tx = app.tx()) {

			assertFalse("A disabled configuration opts its subtree out", ContentExtraction.shouldExtract(file(fileId)).extract());
			tx.success();
		}

		removeConfiguration("/outer/inner");

		try (final Tx tx = app.tx()) {

			assertTrue(ContentExtraction.shouldExtract(file(fileId)).extract());
			assertEquals("Without its own configuration, the outer one applies", "config:/outer", ContentExtraction.resolveOptions(file(fileId), Map.of()).values().get("chunkSize").source());
			tx.success();
		}
	}

	@Test
	public void testOneConfigurationPerTarget() throws Exception {

		configure("/single", Map.of());

		try (final Tx tx = app.tx()) {

			app.create(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, new NodeAttribute<>(Traits.of(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION).key(ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY), folder("/single")));
			tx.success();

			fail("A second configuration for the same folder is rejected");

		} catch (final FrameworkException ex) {

			assertEquals(422, ex.getStatus());
		}
	}

	@Test
	public void testFileAndFolderAreNotChanged() {

		for (final String key : List.of("extractContent", "extractChildContent", "contentSplitter", "contentChunkSize", "contentOcrMode")) {

			assertFalse(Traits.of(StructrTraits.FILE).hasKey(key));
			assertFalse(Traits.of(StructrTraits.FOLDER).hasKey(key));
		}
	}

	@Test
	public void testStatusIsInternal() throws Exception {

		final String fileId       = createFile("/docs", "notes.txt", Fixtures.text());
		final NodeInterface admin = createUser("boss", true);
		final App adminApp        = StructrApp.getInstance(SecurityContext.getInstance(admin.as(User.class), AccessMode.Backend));

		try (final Tx tx = adminApp.tx()) {

			adminApp.getNodeById(fileId).setProperty(Traits.of(StructrTraits.FILE).key(ContentFileStateProperties.STATUS), "done");

			fail("Even admins cannot fake the extraction status");

		} catch (final FrameworkException ex) {

			assertEquals(403, ex.getStatus());
		}
	}

	@Test
	public void testChunksAreOnlyVisibleWithTheirFile() throws Exception {

		final NodeInterface owner  = createUser("owner", false);
		final NodeInterface other  = createUser("other", false);
		final SecurityContext ownerContext = SecurityContext.getInstance(owner.as(User.class), AccessMode.Backend);
		final SecurityContext otherContext = SecurityContext.getInstance(other.as(User.class), AccessMode.Backend);

		final String fileId;

		try (final Tx tx = app.tx()) {

			final NodeInterface file = FileHelper.createFile(ownerContext, new ByteArrayInputStream(Fixtures.html()), "text/html", StructrTraits.FILE, "private.html", null);

			fileId = file.getUuid();
			tx.success();
		}

		evaluate("${{ $.documents.processContent($.find('File', '" + fileId + "'), " + PROCESS_NOW + "); }}");

		try (final Tx tx = app.tx()) {

			assertEquals(4, StructrApp.getInstance(ownerContext).nodeQuery(ContentExtractionTraits.EXTRACTED_CONTENT_NODE).getAsList().size());
			assertEquals("Chunks of a file another user cannot read are invisible", 0, StructrApp.getInstance(otherContext).nodeQuery(ContentExtractionTraits.EXTRACTED_CONTENT_NODE).getAsList().size());

			tx.success();
		}
	}

	// ----- listeners, deletion -----
	@Test
	public void testListenerIsNotified() throws Exception {

		final List<String> events                 = new CopyOnWriteArrayList<>();
		final ContentExtractionListener listener  = new ContentExtractionListener() {

			@Override
			public void onBeforeContentReplaced(final File file, final List<ExtractedContentNode> oldNodes, final List<Chunk> newChunks) {

				events.add("before:" + oldNodes.size() + ":" + newChunks.size());
			}

			@Override
			public void onContentExtracted(final String fileUuid, final List<String> nodeUuids, final EffectiveContentOptions options) {

				events.add("after:" + nodeUuids.size() + ":" + options.split().getChunkSize());
			}
		};

		final String fileId = createFile("/docs", "guide.html", Fixtures.html());

		ContentExtraction.addListener(listener);

		try {

			evaluate("${{ $.documents.processContent($.find('File', '" + fileId + "'), " + PROCESS_NOW + "); }}");
			evaluate("${{ $.documents.processContent($.find('File', '" + fileId + "'), { wait: true, force: true, ocr: 'off', splitter: 'structure', chunkSize: 1000, chunkOverlap: 0 }); }}");

		} finally {

			ContentExtraction.removeListener(listener);
		}

		assertEquals(List.of("before:0:4", "after:4:1000", "before:4:4", "after:4:1000"), events);
	}

	@Test
	public void testDeleteContentAndCascade() throws Exception {

		final String first  = createFile("/docs", "first.html", Fixtures.html());
		final String second = createFile("/docs", "second.html", Fixtures.html());

		evaluate("${{ $.documents.processContent($.find('Folder', { path: '/docs' })[0], " + PROCESS_NOW + "); }}");
		assertEquals(8, totalChunks());

		evaluate("${{ $.documents.deleteExtractedContent($.find('File', '" + first + "')); }}");

		assertEquals(0, chunkCount(first));
		assertNull(status(first));
		assertEquals(4, totalChunks());

		try (final Tx tx = app.tx()) {

			app.delete(app.getNodeById(second));
			tx.success();
		}

		assertEquals("Deleting a file deletes its chunks", 0, totalChunks());
	}

	// ----- functions -----
	@Test
	public void testInfoAndContextFunctions() throws Exception {

		final String fileId = createFile("/handbook", "guide.html", Fixtures.html());

		// configuring a folder starts nothing by itself, existing files are processed explicitly
		configure("/handbook", Map.of());
		evaluate("${{ $.documents.processContent($.find('Folder', { path: '/handbook' })[0], { wait: true, ocr: 'off' }); }}");

		final Object result = evaluate("""
			${{
			    let file    = $.find('File', '%s');
			    let info    = $.documents.extractContentInfo(file);
			    let middle  = file.extractedContentNodes.find(n => n.position === 1);
			    let context = $.documents.getContentContext(middle, 1, 1);

			    [
			        info.extract, info.source, info.target, info.status, info.chunkCount > 0,
			        info.options.chunkSize.source,
			        context.nodes.length,
			        context.text === $.documents.getExtractedText(file).substring(0, context.text.length)
			    ].join('|');
			}}
			""".formatted(fileId));

		assertEquals("true|folder|/handbook|done|true|setting|3|true", result);
	}

	@Test
	public void testProcessContentRequiresWritePermission() throws Exception {

		final String fileId = createFile("/docs", "notes.txt", Fixtures.text());
		final NodeInterface user = createUser("reader", false);

		try (final Tx tx = app.tx()) {

			// readable, but not writable, for the user
			file(fileId).as(AccessControllable.class).grant(Permission.read, user.as(Principal.class));
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(SecurityContext.getInstance(user.as(User.class), AccessMode.Backend)), null, "${{ $.documents.processContent($.find('File', '" + fileId + "'), { wait: true }); }}", "test");
			fail("Processing needs write permission");

		} catch (final FrameworkException ex) {

			assertEquals(403, ex.getStatus());
		}
	}

	@Test
	public void testSearchExtractedContent() throws Exception {

		createFulltextIndex();

		final String fileId = createFile("/docs", "guide.html", Fixtures.html());
		final String other  = createFile("/other", "notes.txt", Fixtures.text());

		evaluate("${{ $.documents.processContent([$.find('File', '" + fileId + "'), $.find('File', '" + other + "')], " + PROCESS_NOW + "); }}");

		final Object result = evaluate("""
			${{
			    let hits     = $.documents.searchExtractedContent('structr.conf', { limit: 5, contextChunks: 1 });
			    let inFolder = $.documents.searchExtractedContent('paragraph', { folder: $.find('Folder', { path: '/docs' })[0] });

			    [ hits.length > 0, hits[0].headingPath.join(' > '), hits[0].file.name, hits[0].context.length > hits[0].text.length, inFolder.length ].join('|');
			}}
			""");

		assertEquals("true|Configuration|guide.html|true|1", result);
	}

	@Test
	public void testSearchExtractedContentInSimpleMode() throws Exception {

		createFulltextIndex();

		final String fileId = createFile("/docs", "guide.html", Fixtures.html());

		evaluate("${{ $.documents.processContent($.find('File', '" + fileId + "'), " + PROCESS_NOW + "); }}");

		// typo and different case: no hit as Lucene query, a hit in simple mode
		final Object result = evaluate("""
			${{
			    let lucene = $.documents.searchExtractedContent('CONFIGURATON');
			    let simple = $.documents.searchExtractedContent('CONFIGURATON', { mode: 'simple' });

			    [ lucene.length, simple.length > 0, simple[0].file.name ].join('|');
			}}
			""");

		assertEquals("0|true|guide.html", result);

		try {

			evaluate("${{ $.documents.searchExtractedContent('x', { mode: 'regex' }); }}");
			fail("An unknown mode must be rejected");

		} catch (final FrameworkException ex) {

			assertEquals(422, ex.getStatus());
		}
	}

	@Test
	public void testSearchFiles() throws Exception {

		createFulltextIndex();

		final String guideId = createFile("/docs", "guide.html", Fixtures.html());
		final String notesId = createFile("/other", "notes.txt", Fixtures.text());
		final NodeInterface user = createUser("reader", false);

		evaluate("${{ $.documents.processContent([$.find('File', '" + guideId + "'), $.find('File', '" + notesId + "')], " + PROCESS_NOW + "); }}");

		final ContentSearchOptions options = new ContentSearchOptions(10, 0.0, null, null, 0, ContentSearchMode.SIMPLE);

		try (final Tx tx = app.tx()) {

			// typos, prefixes and case; the two words are in different chunks of the guide
			final List<FileSearchHit> hits = ContentExtraction.searchFiles(securityContext, "instalation CONFIG", options, 5, 40);

			assertEquals(1, hits.size());
			assertEquals("guide.html", hits.get(0).file().getName());
			assertTrue(hits.get(0).snippets().stream().anyMatch(snippet -> snippet.match().equalsIgnoreCase("installation")));
			assertTrue(hits.get(0).snippets().stream().anyMatch(snippet -> snippet.match().equalsIgnoreCase("configuration")));

			// both files mention a paragraph, only the guide mentions Java as well
			assertEquals(List.of("guide.html"), ContentExtraction.searchFiles(securityContext, "paragraph java", options, 5, 40).stream().map(hit -> hit.file().getName()).toList());
			assertEquals(2, ContentExtraction.searchFiles(securityContext, "PARAGRAPH", options, 5, 40).size());

			// a folder limits the search to its subtree
			final Folder other = folder("/other");

			assertEquals(List.of("notes.txt"), ContentExtraction.searchFiles(securityContext, "paragraph", new ContentSearchOptions(10, 0.0, null, other, 0, ContentSearchMode.SIMPLE), 5, 40).stream().map(hit -> hit.file().getName()).toList());

			tx.success();
		}

		// a user who cannot read the files finds nothing
		final SecurityContext userContext = SecurityContext.getInstance(user.as(User.class), AccessMode.Backend);

		try (final Tx tx = StructrApp.getInstance(userContext).tx()) {

			assertTrue(ContentExtraction.searchFiles(userContext, "paragraph", options, 5, 40).isEmpty());

			tx.success();
		}

		// the REST method behind the Files search box
		try (final Tx tx = app.tx()) {

			createAdminUser();
			tx.success();
		}

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{ query: 'Instalation' }")
			.expect()
				.statusCode(200)
				.body("result",                     hasSize(1))
				.body("result[0].file.name",        equalTo("guide.html"))
				.body("result[0].file.path",        equalTo("/docs/guide.html"))
				.body("result[0].snippets[0].match", equalTo("Installation"))
			.when()
				.post("/ExtractedContentNode/searchFiles");
	}

	@Test
	public void testSearchFindsWordsInsideCode() throws Exception {

		createFulltextIndex();

		final String code   = "{\n    let orderTotal = $.calculate('total');\n\n    $.invoices.sendReminder()\n\n}\n";
		final String fileId = createFile("/scripts", "example.js", code.getBytes(StandardCharsets.UTF_8));

		evaluate("${{ $.documents.processContent($.find('File', '" + fileId + "'), " + PROCESS_NOW + "); }}");

		final ContentSearchOptions options = new ContentSearchOptions(10, 0.0, null, null, 0, ContentSearchMode.SIMPLE);

		try (final Tx tx = app.tx()) {

			// the index holds "invoices.sendreminder" and "ordertotal" as single terms
			final List<FileSearchHit> method   = ContentExtraction.searchFiles(securityContext, "sendReminder", options, 5, 40);
			final List<FileSearchHit> variable = ContentExtraction.searchFiles(securityContext, "Total", options, 5, 40);

			assertEquals(1, method.size());
			assertEquals("example.js", method.get(0).file().getName());
			assertEquals("sendReminder", method.get(0).snippets().get(0).match());

			assertEquals(1, variable.size());
			assertEquals("Total", variable.get(0).snippets().get(0).match());

			tx.success();
		}

		assertEquals("1", String.valueOf(evaluate("${{ $.documents.searchExtractedContent('sendreminder', { mode: 'simple' }).length; }}")));
	}

	@Test
	public void testGlobalSearchFindsContent() throws Exception {

		createFulltextIndex();

		final String fileId = createFile("/docs", "guide.html", Fixtures.html());

		evaluate("${{ $.documents.processContent($.find('File', '" + fileId + "'), " + PROCESS_NOW + "); }}");

		final List<GraphObject> files;
		final List<GraphObject> dom;

		try (final Tx tx = app.tx()) {

			files = SearchNodesCommand.executeSearch(securityContext, "release notes", List.of(DatabaseService.GLOBAL_SEARCH_CONTEXT_FILES), true);
			dom   = SearchNodesCommand.executeSearch(securityContext, "release notes", List.of(DatabaseService.GLOBAL_SEARCH_CONTEXT_DOM), true);

			tx.success();
		}

		final GraphObjectMap hit = (GraphObjectMap) files.stream().filter(r -> fileId.equals(((GraphObjectMap) r).toMap().get("id"))).findFirst().orElse(null);

		assertNotNull("The file must be found by its content", hit);

		final Map<String, Object> row = hit.toMap();

		assertEquals(List.of("extractedContent"), row.get("keys"));
		assertEquals(Boolean.TRUE, row.get("isFilesystemElement"));
		assertEquals("release", ((Map<?, ?>) ((List<?>) row.get("values")).get(0)).get("match").toString().toLowerCase());

		assertTrue("Content is only searched in the files context", dom.stream().noneMatch(r -> fileId.equals(((GraphObjectMap) r).toMap().get("id"))));
	}

	// ----- private methods -----
	private String createFile(final String folderPath, final String name, final byte[] data) throws Exception {

		try (final Tx tx = app.tx()) {

			final Folder folder      = createFolderInTx(folderPath);
			final NodeInterface file = FileHelper.createFile(securityContext, new ByteArrayInputStream(data), null, StructrTraits.FILE, name, folder);

			tx.success();

			return file.getUuid();
		}
	}

	/**
	 * Writes content like ChunkCommand: the last chunk updates the metadata and increases the version.
	 */
	private void writeChunk(final String fileId, final byte[] content, final boolean last) throws Exception {

		try (final Tx tx = app.tx()) {

			final File file = file(fileId);

			try (final OutputStream out = StorageProviderFactory.getStorageProvider(file).getOutputStream()) {

				out.write(content);
			}

			if (last) {

				FileHelper.updateMetadata(file);
				file.increaseVersion();
			}

			tx.success();
		}
	}

	private String createFileWithConfiguration(final String folderPath, final String name, final boolean enabled) throws Exception {

		// the configuration is attached before the content arrives, as it would be through the UI before an upload
		final String uuid;

		try (final Tx tx = app.tx()) {

			final Folder folder      = createFolderInTx(folderPath);
			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder)
			);

			createConfiguration(file, Map.of("enabled", enabled));

			uuid = file.getUuid();

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			FileHelper.writeToFile(file(uuid), Fixtures.text());
			file(uuid).notifyUploadCompletion();
			tx.success();
		}

		return uuid;
	}

	private void createFolder(final String path) throws Exception {

		try (final Tx tx = app.tx()) {

			createFolderInTx(path);
			tx.success();
		}
	}

	private Folder createFolderInTx(final String path) throws FrameworkException {

		return FileHelper.createFolderPath(securityContext, path).as(Folder.class);
	}

	private Folder folder(final String path) throws FrameworkException {

		return app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PATH_PROPERTY), path).getFirst().as(Folder.class);
	}

	private File file(final String uuid) throws FrameworkException {

		return app.getNodeById(StructrTraits.FILE, uuid).as(File.class);
	}

	/**
	 * Creates the folder path and gives the folder a content extraction configuration with the given
	 * values, replacing the one it has.
	 */
	private void configure(final String path, final Map<String, Object> values) throws Exception {

		removeConfiguration(path);

		try (final Tx tx = app.tx()) {

			createConfiguration(createFolderInTx(path), values);
			tx.success();
		}
	}

	private void createConfiguration(final NodeInterface target, final Map<String, Object> values) throws FrameworkException {

		final Traits traits          = Traits.of(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION);
		final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, values);

		properties.put(traits.key(ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY), target);

		app.create(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION, properties);
	}

	private void removeConfiguration(final String path) throws Exception {

		try (final Tx tx = app.tx()) {

			final Folder folder = createFolderInTx(path);

			for (final NodeInterface configuration : app.nodeQuery(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION).getAsList()) {

				final NodeInterface target = configuration.getProperty(Traits.of(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION).key(ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY));
				if (target != null && target.getUuid().equals(folder.getUuid())) {

					app.delete(configuration);
				}
			}

			tx.success();
		}
	}

	private void createFulltextIndex() throws Exception {

		// Structr's index management is off in tests, so the fulltext index is created here
		try (final Tx tx = app.tx()) {

			app.query("CREATE FULLTEXT INDEX " + ContentExtractionTraits.TEXT_FULLTEXT_INDEX + " IF NOT EXISTS FOR (n:ExtractedContentNode) ON EACH [n.text]", Map.of());
			tx.success();
		}

		try (final Tx tx = app.tx()) {

			app.query("CALL db.awaitIndexes(60)", Map.of());
			tx.success();
		}
	}

	private NodeInterface createUser(final String name, final boolean admin) throws Exception {

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), admin)
			);

			tx.success();

			return user;
		}
	}

	private Object property(final File file, final String key) {

		return file.getProperty(Traits.of(StructrTraits.FILE).key(key));
	}

	private String status(final String fileId) throws Exception {

		try (final Tx tx = app.tx()) {

			final String status = (String) property(file(fileId), ContentFileStateProperties.STATUS);

			tx.success();

			return status;
		}
	}

	private String error(final String fileId) throws Exception {

		try (final Tx tx = app.tx()) {

			final String error = (String) property(file(fileId), ContentFileStateProperties.ERROR);

			tx.success();

			return error;
		}
	}

	/**
	 * Waits for an automatic extraction job to finish the file.
	 */
	private String awaitStatus(final String fileId) throws Exception {

		final long timeout = System.currentTimeMillis() + 60_000;

		while (System.currentTimeMillis() < timeout) {

			final String status = status(fileId);
			if (status != null && !"running".equals(status)) {

				return status;
			}

			Thread.sleep(250);
		}

		fail("File " + fileId + " was not extracted within 60 seconds");

		return null;
	}

	private int chunkCount(final String fileId) throws Exception {

		try (final Tx tx = app.tx()) {

			final int count = ContentExtraction.getExtractedContent(file(fileId)).size();

			tx.success();

			return count;
		}
	}

	private int totalChunks() throws Exception {

		try (final Tx tx = app.tx()) {

			final int count = app.nodeQuery(ContentExtractionTraits.EXTRACTED_CONTENT_NODE).getAsList().size();

			tx.success();

			return count;
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
