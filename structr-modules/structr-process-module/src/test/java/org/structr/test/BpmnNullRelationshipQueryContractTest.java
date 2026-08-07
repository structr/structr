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
package org.structr.test;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.process.bpmn.BpmnImporter;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Reading a relationship-backed property and querying for it must agree.
 *
 * <p>BPMN handler methods make this testable, because they are the one case in the codebase where
 * a SchemaMethod has an incoming {@code HAS_METHOD} relationship that is NOT the
 * {@code schemaNode} ownership: {@code SchemaNodeMethodDefinition} (AbstractSchemaNode ->
 * SchemaMethod) and {@code BpmnProcessHasMethod} / {@code BpmnElementHasMethod} (BpmnProcess /
 * BpmnElement -> SchemaMethod) all use the relationship type {@code "HAS_METHOD"}, and the
 * endpoint type is the only thing that distinguishes them.</p>
 *
 * <p>So for a BPMN handler, {@code getProperty(schemaNode)} is null -- the read path filters
 * endpoints by type. This test pins that a query for {@code schemaNode == null} returns the same
 * set that the read says: anything else means the same key answers one way when read and another
 * way when searched, and code that pairs the two (the deployment export of global methods, the
 * SchemaMethod uniqueness validator, global user-function resolution) silently disagrees with
 * itself depending on the database driver in use.</p>
 */
public class BpmnNullRelationshipQueryContractTest extends AbstractProcessEngineTest {

	@Test
	public void testNullRelationshipQueryAgreesWithPropertyRead() throws Exception {

		importProcess("/engine-listeners.bpmn");

		try (final Tx tx = app.tx()) {

			final Traits traits                            = Traits.of(StructrTraits.SCHEMA_METHOD);
			final PropertyKey<String> nameKey              = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey<NodeInterface> schemaNodeKey = traits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);

			// (1) every handler method the importer created, found without using the property at all
			final List<NodeInterface> handlers = new ArrayList<>();

			for (final NodeInterface method : app.nodeQuery(StructrTraits.SCHEMA_METHOD).getAsList()) {

				final String name = method.getProperty(nameKey);
				if (name != null && name.contains("__bpmn__")) {

					handlers.add(method);
				}
			}

			assertTrue("fixture should have produced handler methods", handlers.size() > 0);

			// (2) the READ says: these have no schemaNode
			for (final NodeInterface handler : handlers) {

				final AbstractSchemaNode owningType = handler.as(org.structr.core.entity.SchemaMethod.class).getSchemaNode();

				assertNull("a BPMN handler method must read as having no schemaNode: " + handler.getProperty(nameKey), owningType);
				assertNull("the same, through the property key", handler.getProperty(schemaNodeKey));
			}

			// (3) the QUERY must say the same thing
			final List<String> queried = new ArrayList<>();

			for (final NodeInterface method : app.nodeQuery(StructrTraits.SCHEMA_METHOD).key(schemaNodeKey, null).getAsList()) {

				queried.add(method.getUuid());
			}

			for (final NodeInterface handler : handlers) {

				assertTrue("querying schemaNode == null must return the methods whose schemaNode READS as null"
					+ " -- '" + handler.getProperty(nameKey) + "' is missing, so the read path and the query path disagree"
					+ " (query returned " + queried.size() + " method(s))", queried.contains(handler.getUuid()));
			}

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected failure: " + fex.getMessage());
		}
	}
}
