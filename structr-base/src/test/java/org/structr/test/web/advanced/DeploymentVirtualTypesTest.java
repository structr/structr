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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Virtual types were removed together with the api-builder module. An export that still
 * contains them must import without error, the virtual types are ignored.
 */
public class DeploymentVirtualTypesTest extends DeploymentTestBase {

	@Test
	public void testImportIgnoresVirtualTypes() {

		try (final Tx tx = app.tx()) {

			Page.createNewPage(securityContext, "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try {

			final Path export     = doExport();
			final Path virtualDir = Files.createDirectories(export.resolve("modules").resolve("api-builder"));

			Files.writeString(virtualDir.resolve("virtual-types.json"), "[ { \"name\": \"Projects\", \"sourceType\": \"Project\", \"properties\": [] } ]", StandardCharsets.UTF_8);

			doImport(export);
			deleteExportAt(export);

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Import of an export with virtual types must not fail.");
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Import did not complete", 1, app.nodeQuery(StructrTraits.PAGE).name("test").getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
