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
package org.structr.test.core.graph;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.MigrationService;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Whether the label migration repairs nodes whose labels no longer match their type.
 *
 * A node carries the labels its type had at the time the node was written, so a version upgrade that
 * adds or removes a trait leaves existing nodes behind in both directions: without a label they should
 * have, or with one they should not. Neither is reachable through the typed API - a label that is
 * missing cannot be queried for, and a label that does not belong to the type is not written again -
 * so the broken state is produced here the same way an upgrade produces it, by going at the labels
 * through the database driver.
 *
 * Every test asserts three things in order: that the dry run reports nothing while the database is
 * sound, that it reports the damage without repairing it, and that the apply run repairs it.
 */
public class MigrationLabelTest extends StructrTest {

	@Test
	public void testStaleDataSourceLabelIsRemoved() {

		// A MailTemplate stands in for the flow types: until 03/2025 they implemented an interface that
		// was also called DataSource, and a node was labelled with every interface its type implemented,
		// so the label stayed behind when the interface went away. structr-base cannot see the flow
		// module, and any type without the trait reproduces the situation exactly.
		final String uuid = createNode(StructrTraits.MAIL_TEMPLATE, "stale-data-source");

		assertFalse("a sound database must not report changes", dryRunReportsChanges());

		changeLabels(uuid, Set.of(StructrTraits.DATA_SOURCE), Set.of());

		assertTrue("the dry run must report the stale label", dryRunReportsChanges());
		assertTrue("the dry run must not repair anything", hasLabel(uuid, StructrTraits.DATA_SOURCE));

		migrate();

		assertFalse("the stale DataSource label must be gone", hasLabel(uuid, StructrTraits.DATA_SOURCE));
		assertTrue("the type's own label must survive", hasLabel(uuid, StructrTraits.MAIL_TEMPLATE));
	}

	@Test
	public void testMissingDataSourceLabelIsAdded() {

		// Folder became a data source with the component/widget data sources, so folders written by an
		// earlier version have every other label of their type but not that one.
		final String uuid = createNode(StructrTraits.FOLDER, "missing-data-source");

		assertFalse("a sound database must not report changes", dryRunReportsChanges());

		changeLabels(uuid, Set.of(), Set.of(StructrTraits.DATA_SOURCE));

		assertTrue("the dry run must report the missing label", dryRunReportsChanges());
		assertFalse("the dry run must not repair anything", hasLabel(uuid, StructrTraits.DATA_SOURCE));

		migrate();

		assertTrue("the missing DataSource label must be back", hasLabel(uuid, StructrTraits.DATA_SOURCE));
	}

	@Test
	public void testMissingDataSourceLabelIsAddedToSchemaNodes() {

		// The case this migration was written for: SchemaNode became a data source with the
		// component/widget data sources, so every type definition written by an earlier version is
		// missing the label. Covered separately from Folder because a SchemaNode is not an ordinary
		// node - it reloads the schema when it changes, and the bulk command has to cope with that.
		final String uuid = createNode(StructrTraits.SCHEMA_NODE, "MigrationLabelTestType");

		assertFalse("a sound database must not report changes", dryRunReportsChanges());

		changeLabels(uuid, Set.of(), Set.of(StructrTraits.DATA_SOURCE));

		assertTrue("the dry run must report the missing label", dryRunReportsChanges());
		assertFalse("the dry run must not repair anything", hasLabel(uuid, StructrTraits.DATA_SOURCE));

		migrate();

		assertTrue("the missing DataSource label must be back", hasLabel(uuid, StructrTraits.DATA_SOURCE));
		assertTrue("the type's own label must survive", hasLabel(uuid, StructrTraits.SCHEMA_NODE));
	}

	@Test
	public void testMissingPrincipalLabelIsAdded() {

		final String uuid = createNode(StructrTraits.GROUP, "missing-principal");

		assertFalse("a sound database must not report changes", dryRunReportsChanges());

		changeLabels(uuid, Set.of(), Set.of(StructrTraits.PRINCIPAL));

		assertTrue("the dry run must report the missing label", dryRunReportsChanges());
		assertFalse("the dry run must not repair anything", hasLabel(uuid, StructrTraits.PRINCIPAL));

		migrate();

		assertTrue("the missing Principal label must be back", hasLabel(uuid, StructrTraits.PRINCIPAL));
		assertTrue("the type's own label must survive", hasLabel(uuid, StructrTraits.GROUP));
	}

	@Test
	public void testAnUnrelatedLabelIsLeftAloneOnPrincipals() {

		// The principal step runs with removeUnused off: it exists to add a label that older versions
		// did not write, and taking other labels off User and Group nodes is not part of that.
		final String uuid = createNode(StructrTraits.GROUP, "unrelated-label");

		changeLabels(uuid, Set.of("SomeLabelFromAnEarlierVersion"), Set.of(StructrTraits.PRINCIPAL));

		migrate();

		assertTrue("the missing Principal label must be back", hasLabel(uuid, StructrTraits.PRINCIPAL));
		assertTrue("an unrelated label must not be stripped", hasLabel(uuid, "SomeLabelFromAnEarlierVersion"));
	}

	// ----- private methods -----
	private String createNode(final String type, final String name) {

		try {

			final NodeInterface node = createTestNode(type, name);

			// createTestNode() has closed its transaction, and reading the uuid needs one
			try (final Tx tx = app.tx()) {

				final String uuid = node.getUuid();

				tx.success();

				return uuid;
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());

			return null;
		}
	}

	/**
	 * Adds and removes labels on a node through the database driver, the way an upgraded instance ends
	 * up with them - the typed API would write the label set that belongs to the type.
	 */
	private void changeLabels(final String uuid, final Set<String> toAdd, final Set<String> toRemove) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(uuid);

			for (final String label : toRemove) {

				node.getNode().removeLabel(label);
			}

			if (!toAdd.isEmpty()) {

				node.getNode().addLabels(toAdd);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// the labels were changed behind the framework's back, so anything cached about the node is stale
		FlushCachesCommand.flushAll();
	}

	private boolean hasLabel(final String uuid, final String label) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node        = app.getNodeById(uuid);
			final Set<String> actualLabels  = Iterables.toSet(node.getNode().getLabels());

			tx.success();

			return actualLabels.contains(label);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());

			return false;
		}
	}

	private boolean dryRunReportsChanges() {

		try {

			return MigrationService.execute(true);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());

			return false;
		}
	}

	private void migrate() {

		try {

			MigrationService.execute(false);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		FlushCachesCommand.flushAll();
	}
}
