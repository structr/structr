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
package org.structr.test.schema;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * Vectors of a vector-indexed Double[] property that the index would leave out are refused when they are
 * written. This needs no database with vector indexes: the check uses the size from the schema.
 */
public class VectorPropertyTest extends StructrTest {

	@Test
	public void testVectorsTheIndexWouldLeaveOutAreRefused() {

		createVectorTypes();

		// vectors of the size of the index, no vector and an empty one are fine
		try (final Tx tx = app.tx()) {

			final NodeInterface doc = app.create("CosineDoc", new NodeAttribute<>(embedding("CosineDoc"), new Double[] { 1.0, 0.0, 0.5 }));

			doc.setProperty(embedding("CosineDoc"), null);
			assertNull(doc.getProperty(embedding("CosineDoc")));

			doc.setProperty(embedding("CosineDoc"), new Double[0]);

			app.create("EuclideanDoc", new NodeAttribute<>(embedding("EuclideanDoc"), new Double[] { 0.0, 0.0, 0.0 }));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		assertRefused("CosineDoc", new Double[] { 1.0, 0.0 },                       "vector_size_mismatch",    2,     3);
		assertRefused("CosineDoc", new Double[] { 1.0, 0.0, 0.0, 0.0 },             "vector_size_mismatch",    4,     3);
		assertRefused("CosineDoc", new Double[] { 1.0, Double.NaN, 0.0 },           "vector_value_not_finite", "NaN", 1);
		assertRefused("CosineDoc", new Double[] { 1.0, 0.0, Double.POSITIVE_INFINITY }, "vector_value_not_finite", "Infinity", 2);
		assertRefused("CosineDoc", new Double[] { 1.0, null, 0.0 },                 "vector_value_not_finite", "null", 1);
		assertRefused("CosineDoc", new Double[] { 0.0, 0.0, 0.0 },                  "vector_zero_with_cosine", null,  null);
	}

	@Test
	public void testVectorsFromScriptsAreChecked() {

		createVectorTypes();

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.create('CosineDoc', { embedding: [0.1, 0.2] }); }}", "test");

			fail("A vector of the wrong size must be refused.");

		} catch (FrameworkException fex) {

			assertEquals(422, fex.getStatus());
			assertEquals("vector_size_mismatch", fex.getErrorBuffer().getErrorTokens().get(0).getToken());
		}

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.create('CosineDoc', { embedding: [0.1, 0.2, 0.3] }); }}", "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testVectorIndexNeedsDoubleArrayAndPositiveSize() {

		assertInvalidSchemaProperty("String",      3, "vector_index_needs_double_array");
		assertInvalidSchemaProperty("DoubleArray", 0, "must_be_positive");
	}

	// ----- private methods -----
	private void createVectorTypes() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType cosine    = schema.addType("CosineDoc");
			final JsonType euclidean = schema.addType("EuclideanDoc");

			cosine.addDoubleArrayProperty("embedding").setVectorIndex(3, "cosine");
			euclidean.addDoubleArrayProperty("embedding").setVectorIndex(3, "euclidean");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private PropertyKey<Double[]> embedding(final String type) {

		return Traits.of(type).key("embedding");
	}

	private void assertRefused(final String type, final Double[] vector, final String expectedToken, final Object expectedValue, final Object expectedDetail) {

		try (final Tx tx = app.tx()) {

			app.create(type, new NodeAttribute<>(embedding(type), vector));

			fail("The vector must be refused with " + expectedToken + ".");

		} catch (FrameworkException fex) {

			final ErrorToken token = fex.getErrorBuffer().getErrorTokens().get(0);

			assertEquals(422, fex.getStatus());
			assertEquals(expectedToken, token.getToken());
			assertEquals(type, token.getType());
			assertEquals("embedding", token.getProperty());
			assertEquals(expectedValue, token.getValue());
			assertEquals(expectedDetail, token.getDetail());
		}
	}

	private void assertInvalidSchemaProperty(final String propertyType, final int vectorSize, final String expectedToken) {

		final Traits nodeTraits     = Traits.of(StructrTraits.SCHEMA_NODE);
		final Traits propertyTraits = Traits.of(StructrTraits.SCHEMA_PROPERTY);

		try (final Tx tx = app.tx()) {

			final NodeInterface type = app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(nodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "InvalidVectorDoc" + vectorSize));

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),      type),
				new NodeAttribute<>(propertyTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "embedding"),
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY),    propertyType),
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.VECTOR_DIMENSIONS_PROPERTY), vectorSize)
			);

			tx.success();

			fail("The schema property must be refused with " + expectedToken + ".");

		} catch (FrameworkException fex) {

			assertEquals(422, fex.getStatus());
			assertEquals(expectedToken, fex.getErrorBuffer().getErrorTokens().get(0).getToken());
		}
	}
}
