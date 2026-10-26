/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.maintenance.DeployDataCommand;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * A data import runs without the callbacks that are part of the transaction, onCreate and onSave, unless the
 * parameter doInnerCallbacks asks for them. The import in the default mode seed creates each record again, so the
 * onCreate method of the type shows whether they ran.
 */
public class DataImportCallbacksTest extends StructrUiTest {

	private static final String TYPE = "CallbackTestItem";

	@Test
	public void testInnerCallbacksRunWhenRequested() {

		assertEquals("The import did not run onCreate although doInnerCallbacks was set", "created", noteAfterImport(true));
	}

	@Test
	public void testInnerCallbacksDoNotRunByDefault() {

		assertNull("The import ran onCreate although doInnerCallbacks was not set", noteAfterImport(false));
	}

	// ----- private methods -----
	/**
	 * Exports a record, imports it again and returns the note that onCreate sets in the import.
	 */
	private String noteAfterImport(final boolean doInnerCallbacks) {

		Path archive = null;

		try {

			createCallbackSchema();

			final String id = createItem();

			archive = export();

			importWith(archive, doInnerCallbacks);

			return noteOf(id);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");

		} finally {

			cleanUp(archive);
		}

		return null;
	}

	private void createCallbackSchema() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType(TYPE);

			type.addStringProperty("note");
			type.addMethod("onCreate", "{ if ($.this.name === 'imported') { $.this.note = 'created'; } }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}
	}

	/**
	 * Creates the record under a name that onCreate ignores and renames it afterwards, so that only the onCreate of
	 * the import sets the note. The rename needs a transaction of its own, since onCreate runs at the commit.
	 */
	private String createItem() throws FrameworkException {

		final String id;

		try (final Tx tx = app.tx()) {

			id = app.create(TYPE, "item").getUuid();

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			app.getNodeById(TYPE, id).setProperty(Traits.of(TYPE).key("name"), "imported");

			tx.success();
		}

		return id;
	}

	private String noteOf(final String id) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final String note = app.getNodeById(TYPE, id).getProperty(Traits.of(TYPE).key("note"));

			tx.success();

			return note;
		}
	}

	private Path export() throws FrameworkException {

		final Path tmp                   = Paths.get("/tmp/structr-import-callbacks-test" + System.currentTimeMillis() + System.nanoTime());
		final Map<String, Object> params = new HashMap<>();

		params.put("mode", "export");
		params.put("target", tmp.toString());
		params.put("types", TYPE);

		app.command(DeployDataCommand.class).execute(params);

		return tmp;
	}

	private void importWith(final Path archive, final boolean doInnerCallbacks) throws FrameworkException {

		final Map<String, Object> params = new HashMap<>();

		params.put("mode", "import");
		params.put("source", archive.toString());

		if (doInnerCallbacks) {

			params.put(DeployDataCommand.DO_INNER_CALLBACKS_PARAMETER_NAME, "true");
		}

		app.command(DeployDataCommand.class).execute(params);
	}

	private void cleanUp(final Path archive) {

		if (archive != null) {

			try {

				Files.walkFileTree(archive, new DeploymentTestBase.DeletingFileVisitor());

			} catch (final IOException ignore) {}
		}
	}
}
