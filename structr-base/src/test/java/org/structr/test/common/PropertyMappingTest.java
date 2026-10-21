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
package org.structr.test.common;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyMapping;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * Both directions of {@link PropertyMapping}: input (external values into Structr
 * properties, used by the CSV import) and output (Structr objects into external values,
 * used by the ODF exporters).
 */
public class PropertyMappingTest extends StructrTest {

	@Test
	public void testTransformInput() {

		try (final Tx tx = app.tx()) {

			final PropertyMapping mapping = new PropertyMapping(List.of(
				new PropertyMapping.Entry("name",      "Item name", null,           null),
				new PropertyMapping.Entry("comment",   "comment",   "upper(input)", null),
				new PropertyMapping.Entry("unchanged", null,        null,           null),
				new PropertyMapping.Entry(null,        "computed",  null,           "'output only'")
			));

			final Map<String, Object> input = new LinkedHashMap<>();

			input.put("Item name", "one");
			input.put("comment",   "first");
			input.put("unchanged", "same");
			input.put("unmapped",  "dropped");
			input.put("computed",  "ignored");

			mapping.transformInput(new ActionContext(securityContext), input);

			assertEquals("Mapped value not renamed",       "one",   input.get("name"));
			assertEquals("Input function not applied",     "FIRST", input.get("comment"));
			assertEquals("Missing target must not rename", "same",  input.get("unchanged"));

			assertFalse("Target name must be replaced by source name", input.containsKey("Item name"));
			assertFalse("Unmapped key must be removed",                input.containsKey("unmapped"));
			assertFalse("Output-only entry must not write a value",    input.containsKey(null));
			assertEquals("Unexpected keys in result", 3, input.size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testTransformInputIgnoresMissingValues() {

		try (final Tx tx = app.tx()) {

			final PropertyMapping mapping = new PropertyMapping(List.of(new PropertyMapping.Entry("name", "Item name", "error('must not be called')", null)));
			final Map<String, Object> input = new LinkedHashMap<>();

			mapping.transformInput(new ActionContext(securityContext), input);

			assertFalse("Missing value must not be created, and its input function must not run", input.containsKey("name"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testTransformInputWithSwappedAndSharedTargets() {

		try (final Tx tx = app.tx()) {

			final PropertyMapping mapping = new PropertyMapping(List.of(
				new PropertyMapping.Entry("name",    "comment", null, null),
				new PropertyMapping.Entry("comment", "name",    null, null),
				new PropertyMapping.Entry("copy",    "name",    null, null)
			));

			final Map<String, Object> input = new LinkedHashMap<>();

			input.put("name",    "one");
			input.put("comment", "first");

			mapping.transformInput(new ActionContext(securityContext), input);

			assertEquals("Swapped targets must read the original input", "first", input.get("name"));
			assertEquals("Swapped targets must read the original input", "one",   input.get("comment"));
			assertEquals("Shared target must be read by every entry",    "one",   input.get("copy"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testTransformOutput() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Item");

			type.addStringProperty("comment");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Traits traits      = Traits.of("Item");
			final NodeInterface item = app.create("Item",
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "one"),
				new NodeAttribute<>(traits.key("comment"),                                  "first")
			);

			final PropertyMapping mapping = new PropertyMapping(List.of(
				new PropertyMapping.Entry("name",    "A1", null, null),
				new PropertyMapping.Entry("comment", "A2", null, null),
				new PropertyMapping.Entry("missing", "A3", null, null),
				new PropertyMapping.Entry(null,      "A4", null, "concat(input.name, '-', input.comment)")
			));

			final Map<String, Object> output = mapping.transformOutput(new ActionContext(securityContext), item);

			assertEquals("Unexpected output keys", List.of("A1", "A2", "A3", "A4"), List.copyOf(output.keySet()));

			assertEquals("Built-in property not mapped", "one",       output.get("A1"));
			assertEquals("Custom property not mapped",   "first",     output.get("A2"));
			assertNull("Unknown property must map to null",           output.get("A3"));
			assertEquals("Output function not applied",  "one-first", output.get("A4"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testEntryWithoutSourceOrOutputFunctionIsRejected() {

		try {

			new PropertyMapping(List.of(new PropertyMapping.Entry(null, "A1", "upper(input)", null)));

			fail("Entry without source name and output function must be rejected.");

		} catch (FrameworkException fex) {

			assertEquals("Wrong status code", 422, fex.getStatus());
		}
	}
}
