/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.testng.annotations.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.testng.AssertJUnit.*;

public class AdvancedDeploymentTest extends FullStructrTest {

	private static final Logger logger = LoggerFactory.getLogger(AdvancedDeploymentTest.class.getName());

	@Test
	public void testSchemaImportFromVersion40() {

		try (final Reader reader = new InputStreamReader(AdvancedDeploymentTest.class.getResourceAsStream("/test/schema4.0.json"))) {

			final JsonSchema schema = StructrSchema.createFromSource(reader);
			StructrSchema.replaceDatabaseSchema(app, schema);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		// check existing schema nodes after import
		try (final Tx tx = app.tx()) {

			final Set<String> remainingTypes = new LinkedHashSet<>();

			StructrApp.getInstance().nodeQuery("SchemaNode").getResultStream().forEach(n -> remainingTypes.add(n.getName()));

			assertEquals("Invalid number of remaining types after schema import", 2, remainingTypes.size());
			assertTrue(remainingTypes.contains("Project"));
			assertTrue(remainingTypes.contains("Page"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSchemaImportFromVersion52() {

		try (final Reader reader = new InputStreamReader(AdvancedDeploymentTest.class.getResourceAsStream("/test/schema5.2.json"))) {

			final JsonSchema schema = StructrSchema.createFromSource(reader);
			StructrSchema.replaceDatabaseSchema(app, schema);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		// check existing schema nodes after import
		try (final Tx tx = app.tx()) {

			final Set<String> remainingTypes = new LinkedHashSet<>();

			StructrApp.getInstance().nodeQuery("SchemaNode").getResultStream().forEach(n -> remainingTypes.add(n.getName()));

			for (final String type : remainingTypes) {

				System.out.println(type);
			}

			/*
			assertEquals("Invalid number of remaining types after schema import", 2, remainingTypes.size());
			assertTrue(remainingTypes.contains("Project"));
			assertTrue(remainingTypes.contains("Page"));
			*/

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}
}


























