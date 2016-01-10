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
package org.structr.core.property;

import java.util.Objects;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.TestFour;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class ArrayPropertyTest extends StructrTest {

	public void testStringArrayProperty() {

		try {

			final Property<String[]> instance = TestFour.stringArrayProperty;
			final TestFour testEntity         = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store a string array in the test entitiy
			final String[] arr = new String[] { "one", "two", "three", "four", "five" };

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testEntity, arr);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				String[] newArr = instance.getProperty(securityContext, testEntity, true);

				assertTrue(Objects.deepEquals(arr, newArr));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	public void testSimpleSearchOnNode() {

		try {
			final PropertyMap properties    = new PropertyMap();
			final PropertyKey<String[]> key = TestFour.stringArrayProperty;

			// store a string array in the test entitiy

			final String[] arr1 = new String[] { "one" };
			final String[] arr5 = new String[] { "one", "two", "three", "four", "five" };

			properties.put(key, arr1);

			TestFour testEntity = null;
			try (final Tx tx = app.tx()) {
				testEntity = createTestNode(TestFour.class, properties);
				tx.success();
			}

			assertNotNull(testEntity);


			Result<TestFour> result = null;
			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one"}).getResult();
				assertEquals(1, result.size());
				assertEquals(result.get(0), testEntity);
			}


			try (final Tx tx = app.tx()) {
				testEntity.setProperty(key, arr5);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one"}).getResult();

				assertEquals(1, result.size());
				assertEquals(result.get(0), testEntity);
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one", "two"}).getResult();
				assertEquals(1, result.size());
				assertEquals(result.get(0), testEntity);
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one", "foo"}).getResult();
				assertEquals(0, result.size());
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one", "foo"}, false).getResult();
				assertEquals(1, result.size());
				assertEquals(result.get(0), testEntity);
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}
	/*
	public void testSimpleSearchOnRelationship() {

		try {
			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<String[]> key = TestRelationship.stringArrayProperty;

			// store a string array in the test entitiy
			final String[] arr = new String[] { "one", "two", "three", "four", "five" };

			assertNotNull(testOne);
			assertNotNull(testFour);

			final TestRelationship testEntity = (TestRelationship)createTestRelationship(testOne, testFour, RelType.IS_AT);

			assertNotNull(testEntity);

			StructrApp.getInstance(securityContext).command(TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					// set property
					testEntity.setProperty(key, arr);

					return null;
				}

			});

			// check value from database
			assertEquals(arr, testEntity.getProperty(key));

			Result<TestFour> result = StructrApp.getInstance(securityContext).command(SearchRelationshipCommand.class).execute(
				Search.andExactRelType(StructrApp.getConfiguration().getNamedRelation(TestRelationship.Relation.test_relationships.name())),
				Search.andExactProperty(securityContext, key, arr)
			);

			assertEquals(result.size(), 1);
			assertEquals(result.get(0), testEntity);

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}
	*/
}
