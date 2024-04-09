/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test;

import org.structr.api.Transaction;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.memory.MemoryDatabaseService;
import org.structr.memory.index.filter.MemoryLabelFilter;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class MemoryDBTest {

	@Test
	public void testTransactions() {

		final MemoryDatabaseService service = new MemoryDatabaseService();
		Identity id1                        = null;
		Identity id2                        = null;

		try (final Transaction tx = service.beginTx()) {

			final Map<String, Object> node1 = new LinkedHashMap<>();
			final Map<String, Object> node2 = new LinkedHashMap<>();

			node1.put("type", "Test");
			node1.put("name", "Juhu");

			node2.put("type", "Foo");
			node2.put("name", "test");

			id1 = service.createNode("Test", null, node1).getId();
			id2 = service.createNode("Foo", null, node2).getId();

			tx.success();
		}

		try (final Transaction tx = service.beginTx()) {

			for (final Node node : service.getFilteredNodes(new MemoryLabelFilter<>("Test"))) {

				System.out.println(node);
			}

			tx.success();
		}

	}
}
