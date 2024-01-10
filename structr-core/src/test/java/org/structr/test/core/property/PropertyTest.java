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
package org.structr.test.core.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.function.CryptFunction;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.structr.test.core.entity.*;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.AssertJUnit.*;

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


			List<TestFour> result = null;
			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one"}).getAsList();
				assertEquals(1, result.size());
				assertEquals(result.get(0), testEntity);
			}


			try (final Tx tx = app.tx()) {
				testEntity.setProperty(key, arr5);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one"}).getAsList();

				assertEquals(1, result.size());
				assertEquals(result.get(0), testEntity);
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one", "two"}).getAsList();
				assertEquals(1, result.size());
				assertEquals(result.get(0), testEntity);
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one", "foo"}).getAsList();
				assertEquals(0, result.size());
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestFour.class).and(key, new String[]{"one", "foo"}, false).getAsList();
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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, true).getAsList();

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

				List<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, true).getAsList();

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
		Iterable<TestSix> testSixs        = null;
		Iterable<TestSix> testSixs2       = null;
		int index                         = 0;

		List<Integer> index1              = new LinkedList<>();
		List<Integer> index2              = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			testOne        = createTestNode(TestOne.class);
			testSixs       = createTestNodes(TestSix.class, 20);

			for (final TestSix testSix : testSixs) {
				int i = index++;
				testSix.setProperty(TestSix.index, i);
				System.out.print(i + ", ");
				index1.add(i);
			}

			System.out.println();

			testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);

			tx.success();

		}

		try (final Tx tx = app.tx()) {

			testSixs2 = testOne.getProperty(TestOne.manyToManyTestSixs);

			for (final TestSix testSix : testSixs2) {
				int i = testSix.getProperty(TestSix.index);
				index2.add(i);
			}

			System.out.println();

			assertEquals(index1, index2);

			tx.success();

		}

	}

	@Test
	public void testManyToManyOnCollectionProperty() throws Exception {

		try {

			Property<Iterable<TestOne>> instance = TestSix.manyToManyTestOnes;
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

				List<TestOne> testOnesFromTestSix1 = Iterables.toList(instance.getProperty(securityContext, testSix1, true));
				List<TestOne> testOnesFromTestSix2 = Iterables.toList(instance.getProperty(securityContext, testSix2, true));

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

				List<TestOne> oneTestOnesFromTestSix1 = Iterables.toList(instance.getProperty(securityContext, testSix1, true));

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

		Property<Iterable<TestOne>> instance = TestSix.manyToManyTestOnes;
		String expResult = "collection";
		String result = instance.typeName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of databaseConverter method, of class CollectionProperty.
	 */
	@Test
	public void testDatabaseConverterOnCollectionProperty() {

		Property<Iterable<TestOne>> instance = TestSix.manyToManyTestOnes;
		PropertyConverter expResult = null;
		PropertyConverter result = instance.databaseConverter(securityContext, null);
		assertEquals(expResult, result);
	}

	/**
	 * Test of inputConverter method, of class CollectionProperty.
	 */
	@Test
	public void testInputConverterOnCollectionProperty() {

		Property<Iterable<TestOne>> instance = TestSix.manyToManyTestOnes;
		PropertyConverter result = instance.inputConverter(securityContext);

		assertTrue(result != null);
	}

	/**
	 * Test of relatedType method, of class CollectionProperty.
	 */
	@Test
	public void testRelatedTypeOnCollectionProperty() {

		Property<Iterable<TestOne>> instance = TestSix.manyToManyTestOnes;
		Class expResult = TestOne.class;
		Class result = instance.relatedType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of isCollection method, of class CollectionProperty.
	 */
	@Test
	public void testIsCollectionOnCollectionProperty() {

		Property<Iterable<TestOne>> instance = TestSix.manyToManyTestOnes;
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
	public void testDateArrayProperty() {

		try {

			final Property<Date[]> instance = TestFour.dateArrayProperty;
			final TestFour testEntity         = createTestNode(TestFour.class);

			assertNotNull(testEntity);

			// store a date array in the test entitiy
			final Date[] arr = new Date[] { new Date(123456789L), new Date(234567891L), new Date(345678912L) };

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testEntity, arr);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Date[] newArr = instance.getProperty(securityContext, testEntity, true);

				assertTrue(Objects.deepEquals(arr, newArr));
			}

		} catch (FrameworkException fex) {

			fail("Unable to store date array");
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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, value).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, minValue, maxValue).getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, maxValue, maxMaxValue).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, 3.141592653589793238).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, Double.NaN).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, Double.NEGATIVE_INFINITY).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, Double.POSITIVE_INFINITY).getAsList();

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

				List<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, 3.141592653589793238).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123455.1, 123457.6).getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123456.3, 123456.7).getAsList();

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
				List<TestThree> verifyB = Iterables.toList(testSix1.getProperty(TestSix.oneToManyTestThrees));
				List<TestThree> verifyD = Iterables.toList(testSix2.getProperty(TestSix.oneToManyTestThrees));

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
				List<TestThree> verifyD2 = Iterables.toList(testSix1.getProperty(TestSix.oneToManyTestThrees));
				assertEquals(1, verifyD2.size());
				assertEquals(testThree2, verifyD2.get(0));

				// testSix2 should not have a testThree associated
				List<TestThree> vrfy4 = Iterables.toList(testSix2.getProperty(TestSix.oneToManyTestThrees));
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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, TestEnum.Status1).getAsList();

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

				List<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, TestEnum.Status1).getAsList();

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

			logger.error(fex.getMessage());
			fex.printStackTrace();
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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, 2345).getAsList();

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

				List<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, 2345).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123455, 123457).getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123457, 123458).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, 2857312362L).getAsList();

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

				List<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, 2857312362L).getAsList();

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

				List<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123455L, 123457L).getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestFour> result = app.nodeQuery(TestFour.class).andRange(key, 123457L, 123458L).getAsList();

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

			fail("Error in test");
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

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, "test").getAsList();

				assertEquals(result.size(), 1);
				assertEquals(result.get(0), testEntity);
			}

		} catch (FrameworkException fex) {

			fail("Error in test");
		}

	}

	@Test
	public void testMultilineStringPropertySearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<String> key = TestFour.stringProperty;

			properties.put(key, "test\nabc");

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("test\nabc", testEntity.getProperty(key));

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, "test\nabc").getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Error in test");
		}

	}

	@Test
	public void testMultilineStringPropertyExactSubstringSearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<String> key = TestFour.stringProperty;

			properties.put(key, "test\nabc");

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("test\nabc", testEntity.getProperty(key));

				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, "test").getAsList();

				assertEquals(0, result.size());
			}

		} catch (FrameworkException fex) {

			fail("Error in test");
		}

	}

	@Test
	public void testMultilineStringPropertyInexactSubstringSearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<String> key = TestFour.stringProperty;

			properties.put(key, "xyz\ntest\nabc");

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("xyz\ntest\nabc", testEntity.getProperty(key));

				// inexact searc
				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, "test", false).getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Error in test");
		}

	}

	@Test
	public void testMultilineWithLinefeedStringPropertyInexactSubstringSearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<String> key = TestFour.stringProperty;

			properties.put(key, "xyz\r\ntest\r\nabc");

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("xyz\r\ntest\r\nabc", testEntity.getProperty(key));

				// inexact searc
				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, "test", false).getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Error in test");
		}

	}

	@Test
	public void testCaseInsensitiveMultilineStringPropertyInexactSubstringSearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<String> key = TestFour.stringProperty;

			properties.put(key, "{\n return fooBar;\n}");

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("{\n return fooBar;\n}", testEntity.getProperty(key));

				// inexact search
				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, "foo", false).getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Error in test");
		}

	}

	@Test
	public void testCaseInsensitiveMultilineWithLinefeedStringPropertyInexactSubstringSearchOnNode() {

		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<String> key = TestFour.stringProperty;

			properties.put(key, "xyz\r\nTeSt\r\nabc");

			final TestFour testEntity     = createTestNode(TestFour.class, properties);

			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("xyz\r\nTeSt\r\nabc", testEntity.getProperty(key));

				// inexact searc
				List<TestFour> result = app.nodeQuery(TestFour.class).and(key, "test", false).getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Error in test");
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

				List<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, "test").getAsList();

				assertEquals(1, result.size());
				assertEquals(testEntity, result.get(0));
			}

		} catch (FrameworkException fex) {

			fail("Error in test");
		}
	}

	// ----- typeProperty property tests -----
	@Test
	public void testModifyType() {

		final DatabaseService db      = StructrApp.getInstance().getDatabaseService();
		final Set<String> labelsBefore = new LinkedHashSet<>();
		final Set<String> labelsAfter  = new LinkedHashSet<>();
		String id                     = null;

		labelsBefore.add(AccessControllable.class.getSimpleName());
		labelsBefore.add(TestFour.class.getSimpleName());

		labelsAfter.add(AccessControllable.class.getSimpleName());
		labelsAfter.add(TestFive.class.getSimpleName());

		// create a new node, check labels, modify typeProperty, check labels again

		try (final Tx tx = app.tx()) {

			// create entity of typeProperty TestFour
			final TestFour testEntity = createTestNode(TestFour.class);

			// check if node exists
			assertNotNull(testEntity);

			// check labels before typeProperty change
			assertTrue(Iterables.toSet(testEntity.getNode().getLabels()).containsAll(labelsBefore));

			// save ID for later use
			id = testEntity.getUuid();

			// change typeProperty to TestFive
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

			// check labels after typeProperty change
			assertTrue(Iterables.toSet(testEntity.getNode().getLabels()).containsAll(labelsAfter));

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}

	// ----- function property tests -----

	/** This test creates a new typeProperty "Test" and links it to
 the built-in typeProperty "Group". It then creates a function
 property that references the nameProperty of the related group
 and assumes that a test entity is found by its related
 group nameProperty.
	 */
	@Test
	public void testFunctionPropertyIndexing() {

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

			// create test typeProperty without link to group!
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
			test.setProperty(StructrApp.key(testType, "group"), group);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

		// test
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Class testType  = config.getNodeEntityClass("Test");
			final PropertyKey key = StructrApp.key(testType, "testFunction");

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
	// ----- notion property tests -----

	/**
	 * This test creates a new typeProperty "Test" with different Notion properties.
	 */
	@Test
	public void testNotionPropertyOwner() {

		// schema setup
		try (final Tx tx = app.tx()) {

			final SchemaNode test  = app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "Test")
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "ownerName"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "owner, name"),
					new NodeAttribute<>(SchemaProperty.schemaNode, test)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "ownerEmail"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "owner, eMail"),
					new NodeAttribute<>(SchemaProperty.schemaNode, test)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "ownerPrincipalEmail"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "owner, Principal.eMail"),
					new NodeAttribute<>(SchemaProperty.schemaNode, test)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "ownerName"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "owner,name"),
					new NodeAttribute<>(SchemaProperty.schemaNode, test)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "ownerEmail"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "owner,  eMail"),
					new NodeAttribute<>(SchemaProperty.schemaNode, test)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "ownerPrincipalEmail"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "owner , Principal.eMail"),
					new NodeAttribute<>(SchemaProperty.schemaNode, test)
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}

	/**
	 * This test creates a new typeProperty "Message" with different Notion properties.
	 */
	@Test
	public void testNotionPropertyMessageId() {

		SchemaNode message = null;

		// schema setup
		try (final Tx tx = app.tx()) {

			message  = app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "Message")
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "messageId"),
					new NodeAttribute<>(SchemaProperty.propertyType, "String"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, message),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, message),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "*"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "children"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "parent"),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "HAS_PARENT")
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageId1"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, _messageId"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageId2"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, messageId"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageId3"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, _messageIdProperty"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageId4"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, messageIdProperty"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageId5"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, Message._messageId"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageId6"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, Message.messageId"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageId7"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, Message._messageIdProperty"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageId8"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, Message.messageIdProperty"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}


	/**
	 * This test creates a new typeProperty "Message" with different Notion properties.
	 */
	@Test
	public void testNotionPropertyMessageName() {

		// schema setup
		try (final Tx tx = app.tx()) {

			final SchemaNode message  = app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "Message")
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "messageId"),
					new NodeAttribute<>(SchemaProperty.propertyType, "String"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, message),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, message),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "*"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "children"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "parent"),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "HAS_PARENT")
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageName1"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, name"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}

	/**
	 * This test creates a new typeProperty "Message" with true|false parameter in Notion format.
	 */
	@Test
	public void testNotionPropertyMessageTrueFalse() {

		// schema setup
		try (final Tx tx = app.tx()) {

			final SchemaNode message  = app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "Message")
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "messageId"),
					new NodeAttribute<>(SchemaProperty.propertyType, "String"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, message),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, message),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "*"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "children"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "parent"),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "HAS_PARENT")
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageTrue"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, name, true"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.name, "parentMessageFalse"),
					new NodeAttribute<>(SchemaProperty.propertyType, "Notion"),
					new NodeAttribute<>(SchemaProperty.format, "parent, name, false"),
					new NodeAttribute<>(SchemaProperty.schemaNode, message)
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}
	}

	@Test
	public void testEncryptedStringProperty() {

		// schema setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Project");

			type.addEncryptedProperty("encrypted");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

		final Class<AbstractNode> projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");
		final PropertyKey<String> encrypted         = StructrApp.key(projectType, "encrypted");

		// test initial error when no key is set
		try (final Tx tx = app.tx()) {

			app.create(projectType,
				new NodeAttribute<>(AbstractNode.name, "test"),
				new NodeAttribute<>(encrypted, "plaintext")
			);

			tx.success();

			fail("Encrypted string property should throw an exception when no initial encryption key is set.");

		} catch (FrameworkException fex) {

			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		// set encryption key
		CryptFunction.setEncryptionKey("structr");

		// test success
		try (final Tx tx = app.tx()) {

			app.create(projectType,
				new NodeAttribute<>(AbstractNode.name, "test"),
				new NodeAttribute<>(encrypted, "plaintext")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// test encryption
		try (final Tx tx = app.tx()) {

			final AbstractNode node = app.nodeQuery(projectType).getFirst();

			assertEquals("Invalid getProperty() result of encrypted string property with correct key", "plaintext", node.getProperty(encrypted));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// set wrong encryption key
		CryptFunction.setEncryptionKey("wrong");

		// test encryption
		try (final Tx tx = app.tx()) {

			final AbstractNode node = app.nodeQuery(projectType).getFirst();

			assertNull("Invalid getProperty() result of encrypted string property with wrong key", node.getProperty(encrypted));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// reset encryption key
		CryptFunction.setEncryptionKey(null);

		// compare result (decrypt does not throw an exception, it only returns null)
		try (final Tx tx = app.tx()) {

			final AbstractNode node = app.nodeQuery(projectType).getFirst();

			assertNull("Invalid getProperty() result of encrypted string property with no key", node.getProperty(encrypted));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// set key via configuration setting
		Settings.GlobalSecret.setValue("global");

		// test encryption with wrong key (global)
		try (final Tx tx = app.tx()) {

			final AbstractNode node = app.nodeQuery(projectType).getFirst();

			assertNull("Invalid getProperty() result of encrypted string property with wrong key", node.getProperty(encrypted));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// delete test object
		try (final Tx tx = app.tx()) {

			// delete all nodes of given type
			app.deleteAllNodesOfType(projectType);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// test success
		try (final Tx tx = app.tx()) {

			app.create(projectType,
				new NodeAttribute<>(AbstractNode.name, "test"),
				new NodeAttribute<>(encrypted, "structrtest")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// test encryption
		try (final Tx tx = app.tx()) {

			final AbstractNode node = app.nodeQuery(projectType).getFirst();

			assertEquals("Invalid getProperty() result of encrypted string property with correct key", "structrtest", node.getProperty(encrypted));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}
}