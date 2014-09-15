/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;
import org.structr.schema.importer.GraphGistImporter;
import org.structr.web.common.StructrUiTest;

/**
 *
 * @author Christian Morgner
 */
public class ImportTest extends StructrUiTest {

	public void testImportAndSchemaAnaylzer() {

		final String source =
			"== Test setup\n" +
			"\n" +
			"[source, cypher]\n" +
			"----\n" +
			"CREATE (c:Company { name: 'Company 1', comp_id: '12345', string_name: 'company1', year: 2013, month: 6, day: 7, status: 'test'})\n" +
			"CREATE (p:Company { name: 'Company 2'})\n" +
			"----\n";

		final List<String> sourceLines = GraphGistImporter.extractSources(new ByteArrayInputStream(source.getBytes(Charset.forName("utf-8"))));

		try (final Tx tx = app.tx()) {

			GraphGistImporter.importCypher(sourceLines);
			GraphGistImporter.analyzeSchema();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName("Company").getFirst();

			assertNotNull("A schema node with name 'Company' should have been created: ", schemaNode);

			assertEquals("Company schema node should have a 'name' property with value 'String': ",        "String",  schemaNode.getProperty(new StringProperty("_name")));
			assertEquals("Company schema node should have a 'comp_id' property with value 'String': ",     "String",  schemaNode.getProperty(new StringProperty("_comp_id")));
			assertEquals("Company schema node should have a 'string_name' property with value 'String': ", "String",  schemaNode.getProperty(new StringProperty("_string_name")));
			assertEquals("Company schema node should have a 'year' property with value 'Long': ",          "Long",    schemaNode.getProperty(new StringProperty("_year")));
			assertEquals("Company schema node should have a 'month' property with value 'Long': ",         "Long",    schemaNode.getProperty(new StringProperty("_year")));
			assertEquals("Company schema node should have a 'day' property with value 'Long': ",           "Long",    schemaNode.getProperty(new StringProperty("_day")));
			assertEquals("Company schema node should have a 'status' property with value 'String': ",      "String",  schemaNode.getProperty(new StringProperty("_status")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


	}
}


/*
*/