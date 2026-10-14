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

import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Counts the queries that operations of the Bolt driver run. With a database on another host, every query costs a
 * network round trip, so a query that is not needed costs time in every request that runs it. The counts do not
 * depend on the machine, unlike the time.
 *
 * <p>The queries of a transaction are counted by the prefetch statistics, per prefetch hint.
 */
public class DatabaseQueryCountTest extends StructrUiTest {

	/**
	 * Deleting a relationship costs the query that deletes it. The end nodes stay in the transaction, so they are not
	 * loaded again for every relationship that is deleted from them.
	 */
	@Test
	public void testDeletingTheRelationshipsOfANodeRunsOneQueryPerRelationship() {

		createItemSchema();

		final long fewParts  = deleteParts("delete 5 relationships", createItem("few", 5));
		final long manyParts = deleteParts("delete 15 relationships", createItem("many", 15));

		assertEquals("Deleting 10 more relationships of a node must run 10 more queries", 10L, manyParts - fewParts);
	}

	/**
	 * Replacing a collection reads the existing relationships first, so the relationships to the removed nodes are in
	 * the relationship cache of the node, and each one costs the query that deletes it.
	 */
	@Test
	public void testReplacingACollectionRunsOneQueryPerRemovedRelationship() {

		createItemSchema();

		final long fewRemoved  = keepFirstParts("remove 5 of 10 parts", createItem("ten", 10), 5);
		final long manyRemoved = keepFirstParts("remove 10 of 15 parts", createItem("fifteen", 15), 5);

		assertEquals("Removing 5 more nodes from a collection must run 5 more queries", 5L, manyRemoved - fewRemoved);
	}

	/**
	 * The driver reads list properties as arrays, so a value that is set again is an equal array, not the same one.
	 */
	@Test
	public void testSettingAnUnchangedArrayPropertyRunsNoQuery() {

		createItemSchema();

		final String uuid = createItem("tags", 0);

		final long read      = queries("read the item", () -> node(uuid));
		final long unchanged = queries("set the same tags", () -> node(uuid).setProperty("tags", new String[] { "a", "b" }));
		final long changed   = queries("set other tags", () -> node(uuid).setProperty("tags", new String[] { "a", "c" }));

		assertEquals("Setting an unchanged array ran a query", read, unchanged);
		assertEquals("Setting a changed array must run one query", read + 1, changed);
	}

	/**
	 * Setting a property also sets the time and the user of the modification, which is written with the value.
	 */
	@Test
	public void testSettingAPropertyRunsOneQuery() {

		createItemSchema();

		final String uuid = createItem("name", 0);

		final long read    = queries("read the item for a name", () -> app.getNodeById("Item", uuid));
		final long changed = queries("set another name", () -> app.getNodeById("Item", uuid).setProperty(Traits.of("Item").key("name"), "other name"));

		assertEquals("Setting a property must run one query, with the access information", read + 1, changed);
	}

	// ----- private methods -----
	private void createItemSchema() {

		if (!Settings.DatabaseDriver.getValue().contains("bolt")) {

			throw new SkipException("The test counts the queries of the bolt driver, skipping test with " + Settings.DatabaseDriver.getValue());
		}

		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType item = schema.addType("Item");
			final JsonObjectType part = schema.addType("Part");

			item.addStringArrayProperty("tags");
			item.relate(part, "HAS", Cardinality.OneToMany, "item", "parts");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		Services.getInstance().getDatabaseService().resetPrefetchStatistics();
	}

	private String createItem(final String name, final int partCount) {

		try (final Tx tx = app.tx()) {

			final NodeInterface item        = app.create("Item", name);
			final List<NodeInterface> parts = new ArrayList<>();

			for (int i = 0; i < partCount; i++) {

				parts.add(app.create("Part", name + i));
			}

			item.setProperty(Traits.of("Item").key("parts"), parts);
			item.setProperty(Traits.of("Item").key("tags"), new String[] { "a", "b" });

			tx.success();

			return item.getUuid();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return null;
	}

	private long deleteParts(final String hint, final String uuid) {

		return queries(hint, () -> {

			for (final Relationship<?> relationship : Iterables.toList(node(uuid).getRelationships(Direction.OUTGOING))) {

				relationship.delete(true);
			}
		});
	}

	private long keepFirstParts(final String hint, final String uuid, final int count) {

		return queries(hint, () -> {

			final PropertyKey<Iterable<NodeInterface>> parts = Traits.of("Item").key("parts");
			final NodeInterface item                         = app.getNodeById("Item", uuid);
			final List<NodeInterface> kept                   = Iterables.toList(item.getProperty(parts)).subList(0, count);

			item.setProperty(parts, new ArrayList<>(kept));
		});
	}

	private Node<?> node(final String uuid) throws FrameworkException {

		return app.getNodeById("Item", uuid).getNode();
	}

	/**
	 * Runs the operation in a transaction of its own and returns how many queries the transaction ran.
	 */
	@SuppressWarnings("unchecked")
	private long queries(final String hint, final Operation operation) {

		try (final Tx tx = app.tx()) {

			tx.prefetchHint(hint);

			operation.run();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final DatabaseService db             = Services.getInstance().getDatabaseService();
		final Map<String, Object> statistics = db.getPrefetchStatistics();

		for (final Map<String, Object> entry : (List<Map<String, Object>>) statistics.get("hints")) {

			if (hint.equals(entry.get("hint"))) {

				final Map<String, Object> transactions = (Map<String, Object>) entry.get(Settings.PrefetchingEnabled.getValue() ? "withPrefetching" : "withoutPrefetching");

				return Math.round((Double) transactions.get("averageQueries"));
			}
		}

		fail("No statistics for " + hint);

		return 0L;
	}

	@FunctionalInterface
	private interface Operation {

		void run() throws FrameworkException;
	}
}
