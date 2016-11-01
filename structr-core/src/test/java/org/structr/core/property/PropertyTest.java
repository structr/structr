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

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Label;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.OneFourOneToOne;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.TestEnum;
import org.structr.core.entity.TestFive;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.entity.TestThree;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.schema.ConfigurationProvider;

/**
 *
 *
 */
public class PropertyTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(PropertyTest.class);

	@Test
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

	@Test
	public void testSimpleStringArraySearchOnNode() {

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

	// ----- boolean property tests -----
	@Test
	public void testBooleanProperty() {

		try {


			final Property<Boolean> key = TestFour.booleanProperty;
			final TestFour testEntity        = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store boolean in the test entitiy
			final Boolean value = Boolean.TRUE;

			try (final Tx tx = app.tx()) {

				testEntity.setProperty(key, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, testEntity.getProperty(key));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testSimpleBooleanPropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Boolean> key = TestFour.booleanProperty;

			properties.put(key, true);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Boolean)true, (Boolean)testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, true).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	@Test
	public void testSimpleBooleanPropertySearchOnRelationship() {

		try {

			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<Boolean> key = OneFourOneToOne.booleanProperty;

			assertNotNull(testOne);
			assertNotNull(testFour);

			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				testEntity.setProperty(key, true);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Boolean)true, (Boolean)testEntity.getProperty(key));

				Result<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, true).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	// ----- collection property tests -----
	@Test
	public void testOneToManyOnCollectionProperty() throws Exception {

		TestOne testOne                   = null;
		List<TestSix> testSixs            = null;
		List<TestSix> testSixs2           = null;
		int index                         = 0;

		List<Integer> index1              = new LinkedList();
		List<Integer> index2              = new LinkedList();

		try (final Tx tx = app.tx()) {

			testOne        = createTestNode(TestOne.class);
			testSixs       = createTestNodes(TestSix.class, 20);

			for (final TestSix testSix : testSixs) {
				int i = index++;
				testSix.setProperty(TestSix.index, i);
				System.out.println(i + " ");
				index1.add(i);
			}

			testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);

			tx.success();

		}

		try (final Tx tx = app.tx()) {

			testSixs2 = testOne.getProperty(TestOne.manyToManyTestSixs);

			for (final TestSix testSix : testSixs2) {
				int i = testSix.getProperty(TestSix.index);
				System.out.println(i + " ");
				index2.add(i);
			}

			assertEquals(index1, index2);

			tx.success();

		}


	}

	@Test
	public void testManyToManyOnCollectionProperty() throws Exception {

		try {

			Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
			TestSix testSix1  = null;
			TestSix testSix2  = null;
			TestOne testOne1  = null;
			TestOne testOne2  = null;

			testSix1 = createTestNode(TestSix.class);
			testSix2 = createTestNode(TestSix.class);

			testOne1 = createTestNode(TestOne.class);
			testOne2 = createTestNode(TestOne.class);

			assertNotNull(testSix1);
			assertNotNull(testSix2);
			assertNotNull(testOne1);
			assertNotNull(testOne2);

			// set two TestOne entities on both TestSix entities
			List<TestOne> twoTestOnesList = new LinkedList<>();
			twoTestOnesList.add(testOne1);
			twoTestOnesList.add(testOne2);

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testSix1, twoTestOnesList);
				instance.setProperty(securityContext, testSix2, twoTestOnesList);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestOne> testOnesFromTestSix1 = instance.getProperty(securityContext, testSix1, true);
				List<TestOne> testOnesFromTestSix2 = instance.getProperty(securityContext, testSix2, true);

				assertNotNull(testOnesFromTestSix1);
				assertNotNull(testOnesFromTestSix2);

				// both entities should have the two related nodes
				assertEquals(2, testOnesFromTestSix1.size());
				assertEquals(2, testOnesFromTestSix2.size());
			}

			// create new list with one TestOne entity
			List<TestOne> oneTestOneList = new LinkedList<>();
			oneTestOneList.add(testOne1);

			try (final Tx tx = app.tx()) {

				// set list with one TestOne node as related nodes
				instance.setProperty(securityContext, testSix1, oneTestOneList);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestOne> oneTestOnesFromTestSix1 = instance.getProperty(securityContext, testSix1, true);

				// entity should have exactly one related node
				assertNotNull(oneTestOnesFromTestSix1);
				assertEquals(1, oneTestOnesFromTestSix1.size());

				assertEquals(oneTestOnesFromTestSix1.get(0).getUuid(), testOne1.getUuid());
			}

		} catch (FrameworkException fex) {

		}

	}

	/**
	 * Test of typeName method, of class CollectionProperty.
	 */
	@Test
	public void testTypeNameOnCollectionProperty() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		String expResult = "collection";
		String result = instance.typeName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of databaseConverter method, of class CollectionProperty.
	 */
	@Test
	public void testDatabaseConverterOnCollectionProperty() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		PropertyConverter expResult = null;
		PropertyConverter result = instance.databaseConverter(securityContext, null);
		assertEquals(expResult, result);
	}

	/**
	 * Test of inputConverter method, of class CollectionProperty.
	 */
	@Test
	public void testInputConverterOnCollectionProperty() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		PropertyConverter result = instance.inputConverter(securityContext);

		assertTrue(result != null);
	}

	/**
	 * Test of relatedType method, of class CollectionProperty.
	 */
	@Test
	public void testRelatedTypeOnCollectionProperty() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		Class expResult = TestOne.class;
		Class result = instance.relatedType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of isCollection method, of class CollectionProperty.
	 */
	@Test
	public void testIsCollectionOnCollectionProperty() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		boolean expResult = true;
		boolean result = instance.isCollection();
		assertEquals(expResult, result);
	}

	// ----- date property tests -----
	@Test
	public void testDateProperty() {

		try {

			final Property<Date> instance = TestFour.dateProperty;
			final TestFour testEntity     = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store Date in the test entitiy
			final Date value = new Date(123456789L);

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testEntity, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testSimpleDatePropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Date> key   = TestFour.dateProperty;
			final Date value              = new Date(123456789L);

			properties.put(key,value);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, value).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	@Test
	public void testDatePropertyRangeSearchOnNode() {

		try {

			final PropertyMap properties = new PropertyMap();
			final PropertyKey<Date> key  = TestFour.dateProperty;
			final Date minValue          = new Date(1234567880L);
			final Date value             = new Date(1234567890L);
			final Date maxValue          = new Date(1234567900L);
			final Date maxMaxValue       = new Date(1234567910L);

			properties.put(key, value);

			final TestFour testEntity = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, minValue, maxValue).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, maxValue, maxMaxValue).getResult();

				assertEquals(0, result.size());

				tx.success();
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	// ----- double property tests -----
	@Test
	public void testDoubleProperty() {

		try {

			final Property<Double> instance = TestFour.doubleProperty;
			final TestFour testEntity        = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store double in the test entitiy
			final Double value = 3.141592653589793238;


			try (final Tx tx = app.tx()) {
				instance.setProperty(securityContext, testEntity, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	/* temporarily disabled because Cypher cannot handle NaN yet..
	public void testNaN() {

		try {

			final Property<Double> instance = TestFour.doubleProperty;
			final TestFour testEntity        = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store NaN double in the test entitiy
			final Double value = Double.NaN;


			try (final Tx tx = app.tx()) {
				instance.setProperty(securityContext, testEntity, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}
	*/

	@Test
	public void testSimpleDoublePropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Double> key = TestFour.doubleProperty;

			properties.put(key, 3.141592653589793238);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(3.141592653589793238, testEntity.getProperty(key), 0.0);

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, 3.141592653589793238).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	/* temporarily disabled because Cypher cannot handle NaN yet..
	public void testNaNSearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Double> key = TestFour.doubleProperty;

			properties.put(key, Double.NaN);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(Double.NaN, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, Double.NaN).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}
	*/

	@Test
	public void testNegativeInfinityDoublePropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Double> key = TestFour.doubleProperty;

			properties.put(key, Double.NEGATIVE_INFINITY);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(Double.NEGATIVE_INFINITY, testEntity.getProperty(key), 0.0);

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, Double.NEGATIVE_INFINITY).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	@Test
	public void testPositiveInfinityDoublePropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Double> key = TestFour.doubleProperty;

			properties.put(key, Double.POSITIVE_INFINITY);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(Double.POSITIVE_INFINITY, testEntity.getProperty(key), 0.0);

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, Double.POSITIVE_INFINITY).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	@Test
	public void testSimpleDoublePropertySearchOnRelationship() {

		try {

			final TestOne testOne      = createTestNode(TestOne.class);
			final TestFour testFour    = createTestNode(TestFour.class);
			final Property<Double> key = OneFourOneToOne.doubleProperty;

			assertNotNull(testOne);
			assertNotNull(testFour);

			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				testEntity.setProperty(key, 3.141592653589793238);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(3.141592653589793238, testEntity.getProperty(key), 0.0);

				Result<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, 3.141592653589793238).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testDoublePropertyRangeSearchOnNode() {

		try {

			final PropertyMap properties = new PropertyMap();
			final Property<Double> key   = OneFourOneToOne.doubleProperty;

			properties.put(key, 123456.2);

			final TestFour testEntity = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Double)123456.2, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123455.1, 123457.6).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123456.3, 123456.7).getResult();

				assertEquals(0, result.size());

				tx.success();
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	// ----- entity property tests -----
	@Test
	public void testOneToOneOnEntityProperty() throws Exception {

		try {

			final TestSix a   = createTestNode(TestSix.class);
			final TestSix c   = createTestNode(TestSix.class);
			final TestThree b = createTestNode(TestThree.class);
			final TestThree d = createTestNode(TestThree.class);

			try (final Tx tx = app.tx()) {

				a.setProperty(AbstractNode.name, "a");
				c.setProperty(AbstractNode.name, "c");
				b.setProperty(AbstractNode.name, "b");
				d.setProperty(AbstractNode.name, "d");
				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);

				fail("Unable to create test nodes");
			}

			assertNotNull(a);
			assertNotNull(c);
			assertNotNull(b);
			assertNotNull(d);

			/**
			 * We test the following here:
			 * A -> B
			 * C -> D
			 *
			 * then connect A -> D, so C and B should not
			 * be related any more
			 */

			// create two connections
			try (final Tx tx = app.tx()) {

				a.setProperty(TestSix.oneToOneTestThree, b);
				c.setProperty(TestSix.oneToOneTestThree, d);
				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);

				fail("Unable to link test nodes");
			}

			try (final Tx tx = app.tx()) {

				// verify connections
				TestThree verifyB = a.getProperty(TestSix.oneToOneTestThree);
				TestThree verifyD = c.getProperty(TestSix.oneToOneTestThree);

				assertTrue(verifyB != null && verifyB.equals(b));
				assertTrue(verifyD != null && verifyD.equals(d));
			}

			try (final Tx tx = app.tx()) {

				a.setProperty(TestSix.oneToOneTestThree, d);
				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);

				fail("Unable to link test nodes");
			}

			try (final Tx tx = app.tx()) {

				// verify connection
				TestThree verifyD2 = a.getProperty(TestSix.oneToOneTestThree);
				assertTrue(verifyD2 != null && verifyD2.equals(d));

				// testSix2 should not have a testThree associated
				TestThree vrfy4 = c.getProperty(TestSix.oneToOneTestThree);
				assertNull(vrfy4);
			}

		} catch (FrameworkException fex) {

		}
	}

	@Test
	public void testOneToManyOnEntityProperty() throws Exception {

		try {

			final TestSix testSix1     = createTestNode(TestSix.class);
			final TestSix testSix2     = createTestNode(TestSix.class);
			final TestThree testThree1 = createTestNode(TestThree.class);
			final TestThree testThree2 = createTestNode(TestThree.class);

			try (final Tx tx = app.tx()) {

				testSix1.setProperty(AbstractNode.name, "a");
				testSix2.setProperty(AbstractNode.name, "c");
				testThree1.setProperty(AbstractNode.name, "b");
				testThree2.setProperty(AbstractNode.name, "d");
				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);

				fail("Unable to create test nodes");
			}

			assertNotNull(testSix1);
			assertNotNull(testThree1);
			assertNotNull(testSix2);
			assertNotNull(testThree2);

			/**
			 * We test the following here:
			 * A -> B
			 * C -> D
			 *
			 * then connect A -> D, so C and B should not
			 * be related any more
			 */
			try (final Tx tx = app.tx()) {

				testSix1.setProperty(TestSix.oneToManyTestThrees, toList(testThree1));
				testSix2.setProperty(TestSix.oneToManyTestThrees, toList(testThree2));
				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);

				fail("Unable to link test nodes");
			}

			try (final Tx tx = app.tx()) {

				// verify connections
				List<TestThree> verifyB = testSix1.getProperty(TestSix.oneToManyTestThrees);
				List<TestThree> verifyD = testSix2.getProperty(TestSix.oneToManyTestThrees);

				assertTrue(verifyB != null && verifyB.get(0).equals(testThree1));
				assertTrue(verifyD != null && verifyD.get(0).equals(testThree2));
			}

			try (final Tx tx = app.tx()) {

				testSix1.setProperty(TestSix.oneToManyTestThrees, toList(testThree2));
				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);

				fail("Unable to link test nodes");
			}

			try (final Tx tx = app.tx()) {

				// verify connection
				List<TestThree> verifyD2 = testSix1.getProperty(TestSix.oneToManyTestThrees);
				assertEquals(1, verifyD2.size());
				assertEquals(testThree2, verifyD2.get(0));

				// testSix2 should not have a testThree associated
				List<TestThree> vrfy4 = testSix2.getProperty(TestSix.oneToManyTestThrees);
				assertEquals(0, vrfy4.size());
			}

		} catch (FrameworkException fex) {

		}
	}

	/**
	 * Test of typeName method, of class EntityProperty.
	 */
	@Test
	public void testTypeNameOnEntityProperty() {

		String expResult = "object";
		String result = TestSix.oneToOneTestThree.typeName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of databaseConverter method, of class EntityProperty.
	 */
	@Test
	public void testDatabaseConverterOnEntityProperty() {

		PropertyConverter expResult = null;
		PropertyConverter result = TestSix.oneToOneTestThree.databaseConverter(securityContext, null);
		assertEquals(expResult, result);
	}

	/**
	 * Test of inputConverter method, of class EntityProperty.
	 */
	@Test
	public void testInputConverterOnEntityProperty() {

		PropertyConverter result = TestSix.oneToOneTestThree.inputConverter(securityContext);

		assertTrue(result != null);
	}

	/**
	 * Test of relatedType method, of class EntityProperty.
	 */
	@Test
	public void testRelatedTypeOnEntityProperty() {

		Class expResult = TestThree.class;
		Class result = TestSix.oneToOneTestThree.relatedType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of isCollection method, of class EntityProperty.
	 */
	@Test
	public void testIsCollectionOnEntityProperty() {

		boolean expResult = false;
		boolean result = TestSix.oneToOneTestThree.isCollection();
		assertEquals(expResult, result);
	}

	// ----- enum property tests -----
	@Test
	public void testSimpleEnumProperty() {

		try {

			final PropertyMap properties    = new PropertyMap();

			properties.put(TestFour.enumProperty, TestEnum.Status1);

			final TestFour testEntity        = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(TestEnum.Status1, testEntity.getProperty(TestFour.enumProperty));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testSimpleEnumPropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<TestEnum> key = TestFour.enumProperty;

			properties.put(key, TestEnum.Status1);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(TestEnum.Status1, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, TestEnum.Status1).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	@Test
	public void testSimpleEnumPropertySearchOnRelationship() {

		try {

			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<TestEnum> key = OneFourOneToOne.enumProperty;

			assertNotNull(testOne);
			assertNotNull(testFour);

			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				testEntity.setProperty(key, TestEnum.Status1);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(TestEnum.Status1, testEntity.getProperty(key));

				Result<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, TestEnum.Status1).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	// ----- function property tests -----
	@Test
	public void testEscapingInFunctionProperty() {

		// create test node with offending quote

		try (final Tx tx = app.tx()) {

			final PropertyMap properties = new PropertyMap();

			properties.put(AbstractNode.name, "Test");
			properties.put(new StringProperty("_functionTest"), "Function({ // \"})");

			app.create(SchemaNode.class, properties);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	// ----- integer property tests -----
	@Test
	public void testIntProperty() {


		try {

			final Property<Integer> instance = TestFour.integerProperty;
			final TestFour testEntity        = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store integer in the test entitiy
			final Integer value = 2345;

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testEntity, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testSimpleIntPropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Integer> key = TestFour.integerProperty;

			properties.put(key, 2345);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Integer)2345, (Integer)testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, 2345).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	@Test
	public void testSimpleIntPropertySearchOnRelationship() {

		try {

			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<Integer> key = OneFourOneToOne.integerProperty;

			assertNotNull(testOne);
			assertNotNull(testFour);

			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				testEntity.setProperty(key, 2345);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Integer)2345, (Integer)testEntity.getProperty(key));

				Result<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, 2345).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testIntPropertyRangeSearchOnNode() {

		try {

			final PropertyMap properties = new PropertyMap();
			final Property<Integer> key  = OneFourOneToOne.integerProperty;

			properties.put(key, 123456);

			final TestFour testEntity = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Integer)123456, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123455, 123457).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123457, 123458).getResult();

				assertEquals(0, result.size());

				tx.success();
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	// ----- long property tests -----
	@Test
	public void testLongProperty() {

		try {

			final Property<Long> instance = TestFour.longProperty;
			final TestFour testEntity     = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store long in the test entitiy
			final Long value = 2857312362L;

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testEntity, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testSimpleLongPropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Long> key = TestFour.longProperty;

			properties.put(key, 2857312362L);

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Long)2857312362L, (Long)testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, 2857312362L).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	@Test
	public void testSimpleLongPropertySearchOnRelationship() {

		try {

			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<Long> key = OneFourOneToOne.longProperty;

			assertNotNull(testOne);
			assertNotNull(testFour);

			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				testEntity.setProperty(key, 2857312362L);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Long)2857312362L, (Long)testEntity.getProperty(key));

				Result<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, 2857312362L).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testLongPropertyRangeSearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Long> key = TestFour.longProperty;

			properties.put(key, 123456L);

			final TestFour testEntity = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals((Long)123456L, testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123455L, 123457L).getResult();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123457L, 123458L).getResult();

				assertEquals(0, result.size());

				tx.success();
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	// ----- string property tests -----
	@Test
	public void testStringProperty() {

		try {

			final Property<String> instance = TestFour.stringProperty;
			final TestFour testEntity        = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store string in the test entitiy
			final String value = "This is a test!";

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testEntity, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	@Test
	public void testSimpleStringPropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<String> key = TestFour.stringProperty;

			properties.put(key, "test");

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("test", testEntity.getProperty(key));

				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, "test").getResult();

				assertEquals(result.size(), 1);
				assertEquals(result.get(0), testEntity);
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}

	}

	@Test
	public void testSimpleStringPropertySearchOnRelationship() {

		try {

			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<String> key   = OneFourOneToOne.stringProperty;

			assertNotNull(testOne);
			assertNotNull(testFour);

			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				testEntity.setProperty(key, "test");
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("test", testEntity.getProperty(key));

				Result<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, "test").getResult();

				assertEquals(result.size(), 1);
				assertEquals(result.get(0), testEntity);
			}

		} catch (FrameworkException fex) {

			fail("Unable to store array");
		}
	}

	// ----- type property tests -----
	@Test
	public void testModifyType() {

		final DatabaseService db      = StructrApp.getInstance().getDatabaseService();
		final Set<Label> labelsBefore = new LinkedHashSet<>();
		final Set<Label> labelsAfter  = new LinkedHashSet<>();
		String id                     = null;

		labelsBefore.add(db.forName(Label.class, AccessControllable.class.getSimpleName()));
		labelsBefore.add(db.forName(Label.class, TestFour.class.getSimpleName()));

		labelsAfter.add(db.forName(Label.class, AccessControllable.class.getSimpleName()));
		labelsAfter.add(db.forName(Label.class, TestFive.class.getSimpleName()));

		// create a new node, check labels, modify type, check labels again

		try (final Tx tx = app.tx()) {

			// create entity of type TestFour
			final TestFour testEntity = createTestNode(TestFour.class);

			// check if node exists
			assertNotNull(testEntity);

			// check labels before type change
			assertTrue(Iterables.toSet(testEntity.getNode().getLabels()).containsAll(labelsBefore));

			// save ID for later use
			id = testEntity.getUuid();

			// change type to TestFive
			// system properties have to be unlocked now, admin rights are not enough anymore
			testEntity.unlockSystemPropertiesOnce();
			testEntity.setProperty(GraphObject.type, TestFive.class.getSimpleName());

			// commit transaction
			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}


		try (final Tx tx = app.tx()) {

			final TestFive testEntity = app.get(TestFive.class, id);

			assertNotNull(testEntity);

			// check labels after type change
			assertTrue(Iterables.toSet(testEntity.getNode().getLabels()).containsAll(labelsAfter));

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}

	// ----- function property tests -----
	@Test
	public void testFunctionPropertyIndexing() {

		/* This test creates a new type "Test" and links it to
		 * the built-in type "Group". It then creates a function
		 * property that references the name of the related group
		 * and assumes that a test entity is found by its related
		 * group name.
		 */

		// schema setup
		try (final Tx tx = app.tx()) {

			final SchemaNode group = app.nodeQuery(SchemaNode.class).andName("Group").getFirst();
			final SchemaNode test  = app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "Test"),
				new NodeAttribute<>(new StringProperty("_testFunction"), "Function(this.group.name)")
			);

			assertNotNull("Invalid schema setup result", group);
			assertNotNull("Invalid schema setup result", test);

			app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, test),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, group),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "*"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "tests"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "group"),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "group")
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

		// entity setup
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider config = StructrApp.getConfiguration();

			final Class testType = config.getNodeEntityClass("Test");

			// create test type without link to group!
			app.create(testType);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

		// entity setup
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider config = StructrApp.getConfiguration();

			final Class testType    = config.getNodeEntityClass("Test");
			final Class groupType   = config.getNodeEntityClass("Group");
			final GraphObject group = app.create(groupType, "testgroup");
			final GraphObject test  = app.nodeQuery(testType).getFirst();

			// create Test with link to Group
			test.setProperty(config.getPropertyKeyForJSONName(testType, "group"), group);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

		// test
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Class testType  = config.getNodeEntityClass("Test");
			final PropertyKey key = config.getPropertyKeyForJSONName(testType, "testFunction");

			// fetch test node
			final GraphObject testNode = app.nodeQuery(testType).getFirst();
			final GraphObject result   = app.nodeQuery(testType).and(key, "testgroup").getFirst();

			// test indexing
			assertEquals("Invalid FunctionProperty indexing result", testNode, result);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

	}
}































