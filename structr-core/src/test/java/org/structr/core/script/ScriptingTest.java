/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.MailTemplate;
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
import org.structr.core.function.DateFormatFunction;
import org.structr.core.function.FindFunction;
import org.structr.core.function.NumberFormatFunction;
import org.structr.core.function.ParseDateFunction;
import org.structr.core.function.RoundFunction;
import org.structr.core.function.ToDateFunction;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;


/**
 *
 *
 */
public class ScriptingTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(ScriptingTest.class.getName());

	@Test
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

			logger.warn("", fex);
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

			logger.warn("", fex);
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

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testGrantViaScripting() {

		// setup phase: create schema nodes
		try (final Tx tx = app.tx()) {

			// create two nodes and associate them with each other
			final SchemaNode sourceNode  = createTestNode(SchemaNode.class, "Source");
			final SchemaMethod method    = createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "doTest01"), new NodeAttribute(SchemaMethod.source, "{ var e = Structr.get('this'); e.grant(Structr.find('TestUser')[0], 'read', 'write'); }"));

			sourceNode.setProperty(SchemaNode.schemaMethods, Arrays.asList(new SchemaMethod[] { method } ));

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
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

			logger.warn("", fex);
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

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		final App userApp = StructrApp.getInstance(SecurityContext.getInstance(testUser, AccessMode.Backend));

		// first test without grant, expect no test object to be found using the user context
		try (final Tx tx = userApp.tx()) { assertEquals("Invalid grant() scripting result", 0, userApp.nodeQuery(sourceType).getAsList().size()); tx.success(); } catch(FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// grant read access to test user
		try (final Tx tx = app.tx()) {

			app.nodeQuery(sourceType).getFirst().invokeMethod("doTest01", Collections.EMPTY_MAP, true);
			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// first test without grant, expect no test object to be found using the user context
		try (final Tx tx = userApp.tx()) { assertEquals("Invalid grant() scripting result", 1, userApp.nodeQuery(sourceType).getAsList().size()); tx.success(); } catch(FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
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

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test phase, find all the things using scripting
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { anInt: 42 })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { anInt: 33 })[0]; }}", "test"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aLong: " + long1 + " })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aLong: " + long2 + " })[0]; }}", "test"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aDouble: " + double1 + " })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aDouble: " + double2 + " })[0]; }}", "test"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { anEnum: 'One' })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { anEnum: 'Two' })[0]; }}", "test"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aBoolean: true })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ return Structr.find('TestOne', { aBoolean: false })[0]; }}", "test"));


			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testWrappingUnwrapping() {

		// setup phase
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);
			final TestOne context             = app.create(TestOne.class);

			Scripting.evaluate(actionContext, context, "${{ Structr.create('Group', { name: 'Group1' } ); }}", "test");
			Scripting.evaluate(actionContext, context, "${{ Structr.create('Group', 'name', 'Group2'); }}", "test");

			assertEquals("Invalid unwrapping result", 2, app.nodeQuery(Group.class).getAsList().size());


			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testEnumPropertyGet() {

		// setup phase
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);
			final TestOne context             = app.create(TestOne.class);

			Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); e.anEnum = 'One'; }}", "test");

			assertEquals("Invalid enum get result", "One", Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); return e.anEnum; }}", "test"));

			assertEquals("Invaliid Javascript enum comparison result", true, Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); return e.anEnum == 'One'; }}", "test"));

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
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

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test phase, find all the things using scripting
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			// test prerequisites
			assertEquals("Invalid prerequisite",     1, group.getProperty(Group.members).size());
			assertEquals("Invalid prerequisite", user2, Scripting.evaluate(actionContext, group, "${{ return Structr.find('TestUser', { name: 'Tester2' })[0]; }}", "test"));

			// test scripting association
			Scripting.evaluate(actionContext, group, "${{ var group = Structr.find('Group')[0]; var users = group.members; users.push(Structr.find('TestUser', { name: 'Tester2' })[0]); }}", "test");
			assertEquals("Invalid scripted array operation result", 2, group.getProperty(Group.members).size());

			// reset group
			group.setProperty(Group.members, Arrays.asList(new Principal[] { user1 } ));

			// test prerequisites
			assertEquals("Invalid prerequisite",     1, group.getProperty(Group.members).size());

			// test direct push on member property
			Scripting.evaluate(actionContext, group, "${{ var group = Structr.find('Group')[0]; group.members.push(Structr.find('TestUser', { name: 'Tester2' })[0]); }}", "test");
			assertEquals("Invalid scripted array operation result", 2, group.getProperty(Group.members).size());



			// test scripting association
			Scripting.evaluate(actionContext, group, "${{ var test = Structr.find('TestOne')[0]; var testSixs = test.manyToManyTestSixs; testSixs.push(Structr.find('TestSix')[0]); }}", "test");
			assertEquals("Invalid scripted array operation result", 1, testOne.getProperty(TestOne.manyToManyTestSixs).size());

			// test direct push on member property
			Scripting.evaluate(actionContext, group, "${{ var test = Structr.find('TestOne')[0]; var testSixs = test.manyToManyTestSixs.push(Structr.find('TestSix')[1]); }}", "test");
			assertEquals("Invalid scripted array operation result", 2, testOne.getProperty(TestOne.manyToManyTestSixs).size());


			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testPropertyConversion() {

		TestOne testOne = null;

		// setup phase
		try (final Tx tx = app.tx()) {

			testOne = app.create(TestOne.class);

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test phase, check value conversion
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aString = 12; }}", "test");
			assertEquals("Invalid scripted property conversion result", "12", testOne.getProperty(TestOne.aString));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.anInt = '12'; }}", "test");
			assertEquals("Invalid scripted property conversion result", 12L, (long)testOne.getProperty(TestOne.anInt));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aDouble = '12.2342'; }}", "test");
			assertEquals("Invalid scripted property conversion result", 12.2342, (double)testOne.getProperty(TestOne.aDouble), 0.0);

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aDouble = 2; }}", "test");
			assertEquals("Invalid scripted property conversion result", 2.0, (double)testOne.getProperty(TestOne.aDouble), 0.0);

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aLong = 2352343457252; }}", "test");
			assertEquals("Invalid scripted property conversion result", 2352343457252L, (long)testOne.getProperty(TestOne.aLong));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aBoolean = true; }}", "test");
			assertEquals("Invalid scripted property conversion result", true, (boolean)testOne.getProperty(TestOne.aBoolean));

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testQuotes() {

		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			Scripting.evaluate(actionContext, app.create(TestOne.class), "${{\n // \"test\n}}", "test");

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	/*
	public void testExtractScripts() {

		testExtraction("${function test() { return 'blah'; } return test();}");
		testExtraction("${function test() { return '{' + \"{\"; } return test();}");
		testExtraction("${function test() {{{ return '{' + \"{\" + '}'; }}} return test();}");

		// test multiple sources directly after each other
		final List<String> scripts = Scripting.extractScripts("${{this.aString}}${upper(true)}${lower(false)}");
		assertEquals("Invalid script extraction result", "${{this.aString}}", scripts.get(0));
		assertEquals("Invalid script extraction result", "${upper(true)}", scripts.get(1));
		assertEquals("Invalid script extraction result", "${lower(false)}", scripts.get(2));

		final List<String> scripts2 = Scripting.extractScripts("${{this.aString}}${{{upper(true)}}}{}{{${{{{lower(false)}}}}");
		assertEquals("Invalid script extraction result", "${{this.aString}}", scripts2.get(0));
		assertEquals("Invalid script extraction result", "${{{upper(true)}}}", scripts2.get(1));
		assertEquals("Invalid script extraction result", "${{{{lower(false)}}}}", scripts2.get(2));

		final List<String> scripts3 = Scripting.extractScripts("}}}{{$[[]{&/(&){}{{${{{{lower(false)}}}}}}}{{}{}{");
		assertEquals("Invalid script extraction result", "${{{{lower(false)}}}}", scripts3.get(0));

	}

	public void testJavascript() {

		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);
			final TestOne test                = createTestNode(TestOne.class);

			test.setProperty(TestOne.anInt             , 1);
			test.setProperty(TestOne.aLong             , 2L);
			test.setProperty(TestOne.aDouble           , 3.0);
			test.setProperty(TestOne.aDate             , new Date());
			test.setProperty(TestOne.anEnum            , TestOne.Status.One);
			test.setProperty(TestOne.aString           , "täst");
			test.setProperty(TestOne.aBoolean          , true);
			test.setProperty(TestOne.anotherString     , "oneTwoThree${{{");
			test.setProperty(TestOne.stringWithQuotes  , "''\"\"''");

			assertEquals("Invalid JavaScript evaluation result", "test", Scripting.replaceVariables(actionContext, test, "${{ return 'test' }}"));
			assertEquals("Invalid JavaScript evaluation result", "1",    Scripting.replaceVariables(actionContext, test, "${{ return Structr.get('this').anInt; }}"));
			assertEquals("Invalid JavaScript evaluation result", "2",    Scripting.replaceVariables(actionContext, test, "${{ return Structr.get('this').aLong; }}"));
			assertEquals("Invalid JavaScript evaluation result", "3.0",  Scripting.replaceVariables(actionContext, test, "${{ return Structr.get('this').aDouble; }}"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	public void testPhp() {

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			assertEquals("Invalid PHP scripting evaluation result", "✓ Hello World from PHP!", Scripting.evaluate(ctx, null, "${quercus{<?\necho \"✓ Hello World from PHP!\";\n#phpinfo();\n?>}}"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	public void testRenjin() {

		//final String source = "${Renjin{print(\"Hello World from R!\")\n#plot(sin, -pi, 2*pi)\n# Define the cars vector with 5 values\ncars <- c(1, 3, 6, 4, 9)\n# Graph the cars vector with all defaults\nplot(cars)}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			final Object result     = Scripting.evaluate(ctx, null, "${Renjin{print(\"Hello World from R!\")}}");
			assertEquals("Invalid R scripting evaluation result", "[1] \"Hello World from R!\"\n", result);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	public void testRuby() {

		final String source = "${ruby{puts \"Hello World from Ruby!\"}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final Object result     = Scripting.evaluate(ctx, null, source);

			System.out.println(result);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}
	*/

	@Test
	public void testPython() {

		try (final Tx tx = app.tx()) {

			final TestUser testUser = createTestNode(TestUser.class, "testuser");
			final ActionContext ctx = new ActionContext(SecurityContext.getInstance(testUser, AccessMode.Backend));

			//assertEquals("Invalid python scripting evaluation result", "Hello World from Python!\n", Scripting.evaluate(ctx, null, "${python{print \"Hello World from Python!\"}}"));

			System.out.println(Scripting.evaluate(ctx, null, "${python{print(Structr.get('me').id)}}", "test"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	/*

	private void testExtraction(final String source) {

		final List<String> scripts = Scripting.extractScripts(source);
		assertEquals("Invalid script extraction result", source, scripts.get(0));
	}
	*/

	@Test
	public void testVariableReplacement() {

		final Date now                    = new Date();
		final SimpleDateFormat format1    = new SimpleDateFormat("dd.MM.yyyy");
		final SimpleDateFormat format2    = new SimpleDateFormat("HH:mm:ss");
		final SimpleDateFormat format3    = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		final String nowString1           = format1.format(now);
		final String nowString2           = format2.format(now);
		final String nowString3           = format3.format(now);
		final DecimalFormat numberFormat1 = new DecimalFormat("###0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		final DecimalFormat numberFormat2 = new DecimalFormat("0000.0000", DecimalFormatSymbols.getInstance(Locale.GERMAN));
		final DecimalFormat numberFormat3 = new DecimalFormat("####", DecimalFormatSymbols.getInstance(Locale.SIMPLIFIED_CHINESE));
		final String numberString1        = numberFormat1.format(2.234);
		final String numberString2        = numberFormat2.format(2.234);
		final String numberString3        = numberFormat3.format(2.234);
		MailTemplate template             = null;
		MailTemplate template2            = null;
		TestOne testOne                   = null;
		TestTwo testTwo                   = null;
		TestThree testThree               = null;
		TestFour testFour                 = null;
		List<TestSix> testSixs            = null;
		int index                         = 0;

		try (final Tx tx = app.tx()) {

			testOne        = createTestNode(TestOne.class);
			testTwo        = createTestNode(TestTwo.class);
			testThree      = createTestNode(TestThree.class);
			testFour       = createTestNode(TestFour.class);
			testSixs       = createTestNodes(TestSix.class, 20);

			// set string array on test four
			testFour.setProperty(TestFour.stringArrayProperty, new String[] { "one", "two", "three", "four" } );

			for (final TestSix testSix : testSixs) {

				testSix.setProperty(TestSix.name, "TestSix" + StringUtils.leftPad(Integer.toString(index), 2, "0"));
				testSix.setProperty(TestSix.index, index);

				index++;
			}

			// create mail template
			template = createTestNode(MailTemplate.class);
			template.setProperty(MailTemplate.name, "TEST");
			template.setProperty(MailTemplate.locale, "en_EN");
			template.setProperty(MailTemplate.text, "This is a template for ${this.name}");

			// create mail template
			template2 = createTestNode(MailTemplate.class);
			template2.setProperty(MailTemplate.name, "TEST2");
			template2.setProperty(MailTemplate.locale, "en_EN");
			template2.setProperty(MailTemplate.text, "${this.aDouble}");

			// check existance
			assertNotNull(testOne);

			testOne.setProperty(TestOne.name, "A-nice-little-name-for-my-test-object");
			testOne.setProperty(TestOne.anInt, 1);
			testOne.setProperty(TestOne.aString, "String");
			testOne.setProperty(TestOne.anotherString, "{\n\ttest: test,\n\tnum: 3\n}");
			testOne.setProperty(TestOne.replaceString, "${this.name}");
			testOne.setProperty(TestOne.aLong, 235242522552L);
			testOne.setProperty(TestOne.aDouble, 2.234);
			testOne.setProperty(TestOne.aDate, now);
			testOne.setProperty(TestOne.anEnum, TestOne.Status.One);
			testOne.setProperty(TestOne.aBoolean, true);
			testOne.setProperty(TestOne.testTwo, testTwo);
			testOne.setProperty(TestOne.testThree, testThree);
			testOne.setProperty(TestOne.testFour,  testFour);
			testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);
			testOne.setProperty(TestOne.cleanTestString, "a<b>c.d'e?f(g)h{i}j[k]l+m/n–o\\p\\q|r's!t,u-v_w`x-y-zöäüßABCDEFGH");
			testOne.setProperty(TestOne.stringWithQuotes, "A'B\"C");

			testTwo.setProperty(TestTwo.name, "testTwo_name");
			testThree.setProperty(TestThree.name, "testThree_name");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);

			// test quotes etc.
			assertEquals("Invalid result for quoted template expression", "''", Scripting.replaceVariables(ctx, testOne, "'${err}'"));
			assertEquals("Invalid result for quoted template expression", " '' ", Scripting.replaceVariables(ctx, testOne, " '${err}' "));
			assertEquals("Invalid result for quoted template expression", "\"\"", Scripting.replaceVariables(ctx, testOne, "\"${this.error}\""));
			assertEquals("Invalid result for quoted template expression", "''''''", Scripting.replaceVariables(ctx, testOne, "'''${this.this.this.error}'''"));
			assertEquals("Invalid result for quoted template expression", "''", Scripting.replaceVariables(ctx, testOne, "'${parent.error}'"));
			assertEquals("Invalid result for quoted template expression", "''", Scripting.replaceVariables(ctx, testOne, "'${this.owner}'"));
			assertEquals("Invalid result for quoted template expression", "''", Scripting.replaceVariables(ctx, testOne, "'${this.alwaysNull}'"));
			assertEquals("Invalid result for quoted template expression", "''", Scripting.replaceVariables(ctx, testOne, "'${parent.owner}'"));

			// test for "empty" return value
			assertEquals("Invalid expressions should yield an empty result", "", Scripting.replaceVariables(ctx, testOne, "${err}"));
			assertEquals("Invalid expressions should yield an empty result", "", Scripting.replaceVariables(ctx, testOne, "${this.error}"));
			assertEquals("Invalid expressions should yield an empty result", "", Scripting.replaceVariables(ctx, testOne, "${this.this.this.error}"));
			assertEquals("Invalid expressions should yield an empty result", "", Scripting.replaceVariables(ctx, testOne, "${parent.error}"));
			assertEquals("Invalid expressions should yield an empty result", "", Scripting.replaceVariables(ctx, testOne, "${this.owner}"));
			assertEquals("Invalid expressions should yield an empty result", "", Scripting.replaceVariables(ctx, testOne, "${this.alwaysNull}"));
			assertEquals("Invalid expressions should yield an empty result", "", Scripting.replaceVariables(ctx, testOne, "${parent.owner}"));

			assertEquals("${this} should evaluate to the current node", testOne.toString(), Scripting.replaceVariables(ctx, testOne, "${this}"));
			//assertEquals("${parent} should evaluate to the context parent node", testOne.toString(), Scripting.replaceVariables(ctx, testOne, "${parent}"));

			assertEquals("${this} should evaluate to the current node", testTwo.toString(), Scripting.replaceVariables(ctx, testTwo, "${this}"));
			//assertEquals("${parent} should evaluate to the context parent node", testOne.toString(), Scripting.replaceVariables(ctx, testOne, "${parent}"));

			assertEquals("Invalid variable reference", testTwo.toString(),   Scripting.replaceVariables(ctx, testOne, "${this.testTwo}"));
			assertEquals("Invalid variable reference", testThree.toString(), Scripting.replaceVariables(ctx, testOne, "${this.testThree}"));
			assertEquals("Invalid variable reference", testFour.toString(),  Scripting.replaceVariables(ctx, testOne, "${this.testFour}"));

			assertEquals("Invalid variable reference", testTwo.getUuid(), Scripting.replaceVariables(ctx, testOne, "${this.testTwo.id}"));
			assertEquals("Invalid variable reference", testThree.getUuid(), Scripting.replaceVariables(ctx, testOne, "${this.testThree.id}"));
			assertEquals("Invalid variable reference", testFour.getUuid(), Scripting.replaceVariables(ctx, testOne, "${this.testFour.id}"));

			assertEquals("Invalid size result", "20", Scripting.replaceVariables(ctx, testOne, "${this.manyToManyTestSixs.size}"));

			try {

				Scripting.replaceVariables(ctx, testOne, "${(this.alwaysNull.size}");
				fail("A mismatched opening bracket should throw an exception.");

			} catch (FrameworkException fex) {
				assertEquals("Invalid expression: mismatched closing bracket after this.alwaysNull.size", fex.getMessage());
			}

			assertEquals("Invalid size result", "", Scripting.replaceVariables(ctx, testOne, "${this.alwaysNull.size}"));

			assertEquals("Invalid variable reference", "1",            Scripting.replaceVariables(ctx, testOne, "${this.anInt}"));
			assertEquals("Invalid variable reference", "String",       Scripting.replaceVariables(ctx, testOne, "${this.aString}"));
			assertEquals("Invalid variable reference", "235242522552", Scripting.replaceVariables(ctx, testOne, "${this.aLong}"));
			assertEquals("Invalid variable reference", "2.234",        Scripting.replaceVariables(ctx, testOne, "${this.aDouble}"));

			// test with property
			assertEquals("Invalid md5() result", "27118326006d3829667a400ad23d5d98",  Scripting.replaceVariables(ctx, testOne, "${md5(this.aString)}"));
			assertEquals("Invalid upper() result", "27118326006D3829667A400AD23D5D98",  Scripting.replaceVariables(ctx, testOne, "${upper(md5(this.aString))}"));
			assertEquals("Invalid upper(lower() result", "27118326006D3829667A400AD23D5D98",  Scripting.replaceVariables(ctx, testOne, "${upper(lower(upper(md5(this.aString))))}"));

			assertEquals("Invalid md5() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${md5(this.alwaysNull)}"));
			assertEquals("Invalid upper() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${upper(this.alwaysNull)}"));
			assertEquals("Invalid lower() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${lower(this.alwaysNull)}"));

			// test literal value as well
			assertEquals("Invalid md5() result", "cc03e747a6afbbcbf8be7668acfebee5",  Scripting.replaceVariables(ctx, testOne, "${md5(\"test123\")}"));

			assertEquals("Invalid lower() result", "string",       Scripting.replaceVariables(ctx, testOne, "${lower(this.aString)}"));
			assertEquals("Invalid upper() result", "STRING",       Scripting.replaceVariables(ctx, testOne, "${upper(this.aString)}"));

			// merge
			assertEquals("Invalid merge() result", "[one, two, three]", Scripting.replaceVariables(ctx, testOne, "${merge('one', 'two', 'three')}"));
			assertEquals("Invalid merge() result", "[one, two, three, two, one, two, three]", Scripting.replaceVariables(ctx, testOne, "${merge(merge('one', 'two', 'three'), 'two', merge('one', 'two', 'three'))}"));
			assertEquals("Invalid merge() result", "[1, 2, 3, 4, 5, 6, 7, 8]", Scripting.replaceVariables(ctx, testOne, "${merge(merge('1', '2', '3'), merge('4', '5', merge('6', '7', '8')))}"));
			assertEquals("Invalid merge() result", "[1, 2, 3, 4, 5, 6, 1, 2, 3, 8]", Scripting.replaceVariables(ctx, testOne, "${ ( store('list', merge('1', '2', '3')), merge(retrieve('list'), merge('4', '5', merge('6', retrieve('list'), '8'))) )}"));

			// merge_unique
			assertEquals("Invalid merge_unique() result", "[one, two, three]", Scripting.replaceVariables(ctx, testOne, "${merge_unique('one', 'two', 'three', 'two')}"));
			assertEquals("Invalid merge_unique() result", "[one, two, three]", Scripting.replaceVariables(ctx, testOne, "${merge_unique(merge_unique('one', 'two', 'three'), 'two', merge_unique('one', 'two', 'three'))}"));
			assertEquals("Invalid merge_unique() result", "[1, 2, 3, 4, 5, 6, 7, 8]", Scripting.replaceVariables(ctx, testOne, "${merge_unique(merge_unique('1', '2', '3'), merge_unique('4', '5', merge_unique('6', '7', '8')))}"));
			assertEquals("Invalid merge_unique() result", "[1, 2, 3, 4, 5, 6, 8]", Scripting.replaceVariables(ctx, testOne, "${ ( store('list', merge_unique('1', '2', '3')), merge_unique(retrieve('list'), merge_unique('4', '5', merge_unique('6', retrieve('list'), '8'))) )}"));

			// complement
			assertEquals("Invalid complement() result", "[]", Scripting.replaceVariables(ctx, testOne, "${complement(merge('one', 'two', 'three'), 'one', merge('two', 'three', 'four'))}"));
			assertEquals("Invalid complement() result", "[two]", Scripting.replaceVariables(ctx, testOne, "${complement(merge('one', 'two', 'three'), merge('one', 'four', 'three'))}"));

			// join
			assertEquals("Invalid join() result", "one,two,three", Scripting.replaceVariables(ctx, testOne, "${join(merge(\"one\", \"two\", \"three\"), \",\")}"));

			// concat
			assertEquals("Invalid concat() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(\"one\", \"two\", \"three\")}"));
			assertEquals("Invalid concat() result", "oneStringthree", Scripting.replaceVariables(ctx, testOne, "${concat(\"one\", this.aString, \"three\")}"));
			assertEquals("Invalid concat() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${concat(this.alwaysNull, this.alwaysNull)}"));

			// split
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one,two,three\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one;two;three\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one two three\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one	two	three\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one;two;three\", \";\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one,two,three\", \",\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one.two.three\", \".\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one two three\", \" \"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one+two+three\", \"+\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one|two|three\", \"|\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one::two::three\", \"::\"))}"));
			assertEquals("Invalid split() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one-two-three\", \"-\"))}"));
			assertEquals("Invalid split() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${split(this.alwaysNull)}"));

			// split_regex
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one,two,three\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one;two;three\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one two three\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one	two	three\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one;two;three\", \";\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one,two,three\", \",\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one.two.three\", \"\\.\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one two three\", \" \"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one+two+three\", \"+\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one|two|three\", \"|\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one::two::three\", \"::\"))}"));
			assertEquals("Invalid split_regex() result", "onetwothree", Scripting.replaceVariables(ctx, testOne, "${concat(split(\"one-two-three\", \"-\"))}"));
			assertEquals("Invalid split_regex() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${split(this.alwaysNull)}"));

			// abbr
			assertEquals("Invalid abbr() result", "oneStringt…", Scripting.replaceVariables(ctx, testOne, "${abbr(concat(\"one\", this.aString, \"three\"), 10)}"));
			assertEquals("Invalid abbr() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${abbr(this.alwaysNull, 10)}"));

			// capitalize..
			assertEquals("Invalid capitalize() result", "One_two_three", Scripting.replaceVariables(ctx, testOne, "${capitalize(concat(\"one_\", \"two_\", \"three\"))}"));
			assertEquals("Invalid capitalize() result", "One_Stringthree", Scripting.replaceVariables(ctx, testOne, "${capitalize(concat(\"one_\", this.aString, \"three\"))}"));
			assertEquals("Invalid capitalize() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${capitalize(this.alwaysNull)}"));

			// titleize
			assertEquals("Invalid titleize() result", "One Two Three", Scripting.replaceVariables(ctx, testOne, "${titleize(concat(\"one_\", \"two_\", \"three\"), \"_\")}"));
			assertEquals("Invalid titleize() result", "One Stringthree", Scripting.replaceVariables(ctx, testOne, "${titleize(concat(\"one_\", this.aString, \"three\"), \"_\")}"));
			assertEquals("Invalid titleize() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${titleize(this.alwaysNull)}"));

			// num (explicit number conversion)
			assertEquals("Invalid num() result", "2.234", Scripting.replaceVariables(ctx, testOne, "${num(2.234)}"));
			assertEquals("Invalid num() result", "2.234", Scripting.replaceVariables(ctx, testOne, "${num(this.aDouble)}"));
			assertEquals("Invalid num() result", "1.0", Scripting.replaceVariables(ctx, testOne, "${num(this.anInt)}"));
			assertEquals("Invalid num() result", "", Scripting.replaceVariables(ctx, testOne, "${num(\"abc\")}"));
			assertEquals("Invalid num() result", "", Scripting.replaceVariables(ctx, testOne, "${num(this.aString)}"));
			assertEquals("Invalid num() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${num(this.alwaysNull)}"));

			// index_of
			assertEquals("Invalid index_of() result", "19", Scripting.replaceVariables(ctx, testOne, "${index_of(this.name, 'for')}"));
			assertEquals("Invalid index_of() result", "-1", Scripting.replaceVariables(ctx, testOne, "${index_of(this.name, 'entity')}"));
			assertEquals("Invalid index_of() result", "19", Scripting.replaceVariables(ctx, testOne, "${index_of('a-nice-little-name-for-my-test-object', 'for')}"));
			assertEquals("Invalid index_of() result", "-1", Scripting.replaceVariables(ctx, testOne, "${index_of('a-nice-little-name-for-my-test-object', 'entity')}"));

			// contains
			assertEquals("Invalid contains() result", "true", Scripting.replaceVariables(ctx, testOne, "${contains(this.name, 'for')}"));
			assertEquals("Invalid contains() result", "false", Scripting.replaceVariables(ctx, testOne, "${contains(this.name, 'entity')}"));
			assertEquals("Invalid contains() result", "true", Scripting.replaceVariables(ctx, testOne, "${contains('a-nice-little-name-for-my-test-object', 'for')}"));
			assertEquals("Invalid contains() result", "false", Scripting.replaceVariables(ctx, testOne, "${contains('a-nice-little-name-for-my-test-object', 'entity')}"));

			// contains with collection / entity
			assertEquals("Invalid contains() result", "true", Scripting.replaceVariables(ctx, testOne, "${contains(this.manyToManyTestSixs, first(find('TestSix')))}"));
			assertEquals("Invalid contains() result", "false", Scripting.replaceVariables(ctx, testOne, "${contains(this.manyToManyTestSixs, first(find('TestFive')))}"));

			// substring
			assertEquals("Invalid substring() result", "for", Scripting.replaceVariables(ctx, testOne, "${substring(this.name, 19, 3)}"));
			assertEquals("Invalid substring() result", "", Scripting.replaceVariables(ctx, testOne, "${substring(this.name, -1, -1)}"));
			assertEquals("Invalid substring() result", "", Scripting.replaceVariables(ctx, testOne, "${substring(this.name, 100, -1)}"));
			assertEquals("Invalid substring() result", "", Scripting.replaceVariables(ctx, testOne, "${substring(this.name, 5, -2)}"));
			assertEquals("Invalid substring() result", "for", Scripting.replaceVariables(ctx, testOne, "${substring('a-nice-little-name-for-my-test-object', 19, 3)}"));
			assertEquals("Invalid substring() result", "ice-little-name-for-my-test-object", Scripting.replaceVariables(ctx, testOne, "${substring('a-nice-little-name-for-my-test-object', 3)}"));
			assertEquals("Invalid substring() result", "ice", Scripting.replaceVariables(ctx, testOne, "${substring('a-nice-little-name-for-my-test-object', 3, 3)}"));
			assertEquals("Invalid substring() result", "", Scripting.replaceVariables(ctx, testOne, "${substring('a-nice-little-name-for-my-test-object', -1, -1)}"));
			assertEquals("Invalid substring() result", "", Scripting.replaceVariables(ctx, testOne, "${substring('a-nice-little-name-for-my-test-object', 100, -1)}"));
			assertEquals("Invalid substring() result", "", Scripting.replaceVariables(ctx, testOne, "${substring('a-nice-little-name-for-my-test-object', 5, -2)}"));

			// length
			assertEquals("Invalid length() result", "37", Scripting.replaceVariables(ctx, testOne, "${length(this.name)}"));
			assertEquals("Invalid length() result", "37", Scripting.replaceVariables(ctx, testOne, "${length('a-nice-little-name-for-my-test-object')}"));
			assertEquals("Invalid length() result", "4", Scripting.replaceVariables(ctx, testOne, "${length('test')}"));
			assertEquals("Invalid length() result", "", Scripting.replaceVariables(ctx, testOne, "${length(this.alwaysNull)}"));

			// clean
			assertEquals("Invalid clean() result", "abcd-efghijkl-m-n-o-p-q-r-stu-v-w-x-y-zoauabcdefgh", Scripting.replaceVariables(ctx, testOne, "${clean(this.cleanTestString)}"));
			assertEquals("Invalid clean() result", "abcd-efghijkl-m-n-o-p-q-r-stu-v-w-x-y-zoauabcdefgh", Scripting.replaceVariables(ctx, testOne, "${clean(get(this, \"cleanTestString\"))}"));
			assertEquals("Invalid clean() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${clean(this.alwaysNull)}"));

			// urlencode
			assertEquals("Invalid urlencode() result", "a%3Cb%3Ec.d%27e%3Ff%28g%29h%7Bi%7Dj%5Bk%5Dl%2Bm%2Fn%E2%80%93o%5Cp%5Cq%7Cr%27s%21t%2Cu-v_w%60x-y-z%C3%B6%C3%A4%C3%BC%C3%9FABCDEFGH", Scripting.replaceVariables(ctx, testOne, "${urlencode(this.cleanTestString)}"));
			assertEquals("Invalid urlencode() result", "a%3Cb%3Ec.d%27e%3Ff%28g%29h%7Bi%7Dj%5Bk%5Dl%2Bm%2Fn%E2%80%93o%5Cp%5Cq%7Cr%27s%21t%2Cu-v_w%60x-y-z%C3%B6%C3%A4%C3%BC%C3%9FABCDEFGH", Scripting.replaceVariables(ctx, testOne, "${urlencode(get(this, \"cleanTestString\"))}"));
			assertEquals("Invalid urlencode() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${urlencode(this.alwaysNull)}"));

			// escape_javascript
			assertEquals("Invalid escape_javascript() result", "A\\'B\\\"C", Scripting.replaceVariables(ctx, testOne, "${escape_javascript(this.stringWithQuotes)}"));
			assertEquals("Invalid escape_javascript() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${escape_javascript(this.alwaysNull)}"));

			// escape_json
			assertEquals("Invalid escape_json() result", "A'B\\\"C", Scripting.replaceVariables(ctx, testOne, "${escape_json(this.stringWithQuotes)}"));
			assertEquals("Invalid escape_json() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${escape_json(this.alwaysNull)}"));

			// if etc.
			assertEquals("Invalid if() result", "true",  Scripting.replaceVariables(ctx, testOne,  "${if(\"true\", \"true\", \"false\")}"));
			assertEquals("Invalid if() result", "false", Scripting.replaceVariables(ctx, testOne,  "${if(\"false\", \"true\", \"false\")}"));

			// empty
			assertEquals("Invalid empty() result", "true",  Scripting.replaceVariables(ctx, testOne,  "${empty(\"\")}"));
			assertEquals("Invalid empty() result", "false",  Scripting.replaceVariables(ctx, testOne, "${empty(\" \")}"));
			assertEquals("Invalid empty() result", "false",  Scripting.replaceVariables(ctx, testOne, "${empty(\"   \")}"));
			assertEquals("Invalid empty() result", "false",  Scripting.replaceVariables(ctx, testOne, "${empty(\"xyz\")}"));
			assertEquals("Invalid empty() result with null value", "true", Scripting.replaceVariables(ctx, testOne, "${empty(this.alwaysNull)}"));

			assertEquals("Invalid if(empty()) result", "false",  Scripting.replaceVariables(ctx, testOne,  "${if(empty(\"test\"), true, false)}"));
			assertEquals("Invalid if(empty()) result", "false",  Scripting.replaceVariables(ctx, testOne,  "${if(empty(\"test\n\"), true, false)}"));

			// functions can NOT handle literal strings containing newlines  (disabled for now, because literal strings pose problems in the matching process)
			assertEquals("Invalid if(empty()) result", "false",  Scripting.replaceVariables(ctx, testOne,  "${if(empty(\"\n\"), true, false)}"));
			assertEquals("Invalid if(empty()) result", "false",  Scripting.replaceVariables(ctx, testOne,  "${if(empty(\"\n\"), \"true\", \"false\")}"));

			// functions CAN handle variable values with newlines!
			assertEquals("Invalid if(empty()) result", "false",  Scripting.replaceVariables(ctx, testOne,  "${if(empty(this.anotherString), \"true\", \"false\")}"));

			// equal
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(this.id, this.id)}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(\"1\", this.anInt)}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(1, this.anInt)}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(1.0, this.anInt)}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(this.anInt, \"1\")}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(this.anInt, 1)}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(this.anInt, 1.0)}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(this.aBoolean, \"true\")}"));
			assertEquals("Invalid equal() result", "false",  Scripting.replaceVariables(ctx, testOne, "${equal(this.aBoolean, \"false\")}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(this.aBoolean, true)}"));
			assertEquals("Invalid equal() result", "false",  Scripting.replaceVariables(ctx, testOne, "${equal(this.aBoolean, false)}"));
			assertEquals("Invalid equal() result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(this.anEnum, 'One')}"));

			// if + equal
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(this.id, this.id), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(\"abc\", \"abc\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(3, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(\"3\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(3.1414, 3.1414), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(\"3.1414\", \"3.1414\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(23.44242222243633337234623462, 23.44242222243633337234623462), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(\"23.44242222243633337234623462\", \"23.44242222243633337234623462\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(13, 013), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(equal(13, \"013\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"13\", \"013\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal()) result", "false",  Scripting.replaceVariables(ctx, testOne, "${if(equal(\"13\", \"00013\"), \"true\", \"false\")}"));

			// disabled: java StreamTokenizer can NOT handle scientific notation
//			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(23.4462, 2.34462e1)}"));
//			assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(0.00234462, 2.34462e-3)}"));
//			assertEquals("Invalid if(equal()) result with null value", "false",  Scripting.replaceVariables(ctx, testOne, "${equal(this.alwaysNull, 2.34462e-3)}"));
			assertEquals("Invalid if(equal()) result with null value", "false",  Scripting.replaceVariables(ctx, testOne, "${equal(0.00234462, this.alwaysNull)}"));
			assertEquals("Invalid if(equal()) result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(this.alwaysNull, this.alwaysNull)}"));

			// if + equal + add
			assertEquals("Invalid if(equal(add())) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"2\", add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, add(1, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, add(\"1\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, add(1, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, add(1, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, add(1, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, add(\"1\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, add(1, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, add(1, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(20, add(\"10\", \"10\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(20, add(\"10\", \"010\")), \"true\", \"false\")}"));

			// eq
			assertEquals("Invalideq) result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.id, this.id)}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(\"1\", this.anInt)}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(1, this.anInt)}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(1.0, this.anInt)}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.anInt, \"1\")}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.anInt, 1)}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.anInt, 1.0)}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.aBoolean, \"true\")}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(this.aBoolean, \"false\")}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.aBoolean, true)}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(this.aBoolean, false)}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.anEnum, 'One')}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq('', '')}"));

			// eq with empty string and number
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(3, '')}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq('', 12.3456)}"));

			// eq with null
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(this.alwaysNull, 'xyz')}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq('xyz', this.alwaysNull)}"));

			// if + eq
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(this.id, this.id), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(\"abc\", \"abc\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(3, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(\"3\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(3.1414, 3.1414), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(\"3.1414\", \"3.1414\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(23.44242222243633337234623462, 23.44242222243633337234623462), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(\"23.44242222243633337234623462\", \"23.44242222243633337234623462\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(13, 013), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(eq(13, \"013\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(eq(\"13\", \"013\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq()) result", "false",  Scripting.replaceVariables(ctx, testOne, "${if(eq(\"13\", \"00013\"), \"true\", \"false\")}"));

			// disabled: java StreamTokenizer can NOT handle scientific notation
//			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(23.4462, 2.34462e1)}"));
//			assertEquals("Invalid if(eq()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(0.00234462, 2.34462e-3)}"));
//			assertEquals("Invalid if(eq()) result with null value", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(this.alwaysNull, 2.34462e-3)}"));
			assertEquals("Invalid if(eq()) result with null value", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(0.00234462, this.alwaysNull)}"));
			assertEquals("Invalid if(eq()) result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(this.alwaysNull, this.alwaysNull)}"));

			// if + eq + add
			assertEquals("Invalid if(eq(add())) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(eq(\"2\", add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(eq(\"2\", add(\"2\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2, add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2, add(1, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2, add(\"1\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2, add(1, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2, add(1, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2.0, add(\"1\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2.0, add(1, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2.0, add(\"1\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2.0, add(1, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(2.0, add(1, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(20, add(\"10\", \"10\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(eq(add())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(eq(20, add(\"10\", \"010\")), \"true\", \"false\")}"));


			// add with null
			assertEquals("Invalid add() result with null value", "10.0",  Scripting.replaceVariables(ctx, testOne, "${add(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid add() result with null value", "11.0",  Scripting.replaceVariables(ctx, testOne, "${add(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid add() result with null value", "0.0",  Scripting.replaceVariables(ctx, testOne, "${add(this.alwaysNull, this.alwaysNull)}"));

			// if + lt
			assertEquals("Invalid if(lt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lt(\"2\", \"2\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(\"2\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(\"2000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(\"2.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(\"2000000.0\", \"3000000.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(\"12\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(\"12000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(\"12.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(\"12000000.0\", \"3000000.0\"), \"true\", \"false\")}"));

			assertEquals("Invalid if(lt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lt(2, 2), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(2, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(2000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(2.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(2000000.0, 3000000.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lt(12, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lt(1200000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lt(12000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lt(12.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lt(12000000.0, 3000000.0), \"true\", \"false\")}"));

			// compare numbers written as strings as numbers
			assertEquals("Invalid if(lt()) result", "true", Scripting.replaceVariables(ctx, testOne, "${lt(\"1200\", \"30\")}"));

			// lt with numbers and empty string
			assertEquals("Invalid lt() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${lt(10, '')}"));
			assertEquals("Invalid lt() result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${lt('', 11)}"));
			assertEquals("Invalid lt() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${lt('', '')}"));

			// lt with null
			assertEquals("Invalid lt() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${lt(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid lt() result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${lt(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid lt() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${lt(this.alwaysNull, this.alwaysNull)}"));

			// if + gt
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(\"2\", \"2\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(\"2\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(\"2000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(\"2.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(\"2000000.0\", \"3000000.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(\"12\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(\"12000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(\"12.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false",  Scripting.replaceVariables(ctx, testOne, "${if(gt(\"12000000.0\", \"3000000.0\"), \"true\", \"false\")}"));

			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(2, 2), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(2, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(2000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(2.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gt(2000000.0, 3000000.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gt(12, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gt(12000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gt(12.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gt()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gt(12000000.0, 3000000.0), \"true\", \"false\")}"));

			// gt with null
			assertEquals("Invalid gt() result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${gt(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid gt() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${gt(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid gt() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${gt(this.alwaysNull, this.alwaysNull)}"));

			// gt with numbers and empty string
			assertEquals("Invalid gt() result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${gt(10, '')}"));
			assertEquals("Invalid gt() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${gt('', 11)}"));
			assertEquals("Invalid gt() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${gt('', '')}"));

			// if + lte
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"2\", \"2\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"2\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"2000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"2.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"2000000.0\", \"3000000.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"12\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"12000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"12.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(\"12000000.0\", \"3000000.0\"), \"true\", \"false\")}"));

			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(2, 2), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(2, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(2000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(2.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(lte(2000000.0, 3000000.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lte(12, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lte(12000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lte(12.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(lte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(lte(12000000.0, 3000000.0), \"true\", \"false\")}"));

			// lte with null
			assertEquals("Invalid lte() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${lte(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid lte() result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${lte(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid lte() result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${lte(this.alwaysNull, this.alwaysNull)}"));

			// if + gte
			assertEquals("Invalid if(gte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gte(\"2\", \"2\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(\"2\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(\"2000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(\"2.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(\"2000000.0\", \"3000000.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(\"12\", \"3\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(\"12000000\", \"3000000\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(\"12.0\", \"3.0\"), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(\"12000000.0\", \"3000000.0\"), \"true\", \"false\")}"));

			assertEquals("Invalid if(gte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gte(2, 2), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(2, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(2000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(2.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(gte(2000000.0, 3000000.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gte(12, 3), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gte(12000000, 3000000), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gte(12.0, 3.0), \"true\", \"false\")}"));
			assertEquals("Invalid if(gte()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(gte(12000000.0, 3000000.0), \"true\", \"false\")}"));

			// gte with null
			assertEquals("Invalid gte() result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${gte(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid gte() result with null value", "false",  Scripting.replaceVariables(ctx, testOne, "${gte(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid gte() result with null value", "true",  Scripting.replaceVariables(ctx, testOne, "${gte(this.alwaysNull, this.alwaysNull)}"));

			// if + equal + subt
			assertEquals("Invalid if(equal(subt())) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"2\", subt(\"3\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"2\", subt(\"4\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, subt(\"3\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, subt(3, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, subt(\"3\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, subt(3, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, subt(3, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, subt(\"3\", \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, subt(3, 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, subt(\"3\", 1)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, subt(3, \"1\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, subt(3, 1.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(20, subt(\"30\", \"10\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(subt())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(20, subt(\"30\", \"010\")), \"true\", \"false\")}"));

			// subt with null
			assertEquals("Invalid subt() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${subt(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid subt() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${subt(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid subt() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${subt(this.alwaysNull, this.alwaysNull)}"));

			// if + equal + mult
			assertEquals("Invalid if(equal(mult())) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"6\", mult(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"6\", mult(\"4\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6, mult(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6, mult(3, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6, mult(\"3\", 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6, mult(3, \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6, mult(3, 2.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6.0, mult(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6.0, mult(3, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6.0, mult(\"3\", 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6.0, mult(3, \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(6.0, mult(3, 2.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(600, mult(\"30\", \"20\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(mult())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(600, mult(\"30\", \"020\")), \"true\", \"false\")}"));

			// mult with null
			assertEquals("Invalid mult() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${mult(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid mult() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${mult(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid mult() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${mult(this.alwaysNull, this.alwaysNull)}"));

			// if + equal + quot
			assertEquals("Invalid if(equal(quot())) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"1.5\", quot(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"1.5\", quot(\"5\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(1.5, quot(\"3\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(1.5, quot(3, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(1.5, quot(\"3\", 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(1.5, quot(3, \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(1.5, quot(3, 2.0)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(15, quot(\"30\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(quot())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(15, quot(\"30\", \"02\")), \"true\", \"false\")}"));

			// quot with null
			assertEquals("Invalid quot() result with null value", "10.0",  Scripting.replaceVariables(ctx, testOne, "${quot(10, this.alwaysNull)}"));
			assertEquals("Invalid quot() result with null value", "10.0",  Scripting.replaceVariables(ctx, testOne, "${quot(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid quot() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${quot(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid quot() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${quot(this.alwaysNull, this.alwaysNull)}"));

			// if + equal + round
			assertEquals("Invalid if(equal(round())) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"1.9\", round(\"1.9\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"2\", round(\"2.5\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"2\", round(\"1.999999\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(\"2\", round(\"2.499999\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(1.9, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(2.5, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(1.999999, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(2.499999, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(2, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.4, round(2.4, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.23, round(2.225234, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(1.9, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(2.5, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(1.999999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "false", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(2.499999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(1.999999, round(1.999999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.499999, round(2.499999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(1.999999999, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, round(2, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.4, round(2.4, 8)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.225234, round(2.225234, 8)), \"true\", \"false\")}"));

			// disabled because scientific notation is not supported :(
			//assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(0.00245, round(2.45e-3, 8)), \"true\", \"false\")}"));
			//assertEquals("Invalid if(equal(round())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(245, round(2.45e2, 8)), \"true\", \"false\")}"));

			// round with null
			assertEquals("Invalid round() result", "10",                                              Scripting.replaceVariables(ctx, testOne, "${round(\"10\")}"));
			assertEquals("Invalid round() result with null value", "",                                Scripting.replaceVariables(ctx, testOne, "${round(this.alwaysNull)}"));
			assertEquals("Invalid round() result with null value", RoundFunction.ERROR_MESSAGE_ROUND, Scripting.replaceVariables(ctx, testOne, "${round(this.alwaysNull, this.alwaysNull)}"));

			// if + equal + max
			assertEquals("Invalid if(equal(max())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(\"2\", max(\"1.9\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(max())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2, max(1.9, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(max())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(2.0, max(1.9, 2)), \"true\", \"false\")}"));

			// max with null
			assertEquals("Invalid max() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${max(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid max() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${max(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid max() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${max(this.alwaysNull, this.alwaysNull)}"));

			// if + equal + min
			assertEquals("Invalid if(equal(min())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(\"1.9\", min(\"1.9\", \"2\")), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(min())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(1.9, min(1.9, 2)), \"true\", \"false\")}"));
			assertEquals("Invalid if(equal(min())) result", "true",  Scripting.replaceVariables(ctx, testOne, "${if(equal(1, min(1, 2)), \"true\", \"false\")}"));

			// min with null
			assertEquals("Invalid min() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${min(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid min() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${min(this.alwaysNull, \"11\")}"));
			assertEquals("Invalid min() result with null value", "",  Scripting.replaceVariables(ctx, testOne, "${min(this.alwaysNull, this.alwaysNull)}"));

			// date_format
			assertEquals("Invalid date_format() result", nowString1, Scripting.replaceVariables(ctx, testOne, "${date_format(this.aDate, \"" + format1.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", nowString2, Scripting.replaceVariables(ctx, testOne, "${date_format(this.aDate, \"" + format2.toPattern() + "\")}"));
			assertEquals("Invalid date_format() result", nowString3, Scripting.replaceVariables(ctx, testOne, "${date_format(this.aDate, \"" + format3.toPattern() + "\")}"));

			// date_format with null
			assertEquals("Invalid date_format() result with null value", "",                                           Scripting.replaceVariables(ctx, testOne, "${date_format(this.alwaysNull, \"yyyy\")}"));
			assertEquals("Invalid date_format() result with null value", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid date_format() result with null value", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format(this.alwaysNull, this.alwaysNull)}"));

			// date_format error messages
			assertEquals("Invalid date_format() result for wrong number of parameters", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format()}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format(this.aDouble)}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format(this.aDouble, this.aDouble, this.aDouble)}"));

			// parse_date
			assertEquals("Invalid parse_date() result", ParseDateFunction.ERROR_MESSAGE_PARSE_DATE, Scripting.replaceVariables(ctx, testOne, "${parse_date('2015-12-12')}"));
			assertEquals("Invalid parse_date() result", "2015-12-12T00:00:00+0000", Scripting.replaceVariables(ctx, testOne, "${parse_date('2015-12-12', 'yyyy-MM-dd')}"));

			// to_date
			assertEquals("Invalid to_date() result", ToDateFunction.ERROR_MESSAGE_TO_DATE, Scripting.replaceVariables(ctx, testOne, "${to_date()}"));
			assertEquals("Invalid to_date() result", "2016-09-06T22:44:45+0000", Scripting.replaceVariables(ctx, testOne, "${to_date(1473201885000)}"));

			// number_format error messages
			assertEquals("Invalid number_format() result for wrong number of parameters", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT, Scripting.replaceVariables(ctx, testOne, "${number_format()}"));
			assertEquals("Invalid number_format() result for wrong number of parameters", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT, Scripting.replaceVariables(ctx, testOne, "${number_format(this.aDouble)}"));
			assertEquals("Invalid number_format() result for wrong number of parameters", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT, Scripting.replaceVariables(ctx, testOne, "${number_format(this.aDouble, this.aDouble)}"));
			assertEquals("Invalid number_format() result for wrong number of parameters", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT, Scripting.replaceVariables(ctx, testOne, "${number_format(this.aDouble, this.aDouble, \"\", \"\")}"));
			assertEquals("Invalid number_format() result for wrong number of parameters", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT, Scripting.replaceVariables(ctx, testOne, "${number_format(this.aDouble, this.aDouble, \"\", \"\", \"\")}"));

			assertEquals("Invalid number_format() result", numberString1, Scripting.replaceVariables(ctx, testOne, "${number_format(this.aDouble, \"en\", \"" + numberFormat1.toPattern() + "\")}"));
			assertEquals("Invalid number_format() result", numberString2, Scripting.replaceVariables(ctx, testOne, "${number_format(this.aDouble, \"de\", \"" + numberFormat2.toPattern() + "\")}"));
			assertEquals("Invalid number_format() result", numberString3, Scripting.replaceVariables(ctx, testOne, "${number_format(this.aDouble, \"zh\", \"" + numberFormat3.toPattern() + "\")}"));
			assertEquals("Invalid number_format() result",   "123456.79", Scripting.replaceVariables(ctx, testOne, "${number_format(123456.789012, \"en\", \"0.00\")}"));
			assertEquals("Invalid number_format() result", "123456.7890", Scripting.replaceVariables(ctx, testOne, "${number_format(123456.789012, \"en\", \"0.0000\")}"));
			assertEquals("Invalid number_format() result",   "123456,79", Scripting.replaceVariables(ctx, testOne, "${number_format(123456.789012, \"de\", \"0.00\")}"));
			assertEquals("Invalid number_format() result", "123456,7890", Scripting.replaceVariables(ctx, testOne, "${number_format(123456.789012, \"de\", \"0.0000\")}"));
			assertEquals("Invalid number_format() result",   "123456.79", Scripting.replaceVariables(ctx, testOne, "${number_format(123456.789012, \"zh\", \"0.00\")}"));
			assertEquals("Invalid number_format() result", "123456.7890", Scripting.replaceVariables(ctx, testOne, "${number_format(123456.789012, \"zh\", \"0.0000\")}"));

			// number_format with null
			assertEquals("Invalid number_format() result with null value", "",    Scripting.replaceVariables(ctx, testOne, "${number_format(this.alwaysNull, \"en\", \"#\")}"));
			assertEquals("Invalid number_format() result with null parameter(s)", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT,  Scripting.replaceVariables(ctx, testOne, "${number_format(\"10\", this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid number_format() result with null parameter(s)", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT,  Scripting.replaceVariables(ctx, testOne, "${number_format(\"10\", \"de\", this.alwaysNull)}"));
			assertEquals("Invalid number_format() result with null parameter(s)", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT,  Scripting.replaceVariables(ctx, testOne, "${number_format(\"10\", this.alwaysNull, \"#\")}"));
			assertEquals("Invalid number_format() result with null parameter(s)", NumberFormatFunction.ERROR_MESSAGE_NUMBER_FORMAT,  Scripting.replaceVariables(ctx, testOne, "${number_format(this.alwaysNull, this.alwaysNull, this.alwaysNull)}"));

			// not
			assertEquals("Invalid not() result", "true",  Scripting.replaceVariables(ctx, testOne, "${not(false)}"));
			assertEquals("Invalid not() result", "false", Scripting.replaceVariables(ctx, testOne, "${not(true)}"));
			assertEquals("Invalid not() result", "true",  Scripting.replaceVariables(ctx, testOne, "${not(\"false\")}"));
			assertEquals("Invalid not() result", "false", Scripting.replaceVariables(ctx, testOne, "${not(\"true\")}"));

			// not with null
			assertEquals("Invalid not() result with null value", "true", Scripting.replaceVariables(ctx, testOne, "${not(this.alwaysNull)}"));

			// and
			assertEquals("Invalid and() result", "true",  Scripting.replaceVariables(ctx, testOne, "${and(true, true)}"));
			assertEquals("Invalid and() result", "false", Scripting.replaceVariables(ctx, testOne, "${and(true, false)}"));
			assertEquals("Invalid and() result", "false", Scripting.replaceVariables(ctx, testOne, "${and(false, true)}"));
			assertEquals("Invalid and() result", "false", Scripting.replaceVariables(ctx, testOne, "${and(false, false)}"));

			// and with null
			assertEquals("Invalid and() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${and(this.alwaysNull, this.alwaysNull)}"));

			// or
			assertEquals("Invalid or() result", "true",  Scripting.replaceVariables(ctx, testOne, "${or(true, true)}"));
			assertEquals("Invalid or() result", "true", Scripting.replaceVariables(ctx, testOne, "${or(true, false)}"));
			assertEquals("Invalid or() result", "true", Scripting.replaceVariables(ctx, testOne, "${or(false, true)}"));
			assertEquals("Invalid or() result", "false", Scripting.replaceVariables(ctx, testOne, "${and(false, false)}"));

			// or with null
			assertEquals("Invalid or() result with null value", "false", Scripting.replaceVariables(ctx, testOne, "${or(this.alwaysNull, this.alwaysNull)}"));

			// get
			assertEquals("Invalid get() result", "1",  Scripting.replaceVariables(ctx, testOne, "${get(this, \"anInt\")}"));
			assertEquals("Invalid get() result", "String",  Scripting.replaceVariables(ctx, testOne, "${get(this, \"aString\")}"));
			assertEquals("Invalid get() result", "2.234",  Scripting.replaceVariables(ctx, testOne, "${get(this, \"aDouble\")}"));
			assertEquals("Invalid get() result", testTwo.toString(),  Scripting.replaceVariables(ctx, testOne, "${get(this, \"testTwo\")}"));
			assertEquals("Invalid get() result", testTwo.getUuid(),  Scripting.replaceVariables(ctx, testOne, "${get(get(this, \"testTwo\"), \"id\")}"));
			assertEquals("Invalid get() result", testSixs.get(0).getUuid(),  Scripting.replaceVariables(ctx, testOne, "${get(first(get(this, \"manyToManyTestSixs\")), \"id\")}"));

			// size
			assertEquals("Invalid size() result", "20", Scripting.replaceVariables(ctx, testOne, "${size(this.manyToManyTestSixs)}"));
			assertEquals("Invalid size() result", "0", Scripting.replaceVariables(ctx, testOne, "${size(null)}"));
			assertEquals("Invalid size() result", "0", Scripting.replaceVariables(ctx, testOne, "${size(xyz)}"));

			// is_collection
			assertEquals("Invalid is_collection() result", "true", Scripting.replaceVariables(ctx, testOne, "${is_collection(this.manyToManyTestSixs)}"));
			assertEquals("Invalid is_collection() result", "false", Scripting.replaceVariables(ctx, testOne, "${is_collection(this.name)}"));
			assertEquals("Invalid is_collection() result", "false", Scripting.replaceVariables(ctx, testOne, "${is_collection(null)}"));
			assertEquals("Invalid is_collection() result", "false", Scripting.replaceVariables(ctx, testOne, "${is_collection(xyz)}"));

			// is_entity
			assertEquals("Invalid is_entity() result", "true", Scripting.replaceVariables(ctx, testOne, "${is_entity(this.testFour)}"));
			assertEquals("Invalid is_entity() result", "false", Scripting.replaceVariables(ctx, testOne, "${is_entity(this.manyToManyTestSixs)}"));
			assertEquals("Invalid is_entity() result", "false", Scripting.replaceVariables(ctx, testOne, "${is_entity(this.name)}"));
			assertEquals("Invalid is_entity() result", "false", Scripting.replaceVariables(ctx, testOne, "${is_entity(null)}"));
			assertEquals("Invalid is_entity() result", "false", Scripting.replaceVariables(ctx, testOne, "${is_entity(xyz)}"));

			// first / last / nth
			assertEquals("Invalid first() result", testSixs.get( 0).toString(), Scripting.replaceVariables(ctx, testOne, "${first(this.manyToManyTestSixs)}"));
			assertEquals("Invalid last() result",  testSixs.get(19).toString(), Scripting.replaceVariables(ctx, testOne, "${last(this.manyToManyTestSixs)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 2).toString(), Scripting.replaceVariables(ctx, testOne, "${nth(this.manyToManyTestSixs,  2)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 7).toString(), Scripting.replaceVariables(ctx, testOne, "${nth(this.manyToManyTestSixs,  7)}"));
			assertEquals("Invalid nth() result",   testSixs.get( 9).toString(), Scripting.replaceVariables(ctx, testOne, "${nth(this.manyToManyTestSixs,  9)}"));
			assertEquals("Invalid nth() result",  testSixs.get(12).toString(), Scripting.replaceVariables(ctx, testOne, "${nth(this.manyToManyTestSixs, 12)}"));
			assertEquals("Invalid nth() result",  "", Scripting.replaceVariables(ctx, testOne, "${nth(this.manyToManyTestSixs, 21)}"));

			// first / last / nth with null
			assertEquals("Invalid first() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${first(this.alwaysNull)}"));
			assertEquals("Invalid last() result with null value",  "", Scripting.replaceVariables(ctx, testOne, "${last(this.alwaysNull)}"));
			assertEquals("Invalid nth() result with null value",   "", Scripting.replaceVariables(ctx, testOne, "${nth(this.alwaysNull,  2)}"));
			assertEquals("Invalid nth() result with null value",   "", Scripting.replaceVariables(ctx, testOne, "${nth(this.alwaysNull,  7)}"));
			assertEquals("Invalid nth() result with null value",   "", Scripting.replaceVariables(ctx, testOne, "${nth(this.alwaysNull,  9)}"));
			assertEquals("Invalid nth() result with null value",  "", Scripting.replaceVariables(ctx, testOne, "${nth(this.alwaysNull, 12)}"));
			assertEquals("Invalid nth() result with null value",  "", Scripting.replaceVariables(ctx, testOne, "${nth(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid nth() result with null value",  "", Scripting.replaceVariables(ctx, testOne, "${nth(this.alwaysNull, blah)}"));

			// each with null

			// get with null

			// set with null

			// set date (JS scripting)
			assertEquals("Setting the current date/time should not produce output (JS)", "", Scripting.replaceVariables(ctx, testOne, "${{var t = Structr.get('this'); t.aDate = new Date();}}"));

			try {

				// set date (old scripting)
				Scripting.replaceVariables(ctx, testOne, "${set(this, 'aDate', now)}");

			} catch (FrameworkException fex) {
				fail("Setting the current date/time should not cause an Exception (StructrScript)");
			}

			Scripting.replaceVariables(ctx, testOne, "${if(empty(this.alwaysNull), set(this, \"doResult\", true), set(this, \"doResult\", false))}");
			assertEquals("Invalid do() result", "true", Scripting.replaceVariables(ctx, testOne, "${this.doResult}"));

			Scripting.replaceVariables(ctx, testOne, "${if(empty(this.name), set(this, \"doResult\", true), set(this, \"doResult\", false))}");
			assertEquals("Invalid do() result", "false", Scripting.replaceVariables(ctx, testOne, "${this.doResult}"));

			// template method
			assertEquals("Invalid template() result", "This is a template for A-nice-little-name-for-my-test-object", Scripting.replaceVariables(ctx, testOne, "${template(\"TEST\", \"en_EN\", this)}"));

			// more complex tests
			Scripting.replaceVariables(ctx, testOne, "${each(split(\"setTestInteger1,setTestInteger2,setTestInteger3\"), set(this, data, 1))}");
			assertEquals("Invalid each() result", "1", Scripting.replaceVariables(ctx, testOne, "${get(this, \"setTestInteger1\")}"));
			assertEquals("Invalid each() result", "1", Scripting.replaceVariables(ctx, testOne, "${get(this, \"setTestInteger2\")}"));
			assertEquals("Invalid each() result", "1", Scripting.replaceVariables(ctx, testOne, "${get(this, \"setTestInteger3\")}"));

			// complex each expression, sets the value of "testString" to the concatenated IDs of all testSixs that are linked to "this"
			Scripting.replaceVariables(ctx, testOne, "${each(this.manyToManyTestSixs, set(this, \"testString\", concat(get(this, \"testString\"), data.id)))}");
			assertEquals("Invalid each() result", "640", Scripting.replaceVariables(ctx, testOne, "${length(this.testString)}"));

			assertEquals("Invalid if(equal()) result", "String",  Scripting.replaceVariables(ctx, testOne, "${if(empty(this.alwaysNull), titleize(this.aString, '-'), this.alwaysNull)}"));
			assertEquals("Invalid if(equal()) result", "String",  Scripting.replaceVariables(ctx, testOne, "${if(empty(this.aString), titleize(this.alwaysNull, '-'), this.aString)}"));

			assertNull("Invalid result for special null value", Scripting.replaceVariables(ctx, testOne, "${null}"));
			assertNull("Invalid result for special null value", Scripting.replaceVariables(ctx, testOne, "${if(equal(this.anInt, 15), \"selected\", null)}"));

			// tests from real-life examples
			assertEquals("Invalid replacement result", "tile plan ", Scripting.replaceVariables(ctx, testOne, "tile plan ${plan.bannerTag}"));

			// more tests with pre- and postfixes
			assertEquals("Invalid replacement result", "abcdefghijklmnop", Scripting.replaceVariables(ctx, testOne, "abcdefgh${blah}ijklmnop"));
			assertEquals("Invalid replacement result", "abcdefghStringijklmnop", Scripting.replaceVariables(ctx, testOne, "abcdefgh${this.aString}ijklmnop"));
			assertEquals("Invalid replacement result", "#String", Scripting.replaceVariables(ctx, testOne, "#${this.aString}"));
			assertEquals("Invalid replacement result", "doc_sections/"+ testOne.getUuid() + "/childSections?sort=pos", Scripting.replaceVariables(ctx, testOne, "doc_sections/${this.id}/childSections?sort=pos"));
			assertEquals("Invalid replacement result", "A Nice Little Name For My Test Object", Scripting.replaceVariables(ctx, testOne, "${titleize(this.name, '-')}"));
			assertEquals("Invalid replacement result", "STRINGtrueFALSE", Scripting.replaceVariables(ctx, testOne, "${upper(this.aString)}${lower(true)}${upper(false)}"));

			// test store and retrieve
			assertEquals("Invalid store() result", "", Scripting.replaceVariables(ctx, testOne, "${store('tmp', this.name)}"));
			assertEquals("Invalid stored value", "A-nice-little-name-for-my-test-object", ctx.retrieve("tmp"));
			assertEquals("Invalid retrieve() result", "A-nice-little-name-for-my-test-object", Scripting.replaceVariables(ctx, testOne, "${retrieve('tmp')}"));
			assertEquals("Invalid retrieve() result", "", Scripting.replaceVariables(new ActionContext(securityContext), testOne, "${retrieve('tmp')}"));

			// test store and retrieve within filter expression
			assertEquals("Invalid store() result", "", Scripting.replaceVariables(ctx, testOne, "${store('tmp', 10)}"));
			assertEquals("Invalid retrieve() result in filter expression", "9",  Scripting.replaceVariables(ctx, testOne, "${size(filter(this.manyToManyTestSixs, gt(data.index, 10)))}"));
			assertEquals("Invalid retrieve() result in filter expression", "9",  Scripting.replaceVariables(ctx, testOne, "${size(filter(this.manyToManyTestSixs, gt(data.index, retrieve('tmp'))))}"));

			// retrieve object and access attribute
			assertEquals("Invalid store() result", "", Scripting.replaceVariables(ctx, testOne, "${store('testOne', this)}"));
			assertEquals("Invalid retrieve() result", "A-nice-little-name-for-my-test-object", Scripting.replaceVariables(ctx, testOne, "${retrieve('testOne').name}"));

			// retrieve stored object attribute in if() expression via get() function
			assertEquals("Invalid retrieve() result", "A-nice-little-name-for-my-test-object", Scripting.replaceVariables(ctx, testOne, "${if(false,'true', get(retrieve('testOne'), 'name'))}"));

			// retrieve stored object attribute in if() expression via 'dot-name'
			assertEquals("Invalid retrieve() result", "A-nice-little-name-for-my-test-object", Scripting.replaceVariables(ctx, testOne, "${if(false,'true', retrieve('testOne').name)}"));

			// test replace() method
			assertEquals("Invalid replace() result", "A-nice-little-name-for-my-test-object", Scripting.replaceVariables(ctx, testOne, "${replace(this.replaceString, this)}"));

			// test error method
			try {
				Actions.execute(securityContext, testTwo, "${error(\"base\", \"test1\")}", "test");
				fail("error() should throw an exception.");

			} catch (FrameworkException fex) { }

			try {
				Actions.execute(securityContext, testTwo, "${error(\"base\", \"test1\", \"test2\")}", "test");
				fail("error() should throw an exception.");

			} catch (FrameworkException fex) { }

			// test multiline statements
			assertEquals("Invalid replace() result", "equal", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, 2),\n    (\"equal\"),\n    (\"not equal\")\n)}"));
			assertEquals("Invalid replace() result", "not equal", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, 3),\n    (\"equal\"),\n    (\"not equal\")\n)}"));

			assertEquals("Invalid keys() / join() result", "id,name,owner,type,createdBy,deleted,hidden,createdDate,lastModifiedDate,visibleToPublicUsers,visibleToAuthenticatedUsers,visibilityStartDate,visibilityEndDate", Scripting.replaceVariables(ctx, testOne, "${join(keys(this, 'ui'), ',')}"));
			assertEquals("Invalid values() / join() result", "A-nice-little-name-for-my-test-object,1,String", Scripting.replaceVariables(ctx, testOne, "${join(values(this, 'protected'), ',')}"));

			// test default values
			assertEquals("Invalid string default value", "blah", Scripting.replaceVariables(ctx, testOne, "${this.alwaysNull!blah}"));
			assertEquals("Invalid numeric default value", "12", Scripting.replaceVariables(ctx, testOne, "${this.alwaysNull!12}"));

			// Number default value
			assertEquals("true", Scripting.replaceVariables(ctx, testOne, "${equal(42, this.alwaysNull!42)}"));

			// complex multi-statement tests
			Scripting.replaceVariables(ctx, testOne, "${(set(this, \"isValid\", true), each(this.manyToManyTestSixs, set(this, \"isValid\", and(this.isValid, equal(length(data.id), 32)))))}");
			assertEquals("Invalid multiline statement test result", "true", Scripting.replaceVariables(ctx, testOne, "${this.isValid}"));

			Scripting.replaceVariables(ctx, testOne, "${(set(this, \"isValid\", true), each(this.manyToManyTestSixs, set(this, \"isValid\", and(this.isValid, gte(now, data.createdDate)))))}");
			assertEquals("Invalid multiline statement test result", "true", Scripting.replaceVariables(ctx, testOne, "${this.isValid}"));

			Scripting.replaceVariables(ctx, testOne, "${(set(this, \"isValid\", false), each(this.manyToManyTestSixs, set(this, \"isValid\", and(this.isValid, gte(now, data.createdDate)))))}");
			assertEquals("Invalid multiline statement test result", "false", Scripting.replaceVariables(ctx, testOne, "${this.isValid}"));

			// test multiple nested dot-separated properties (this.parent.parent.parent)
			assertEquals("Invalid multilevel property expression result", "false", Scripting.replaceVariables(ctx, testOne, "${empty(this.testThree.testOne.testThree)}"));

			// test extract() with additional evaluation function
			assertEquals("Invalid filter() result", "1",  Scripting.replaceVariables(ctx, testOne, "${size(filter(this.manyToManyTestSixs, equal(data.index, 4)))}"));
			assertEquals("Invalid filter() result", "9",  Scripting.replaceVariables(ctx, testOne, "${size(filter(this.manyToManyTestSixs, gt(data.index, 10)))}"));
			assertEquals("Invalid filter() result", "10", Scripting.replaceVariables(ctx, testOne, "${size(filter(this.manyToManyTestSixs, gte(data.index, 10)))}"));

			// test complex multiline statement replacement
			final String test =
				"${if(lte(template('TEST2', 'en_EN', this), 2), '<2', '>2')}\n" +		// first expression should evaluate to ">2"
				"${if(lte(template('TEST2', 'en_EN', this), 3), '<3', '>3')}"			// second expression should evaluate to "<3"
			;

			final String result = Scripting.replaceVariables(ctx, testOne, test);

			assertEquals("Invalid multiline and template() result", ">2\n<3", result);

			// incoming
			assertEquals("Invalid number of incoming relationships", "20",  Scripting.replaceVariables(ctx, testOne, "${size(incoming(this))}"));
			assertEquals("Invalid number of incoming relationships", "20",  Scripting.replaceVariables(ctx, testOne, "${size(incoming(this, 'MANY_TO_MANY'))}"));
			assertEquals("Invalid number of incoming relationships", "1",   Scripting.replaceVariables(ctx, testTwo, "${size(incoming(this))}"));
			assertEquals("Invalid number of incoming relationships", "1",   Scripting.replaceVariables(ctx, testThree, "${size(incoming(this))}"));
			assertEquals("Invalid relationship type", "IS_AT",              Scripting.replaceVariables(ctx, testTwo, "${get(incoming(this), 'relType')}"));
			assertEquals("Invalid relationship type", "OWNS",               Scripting.replaceVariables(ctx, testThree, "${get(incoming(this), 'relType')}"));

			// outgoing
			assertEquals("Invalid number of outgoing relationships", "3",  Scripting.replaceVariables(ctx, testOne, "${size(outgoing(this))}"));
			assertEquals("Invalid number of outgoing relationships", "2",  Scripting.replaceVariables(ctx, testOne, "${size(outgoing(this, 'IS_AT'))}"));
			assertEquals("Invalid number of outgoing relationships", "1",  Scripting.replaceVariables(ctx, testOne, "${size(outgoing(this, 'OWNS' ))}"));
			assertEquals("Invalid relationship type", "IS_AT",             Scripting.replaceVariables(ctx, testOne, "${get(first(outgoing(this, 'IS_AT')), 'relType')}"));
			assertEquals("Invalid relationship type", "OWNS",              Scripting.replaceVariables(ctx, testOne, "${get(outgoing(this, 'OWNS'), 'relType')}"));

			// has_relationships
			assertEquals("Invalid result of has_relationship", "false",  Scripting.replaceVariables(ctx, testOne, "${has_relationship(this, this)}"));

			assertEquals("Invalid result of has_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')))}"));
			assertEquals("Invalid result of has_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')), 'IS_AT')}"));
			assertEquals("Invalid result of has_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT')}"));
			assertEquals("Invalid result of has_relationship", "false",  Scripting.replaceVariables(ctx, testOne, "${has_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')), 'THIS_DOES_NOT_EXIST')}"));

			assertEquals("Invalid result of has_relationship", "true",  Scripting.replaceVariables(ctx, testTwo, "${has_relationship(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this)}"));
			assertEquals("Invalid result of has_relationship", "true",  Scripting.replaceVariables(ctx, testTwo, "${has_relationship(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')))}"));

			assertEquals("Invalid result of has_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_relationship(this, first(find('TestThree', 'name', 'testThree_name')))}"));
			assertEquals("Invalid result of has_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_relationship(this, first(find('TestThree', 'name', 'testThree_name')), 'OWNS')}"));

			assertEquals("Invalid result of has_relationship", "false", Scripting.replaceVariables(ctx, testTwo, "${has_relationship(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this, 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_relationship", "false",  Scripting.replaceVariables(ctx, testOne, "${has_relationship(this, first(find('TestThree', 'name', 'testThree_name')), 'THIS_DOES_NOT_EXIST')}"));

			// has_incoming_relationship
			assertEquals("Invalid result of has_incoming_relationship", "false",  Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(this, this)}"));

			assertEquals("Invalid result of has_incoming_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this)}"));
			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')))}"));

			assertEquals("Invalid result of has_incoming_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT')}"));
			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')), 'IS_AT')}"));

			assertEquals("Invalid result of has_incoming_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT')}"));
			assertEquals("Invalid result of has_incoming_relationship", "false",  Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')), 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_incoming_relationship", "false",  Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this, 'THIS_DOES_NOT_EXIST')}"));

			assertEquals("Invalid result of has_incoming_relationship", "true",  Scripting.replaceVariables(ctx, testTwo, "${has_incoming_relationship(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')))}"));
			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testTwo, "${has_incoming_relationship(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this)}"));

			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(this, first(find('TestThree', 'name', 'testThree_name')))}"));
			assertEquals("Invalid result of has_incoming_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(first(find('TestThree', 'name', 'testThree_name')), this)}"));

			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(this, first(find('TestThree', 'name', 'testThree_name')), 'OWNS')}"));
			assertEquals("Invalid result of has_incoming_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(first(find('TestThree', 'name', 'testThree_name')), this, 'OWNS')}"));

			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(this, first(find('TestThree', 'name', 'testThree_name')), 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(first(find('TestThree', 'name', 'testThree_name')), this, 'THIS_DOES_NOT_EXIST')}"));

			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testTwo, "${has_incoming_relationship(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this, 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testTwo, "${has_incoming_relationship(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(this, first(find('TestThree', 'name', 'testThree_name')), 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_incoming_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_incoming_relationship(first(find('TestThree', 'name', 'testThree_name')), this, 'THIS_DOES_NOT_EXIST')}"));

			// has_outgoing_relationship (since has_outgoing_relationship is just the inverse method to has_outgoing_relationship we can basically reuse the tests and just invert the result - except for the always-false or always-true tests)
			assertEquals("Invalid result of has_outgoing_relationship", "false",  Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(this, this)}"));

			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this)}"));
			assertEquals("Invalid result of has_outgoing_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')))}"));

			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT')}"));
			assertEquals("Invalid result of has_outgoing_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')), 'IS_AT')}"));

			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT')}"));
			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(this, first(find('TestTwo', 'name', 'testTwo_name')), 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this, 'THIS_DOES_NOT_EXIST')}"));

			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testTwo, "${has_outgoing_relationship(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')))}"));
			assertEquals("Invalid result of has_outgoing_relationship", "true",  Scripting.replaceVariables(ctx, testTwo, "${has_outgoing_relationship(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this)}"));

			assertEquals("Invalid result of has_outgoing_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(this, first(find('TestThree', 'name', 'testThree_name')))}"));
			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(first(find('TestThree', 'name', 'testThree_name')), this)}"));

			assertEquals("Invalid result of has_outgoing_relationship", "true",  Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(this, first(find('TestThree', 'name', 'testThree_name')), 'OWNS')}"));
			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(first(find('TestThree', 'name', 'testThree_name')), this, 'OWNS')}"));

			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(this, first(find('TestThree', 'name', 'testThree_name')), 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(first(find('TestThree', 'name', 'testThree_name')), this, 'THIS_DOES_NOT_EXIST')}"));

			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testTwo, "${has_outgoing_relationship(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this, 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testTwo, "${has_outgoing_relationship(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(this, first(find('TestThree', 'name', 'testThree_name')), 'THIS_DOES_NOT_EXIST')}"));
			assertEquals("Invalid result of has_outgoing_relationship", "false", Scripting.replaceVariables(ctx, testOne, "${has_outgoing_relationship(first(find('TestThree', 'name', 'testThree_name')), this, 'THIS_DOES_NOT_EXIST')}"));

			// get_relationships (CAUTION! If the method returns a string (error-case) the size-method returns "1" => it seems like there is one relationsh)
			assertEquals("Invalid number of relationships", "0",  Scripting.replaceVariables(ctx, testOne, "${size(get_relationships(this, this))}"));

			// non-existent relType between nodes which have a relationship
			assertEquals("Invalid number of relationships", "0",  Scripting.replaceVariables(ctx, testOne, "${size(get_relationships(first(find('TestTwo', 'name', 'testTwo_name')), this, 'THIS_DOES_NOT_EXIST'))}"));
			// non-existent relType between a node and itself
			assertEquals("Invalid number of relationships", "0",  Scripting.replaceVariables(ctx, testOne, "${size(get_relationships(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this, 'THIS_DOES_NOT_EXIST'))}"));

			// identical result test (from and to are just switched around)
			assertEquals("Invalid number of relationships", "1",  Scripting.replaceVariables(ctx, testTwo, "${size(get_relationships(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this, 'IS_AT'))}"));
			assertEquals("Invalid number of relationships", "1",  Scripting.replaceVariables(ctx, testTwo, "${size(get_relationships(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), 'IS_AT'))}"));


			// get_incoming_relationships (CAUTION! If the method returns a string (error-case) the size-method returns "1" => it seems like there is one relationsh)
			assertEquals("Invalid number of incoming relationships", "0",  Scripting.replaceVariables(ctx, testOne, "${size(get_incoming_relationships(this, this))}"));

			assertEquals("Invalid number of incoming relationships", "0",  Scripting.replaceVariables(ctx, testOne, "${size(get_incoming_relationships(this, first(find('TestTwo', 'name', 'testTwo_name'))))}"));
			assertEquals("Invalid number of incoming relationships", "1",  Scripting.replaceVariables(ctx, testOne, "${size(get_incoming_relationships(first(find('TestTwo', 'name', 'testTwo_name')), this))}"));
			assertEquals("Invalid number of incoming relationships", "0",  Scripting.replaceVariables(ctx, testOne, "${size(get_incoming_relationships(this, first(find('TestTwo', 'name', 'testTwo_name')), 'IS_AT'))}"));
			assertEquals("Invalid number of incoming relationships", "1",  Scripting.replaceVariables(ctx, testOne, "${size(get_incoming_relationships(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT'))}"));

			assertEquals("Invalid number of incoming relationships", "1",  Scripting.replaceVariables(ctx, testTwo, "${size(get_incoming_relationships(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object'))))}"));
			assertEquals("Invalid number of incoming relationships", "1",Scripting.replaceVariables(ctx, testThree, "${size(get_incoming_relationships(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object'))))}"));
			assertEquals("Invalid relationship type", "IS_AT",             Scripting.replaceVariables(ctx, testTwo, "${get(first(get_incoming_relationships(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')))), 'relType')}"));

			assertEquals("Invalid relationship type", "OWNS",            Scripting.replaceVariables(ctx, testThree, "${get(first(get_incoming_relationships(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')))), 'relType')}"));


			// get_outgoing_relationships (CAUTION! If the method returns a string (error-case) the size-method returns "1" => it seems like there is one relationsh)
			assertEquals("Invalid number of outgoing relationships", "0",  Scripting.replaceVariables(ctx, testOne, "${size(get_outgoing_relationships(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this))}"));

			assertEquals("Invalid number of outgoing relationships", "0",  Scripting.replaceVariables(ctx, testTwo, "${size(get_outgoing_relationships(this, first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object'))))}"));

			assertEquals("Invalid number of outgoing relationships", "1",  Scripting.replaceVariables(ctx, testTwo, "${size(get_outgoing_relationships(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this))}"));
			assertEquals("Invalid number of outgoing relationships", "0",  Scripting.replaceVariables(ctx, testTwo, "${size(get_outgoing_relationships(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this, 'THIS_DOES_NOT_EXIST'))}"));

			assertEquals("Invalid number of outgoing relationships", "1",Scripting.replaceVariables(ctx, testThree, "${size(get_outgoing_relationships(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this))}"));
			assertEquals("Invalid relationship type", "IS_AT",             Scripting.replaceVariables(ctx, testTwo, "${get(first(get_outgoing_relationships(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this)), 'relType')}"));

			assertEquals("Invalid relationship type", "OWNS",            Scripting.replaceVariables(ctx, testThree, "${get(first(get_outgoing_relationships(first(find('TestOne', 'name', 'A-nice-little-name-for-my-test-object')), this)), 'relType')}"));

			// create_relationship
			// lifecycle for relationship t1-[:NEW_RELATIONSHIP_NAME]->t1
			assertEquals("Invalid number of relationships", "0", Scripting.replaceVariables(ctx, testOne, "${size(get_outgoing_relationships(this, this, 'IS_AT'))}"));
			assertEquals("unexpected result of create_relationship", "IS_AT",  Scripting.replaceVariables(ctx, testOne, "${get(create_relationship(this, this, 'IS_AT'), 'relType')}"));
			assertEquals("Invalid number of relationships", "1", Scripting.replaceVariables(ctx, testOne, "${size(get_outgoing_relationships(this, this, 'IS_AT'))}"));
			assertEquals("unexpected result of delete", "",  Scripting.replaceVariables(ctx, testOne, "${delete(first(get_outgoing_relationships(this, this, 'IS_AT')))}"));
			assertEquals("Invalid number of relationships", "0", Scripting.replaceVariables(ctx, testOne, "${size(get_outgoing_relationships(this, this, 'IS_AT'))}"));

			// lifecycle for relationship t2-[:NEW_RELATIONSHIP_NAME]->t1
			assertEquals("Invalid number of relationships", "0", Scripting.replaceVariables(ctx, testOne, "${size(get_outgoing_relationships(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT'))}"));
			assertEquals("unexpected result of create_relationship", "IS_AT",  Scripting.replaceVariables(ctx, testOne, "${get(create_relationship(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT'), 'relType')}"));
			assertEquals("Invalid number of relationships", "1", Scripting.replaceVariables(ctx, testOne, "${size(get_outgoing_relationships(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT'))}"));
			assertEquals("unexpected result of delete", "",  Scripting.replaceVariables(ctx, testOne, "${delete(first(get_outgoing_relationships(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT')))}"));
			assertEquals("Invalid number of relationships", "0", Scripting.replaceVariables(ctx, testOne, "${size(get_outgoing_relationships(first(find('TestTwo', 'name', 'testTwo_name')), this, 'IS_AT'))}"));

			// array index access
			assertEquals("Invalid array index accessor result", testSixs.get(0).getUuid(), Scripting.replaceVariables(ctx, testOne, "${this.manyToManyTestSixs[0]}"));
			assertEquals("Invalid array index accessor result", testSixs.get(2).getUuid(), Scripting.replaceVariables(ctx, testOne, "${this.manyToManyTestSixs[2]}"));
			assertEquals("Invalid array index accessor result", testSixs.get(4).getUuid(), Scripting.replaceVariables(ctx, testOne, "${this.manyToManyTestSixs[4]}"));

			// test new dot notation
			assertEquals("Invalid dot notation result", testSixs.get(0).getProperty(AbstractNode.name), Scripting.replaceVariables(ctx, testOne, "${this.manyToManyTestSixs[0].name}"));
			assertEquals("Invalid dot notation result", testSixs.get(0).getProperty(AbstractNode.name), Scripting.replaceVariables(ctx, testOne, "${sort(find('TestSix'), 'name')[0].name}"));
			assertEquals("Invalid dot notation result", testSixs.get(15).getProperty(AbstractNode.name), Scripting.replaceVariables(ctx, testOne, "${sort(find('TestSix'), 'name')[15].name}"));
			assertEquals("Invalid dot notation result", "20", Scripting.replaceVariables(ctx, testOne, "${this.manyToManyTestSixs.size}"));

			// test array property access
			assertEquals("Invalid string array access result", "one", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty[0]}"));
			assertEquals("Invalid string array access result", "two", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty[1]}"));
			assertEquals("Invalid string array access result", "three", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty[2]}"));
			assertEquals("Invalid string array access result", "four", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty[3]}"));

			// test string array property support in collection access methods
			assertEquals("Invalid string array access result with join()", "one,two,three,four", Scripting.replaceVariables(ctx, testFour, "${join(this.stringArrayProperty, ',')}"));
			assertEquals("Invalid string array access result with concat()", "onetwothreefour", Scripting.replaceVariables(ctx, testFour, "${concat(this.stringArrayProperty)}"));
			assertEquals("Invalid string array access result with first()", "one", Scripting.replaceVariables(ctx, testFour, "${first(this.stringArrayProperty)}"));
			assertEquals("Invalid string array access result with last()", "four", Scripting.replaceVariables(ctx, testFour, "${last(this.stringArrayProperty)}"));
			assertEquals("Invalid string array access result with size()", "4", Scripting.replaceVariables(ctx, testFour, "${size(this.stringArrayProperty)}"));
			assertEquals("Invalid string array access result with .size", "4", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty.size}"));
			assertEquals("Invalid string array access result with nth", "one", Scripting.replaceVariables(ctx, testFour, "${nth(this.stringArrayProperty, 0)}"));
			assertEquals("Invalid string array access result with nth", "two", Scripting.replaceVariables(ctx, testFour, "${nth(this.stringArrayProperty, 1)}"));
			assertEquals("Invalid string array access result with nth", "three", Scripting.replaceVariables(ctx, testFour, "${nth(this.stringArrayProperty, 2)}"));
			assertEquals("Invalid string array access result with nth", "four", Scripting.replaceVariables(ctx, testFour, "${nth(this.stringArrayProperty, 3)}"));
			assertEquals("Invalid string array access result with contains()", "true", Scripting.replaceVariables(ctx, testFour, "${contains(this.stringArrayProperty, 'two')}"));
			assertEquals("Invalid string array access result with contains()", "false", Scripting.replaceVariables(ctx, testFour, "${contains(this.stringArrayProperty, 'five')}"));

			// sort
			assertEquals("Invalid sort result", "[b, a, c]", Scripting.replaceVariables(ctx, null, "${merge('b', 'a', 'c')}"));
			assertEquals("Invalid sort result", "[a, b, c]", Scripting.replaceVariables(ctx, null, "${sort(merge('b', 'a', 'c'))}"));
			assertEquals("Invalid sort result", "",          Scripting.replaceVariables(ctx, null, "${sort()}"));
			assertEquals("Invalid sort result", "[A-nice-little-name-for-my-test-object, testThree_name, testTwo_name]", Scripting.replaceVariables(ctx, testOne, "${extract(sort(merge(this, this.testTwo, this.testThree), 'name'), 'name')}"));
			assertEquals("Invalid sort result", "[A-nice-little-name-for-my-test-object, testThree_name, testTwo_name]", Scripting.replaceVariables(ctx, testOne, "${extract(sort(merge(this.testTwo, this, this.testThree), 'name'), 'name')}"));
			assertEquals("Invalid sort result", "[A-nice-little-name-for-my-test-object, testThree_name, testTwo_name]", Scripting.replaceVariables(ctx, testOne, "${extract(sort(merge(this.testTwo, this.testThree, this), 'name'), 'name')}"));

			// find
			assertEquals("Invalid find() result for empty values", testThree.getUuid(), Scripting.replaceVariables(ctx, testOne, "${first(find('TestThree', 'oneToOneTestSix', this.alwaysNull))}"));
			assertEquals("Invalid find() result for empty values", testThree.getUuid(), Scripting.replaceVariables(ctx, testOne, "${first(find('TestThree', 'oneToManyTestSix', this.alwaysNull))}"));

			// find with incorrect number of parameters
			assertEquals("Invalid find() result", FindFunction.ERROR_MESSAGE_FIND_NO_TYPE_SPECIFIED, Scripting.replaceVariables(ctx, testOne, "${find()}"));
			assertEquals("Invalid find() result", FindFunction.ERROR_MESSAGE_FIND_NO_TYPE_SPECIFIED, Scripting.replaceVariables(ctx, testOne, "${find(this.alwaysNull)}"));
			assertEquals("Invalid find() result", FindFunction.ERROR_MESSAGE_FIND_NO_TYPE_SPECIFIED, Scripting.replaceVariables(ctx, testOne, "${find(this.alwaysNull, this.alwaysNull)}"));
			assertEquals("Invalid find() result", FindFunction.ERROR_MESSAGE_FIND_TYPE_NOT_FOUND + "NonExistingType", Scripting.replaceVariables(ctx, testOne, "${find('NonExistingType')}"));

			// search
			assertEquals("Invalid search() result", testOne.getUuid(), Scripting.replaceVariables(ctx, testTwo, "${first(search('TestOne', 'name', 'A-nice-little-name-for-my-test-object'))}"));
			assertEquals("Invalid search() result", testOne.getUuid(), Scripting.replaceVariables(ctx, testTwo, "${first(search('TestOne', 'name', '*little-name-for-my-test-object'))}"));
			assertEquals("Invalid search() result", testOne.getUuid(), Scripting.replaceVariables(ctx, testTwo, "${first(search('TestOne', 'name', 'A-nice-little-name-for*'))}"));

			// negative test for find()
			assertEquals("Invalid find() result", "", Scripting.replaceVariables(ctx, testTwo, "${first(find('TestOne', 'name', '*little-name-for-my-test-object'))}"));
			assertEquals("Invalid find() result", "", Scripting.replaceVariables(ctx, testTwo, "${first(find('TestOne', 'name', 'A-nice-little-name-for*'))}"));

			// create
			Integer noOfOnes = 1;
			assertEquals("Invalid number of TestOne's", ""+noOfOnes, Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne'))}"));

			// currently the creation of nodes must take place in a node of another type
			Scripting.replaceVariables(ctx, testFour, "${create('TestOne', 'name', 'createTestOne1')}");
			noOfOnes++;
			assertEquals("Invalid number of TestOne's", ""+noOfOnes, Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne'))}"));
			assertEquals("Invalid number of TestOne's", "1", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'name', 'createTestOne1'))}"));

			Scripting.replaceVariables(ctx, testFour, "${create('TestOne', 'name', 'createTestOne1')}");
			noOfOnes++;
			assertEquals("Invalid number of TestOne's", ""+noOfOnes, Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne'))}"));
			assertEquals("Invalid number of TestOne's", "2", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'name', 'createTestOne1'))}"));


			// currently this must be executed on another node type
			Scripting.replaceVariables(ctx, testFour, "${create('TestOne', 'name', 'createTestOne2', 'aCreateString', 'newCreateString1')}");
			noOfOnes++;
			assertEquals("Invalid number of TestOne's", ""+noOfOnes, Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne'))}"));
			assertEquals("Invalid number of TestOne's", "1", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'name', 'createTestOne2'))}"));
			assertEquals("Invalid number of TestOne's", "0", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'aCreateString', 'DOES_NOT_EXIST'))}"));
			assertEquals("Invalid number of TestOne's", "1", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'aCreateString', 'newCreateString1'))}"));
			assertEquals("Invalid number of TestOne's", "0", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'name', 'createTestOne2', 'aCreateString', 'NOT_newCreateString1'))}"));
			assertEquals("Invalid number of TestOne's", "1", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'name', 'createTestOne2', 'aCreateString', 'newCreateString1'))}"));


			// currently this must be executed on another node type
			Scripting.replaceVariables(ctx, testFour, "${create('TestOne', 'name', 'createTestOne2', 'aCreateInt', '256')}");
			noOfOnes++;
			assertEquals("Invalid number of TestOne's", ""+noOfOnes, Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne'))}"));
			assertEquals("Invalid number of TestOne's", "2", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'name', 'createTestOne2'))}"));
			assertEquals("Invalid number of TestOne's", "1", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'aCreateInt', '256'))}"));
			assertEquals("Invalid number of TestOne's", "0", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'name', 'createTestOne2', 'aCreateInt', '255'))}"));
			assertEquals("Invalid number of TestOne's", "1", Scripting.replaceVariables(ctx, testOne, "${size(find('TestOne', 'name', 'createTestOne2', 'aCreateInt', '256'))}"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail(fex.getMessage());
		}
	}

	@Test
	public void testSystemProperties () {
		try {

			final TestUser user  = createTestNode(TestUser.class);

			// create new node
			TestOne t1 = createTestNode(TestOne.class, user);

			final SecurityContext userContext     = SecurityContext.getInstance(user, AccessMode.Frontend);
			final App userApp                     = StructrApp.getInstance(userContext);

			try (final Tx tx = userApp.tx()) {

				final ActionContext userActionContext = new ActionContext(userContext, null);

				assertEquals("node should be of type TestOne", "TestOne", Scripting.replaceVariables(userActionContext, t1, "${(get(this, 'type'))}"));

				try {

					assertEquals("setting the type should fail", "TestTwo", Scripting.replaceVariables(userActionContext, t1, "${(set(this, 'type', 'TestThree'), get(this, 'type'))}"));
					fail("setting a system property should fail");

				} catch (FrameworkException fx) { }

				assertEquals("setting the type should work after setting it with unlock_system_properties_once", "TestFour", Scripting.replaceVariables(userActionContext, t1, "${(unlock_system_properties_once(this), set(this, 'type', 'TestFour'), get(this, 'type'))}"));

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}
	}

	@Test
	public void testFunctionRollbackOnError () {

		final ActionContext ctx = new ActionContext(securityContext, null);

		/**
		 * first the old scripting style
		 */
		TestOne testNodeOldScripting = null;

		try (final Tx tx = app.tx()) {

			testNodeOldScripting = createTestNode(TestOne.class);
			testNodeOldScripting.setProperty(TestOne.aString, "InitialString");
			testNodeOldScripting.setProperty(TestOne.anInt, 42);

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

		try (final Tx tx = app.tx()) {

			Scripting.replaceVariables(ctx, testNodeOldScripting, "${ ( set(this, 'aString', 'NewString'), set(this, 'anInt', 'NOT_AN_INTEGER') ) }");
			fail("StructrScript: setting anInt to 'NOT_AN_INTEGER' should cause an Exception");

			tx.success();

		} catch (FrameworkException expected) { }


		try {

			try (final Tx tx = app.tx()) {

				assertEquals("StructrScript: String should still have initial value!", "InitialString", Scripting.replaceVariables(ctx, testNodeOldScripting, "${(get(this, 'aString'))}"));

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}


		/**
		 * then the JS-style scripting
		 */
		TestOne testNodeJavaScript = null;

		try (final Tx tx = app.tx()) {

			testNodeJavaScript = createTestNode(TestOne.class);
			testNodeJavaScript.setProperty(TestOne.aString, "InitialString");
			testNodeJavaScript.setProperty(TestOne.anInt, 42);

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

		try (final Tx tx = app.tx()) {

			Scripting.replaceVariables(ctx, testNodeJavaScript, "${{ var t1 = Structr.get('this'); t1.aString = 'NewString'; t1.anInt = 'NOT_AN_INTEGER'; }}");
			fail("StructrScript: setting anInt to 'NOT_AN_INTEGER' should cause an Exception");

			tx.success();

		} catch (FrameworkException expected) { }


		try {

			try (final Tx tx = app.tx()) {

				assertEquals("JavaScript: String should still have initial value!", "InitialString", Scripting.replaceVariables(ctx, testNodeJavaScript, "${{ var t1 = Structr.get('this'); Structr.print(t1.aString); }}"));

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}
	}

	@Test
	public void testPrivilegedFind () {

		final ActionContext ctx = new ActionContext(securityContext, null);

		TestOne testNode = null;
                String uuid ="";

		try (final Tx tx = app.tx()) {

			testNode = createTestNode(TestOne.class);
			testNode.setProperty(TestOne.aString, "InitialString");
			testNode.setProperty(TestOne.anInt, 42);
                        uuid = testNode.getProperty(new StringProperty("id"));

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

		try (final Tx tx = app.tx()) {

                        assertEquals("JavaScript: Trying to find entity with type,key,value!", "InitialString", Scripting.replaceVariables(ctx, testNode, "${{ var t1 = Structr.first(Structr.find_privileged('TestOne','anInt','42')); Structr.print(t1.aString); }}"));

                        assertEquals("JavaScript: Trying to find entity with type,id!", "InitialString", Scripting.replaceVariables(ctx, testNode, "${{ var t1 = Structr.find_privileged('TestOne','"+uuid+"'); Structr.print(t1.aString); }}"));

                        assertEquals("JavaScript: Trying to find entity with type,key,value,key,value!", "InitialString", Scripting.replaceVariables(ctx, testNode, "${{ var t1 = Structr.first(Structr.find_privileged('TestOne','anInt','42','aString','InitialString')); Structr.print(t1.aString); }}"));

			tx.success();

		} catch (FrameworkException ex) {

                        logger.warn("", ex);
                        fail("Unexpected exception");

                }

	}

	@Test
	public void testDateCopy() {

		final Date now                       = new Date();
		final Date futureDate                = new Date(now.getTime() + 600000);
		final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);

			// Copy dates with/without format in StructrScript
			TestOne testOne          = createTestNode(TestOne.class);
			TestThree testThree      = createTestNode(TestThree.class);

			testOne.setProperty(TestOne.aDate, now);
			Scripting.replaceVariables(ctx, testThree, "${set(this, 'aDateWithFormat', get(find('TestOne', '" + testOne.getUuid() + "'), 'aDate'))}");
			assertEquals("Copying a date (with default format) to a date (with custom format) failed [StructrScript]", isoDateFormat.format(testOne.getProperty(TestOne.aDate)), isoDateFormat.format(testThree.getProperty(TestThree.aDateWithFormat)));

			testThree.setProperty(TestThree.aDateWithFormat, futureDate);
			Scripting.replaceVariables(ctx, testOne, "${set(this, 'aDate', get(find('TestThree', '" + testThree.getUuid() + "'), 'aDateWithFormat'))}");
			assertEquals("Copying a date (with custom format) to a date (with default format) failed [StructrScript]", isoDateFormat.format(testOne.getProperty(TestOne.aDate)), isoDateFormat.format(testThree.getProperty(TestThree.aDateWithFormat)));


			// Perform the same tests in JavaScript
			testOne.setProperty(TestOne.aDate, null);
			testThree.setProperty(TestThree.aDateWithFormat, null);

			testOne.setProperty(TestOne.aDate, now);
			Scripting.replaceVariables(ctx, testThree, "${{ var testThree = Structr.this; var testOne = Structr.find('TestOne', '" + testOne.getUuid() + "');  testThree.aDateWithFormat = testOne.aDate; }}");
			assertEquals("Copying a date (with default format) to a date (with custom format) failed [JavaScript]", isoDateFormat.format(testOne.getProperty(TestOne.aDate)), isoDateFormat.format(testThree.getProperty(TestThree.aDateWithFormat)));

			testThree.setProperty(TestThree.aDateWithFormat, futureDate);
			Scripting.replaceVariables(ctx, testOne, "${{ var testOne = Structr.this; var testThree = Structr.find('TestThree', '" + testThree.getUuid() + "');  testOne.aDate = testThree.aDateWithFormat; }}");
			assertEquals("Copying a date (with custom format) to a date (with default format) failed [JavaScript]", isoDateFormat.format(testOne.getProperty(TestOne.aDate)), isoDateFormat.format(testThree.getProperty(TestThree.aDateWithFormat)));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail(fex.getMessage());
		}

	}

	@Test
	public void testDateOutput() {

		final Date now                       = new Date();
		final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);

			// Copy dates with/without format in StructrScript
			TestOne testOne          = createTestNode(TestOne.class);

			testOne.setProperty(TestOne.aDate, now);

			final String expectedDateOutput = isoDateFormat.format(now);
			final String dateOutput1 = Scripting.replaceVariables(ctx, testOne, "${this.aDate}");
			final String dateOutput2 = Scripting.replaceVariables(ctx, testOne, "${print(this.aDate)}");
			final String dateOutput3 = Scripting.replaceVariables(ctx, testOne, "${{Structr.print(Structr.this.aDate)}}");

			assertEquals("${this.aDate} should yield ISO 8601 date format", expectedDateOutput, dateOutput1);
			assertEquals("${print(this.aDate)} should yield ISO 8601 date format", expectedDateOutput, dateOutput2);
			assertEquals("${Structr.print(Structr.this.aDate)} should yield ISO 8601 date format", expectedDateOutput, dateOutput3);


			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail(fex.getMessage());
		}

	}

	@Test
	public void testGeoCoding() {

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);

			final String locationId = Scripting.replaceVariables(ctx, null, "${create('Location')}");

			final GeoCodingResult result = GeoHelper.geocode("", null, null, "Darmstadt", null, "");

			if (result != null) {
				// If geocoding itself fails, the test can not work => ignore

				Double lat = result.getLatitude();
				Double lon = result.getLongitude();

				Scripting.replaceVariables(ctx, null, "${set(find('Location', '" + locationId + "'), geocode('Darmstadt', '', ''))}");

				assertEquals("Latitude should be identical", lat.toString(), Scripting.replaceVariables(ctx, null, "${get(find('Location', '" + locationId + "'), 'latitude')}"));
				assertEquals("Longitude should be identical", lon.toString(), Scripting.replaceVariables(ctx, null, "${get(find('Location', '" + locationId + "'), 'longitude')}"));

			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);

			fail(fex.getMessage());
		}

	}
}
