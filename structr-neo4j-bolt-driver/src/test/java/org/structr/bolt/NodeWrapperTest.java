/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.bolt;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Transaction;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

public class NodeWrapperTest {

	private static final Logger logger = LoggerFactory.getLogger(NodeWrapperTest.class.getName());

	@Test
	public void testDeleteException() {

		try {

			Settings.DatabasePath.setValue(Files.createTempDirectory("structr-test").toFile().getAbsolutePath());
			Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		final BoltDatabaseService s = new BoltDatabaseService();
		s.initialize();

		// create new node
		try (final Transaction tx = s.beginTx()) {

			final Node node1 = s.createNode(Collections.EMPTY_SET, Collections.EMPTY_MAP);
			final Node node2 = s.createNode(Collections.EMPTY_SET, Collections.EMPTY_MAP);

			final Relationship rel = node1.createRelationshipTo(node2, s.forName(RelationshipType.class, "TEST"));

			rel.delete(true);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		s.shutdown();
	}


	@Test
	public void testBasicTransactions() {

		try {

			Settings.DatabasePath.setValue(Files.createTempDirectory("structr-test").toFile().getAbsolutePath());
			Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		final BoltDatabaseService s = new BoltDatabaseService();
		s.initialize();

		long id = 0L;

		// create new node
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.createNode(Collections.EMPTY_SET, Collections.EMPTY_MAP);

			id = node.getId();

			tx.success();
		}

		// set property
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);
			assertNotNull(node);

			node.setProperty("name", "Test");

			tx.success();
		}

		// set property and don't commit transaction
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);
			assertNotNull(node);

			node.setProperty("name", "Fail");
		}

		// check property value
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);

			assertNotNull(node);
			assertEquals("Invalid setProperty result", "Test", node.getProperty("name"));

			tx.success();
		}

		// remove property
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);

			assertNotNull(node);

			node.removeProperty("name");

			tx.success();
		}

		// check property value
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);

			assertNotNull(node);
			assertNull("Invalid removeProperty result", node.getProperty("name"));

			node.setProperty("key1", "value1");
			node.setProperty("key2", 2);

			final Iterable<String> keys = node.getPropertyKeys();
			final List<String> list     = Iterables.toList(keys);

			assertEquals("Invalid getPropertyKeys result", 2, list.size());
			assertEquals("Invalid getPropertyKeys result", "key1", list.get(0));
			assertEquals("Invalid getPropertyKeys result", "key2", list.get(1));

			tx.success();
		}

		s.shutdown();
	}

}
