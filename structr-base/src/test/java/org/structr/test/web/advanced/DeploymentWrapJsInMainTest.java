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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * wrapJsInMain decides whether a JavaScript method body is wrapped in a main() function, which
 * changes what the body may contain (a top-level return is only legal when wrapped, an import
 * statement only when it is not). The property defaults to TRUE, so a method that does not carry
 * the value through a deployment round-trip silently comes back wrapped, and a body written for
 * the unwrapped mode stops working on the target instance.
 */
public class DeploymentWrapJsInMainTest extends DeploymentTestBase {

	private static final String GLOBAL_METHOD_NAME = "unwrappedGlobalMethod";
	private static final String TYPE_NAME          = "WrapJsTestType";
	private static final String TYPE_METHOD_NAME   = "unwrappedTypeMethod";

	@Test
	public void testWrapJsInMainSurvivesDeploymentRoundtrip() {

		// setup: one global and one type-bound method, both explicitly NOT wrapped
		try (final Tx tx = app.tx()) {

			final Traits methodTraits                    = Traits.of(StructrTraits.SCHEMA_METHOD);
			final PropertyKey<String> nameKey            = methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey<String> sourceKey          = methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY);
			final PropertyKey<Boolean> wrapKey           = methodTraits.key(SchemaMethodTraitDefinition.WRAP_JS_IN_MAIN_PROPERTY);
			final PropertyKey<NodeInterface> schemaNodeKey = methodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY);

			// a method without a schemaNode is a global method
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(nameKey,   GLOBAL_METHOD_NAME),
				new NodeAttribute<>(sourceKey, "{ $.log('global'); }"),
				new NodeAttribute<>(wrapKey,   Boolean.FALSE)
			);

			final NodeInterface schemaNode = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), TYPE_NAME)
			);

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(nameKey,       TYPE_METHOD_NAME),
				new NodeAttribute<>(sourceKey,     "{ $.log('type-bound'); }"),
				new NodeAttribute<>(wrapKey,       Boolean.FALSE),
				new NodeAttribute<>(schemaNodeKey, schemaNode)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception during setup.");
		}

		// guard: the value really is false before the round-trip, so a failure below is
		// about the deployment and not about the property refusing to be set
		assertWrapJsInMain("before the round-trip", Boolean.FALSE, Boolean.FALSE);

		// export to disk, wipe the database, import it back
		doImportExportRoundtrip(true);

		// wrapJsInMain must come back as it went in
		assertWrapJsInMain("after the round-trip", Boolean.FALSE, Boolean.FALSE);
	}

	// ----- private methods -----
	private void assertWrapJsInMain(final String when, final Boolean expectedGlobal, final Boolean expectedTypeBound) {

		try (final Tx tx = app.tx()) {

			final Traits methodTraits          = Traits.of(StructrTraits.SCHEMA_METHOD);
			final PropertyKey<Boolean> wrapKey = methodTraits.key(SchemaMethodTraitDefinition.WRAP_JS_IN_MAIN_PROPERTY);

			final NodeInterface globalMethod = app.nodeQuery(StructrTraits.SCHEMA_METHOD).name(GLOBAL_METHOD_NAME).getFirst();
			assertNotNull("global method " + GLOBAL_METHOD_NAME + " not found " + when, globalMethod);
			assertEquals("wrapJsInMain of the global method is wrong " + when, expectedGlobal, globalMethod.getProperty(wrapKey));

			final NodeInterface typeMethod = app.nodeQuery(StructrTraits.SCHEMA_METHOD).name(TYPE_METHOD_NAME).getFirst();
			assertNotNull("type-bound method " + TYPE_METHOD_NAME + " not found " + when, typeMethod);
			assertEquals("wrapJsInMain of the type-bound method is wrong " + when, expectedTypeBound, typeMethod.getProperty(wrapKey));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while checking wrapJsInMain " + when + ".");
		}
	}
}
