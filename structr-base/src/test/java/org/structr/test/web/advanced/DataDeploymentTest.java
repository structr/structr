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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
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
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class DataDeploymentTest extends StructrUiTest {

	@Test
	public void testDataDeploymentWithMultipleInheritance() {

		// setup: create the classical diamond-shaped inheritance
		//    A
		//   / \
		//  B   C
		//   \ /
		//    D
		//
		// Each type has its own method and, we expect TestD to inherit all four methods.

		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType testA       = sourceSchema.addType("TestA");
			final JsonType testB       = sourceSchema.addType("TestB");
			final JsonType testC       = sourceSchema.addType("TestC");
			final JsonType testD       = sourceSchema.addType("TestD");

			testB.addTrait("TestA");
			testC.addTrait("TestA");

			testD.addTrait("TestB");
			testD.addTrait("TestC");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// create test data
		try (final Tx tx = app.tx()) {

			app.create("TestA", "Test A");
			app.create("TestB", "Test B");
			app.create("TestC", "Test C");
			app.create("TestD", "Test D");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}


		try {

			final Path exportPath = doExport("TestA", "TestB", "TestC", "TestD");

			try (final Tx tx = app.tx()) {

				// delete all objects of type TestA (includes TestB, TestC and TestD)
				for (final NodeInterface n : app.nodeQuery("TestA").getResultStream()) {

					app.delete(n);
				}

				tx.success();
			}

			doImport(exportPath);
			deleteExportAt(exportPath);

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		// check data
		try (final Tx tx = app.tx()) {

			final PropertyKey<String> nameKey = Traits.of("TestA").key("name");
			final List<NodeInterface> nodes = app.nodeQuery("TestA").sort(nameKey).getAsList();

			assertEquals("Invalid number of nodes after data deployment roundtrip", 4, nodes.size());

			assertEquals("Invalid node name after data deployment roundtrip", "Test A", nodes.get(0).getName());
			assertEquals("Invalid node name after data deployment roundtrip", "Test B", nodes.get(1).getName());
			assertEquals("Invalid node name after data deployment roundtrip", "Test C", nodes.get(2).getName());
			assertEquals("Invalid node name after data deployment roundtrip", "Test D", nodes.get(3).getName());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- protected methods -----
	protected Path doExport(final String... types) throws FrameworkException {

		final DeployDataCommand cmd = app.command(DeployDataCommand.class);
		final Path tmp              = Paths.get("/tmp/structr-data-deployment-test" + System.currentTimeMillis() + System.nanoTime());

		// export to temp directory
		final Map<String, Object> firstExportParams = new HashMap<>();
		firstExportParams.put("mode", "export");
		firstExportParams.put("target", tmp.toString());
		firstExportParams.put("types",  StringUtils.join(types, ","));

		// execute deploy command
		cmd.execute(firstExportParams);

		return tmp;
	}

	protected void doImport(final Path path) throws FrameworkException {

		final DeployDataCommand cmd = app.command(DeployDataCommand.class);

		// import from exported source
		final Map<String, Object> firstImportParams = new HashMap<>();
		firstImportParams.put("mode", "import");
		firstImportParams.put("source", path.toString());

		// execute deploy command
		cmd.execute(firstImportParams);
	}

	protected void deleteExportAt(final Path path) throws IOException {
		Files.walkFileTree(path, new DeploymentTestBase.DeletingFileVisitor());
	}
}