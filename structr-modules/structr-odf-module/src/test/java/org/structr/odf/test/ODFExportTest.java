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
package org.structr.odf.test;

import com.google.gson.JsonObject;
import io.restassured.RestAssured;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.dom.OdfContentDom;
import org.odftoolkit.odfdom.pkg.OdfPackage;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.odf.entity.ODFExporter;
import org.structr.odf.traits.definitions.ODFExporterTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the property mapping of the ODS and ODT exporters: which values end up in which
 * cell or user field, and which mappings are rejected.
 */
public class ODFExportTest extends ODSTestBase {

	private static final String TEXT_NS   = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
	private static final String OFFICE_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";

	@Test
	public void testODSExportAttributes() {

		String exporterId = null;
		String exportedId = null;

		createAdminUser();

		try (final Tx tx = app.tx()) {

			final File template      = createTemplate("template.ods", createSpreadsheetTemplate());
			final NodeInterface node = createExporter(StructrTraits.ODS_EXPORTER, template);

			setPropertyMapping(node,
				new String[] { "name", "A1", null },
				new String[] { null,   "B2", "concat(input.name, '!')" },
				new String[] { null,   "C3", "split('x,y,z')" },
				new String[] { null,   "D4", "{ 42; }" }
			);

			exporterId = node.getUuid();
			exportedId = app.create(StructrTraits.GROUP, "Exported").getUuid();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		exportAttributes(StructrTraits.ODS_EXPORTER, exporterId, exportedId);

		try (final Tx tx = app.tx()) {

			try (final InputStream is = getResultDocumentStream(exporterId)) {

				final OdfSpreadsheetDocument document = OdfSpreadsheetDocument.loadDocument(is);
				final OdfTable sheet                  = document.getTableList().get(0);

				assertEquals("Direct property mapping not exported", "Exported",  sheet.getCellByPosition("A1").getStringValue());
				assertEquals("Output function not exported",         "Exported!", sheet.getCellByPosition("B2").getStringValue());

				assertEquals("Collection must be exported downwards, one element per row", "x", sheet.getCellByPosition("C3").getStringValue());
				assertEquals("Collection must be exported downwards, one element per row", "y", sheet.getCellByPosition("C4").getStringValue());
				assertEquals("Collection must be exported downwards, one element per row", "z", sheet.getCellByPosition("C5").getStringValue());

				assertEquals("Number not exported", 42.0, sheet.getCellByPosition("D4").getDoubleValue());

				document.close();
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testODTExportAttributes() {

		String exporterId = null;
		String exportedId = null;

		createAdminUser();

		try (final Tx tx = app.tx()) {

			final File template      = createTemplate("template.odt", createTextTemplate("title", "list", "empty", "untouched"));
			final NodeInterface node = createExporter(StructrTraits.ODT_EXPORTER, template);

			setPropertyMapping(node, new String[] { "name", "title", null }, new String[] { null,   "list",  "split('x,y,z')" }, new String[] { null,   "empty", "null" }
			);

			exporterId = node.getUuid();
			exportedId = app.create(StructrTraits.GROUP, "Exported").getUuid();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		exportAttributes(StructrTraits.ODT_EXPORTER, exporterId, exportedId);

		try (final Tx tx = app.tx()) {

			try (final InputStream is = getResultDocumentStream(exporterId)) {

				final OdfDocument document       = OdfDocument.loadDocument(is);
				final NodeList fields            = document.getContentRoot().getElementsByTagName("text:user-field-decl");
				final Map<String, String> values = new LinkedHashMap<>();

				for (int i = 0; i < fields.getLength(); i++) {

					final Element field = (Element)fields.item(i);

					values.put(field.getAttribute("text:name"), field.getAttribute("office:string-value"));
				}

				assertEquals("Direct property mapping not exported", "Exported",  values.get("title"));
				assertEquals("Collection not exported",              "x\ny\nz\n", values.get("list"));

				assertEquals("Null value must not overwrite the value from the template", "initial", values.get("empty"));
				assertEquals("Unmapped field must keep the value from the template",     "initial", values.get("untouched"));

				document.close();
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testODSExportCustomPropertyWithShorthandMapping() {

		String exporterId = null;
		String exportedId = null;

		createAdminUser();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Item");

			type.addStringProperty("comment");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final File template      = createTemplate("template.ods", createSpreadsheetTemplate());
			final NodeInterface node = createExporter(StructrTraits.ODS_EXPORTER, template);

			// a plain string value is the name of the source property
			node.setProperty(Traits.of(StructrTraits.ODF_EXPORTER).key(ODFExporterTraitDefinition.PROPERTY_MAPPING_PROPERTY), "{ \"A1\": \"comment\", \"A2\": \"name\" }");

			exporterId = node.getUuid();
			exportedId = app.create("Item",
				new NodeAttribute<>(Traits.of("Item").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Exported"),
				new NodeAttribute<>(Traits.of("Item").key("comment"),                                  "custom value")
			).getUuid();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		exportAttributes(StructrTraits.ODS_EXPORTER, exporterId, exportedId);

		try (final Tx tx = app.tx()) {

			try (final InputStream is = getResultDocumentStream(exporterId)) {

				final OdfSpreadsheetDocument document = OdfSpreadsheetDocument.loadDocument(is);
				final OdfTable sheet                  = document.getTableList().get(0);

				assertEquals("Custom property not exported",   "custom value", sheet.getCellByPosition("A1").getStringValue());
				assertEquals("Built-in property not exported", "Exported",     sheet.getCellByPosition("A2").getStringValue());

				document.close();
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testExportWithoutMappingIsRejected() {

		testExportIsRejected(null, null, 422);
	}

	@Test
	public void testExportWithInvalidMappingIsRejected() {

		testExportIsRejected("[ \"no object\" ]", null, 422);
	}

	@Test
	public void testExportWithIncompleteMappingEntryIsRejected() {

		testExportIsRejected("{ \"A1\": { \"input\": \"upper(input)\" } }", null, 422);
	}

	@Test
	public void testExportWithArrayMappingValueIsRejected() {

		testExportIsRejected("{ \"A1\": [ \"name\" ] }", null, 422);
	}

	@Test
	public void testExportWithNumericMappingValueIsRejected() {

		testExportIsRejected("{ \"A1\": { \"source\": 5 } }", null, 422);
	}

	@Test
	public void testExportOfUnknownNodeIsRejected() {

		testExportIsRejected("{ \"A1\": \"name\" }", "00000000000000000000000000000000", 404);
	}

	@Test
	public void testExportUsesSecurityContextOfCaller() {

		final Traits traits = Traits.of(StructrTraits.NODE_INTERFACE);
		String exporterId   = null;
		String readableId   = null;
		String secretId     = null;

		createAdminUser();

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),  "tester"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "test")
			);

			final File template      = createTemplate("template.ods", createSpreadsheetTemplate());
			final NodeInterface node = createExporter(StructrTraits.ODS_EXPORTER, template);

			setPropertyMapping(node, new String[] { "name", "A1", null });

			node.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			exporterId = node.getUuid();
			readableId = app.create(StructrTraits.GROUP, new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Readable"), new NodeAttribute<>(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true)).getUuid();
			secretId   = app.create(StructrTraits.GROUP, "Secret").getUuid();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// the admin creates the result document, the user only exports into it
		callMethod(ADMIN_USERNAME, ADMIN_PASSWORD, StructrTraits.ODS_EXPORTER, exporterId, "createDocumentFromTemplate", null, 200);

		try (final Tx tx = app.tx()) {

			final File result = app.getNodeById(StructrTraits.ODF_EXPORTER, exporterId).as(ODFExporter.class).getResultDocument();

			result.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		grant(StructrTraits.ODS_EXPORTER + "/_id/exportAttributes", UiAuthenticator.AUTH_USER_POST, true);

		callMethod("tester", "test", StructrTraits.ODS_EXPORTER, exporterId, "exportAttributes", readableId, 200);
		callMethod("tester", "test", StructrTraits.ODS_EXPORTER, exporterId, "exportAttributes", secretId,   404);

		try (final Tx tx = app.tx()) {

			try (final InputStream is = getResultDocumentStream(exporterId)) {

				final OdfSpreadsheetDocument document = OdfSpreadsheetDocument.loadDocument(is);

				assertEquals("Readable node not exported", "Readable", document.getTableList().get(0).getCellByPosition("A1").getStringValue());

				document.close();
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testExportImageUsesSecurityContextOfCaller() {

		final Traits traits = Traits.of(StructrTraits.NODE_INTERFACE);
		final byte[] png    = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
		String exporterId   = null;
		String readableId   = null;
		String secretId     = null;

		createAdminUser();

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),  "tester"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "test")
			);

			final File template      = createTemplate("template.odt", createTextTemplate());
			final NodeInterface node = createExporter(StructrTraits.ODT_EXPORTER, template);

			node.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			final NodeInterface readable = FileHelper.createFile(securityContext, png, "image/png", StructrTraits.IMAGE, "readable.png", true);
			final NodeInterface secret   = FileHelper.createFile(securityContext, png, "image/png", StructrTraits.IMAGE, "secret.png", true);

			readable.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			exporterId = node.getUuid();
			readableId = readable.getUuid();
			secretId   = secret.getUuid();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// the admin creates the result document, the user only exports into it
		callMethod(ADMIN_USERNAME, ADMIN_PASSWORD, StructrTraits.ODT_EXPORTER, exporterId, "createDocumentFromTemplate", null, 200);

		try (final Tx tx = app.tx()) {

			final File result = app.getNodeById(StructrTraits.ODF_EXPORTER, exporterId).as(ODFExporter.class).getResultDocument();

			result.setProperty(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		grant(StructrTraits.ODT_EXPORTER + "/_id/exportImage", UiAuthenticator.AUTH_USER_POST, true);

		callMethod("tester", "test", StructrTraits.ODT_EXPORTER, exporterId, "exportImage", readableId, 200);
		callMethod("tester", "test", StructrTraits.ODT_EXPORTER, exporterId, "exportImage", secretId,   404);

		try (final Tx tx = app.tx()) {

			try (final InputStream is = getResultDocumentStream(exporterId)) {

				final OdfPackage pkg = OdfPackage.loadPackage(is);

				assertTrue("Readable image not exported",                      pkg.contains("Pictures/readable.png"));
				assertFalse("Image without read access must not be exported", pkg.contains("Pictures/secret.png"));

				pkg.close();
			}

			for (final String id : List.of(readableId, secretId)) {

				try (final InputStream is = StorageProviderFactory.getStorageProvider(app.getNodeById(StructrTraits.IMAGE, id).as(File.class)).getInputStream()) {

					assertTrue("Exporting an image must not change the image itself", Arrays.equals(png, is.readAllBytes()));
				}
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	// ----- private methods -----
	/**
	 * Configures the mapping of the given exporter. Each entry is { sourceName, targetName, outputFunction }.
	 */
	private void setPropertyMapping(final NodeInterface exporter, final String[]... entries) throws FrameworkException {

		final JsonObject mapping = new JsonObject();

		for (final String[] entry : entries) {

			final JsonObject value = new JsonObject();

			value.addProperty("source", entry[0]);
			value.addProperty("output", entry[2]);

			mapping.add(entry[1], value);
		}

		exporter.setProperty(Traits.of(StructrTraits.ODF_EXPORTER).key(ODFExporterTraitDefinition.PROPERTY_MAPPING_PROPERTY), mapping.toString());
	}

	/**
	 * Calls exportAttributes with the given mapping and expects it to fail. Without a UUID, an
	 * existing node is exported.
	 */
	private void testExportIsRejected(final String propertyMapping, final String uuid, final int expectedStatusCode) {

		String exporterId = null;
		String exportedId = uuid;

		createAdminUser();

		try (final Tx tx = app.tx()) {

			final File template      = createTemplate("template.ods", createSpreadsheetTemplate());
			final NodeInterface node = createExporter(StructrTraits.ODS_EXPORTER, template);

			node.setProperty(Traits.of(StructrTraits.ODF_EXPORTER).key(ODFExporterTraitDefinition.PROPERTY_MAPPING_PROPERTY), propertyMapping);

			exporterId = node.getUuid();

			if (exportedId == null) {

				exportedId = app.create(StructrTraits.GROUP, "Exported").getUuid();
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		callMethod(ADMIN_USERNAME, ADMIN_PASSWORD, StructrTraits.ODS_EXPORTER, exporterId, "exportAttributes", exportedId, expectedStatusCode);
	}

	private NodeInterface createExporter(final String type, final File template) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.ODF_EXPORTER);

		return app.create(type,
			new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "exporter"),
			new NodeAttribute<>(traits.key(ODFExporterTraitDefinition.DOCUMENT_TEMPLATE_PROPERTY), template)
		);
	}

	private File createTemplate(final String name, final byte[] data) throws Exception {

		return FileHelper.createFile(securityContext, data, "application/octet-stream", StructrTraits.FILE, name, true).as(File.class);
	}

	private byte[] createSpreadsheetTemplate() throws Exception {

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (final OdfSpreadsheetDocument document = OdfSpreadsheetDocument.newSpreadsheetDocument()) {

			document.save(out);
		}

		return out.toByteArray();
	}

	private byte[] createTextTemplate(final String... fieldNames) throws Exception {

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (final OdfTextDocument document = OdfTextDocument.newTextDocument()) {

			final OdfContentDom dom = document.getContentDom();
			final Element text      = document.getContentRoot();
			final Element decls     = dom.createElementNS(TEXT_NS, "text:user-field-decls");

			for (final String fieldName : fieldNames) {

				final Element decl = dom.createElementNS(TEXT_NS, "text:user-field-decl");

				decl.setAttributeNS(TEXT_NS,   "text:name",           fieldName);
				decl.setAttributeNS(OFFICE_NS, "office:value-type",   "string");
				decl.setAttributeNS(OFFICE_NS, "office:string-value", "initial");

				decls.appendChild(decl);
			}

			text.insertBefore(decls, text.getFirstChild());

			document.save(out);
		}

		return out.toByteArray();
	}

	private void exportAttributes(final String type, final String exporterId, final String exportedId) {

		callMethod(ADMIN_USERNAME, ADMIN_PASSWORD, type, exporterId, "createDocumentFromTemplate", null,       200);
		callMethod(ADMIN_USERNAME, ADMIN_PASSWORD, type, exporterId, "exportAttributes",           exportedId, 200);
	}

	private void callMethod(final String username, final String password, final String type, final String exporterId, final String method, final String uuid, final int expectedStatusCode) {

		RestAssured
			.given()
			.header(X_USER_HEADER, username)
			.header(X_PASSWORD_HEADER, password)
			.body(uuid != null ? "{ \"uuid\": \"" + uuid + "\" }" : "{}")
			.expect()
			.statusCode(expectedStatusCode)
			.when()
			.post("/" + type + "/" + exporterId + "/" + method);
	}

	private InputStream getResultDocumentStream(final String exporterId) throws FrameworkException {

		final File result = app.getNodeById(StructrTraits.ODF_EXPORTER, exporterId).as(ODFExporter.class).getResultDocument();

		return StorageProviderFactory.getStorageProvider(result).getInputStream();
	}
}
