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
package org.structr.test.core.script;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Searching an array property from a script (ticket 1417).
 *
 * <p>The search follows the REST semantics: an exact search, like a key and value given to find() or the equals
 * predicate, selects the objects whose array is equal to the search value, and a single value counts as an array
 * with one element. The contains predicate selects the objects whose array contains every element of the search
 * value. Script values used to reach the search as they came from the script, so a single value or a script array
 * caused an exception.
 */
public class ArrayPropertySearchTest extends StructrTest {

	@Test
	public void findWithKeyAndValue() {

		createItems();

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			assertEquals("find() with an array value must select the objects with an equal array", List.of("item1"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', 'tags', ['a', 'b', 'c']).map(i => i.name); }}", "findWithKeyAndValue"));

			assertEquals("find() with a single value must select the objects whose array consists of that value", List.of("item2"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', 'tags', 'a').map(i => i.name); }}", "findWithKeyAndValue"));

			assertEquals("find() with a map and a single value must select the objects whose array consists of that value", List.of("item2"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', { tags: 'a' }).map(i => i.name); }}", "findWithKeyAndValue"));

			assertEquals("find() with a single value must work in StructrScript", "item2",
				Scripting.evaluate(ctx, null, "${join(extract(find('Item', 'tags', 'a'), 'name'), ',')}", "findWithKeyAndValue"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void equalsPredicate() {

		createItems();

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			assertEquals("The equals predicate with a single value must select the objects whose array consists of that value", List.of("item2"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', $.predicate.equals('tags', 'a')).map(i => i.name); }}", "equalsPredicate"));

			assertEquals("The equals predicate with an array must select the objects with an equal array", List.of("item2"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', $.predicate.equals('tags', ['a'])).map(i => i.name); }}", "equalsPredicate"));

			assertEquals("The equals predicate with an array must select the objects with an equal array", List.of("item1"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', $.predicate.equals('tags', ['a', 'b', 'c'])).map(i => i.name); }}", "equalsPredicate"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void containsPredicate() {

		createItems();

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			assertEquals("The contains predicate with a single value must select the objects whose array contains it", List.of("item1", "item2"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', $.predicate.contains('tags', 'a'), $.predicate.sort('name')).map(i => i.name); }}", "containsPredicate"));

			assertEquals("The contains predicate with an array must select the objects whose array contains all of its elements", List.of("item1"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', $.predicate.contains('tags', ['a', 'b'])).map(i => i.name); }}", "containsPredicate"));

			assertEquals("The contains predicate with an array must select the objects whose array contains all of its elements", List.of("item3"),
				Scripting.evaluate(ctx, null, "${{ $.find('Item', $.predicate.contains('tags', ['b', 'd'])).map(i => i.name); }}", "containsPredicate"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- private methods -----
	private void createItems() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType item     = schema.addType("Item");

			item.addStringArrayProperty("tags");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final PropertyKey<String> name   = Traits.of("Item").key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<String[]> tags = Traits.of("Item").key("tags");

		try (final Tx tx = app.tx()) {

			app.create("Item", new NodeAttribute<>(name, "item1"), new NodeAttribute<>(tags, new String[] { "a", "b", "c" }));
			app.create("Item", new NodeAttribute<>(name, "item2"), new NodeAttribute<>(tags, new String[] { "a" }));
			app.create("Item", new NodeAttribute<>(name, "item3"), new NodeAttribute<>(tags, new String[] { "b", "d" }));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}
}
