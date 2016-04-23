/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.schema;

import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class SchemaDeleteRelationshipTest extends StructrTest {

	public void test00DeleteSchemaRelationshipInView() {

		SchemaRelationshipNode rel = null;

		try (final Tx tx = app.tx()) {

			// create source and target node
			final SchemaNode fooNode = app.create(SchemaNode.class, "Foo");
			final SchemaNode barNode = app.create(SchemaNode.class, "Bar");

			// create relationship
			rel = app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, fooNode),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, barNode),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "narf")
			);

			// create "public" view that contains the related property
			app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "public"),
				new NodeAttribute<>(SchemaView.schemaNode, fooNode),
				new NodeAttribute<>(SchemaView.nonGraphProperties, "type, id, narfBars")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.delete(rel);
			tx.success();

		} catch (Throwable t) {

			// deletion of relationship should not fail

			t.printStackTrace();
			fail("Unexpected exception");
		}
	}

}
