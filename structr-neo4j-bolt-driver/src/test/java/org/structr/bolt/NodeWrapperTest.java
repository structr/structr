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
package org.structr.bolt;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import org.junit.Assert;
import org.junit.Test;
import org.structr.api.Transaction;
import org.structr.api.config.Structr;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;

public class NodeWrapperTest {

	@Test
	public void testDeleteException() {

		final BoltDatabaseService s = new BoltDatabaseService();
		final Properties config     = new Properties();

		try {
			config.put(Structr.DATABASE_PATH, Files.createTempDirectory("structr-test").toFile().getAbsolutePath());
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		config.setProperty(Structr.DATABASE_CONNECTION_URL, Structr.TEST_DATABASE_URL);

		s.initialize(config);

		// create new node
		try (final Transaction tx = s.beginTx()) {

			final Node node1 = s.createNode();
			final Node node2 = s.createNode();

			final Relationship rel = node1.createRelationshipTo(node2, s.forName(RelationshipType.class, "TEST"));

			rel.delete();

			tx.success();
		}

		s.shutdown();
	}


	@Test
	public void testSomeMethod() {

		final BoltDatabaseService s = new BoltDatabaseService();
		final Properties config     = new Properties();

		try {
			config.put(Structr.DATABASE_PATH, Files.createTempDirectory("structr-test").toFile().getAbsolutePath());
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		config.setProperty(Structr.DATABASE_CONNECTION_URL, Structr.TEST_DATABASE_URL);

		s.initialize(config);

		long id = 0L;

		// create new node
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.createNode();

			id = node.getId();

			tx.success();
		}

		// set property
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);
			Assert.assertNotNull(node);

			node.setProperty("name", "Test");

			tx.success();
		}

		// set property and don't commit transaction
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);
			Assert.assertNotNull(node);

			node.setProperty("name", "Fail");
		}

		// check property value
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);

			Assert.assertNotNull(node);
			Assert.assertEquals("Invalid setProperty result", "Test", node.getProperty("name"));

			tx.success();
		}

		// remove property
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);

			Assert.assertNotNull(node);

			node.removeProperty("name");

			tx.success();
		}

		// check property value
		try (final Transaction tx = s.beginTx()) {

			final Node node = s.getNodeById(id);

			Assert.assertNotNull(node);
			Assert.assertNull("Invalid removeProperty result", node.getProperty("name"));

			node.setProperty("key1", "value1");
			node.setProperty("key2", 2);

			final Iterable<String> keys = node.getPropertyKeys();
			final List<String> list     = Iterables.toList(keys);

			Assert.assertEquals("Invalid getPropertyKeys result", 2, list.size());
			Assert.assertEquals("Invalid getPropertyKeys result", "key1", list.get(0));
			Assert.assertEquals("Invalid getPropertyKeys result", "key2", list.get(1));

			tx.success();
		}

		s.shutdown();
	}

}
