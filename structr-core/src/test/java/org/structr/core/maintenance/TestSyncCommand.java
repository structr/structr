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
package org.structr.core.maintenance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.structr.api.graph.Label;
import org.structr.api.util.Iterables;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.TestEleven;
import org.structr.core.entity.TestOne;
import org.structr.core.graph.SyncCommand;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class TestSyncCommand extends StructrTest {

	private final static String EXPORT_FILENAME = "___structr-test-export___.zip";

	public void testSyncCommandParameters() {

		try {

			// test failure with non-existing mode
			try {

				// test failure with wrong mode
				app.command(SyncCommand.class).execute(toMap("mode", "non-existing-mode", "file", EXPORT_FILENAME));
				fail("Using SyncCommand with a non-existing mode should throw an exception.");

			} catch (FrameworkException fex) {

				// status: 400
				assertEquals(400, fex.getStatus());
				assertEquals("Please specify sync mode (import|export).", fex.getMessage());
			}

			// test failure with omitted file name
			try {

				// test failure with wrong mode
				app.command(SyncCommand.class).execute(toMap("mode", "export"));
				fail("Using SyncCommand without file parameter should throw an exception.");

			} catch (FrameworkException fex) {

				// status: 400
				assertEquals(400, fex.getStatus());
				assertEquals("Please specify sync file.", fex.getMessage());
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testSyncCommandBasicExportImport() {

		try {
			// create test nodes
			createTestNodes(TestOne.class, 100);

			// test export
			app.command(SyncCommand.class).execute(toMap("mode", "export", "file", EXPORT_FILENAME));

			final Path exportFile = Paths.get(EXPORT_FILENAME);

 			assertTrue("Export file doesn't exist!", Files.exists(exportFile));

			// stop existing and start new database
			this.tearDown();
			this.setUp();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME));

			try (final Tx tx = app.tx()) {
				assertEquals(100, app.nodeQuery(TestOne.class).getResult().size());
			}

			// clean-up after test
			Files.delete(exportFile);

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testSyncCommandInheritance() {

		try {
			// create test nodes
			final List<TestEleven> testNodes = createTestNodes(TestEleven.class, 10);

			try (final Tx tx = app.tx()) {

				for (final TestEleven node : testNodes) {

					Iterable<Label> labels = node.getNode().getLabels();

					assertEquals(7, Iterables.count(labels));

					for (final Label label : labels) {
						System.out.print(label.name() + " ");
					}
					System.out.println();

					assertEquals("First label has to be AbstractNode",       Iterables.toList(labels).get(0).name(), "AbstractNode");
					assertEquals("Second label has to be NodeInterface",     Iterables.toList(labels).get(1).name(), "NodeInterface");
					assertEquals("Third label has to be AccessControllable", Iterables.toList(labels).get(2).name(), "AccessControllable");
					assertEquals("Fourth label has to be CMISInfo",          Iterables.toList(labels).get(3).name(), "CMISInfo");
					assertEquals("Firth label has to be CMISItemInfo",       Iterables.toList(labels).get(4).name(), "CMISItemInfo");
					assertEquals("Sixth label has to be TestEleven",         Iterables.toList(labels).get(5).name(), "TestEleven");
					assertEquals("Seventh label has to be TestOne",          Iterables.toList(labels).get(6).name(), "TestOne");
				}

				tx.success();
			}


			// test export
			app.command(SyncCommand.class).execute(toMap("mode", "export", "file", EXPORT_FILENAME));

			final Path exportFile = Paths.get(EXPORT_FILENAME);

 			assertTrue("Export file doesn't exist!", Files.exists(exportFile));

			// stop existing and start new database
			this.tearDown();
			this.setUp();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME));


			try (final Tx tx = app.tx()) {

				final Result<TestEleven> result = app.nodeQuery(TestEleven.class).getResult();
				assertEquals(10, result.size());

				for (final TestEleven node : result.getResults()) {

					Iterable<Label> labels = node.getNode().getLabels();

					assertEquals(7, Iterables.count(labels));

					assertEquals("First label has to be AbstractNode",       Iterables.toList(labels).get(0).name(), "AbstractNode");
					assertEquals("Second label has to be NodeInterface",     Iterables.toList(labels).get(1).name(), "NodeInterface");
					assertEquals("Third label has to be AccessControllable", Iterables.toList(labels).get(2).name(), "AccessControllable");
					assertEquals("Fourth label has to be CMISInfo",          Iterables.toList(labels).get(3).name(), "CMISInfo");
					assertEquals("Firth label has to be CMISItemInfo",       Iterables.toList(labels).get(4).name(), "CMISItemInfo");
					assertEquals("Sixth label has to be TestEleven",         Iterables.toList(labels).get(5).name(), "TestEleven");
					assertEquals("Seventh label has to be TestOne",          Iterables.toList(labels).get(6).name(), "TestOne");

				}

				tx.success();
			}

			// clean-up after test
			Files.delete(exportFile);

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

}
