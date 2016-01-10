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
package org.structr.common;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;

/**
 *
 *
 */
public class ConcurrentTests extends StructrTest {

	public void testConcurrentValidation() {

		final int count = 100;

		try (final Tx tx = app.tx()) {

			app.create(SchemaNode.class,
				new NodeAttribute(SchemaNode.name, "Item"),
				new NodeAttribute(new StringProperty("_name"), "+String!")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final Class type = StructrApp.getConfiguration().getNodeEntityClass("Item");
		assertNotNull(type);


		final PropertyKey name = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, "name", false);
		assertNotNull(name);

		final Runnable tester = new Runnable() {

			@Override
			public void run() {

				for (int i=0; i<count; i++) {

					// testing must be done in an isolated transaction
					try (final Tx tx = app.tx()) {

						app.create(type, "Item" + i);

						tx.success();

					} catch (FrameworkException fex) {}

				}
			}
		};

		// submit three test instances
		final ExecutorService executor = Executors.newCachedThreadPool();
		final Future f1                = executor.submit(tester);
		final Future f2                = executor.submit(tester);
		final Future f3                = executor.submit(tester);

		try {
			f1.get();
			f2.get();
			f3.get();

		} catch (Throwable ex) {}


		List<GraphObject> result = null;

		try (final Tx tx = app.tx()) {

			result = app.nodeQuery(type).getAsList();


			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify that only count entities have been created.
		assertEquals("Invalid concurrent validation result", count, result.size());
	}

	public void testConcurrentValidationOnDynamicProperty() {

		final int count = 100;

		try (final Tx tx = app.tx()) {

			app.create(SchemaNode.class,
				new NodeAttribute(SchemaNode.name, "Item"),
				new NodeAttribute(new StringProperty("_testXYZ"), "+String!")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final Class type = StructrApp.getConfiguration().getNodeEntityClass("Item");
		assertNotNull(type);


		final PropertyKey testXYZ = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, "testXYZ", false);
		assertNotNull(testXYZ);

		final Runnable tester = new Runnable() {

			@Override
			public void run() {

				for (int i=0; i<count; i++) {

					// testing must be done in an isolated transaction
					try (final Tx tx = app.tx()) {

						app.create(type, new NodeAttribute(testXYZ, "Item" + i));

						tx.success();

					} catch (FrameworkException fex) {}

				}
			}
		};

		// submit three test instances
		final ExecutorService executor = Executors.newCachedThreadPool();
		final Future f1                = executor.submit(tester);
		final Future f2                = executor.submit(tester);
		final Future f3                = executor.submit(tester);

		try {
			f1.get();
			f2.get();
			f3.get();

		} catch (Throwable ex) {}


		List<GraphObject> result = null;

		try (final Tx tx = app.tx()) {

			result = app.nodeQuery(type).getAsList();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify that only count entities have been created.
		assertEquals("Invalid concurrent validation result", count, result.size());
	}
}
