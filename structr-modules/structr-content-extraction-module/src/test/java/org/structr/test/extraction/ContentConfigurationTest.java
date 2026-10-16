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
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.entity.ContentExtractionConfiguration;
import org.structr.extraction.traits.definitions.ContentExtractionConfigurationTraitDefinition;
import org.structr.extraction.traits.properties.ContentFileStateProperties;
import org.structr.test.web.advanced.DeploymentTestBase;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.AssertJUnit.*;

/**
 * Content extraction configurations: the methods behind the Files dialog, and deployment.
 */
public class ContentConfigurationTest extends DeploymentTestBase {

	private static final String TYPE = ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION;

	@Test
	public void testDialogMethods() throws Exception {

		final String folderId;
		final String fileId;

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final NodeInterface folder = FileHelper.createFolderPath(securityContext, "/kb");
			final NodeInterface file   = FileHelper.createFile(securityContext, new ByteArrayInputStream(Fixtures.text()), "text/plain", StructrTraits.FILE, "notes.txt", folder.as(Folder.class));

			folderId = folder.getUuid();
			fileId   = file.getUuid();

			tx.success();
		}

		// a folder without configuration: defaults and allowed values for the dialog
		request("{ target: '" + folderId + "' }")
			.expect()
				.statusCode(200)
				.body("result.target.path",              equalTo("/kb"))
				.body("result.canEdit",                  equalTo(true))
				.body("result.configuration",            nullValue())
				.body("result.inherited",                nullValue())
				.body("result.decision.extract",         equalTo(false))
				.body("result.defaults.chunkSize",       equalTo(512))
				.body("result.choices.splitter",         hasItem("structure"))
			.when()
				.post("/" + TYPE + "/describe");

		// create: empty values stay unset
		request("{ target: '" + folderId + "', values: { enabled: true, chunkSize: 256, chunkOverlap: '', splitter: 'recursive', separators: ['\\n\\n', ' '] } }")
			.expect()
				.statusCode(200)
				.body("result.configuration.enabled",               equalTo(true))
				.body("result.configuration.options.chunkSize",     equalTo(256))
				.body("result.configuration.options.chunkOverlap",  nullValue())
				.body("result.configuration.options.separators",    equalTo(List.of("\n\n", " ")))
				.body("result.decision.extract",                    equalTo(true))
				.body("result.effective.chunkSize.source",          equalTo("config:/kb"))
				.body("result.effective.chunkOverlap.source",       equalTo("setting"))
			.when()
				.post("/" + TYPE + "/save");

		// a file below the folder inherits it, and shows its extraction state
		request("{ target: '" + fileId + "' }")
			.expect()
				.statusCode(200)
				.body("result.configuration",   nullValue())
				.body("result.inherited.path",  equalTo("/kb"))
				.body("result.decision.source", equalTo("folder"))
				.body("result.state.chunkCount", equalTo(0))
			.when()
				.post("/" + TYPE + "/describe");

		// update: invalid values are rejected
		request("{ target: '" + folderId + "', values: { chunkSize: 5 } }")
			.expect()
				.statusCode(422)
			.when()
				.post("/" + TYPE + "/save");

		request("{ target: '" + folderId + "', values: { unknown: 5 } }")
			.expect()
				.statusCode(422)
			.when()
				.post("/" + TYPE + "/save");

		// update keeps the node
		request("{ target: '" + folderId + "', values: { enabled: false, chunkSize: 300 } }")
			.expect()
				.statusCode(200)
				.body("result.configuration.options.chunkSize", equalTo(300))
				.body("result.decision.extract",                equalTo(false))
			.when()
				.post("/" + TYPE + "/save");

		try (final Tx tx = app.tx()) {

			assertEquals("Saving again updates the configuration", 1, app.nodeQuery(TYPE).getAsList().size());
			tx.success();
		}

		request("{ target: '" + folderId + "' }")
			.expect()
				.statusCode(200)
				.body("result.configuration", nullValue())
			.when()
				.post("/" + TYPE + "/remove");

		try (final Tx tx = app.tx()) {

			assertEquals(0, app.nodeQuery(TYPE).getAsList().size());
			tx.success();
		}
	}

	@Test
	public void testDeploymentRoundtrip() throws Exception {

		final String folderId;
		final String configurationId;

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "exported"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true)
			);

			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, TYPE, Map.of(
				"enabled", false,
				"chunkSize", 300,
				"separators", List.of("\n\n", " "),
				"includeHeadingPath", true,
				"ocr", "off"
			));

			properties.put(Traits.of(TYPE).key(ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY), folder);

			folderId        = folder.getUuid();
			configurationId = app.create(TYPE, properties).getUuid();

			tx.success();
		}

		doImportExportRoundtrip(true);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(TYPE, configurationId);

			assertNotNull("The configuration survives the deployment roundtrip with its UUID", node);

			final ContentExtractionConfiguration configuration = node.as(ContentExtractionConfiguration.class);

			assertEquals(folderId, configuration.getTarget().getUuid());
			assertFalse("enabled = false survives", configuration.isEnabled());
			assertEquals(Map.of("chunkSize", 300, "separators", List.of("\n\n", " "), "includeHeadingPath", true, "ocr", "off"), configuration.getOptionValues());

			tx.success();
		}
	}

	@Test
	public void testDeploymentImportExtractsConfiguredFiles() throws Exception {

		final String extractedId;
		final String disabledId;

		try (final Tx tx = app.tx()) {

			final NodeInterface knowledge = exportedFolder("knowledge");
			final NodeInterface disabled  = exportedFolder("disabled");

			extractedId = FileHelper.createFile(securityContext, new ByteArrayInputStream(Fixtures.text()), "text/plain", StructrTraits.FILE, "notes.txt", knowledge.as(Folder.class)).getUuid();
			disabledId  = FileHelper.createFile(securityContext, new ByteArrayInputStream(Fixtures.text()), "text/plain", StructrTraits.FILE, "notes.txt", disabled.as(Folder.class)).getUuid();

			createConfiguration(knowledge, true);
			createConfiguration(disabled, false);

			tx.success();
		}

		// chunks are not exported, and the files are imported before their configurations
		doImportExportRoundtrip(true);

		assertEquals("Files of an enabled configuration are extracted after the import", "done", awaitStatus(extractedId));
		assertTrue("The imported file has chunks again", chunkCount(extractedId) > 0);

		// the same job has finished the enabled file, so it would have processed the other one as well
		assertNull("Files of a disabled configuration are not extracted", status(disabledId));
	}

	// ----- private methods -----
	private NodeInterface exportedFolder(final String name) throws Exception {

		return app.create(StructrTraits.FOLDER,
			new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
			new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true)
		);
	}

	private void createConfiguration(final NodeInterface target, final boolean enabled) throws Exception {

		final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, TYPE, Map.of("enabled", enabled));

		properties.put(Traits.of(TYPE).key(ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY), target);

		app.create(TYPE, properties);
	}

	private String status(final String fileId) throws Exception {

		try (final Tx tx = app.tx()) {

			final String status = (String) app.getNodeById(StructrTraits.FILE, fileId).getProperty(Traits.of(StructrTraits.FILE).key(ContentFileStateProperties.STATUS));

			tx.success();

			return status;
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

			final int count = ContentExtraction.getExtractedContent(app.getNodeById(StructrTraits.FILE, fileId).as(File.class)).size();

			tx.success();

			return count;
		}
	}

	private RequestSpecification request(final String body) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.header(X_USER_HEADER, ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeMatches(greaterThan(422)))
				.body(body);
	}
}
