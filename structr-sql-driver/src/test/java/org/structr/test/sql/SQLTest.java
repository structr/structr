/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.test.sql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.structr.api.Transaction;
import org.structr.api.graph.Node;
import org.structr.sql.SQLDatabaseService;
import org.testng.annotations.Test;

/**
 *
 */
public class SQLTest {

	@Test
	public void test0001() {

		final SQLDatabaseService service = new SQLDatabaseService();
		int count = 0;

		service.initialize("test");

		try (final Transaction tx = service.beginTx()) {

			final Set<String> labels             = new LinkedHashSet<>();
			final Map<String, Object> properties = new LinkedHashMap<>();

			labels.add("NodeInterface");
			labels.add("AbstractNode");
			labels.add("Project");

			properties.put("type", "Project");
			properties.put("name", "teeeeeest");

			service.createNode("Project", labels, properties);

			tx.success();
		}

		try (final Transaction tx = service.beginTx()) {

			System.out.println("#################### all nodes");

			for (final Node node : service.getAllNodes()) {

				System.out.println(node);

				for (final String label : node.getLabels()) {
					System.out.println("        " + label);
				}
			}

			System.out.println("#################### nodes by label");

			for (final Node node : service.getNodesByLabel("NodeInterface")) {

				System.out.println(node);

				for (final String label : node.getLabels()) {
					System.out.println("        " + label);
				}
			}

			System.out.println("#################### nodes by type");

			for (final Node node : service.getNodesByTypeProperty("NodeInterface")) {

				System.out.println(node);

				for (final String label : node.getLabels()) {
					System.out.println("        " + label);
				}
			}

			tx.success();
		}

		service.shutdown();
	}
}
