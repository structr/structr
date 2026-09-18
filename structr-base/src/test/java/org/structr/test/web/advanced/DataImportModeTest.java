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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.maintenance.DeployDataCommand;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * What a data import does with a record the target already has.
 *
 * <p>The command was written as a seeder and does one thing: delete the record by uuid and create it
 * again. That is right for filling an empty instance and wrong wherever the target holds data worth
 * keeping, because a recreated node keeps only the relationships that are in the archive as well, and
 * because it overwrites a target that may be deliberately different.</p>
 *
 * <p>The three modes are told apart by one question: an archive record whose uuid exists on the target.
 * seed replaces it, append leaves it, update writes onto it.</p>
 */
public class DataImportModeTest extends StructrUiTest {

	private static final String TYPE = "ModeTestItem";

	@Test
	public void testTheThreeModesDifferOnARecordTheTargetAlreadyHas() {

		Path archive = null;

		try {

			createModeSchema();

			// the archive: one record, named "from-archive"
			final String id = createItem("from-archive");

			archive = export();

			// the target's own copy of the same record, deliberately different
			rename(id, "changed-on-target");

			// append leaves what is already there
			importWith(archive, "append");
			assertEquals("append must not touch a record the target already has", "changed-on-target", nameOf(id));

			// update writes the archive's values onto it
			importWith(archive, "update");
			assertEquals("update must write the archive's value onto the existing record", "from-archive", nameOf(id));

			// and seed, the default, replaces it: same uuid, archive's values
			rename(id, "changed-again");

			importWith(archive, "seed");
			assertEquals("seed recreates the record from the archive", "from-archive", nameOf(id));

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());

		} finally {

			cleanUp(archive);
		}
	}

	@Test
	public void testUpdateKeepsTheNodeRatherThanRecreatingIt() {

		Path archive = null;

		try {

			createModeSchema();

			final String id = createItem("from-archive");

			archive = export();

			// a node the export set never mentions, linked to the record: the reason update exists
			final String outsiderId = createOutsiderLinkedTo(id);

			importWith(archive, "update");

			assertNotNull("update must not delete the record", nameOf(id));

			try (final Tx tx = app.tx()) {

				final NodeInterface outsider = app.getNodeById(outsiderId);

				assertNotNull("a node outside the export set must survive an update", outsider);

				tx.success();
			}

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());

		} finally {

			cleanUp(archive);
		}
	}

	@Test
	public void testAnUnknownModeIsRefusedRatherThanTreatedAsTheDestructiveDefault() {

		Path archive = null;

		try {

			createModeSchema();

			final String id = createItem("from-archive");

			archive = export();

			rename(id, "changed-on-target");

			final DeployDataCommand cmd      = app.command(DeployDataCommand.class);
			final Map<String, Object> params = new HashMap<>();

			params.put("mode", "import");
			params.put("source", archive.toString());
			params.put("importMode", "replace-everything");

			cmd.execute(params);

			// a typo must not fall through to seed, which would have overwritten the target
			assertEquals("an unknown mode should be refused", 422, cmd.getCommandStatusCode());
			assertEquals("the record must be untouched after a refused import", "changed-on-target", nameOf(id));

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());

		} finally {

			cleanUp(archive);
		}
	}

	@Test
	public void testAnInstanceThatCallsItselfProductionRefusesADataImport() {

		Path archive             = null;
		final String previousStage = Settings.InstanceStage.getValue("");

		try {

			createModeSchema();

			final String id = createItem("from-archive");

			archive = export();

			rename(id, "live-data");

			Settings.InstanceStage.setValue("production");

			final DeployDataCommand cmd      = app.command(DeployDataCommand.class);
			final Map<String, Object> params = new HashMap<>();

			params.put("mode", "import");
			params.put("source", archive.toString());

			cmd.execute(params);

			assertEquals("a data import into a production instance should be refused", 422, cmd.getCommandStatusCode());
			assertEquals("the refused import must not have touched the data", "live-data", nameOf(id));

			// the guard is a warning, not a wall: it is overridable, because the platform cannot actually know
			final DeployDataCommand forced    = app.command(DeployDataCommand.class);
			final Map<String, Object> forcedParams = new HashMap<>();

			forcedParams.put("mode", "import");
			forcedParams.put("source", archive.toString());
			forcedParams.put("force", "true");

			forced.execute(forcedParams);

			assertEquals("a forced import proceeds", "from-archive", nameOf(id));

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());

		} finally {

			Settings.InstanceStage.setValue(previousStage);

			cleanUp(archive);
		}
	}

	@Test
	public void testAnyOtherStageImportsNormally() {

		Path archive               = null;
		final String previousStage = Settings.InstanceStage.getValue("");

		try {

			createModeSchema();

			final String id = createItem("from-archive");

			archive = export();

			rename(id, "changed");

			// only "production" refuses; an unnamed or differently named instance is not second-guessed
			for (final String stage : new String[] { "", "dev", "staging" }) {

				Settings.InstanceStage.setValue(stage);
				rename(id, "changed");

				importWith(archive, "seed");

				assertEquals("stage '" + stage + "' should not block an import", "from-archive", nameOf(id));
			}

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());

		} finally {

			Settings.InstanceStage.setValue(previousStage);

			cleanUp(archive);
		}
	}

	@Test
	public void testTheCommandNamesTheTypesThatHoldRecords() {

		try {

			createModeSchema();

			createItem("one");
			createItem("two");

			final DeployDataCommand cmd      = app.command(DeployDataCommand.class);
			final Map<String, Object> params = new HashMap<>();

			params.put("mode", "types");

			cmd.execute(params);

			final List<Map<String, Object>> types = (List<Map<String, Object>>) cmd.getCommandResult();

			assertNotNull("the command should answer with the types it found", types);

			final Map<String, Object> mine = types.stream()
				.filter(t -> TYPE.equals(t.get("type")))
				.findFirst()
				.orElse(null);

			assertNotNull("a type that holds records has to be named: " + types, mine);
			assertEquals("with the number of records it holds", 2L, ((Number) mine.get("count")).longValue());

			// a type nobody created anything of is not worth exporting and is left out
			assertTrue("a type without records should not be listed: " + types,
				types.stream().allMatch(t -> ((Number) t.get("count")).longValue() > 0));

		} catch (final Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t.getMessage());
		}
	}

	// ----- helpers -----

	private void createModeSchema() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType item     = schema.addType(TYPE);

			item.addStringProperty("note");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}
	}

	private String createItem(final String name) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(TYPE, name);
			final String id          = node.getUuid();

			tx.success();

			return id;
		}
	}

	private String createOutsiderLinkedTo(final String id) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			// the outsider owns the record, which is a relationship the archive does not carry
			final NodeInterface outsider = app.create("Group", "outsiders");
			final NodeInterface item     = app.getNodeById(id);

			item.setProperty(Traits.of(TYPE).key("owner"), outsider);

			tx.success();

			return outsider.getUuid();
		}
	}

	private void rename(final String id, final String name) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			app.getNodeById(id).setProperty(Traits.of(TYPE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

			tx.success();
		}
	}

	private String nameOf(final String id) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(id);
			final String name        = node != null ? node.getName() : null;

			tx.success();

			return name;
		}
	}

	private Path export() throws FrameworkException {

		final Path tmp                   = Paths.get("/tmp/structr-import-mode-test" + System.currentTimeMillis() + System.nanoTime());
		final Map<String, Object> params = new HashMap<>();

		params.put("mode", "export");
		params.put("target", tmp.toString());
		params.put("types", StringUtils.join(new String[] { TYPE }, ","));

		app.command(DeployDataCommand.class).execute(params);

		return tmp;
	}

	private void importWith(final Path archive, final String mode) throws FrameworkException {

		final Map<String, Object> params = new HashMap<>();

		params.put("mode", "import");
		params.put("source", archive.toString());
		params.put("importMode", mode);

		app.command(DeployDataCommand.class).execute(params);
	}

	private void cleanUp(final Path archive) {

		if (archive != null) {

			try {

				Files.walkFileTree(archive, new DeploymentTestBase.DeletingFileVisitor());

			} catch (final IOException ignore) {}
		}
	}
}
