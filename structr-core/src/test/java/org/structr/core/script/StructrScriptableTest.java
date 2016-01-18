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
package org.structr.core.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestOne.Status;
import org.structr.core.entity.TestSix;
import org.structr.core.entity.TestThree;
import org.structr.core.entity.TestTwo;
import org.structr.core.entity.TestUser;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;


/**
 *
 *
 */


public class StructrScriptableTest extends StructrTest {

	public void testSetPropertyWithDynamicNodes() {

		/**
		 * This test creates two connected SchemaNodes and tests the script-based
		 * association of one instance with several others in the onCreate method.
		 */

		final long currentTimeMillis    = System.currentTimeMillis();
		Class sourceType                = null;
		Class targetType                = null;
		PropertyKey targetsProperty     = null;
		EnumProperty testEnumProperty   = null;
		PropertyKey testBooleanProperty = null;
		PropertyKey testIntegerProperty = null;
		PropertyKey testStringProperty  = null;
		PropertyKey testDoubleProperty  = null;
		PropertyKey testDateProperty = null;
		Class testEnumType              = null;

		// setup phase: create schema nodes
		try (final Tx tx = app.tx()) {

			// create two nodes and associate them with each other
			final SchemaNode sourceNode  = createTestNode(SchemaNode.class, "Source");
			final SchemaNode targetNode  = createTestNode(SchemaNode.class, "Target");

			final List<SchemaProperty> properties = new LinkedList<>();
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "testBoolean"), new NodeAttribute(SchemaProperty.propertyType, "Boolean")));
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "testInteger"), new NodeAttribute(SchemaProperty.propertyType, "Integer")));
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "testString"), new NodeAttribute(SchemaProperty.propertyType, "String")));
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "testDouble"), new NodeAttribute(SchemaProperty.propertyType, "Double")));
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "testEnum"), new NodeAttribute(SchemaProperty.propertyType, "Enum"), new NodeAttribute(SchemaProperty.format, "OPEN, CLOSED, TEST")));
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "testDate"), new NodeAttribute(SchemaProperty.propertyType, "Date")));
			sourceNode.setProperty(SchemaNode.schemaProperties, properties);

			final List<SchemaMethod> methods = new LinkedList<>();
			methods.add(createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "onCreate"), new NodeAttribute(SchemaMethod.source, "{ var e = Structr.get('this'); e.targets = Structr.find('Target'); }")));
			methods.add(createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "doTest01"), new NodeAttribute(SchemaMethod.source, "{ var e = Structr.get('this'); e.testEnum = 'OPEN'; }")));
			methods.add(createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "doTest02"), new NodeAttribute(SchemaMethod.source, "{ var e = Structr.get('this'); e.testEnum = 'CLOSED'; }")));
			methods.add(createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "doTest03"), new NodeAttribute(SchemaMethod.source, "{ var e = Structr.get('this'); e.testEnum = 'TEST'; }")));
			methods.add(createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "doTest04"), new NodeAttribute(SchemaMethod.source, "{ var e = Structr.get('this'); e.testEnum = 'INVALID'; }")));
			methods.add(createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "doTest05"), new NodeAttribute(SchemaMethod.source, "{ var e = Structr.get('this'); e.testBoolean = true; e.testInteger = 123; e.testString = 'testing..'; e.testDouble = 1.2345; e.testDate = new Date(" + currentTimeMillis + "); }")));
			sourceNode.setProperty(SchemaNode.schemaMethods, methods);

			final PropertyMap propertyMap = new PropertyMap();

			propertyMap.put(SchemaRelationshipNode.sourceId,       sourceNode.getUuid());
			propertyMap.put(SchemaRelationshipNode.targetId,       targetNode.getUuid());
			propertyMap.put(SchemaRelationshipNode.sourceJsonName, "source");
			propertyMap.put(SchemaRelationshipNode.targetJsonName, "targets");
			propertyMap.put(SchemaRelationshipNode.sourceMultiplicity, "*");
			propertyMap.put(SchemaRelationshipNode.targetMultiplicity, "*");
			propertyMap.put(SchemaRelationshipNode.relationshipType, "HAS");

			app.create(SchemaRelationshipNode.class, propertyMap);

			tx.success();


		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final ConfigurationProvider config = StructrApp.getConfiguration();

			sourceType          = config.getNodeEntityClass("Source");
			targetType          = config.getNodeEntityClass("Target");
			targetsProperty     = config.getPropertyKeyForJSONName(sourceType, "targets");

			// we need to cast to EnumProperty in order to obtain the dynamic enum type
			testEnumProperty    = (EnumProperty)config.getPropertyKeyForJSONName(sourceType, "testEnum");
			testEnumType        = testEnumProperty.getEnumType();

			testBooleanProperty = config.getPropertyKeyForJSONName(sourceType, "testBoolean");
			testIntegerProperty = config.getPropertyKeyForJSONName(sourceType, "testInteger");
			testStringProperty  = config.getPropertyKeyForJSONName(sourceType, "testString");
			testDoubleProperty  = config.getPropertyKeyForJSONName(sourceType, "testDouble");
			testDateProperty    = config.getPropertyKeyForJSONName(sourceType, "testDate");

			assertNotNull(sourceType);
			assertNotNull(targetType);
			assertNotNull(targetsProperty);

			// create 5 target nodes
			createTestNodes(targetType, 5);

			// create source node
			createTestNodes(sourceType, 5);

			tx.success();


		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		// check phase: source node should have all five target nodes associated with HAS
		try (final Tx tx = app.tx()) {

			// check all source nodes
			for (final Object obj : app.nodeQuery(sourceType).getAsList()) {

				assertNotNull("Invalid nodeQuery result", obj);

				final GraphObject sourceNode = (GraphObject)obj;

				// test contents of "targets" property
				final Object targetNodesObject = sourceNode.getProperty(targetsProperty);
				assertTrue("Invalid getProperty result for scripted association", targetNodesObject instanceof List);

				final List list = (List)targetNodesObject;
				assertEquals("Invalid getProperty result for scripted association", 5, list.size());
			}

			final GraphObject sourceNode = app.nodeQuery(sourceType).getFirst();

			// set testEnum property to OPEN via doTest01 function call, check result
			sourceNode.invokeMethod("doTest01", Collections.EMPTY_MAP, true);
			assertEquals("Invalid setProperty result for EnumProperty", testEnumType.getEnumConstants()[0], sourceNode.getProperty(testEnumProperty));

			// set testEnum property to CLOSED via doTest02 function call, check result
			sourceNode.invokeMethod("doTest02", Collections.EMPTY_MAP, true);
			assertEquals("Invalid setProperty result for EnumProperty", testEnumType.getEnumConstants()[1], sourceNode.getProperty(testEnumProperty));

			// set testEnum property to TEST via doTest03 function call, check result
			sourceNode.invokeMethod("doTest03", Collections.EMPTY_MAP, true);
			assertEquals("Invalid setProperty result for EnumProperty", testEnumType.getEnumConstants()[2], sourceNode.getProperty(testEnumProperty));

			// set testEnum property to INVALID via doTest03 function call, expect previous value & error
			try {
				sourceNode.invokeMethod("doTest04", Collections.EMPTY_MAP, true);
				assertEquals("Invalid setProperty result for EnumProperty",    testEnumType.getEnumConstants()[2], sourceNode.getProperty(testEnumProperty));
				fail("Setting EnumProperty to invalid value should result in an Exception!");

			} catch (FrameworkException fx) {}

			// test other property types
			sourceNode.invokeMethod("doTest05", Collections.EMPTY_MAP, true);
			assertEquals("Invalid setProperty result for BooleanProperty",                         true, sourceNode.getProperty(testBooleanProperty));
			assertEquals("Invalid setProperty result for IntegerProperty",                          123, sourceNode.getProperty(testIntegerProperty));
			assertEquals("Invalid setProperty result for StringProperty",                   "testing..", sourceNode.getProperty(testStringProperty));
			assertEquals("Invalid setProperty result for DoubleProperty",                        1.2345, sourceNode.getProperty(testDoubleProperty));
			assertEquals("Invalid setProperty result for DateProperty",     new Date(currentTimeMillis), sourceNode.getProperty(testDateProperty));

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testGrantViaScripting() {

		// setup phase: create schema nodes
		try (final Tx tx = app.tx()) {

			// create two nodes and associate them with each other
			final SchemaNode sourceNode  = createTestNode(SchemaNode.class, "Source");
			final SchemaMethod method    = createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "doTest01"), new NodeAttribute(SchemaMethod.source, "{ var e = Structr.get('this'); e.grant(Structr.find('TestUser')[0], 'read', 'write'); }"));

			sourceNode.setProperty(SchemaNode.schemaMethods, Arrays.asList(new SchemaMethod[] { method } ));

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Class sourceType             = config.getNodeEntityClass("Source");
		Principal testUser                 = null;

		// create test node as superuser
		try (final Tx tx = app.tx()) {

			app.create(sourceType);
			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// create test user
		try (final Tx tx = app.tx()) {

			testUser = app.create(TestUser.class,
				new NodeAttribute<>(Principal.name,     "test"),
				new NodeAttribute<>(Principal.password, "test")
			);

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final App userApp = StructrApp.getInstance(SecurityContext.getInstance(testUser, AccessMode.Backend));

		// first test without grant, expect no test object to be found using the user context
		try (final Tx tx = userApp.tx()) { assertEquals("Invalid grant() scripting result", 0, userApp.nodeQuery(sourceType).getAsList().size()); tx.success(); } catch(FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// grant read access to test user
		try (final Tx tx = app.tx()) {

			app.nodeQuery(sourceType).getFirst().invokeMethod("doTest01", Collections.EMPTY_MAP, true);
			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// first test without grant, expect no test object to be found using the user context
		try (final Tx tx = userApp.tx()) { assertEquals("Invalid grant() scripting result", 1, userApp.nodeQuery(sourceType).getAsList().size()); tx.success(); } catch(FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testScriptedFindWithJSONObject() {

		final Random random    = new Random();
		final long long1       = 13475233523455L;
		final long long2       = 327326252322L;
		final double double1   = 1234.56789;
		final double double2   = 5678.975321;

		List<TestSix> testSixs = null;
		TestOne testOne1       = null;
		TestOne testOne2       = null;
		TestTwo testTwo1       = null;
		TestTwo testTwo2       = null;
		TestThree testThree1   = null;
		TestThree testThree2   = null;
		TestFour testFour1     = null;
		TestFour testFour2     = null;
		Date date1             = null;
		Date date2             = null;

		// setup phase
		try (final Tx tx = app.tx()) {

			testSixs             = createTestNodes(TestSix.class, 10);
			testOne1             = app.create(TestOne.class);
			testOne2             = app.create(TestOne.class);
			testTwo1             = app.create(TestTwo.class);
			testTwo2             = app.create(TestTwo.class);
			testThree1           = app.create(TestThree.class);
			testThree2           = app.create(TestThree.class);
			testFour1            = app.create(TestFour.class);
			testFour2            = app.create(TestFour.class);
			date1                = new Date(random.nextLong());
			date2                = new Date();

			testOne1.setProperty(TestOne.anInt             , 42);
			testOne1.setProperty(TestOne.aLong             , long1);
			testOne1.setProperty(TestOne.aDouble           , double1);
			testOne1.setProperty(TestOne.aDate             , date1);
			testOne1.setProperty(TestOne.anEnum            , Status.One);
			testOne1.setProperty(TestOne.aString           , "aString1");
			testOne1.setProperty(TestOne.aBoolean          , true);
			testOne1.setProperty(TestOne.testTwo           , testTwo1);
			testOne1.setProperty(TestOne.testThree         , testThree1);
			testOne1.setProperty(TestOne.testFour          , testFour1);
			testOne1.setProperty(TestOne.manyToManyTestSixs, testSixs.subList(0, 5));

			testOne2.setProperty(TestOne.anInt             , 33);
			testOne2.setProperty(TestOne.aLong             , long2);
			testOne2.setProperty(TestOne.aDouble           , double2);
			testOne2.setProperty(TestOne.aDate             , date2);
			testOne2.setProperty(TestOne.anEnum            , Status.Two);
			testOne2.setProperty(TestOne.aString           , "aString2");
			testOne2.setProperty(TestOne.aBoolean          , false);
			testOne2.setProperty(TestOne.testTwo           , testTwo2);
			testOne2.setProperty(TestOne.testThree         , testThree2);
			testOne2.setProperty(TestOne.testFour          , testFour2);
			testOne2.setProperty(TestOne.manyToManyTestSixs, testSixs.subList(5, 10));

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test phase, find all the things using scripting
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { anInt: 42 })[0]; }}"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { anInt: 33 })[0]; }}"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aLong: " + long1 + " })[0]; }}"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aLong: " + long2 + " })[0]; }}"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aDouble: " + double1 + " })[0]; }}"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aDouble: " + double2 + " })[0]; }}"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { anEnum: 'One' })[0]; }}"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { anEnum: 'Two' })[0]; }}"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aBoolean: true })[0]; }}"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aBoolean: false })[0]; }}"));


			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testWrappingUnwrapping() {

		// setup phase
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);
			final TestOne context             = app.create(TestOne.class);

			Scripting.evaluate(actionContext, context, "${{ Structr.create('Group', { name: 'Group1' } ); }}");
			Scripting.evaluate(actionContext, context, "${{ Structr.create('Group', 'name', 'Group2'); }}");

			assertEquals("Invalid unwrapping result", 2, app.nodeQuery(Group.class).getAsList().size());


			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testEnumPropertyGet() {

		// setup phase
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);
			final TestOne context             = app.create(TestOne.class);

			Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); e.anEnum = 'One'; }}");

			assertEquals("Invalid enum get result", "One", Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); return e.anEnum; }}"));

			assertEquals("Invaliid Javascript enum comparison result", true, Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); return e.anEnum == 'One'; }}"));

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testCollectionOperations() {

		Group group            = null;
		TestUser user1         = null;
		TestUser user2         = null;
		TestOne testOne        = null;

		// setup phase
		try (final Tx tx = app.tx()) {

			group = app.create(Group.class, "Group");
			user1  = app.create(TestUser.class, "Tester1");
			user2  = app.create(TestUser.class, "Tester2");

			group.setProperty(Group.members, Arrays.asList(new Principal[] { user1 } ));


			testOne = app.create(TestOne.class);
			createTestNodes(TestSix.class, 10);

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test phase, find all the things using scripting
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			// test prerequisites
			assertEquals("Invalid prerequisite",     1, group.getProperty(Group.members).size());
			assertEquals("Invalid prerequisite", user2, Scripting.evaluate(actionContext, group, "${{ return Structr.find('TestUser', { name: 'Tester2' })[0]; }}"));

			// test scripting association
			Scripting.evaluate(actionContext, group, "${{ var group = Structr.find('Group')[0]; var users = group.members; users.push(Structr.find('TestUser', { name: 'Tester2' })[0]); }}");
			assertEquals("Invalid scripted array operation result", 2, group.getProperty(Group.members).size());

			// reset group
			group.setProperty(Group.members, Arrays.asList(new Principal[] { user1 } ));

			// test prerequisites
			assertEquals("Invalid prerequisite",     1, group.getProperty(Group.members).size());

			// test direct push on member property
			Scripting.evaluate(actionContext, group, "${{ var group = Structr.find('Group')[0]; group.members.push(Structr.find('TestUser', { name: 'Tester2' })[0]); }}");
			assertEquals("Invalid scripted array operation result", 2, group.getProperty(Group.members).size());



			// test scripting association
			Scripting.evaluate(actionContext, group, "${{ var test = Structr.find('TestOne')[0]; var testSixs = test.manyToManyTestSixs; testSixs.push(Structr.find('TestSix')[0]); }}");
			assertEquals("Invalid scripted array operation result", 1, testOne.getProperty(TestOne.manyToManyTestSixs).size());

			// test direct push on member property
			Scripting.evaluate(actionContext, group, "${{ var test = Structr.find('TestOne')[0]; var testSixs = test.manyToManyTestSixs.push(Structr.find('TestSix')[1]); }}");
			assertEquals("Invalid scripted array operation result", 2, testOne.getProperty(TestOne.manyToManyTestSixs).size());


			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testPropertyConversion() {

		TestOne testOne = null;

		// setup phase
		try (final Tx tx = app.tx()) {

			testOne = app.create(TestOne.class);

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test phase, check value conversion
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aString = 12; }}");
			assertEquals("Invalid scripted property conversion result", "12", testOne.getProperty(TestOne.aString));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.anInt = '12'; }}");
			assertEquals("Invalid scripted property conversion result", 12, (int)testOne.getProperty(TestOne.anInt));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aDouble = '12.2342'; }}");
			assertEquals("Invalid scripted property conversion result", 12.2342, (double)testOne.getProperty(TestOne.aDouble));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aDouble = 2; }}");
			assertEquals("Invalid scripted property conversion result", 2.0, (double)testOne.getProperty(TestOne.aDouble));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aLong = 2352343457252; }}");
			assertEquals("Invalid scripted property conversion result", 2352343457252L, (long)testOne.getProperty(TestOne.aLong));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aBoolean = true; }}");
			assertEquals("Invalid scripted property conversion result", true, (boolean)testOne.getProperty(TestOne.aBoolean));

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testQuotes() {

		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			Scripting.evaluate(actionContext, app.create(TestOne.class), "${{\n // \"test\n}}");

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
