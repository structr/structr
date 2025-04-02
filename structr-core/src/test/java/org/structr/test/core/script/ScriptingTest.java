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
package org.structr.test.core.script;

import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.asciidoctor.internal.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.*;
import org.structr.api.util.Iterables;
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.function.DateFormatFunction;
import org.structr.core.function.FindFunction;
import org.structr.core.function.FunctionInfoFunction;
import org.structr.core.function.NumberFormatFunction;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.ScriptTestHelper;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.AbstractSchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.GroupTraitDefinition;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaRelationshipNodeTraitDefinition;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.structr.web.entity.User;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.testng.AssertJUnit.*;


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
		String sourceType               = null;
		String targetType               = null;
		PropertyKey targetsProperty     = null;
		PropertyKey testEnumProperty    = null;
		PropertyKey testBooleanProperty = null;
		PropertyKey testIntegerProperty = null;
		PropertyKey testStringProperty  = null;
		PropertyKey testDoubleProperty  = null;
		PropertyKey testDateProperty    = null;

		// setup phase: create schema nodes
		try (final Tx tx = app.tx()) {

			// create two nodes and associate them with each other
			final NodeInterface sourceNode      = createTestNode(StructrTraits.SCHEMA_NODE, "TestSource");
			final NodeInterface targetNode      = createTestNode(StructrTraits.SCHEMA_NODE, "TestTarget");
			final PropertyKey<String> typeKey   = Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY);
			final PropertyKey<String> formatKey = Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.FORMAT_PROPERTY);
			final PropertyKey<String> sourceKey = Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY);

			final List<NodeInterface> properties = new LinkedList<>();
			properties.add(createTestNode(StructrTraits.SCHEMA_PROPERTY, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testBoolean"), new NodeAttribute(typeKey, "Boolean")));
			properties.add(createTestNode(StructrTraits.SCHEMA_PROPERTY, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testInteger"), new NodeAttribute(typeKey, "Integer")));
			properties.add(createTestNode(StructrTraits.SCHEMA_PROPERTY, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testString"), new NodeAttribute(typeKey, "String")));
			properties.add(createTestNode(StructrTraits.SCHEMA_PROPERTY, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testDouble"), new NodeAttribute(typeKey, "Double")));
			properties.add(createTestNode(StructrTraits.SCHEMA_PROPERTY, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testEnum"), new NodeAttribute(typeKey, "Enum"), new NodeAttribute(formatKey, "OPEN, CLOSED, TEST")));
			properties.add(createTestNode(StructrTraits.SCHEMA_PROPERTY, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testDate"), new NodeAttribute(typeKey, "Date")));
			sourceNode.setProperty(Traits.of(StructrTraits.SCHEMA_NODE).key(AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY), properties);

			final List<NodeInterface> methods = new LinkedList<>();
			methods.add(createTestNode(StructrTraits.SCHEMA_METHOD, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "onCreate"), new NodeAttribute(sourceKey, "{ var e = Structr.get('this'); e.testtargets = Structr.find('TestTarget'); }")));
			methods.add(createTestNode(StructrTraits.SCHEMA_METHOD, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "doTest01"), new NodeAttribute(sourceKey, "{ var e = Structr.get('this'); e.testEnum = 'OPEN'; }")));
			methods.add(createTestNode(StructrTraits.SCHEMA_METHOD, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "doTest02"), new NodeAttribute(sourceKey, "{ var e = Structr.get('this'); e.testEnum = 'CLOSED'; }")));
			methods.add(createTestNode(StructrTraits.SCHEMA_METHOD, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "doTest03"), new NodeAttribute(sourceKey, "{ var e = Structr.get('this'); e.testEnum = 'TEST'; }")));
			methods.add(createTestNode(StructrTraits.SCHEMA_METHOD, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "doTest04"), new NodeAttribute(sourceKey, "{ var e = Structr.get('this'); e.testEnum = 'INVALID'; }")));
			methods.add(createTestNode(StructrTraits.SCHEMA_METHOD, new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "doTest05"), new NodeAttribute(sourceKey, "{ var e = Structr.get('this'); e.testBoolean = true; e.testInteger = 123; e.testString = 'testing..'; e.testDouble = 1.2345; e.testDate = new Date(" + currentTimeMillis + "); }")));
			sourceNode.setProperty(Traits.of(StructrTraits.SCHEMA_NODE).key(AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY), methods);

			final PropertyMap propertyMap = new PropertyMap();
			final Traits traits           = Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE);

			propertyMap.put(traits.key(RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY),       sourceNode.getUuid());
			propertyMap.put(traits.key(RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY),       targetNode.getUuid());
			propertyMap.put(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_JSON_NAME_PROPERTY), "testsource");
			propertyMap.put(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_JSON_NAME_PROPERTY), "testtargets");
			propertyMap.put(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY), "*");
			propertyMap.put(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY), "*");
			propertyMap.put(traits.key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY), "HAS");

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE, propertyMap);

			tx.success();


		} catch(FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			sourceType          = "TestSource";
			targetType          = "TestTarget";
			targetsProperty     = Traits.of(sourceType).key("testtargets");
			testEnumProperty    = Traits.of(sourceType).key("testEnum");
			testBooleanProperty = Traits.of(sourceType).key("testBoolean");
			testIntegerProperty = Traits.of(sourceType).key("testInteger");
			testStringProperty  = Traits.of(sourceType).key("testString");
			testDoubleProperty  = Traits.of(sourceType).key("testDouble");
			testDateProperty    = Traits.of(sourceType).key("testDate");

			assertNotNull(sourceType);
			assertNotNull(targetType);
			assertNotNull(targetsProperty);

			// create 5 target nodes
			createTestNodes(targetType, 5);

			// create source node
			createTestNodes(sourceType, 5);

			tx.success();


		} catch(FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}


		// check phase: source node should have all five target nodes associated with HAS
		try (final Tx tx = app.tx()) {

			// check all source nodes
			for (final Object obj : app.nodeQuery(sourceType).getAsList()) {

				assertNotNull("Invalid nodeQuery result", obj);

				final NodeInterface sourceNode = (NodeInterface)obj;

				// test contents of "targets" property
				final Object targetNodesObject = sourceNode.getProperty(targetsProperty);
				assertTrue("Invalid getProperty result for scripted association", targetNodesObject instanceof Iterable);

				final Iterable iterable = (Iterable)targetNodesObject;
				assertEquals("Invalid getProperty result for scripted association", 5, Iterables.count(iterable));
			}

			final NodeInterface sourceNode = app.nodeQuery(sourceType).getFirst();
			final EvaluationHints hints   = new EvaluationHints();

			// set testEnum property to OPEN via doTest01 function call, check result
			invokeMethod(securityContext, sourceNode, "doTest01", Collections.EMPTY_MAP, true, hints);
			assertEquals("Invalid setProperty result for EnumProperty", "OPEN", sourceNode.getProperty(testEnumProperty));

			// set testEnum property to CLOSED via doTest02 function call, check result
			invokeMethod(securityContext, sourceNode, "doTest02", Collections.EMPTY_MAP, true, hints);
			assertEquals("Invalid setProperty result for EnumProperty", "CLOSED", sourceNode.getProperty(testEnumProperty));

			// set testEnum property to TEST via doTest03 function call, check result
			invokeMethod(securityContext, sourceNode, "doTest03", Collections.EMPTY_MAP, true, hints);
			assertEquals("Invalid setProperty result for EnumProperty", "TEST", sourceNode.getProperty(testEnumProperty));

			// set testEnum property to INVALID via doTest03 function call, expect previous value & error
			try {
				invokeMethod(securityContext, sourceNode, "doTest04", Collections.EMPTY_MAP, true, hints);
				assertEquals("Invalid setProperty result for EnumProperty", "TEST", sourceNode.getProperty(testEnumProperty));
				fail("Setting EnumProperty to invalid value should result in an Exception!");

			} catch (FrameworkException fx) {}

			// test other property types
			invokeMethod(securityContext, sourceNode, "doTest05", Collections.EMPTY_MAP, true, hints);
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

	@Test
	public void testGrantViaScripting() {

		// setup phase: create schema nodes
		try (final Tx tx = app.tx()) {

			// create two nodes and associate them with each other
			final NodeInterface sourceNode  = createTestNode(StructrTraits.SCHEMA_NODE, "TestSource");
			final NodeInterface method      = createTestNode(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "doTest01"),
					new NodeAttribute(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ var e = Structr.get('this'); e.grant(Structr.find('Principal')[0], 'read', 'write'); }")
			);

			sourceNode.setProperty(Traits.of(StructrTraits.SCHEMA_NODE).key(AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY), List.of(method));

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String sourceType            = "TestSource";
		NodeInterface testUser             = null;

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

			testUser = app.create(StructrTraits.USER,
					new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "test"),
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "test")
			);

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		final App userApp = StructrApp.getInstance(SecurityContext.getInstance(testUser.as(User.class), AccessMode.Backend));

		// first test without grant, expect no test object to be found using the user context
		try (final Tx tx = userApp.tx()) { assertEquals("Invalid grant() scripting result", 0, userApp.nodeQuery(sourceType).getAsList().size()); tx.success(); } catch(FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// grant read access to test user
		try (final Tx tx = app.tx()) {

			final NodeInterface node    = app.nodeQuery(sourceType).getFirst();
			final AbstractMethod method = Methods.resolveMethod(node.getTraits(), "doTest01");
			if (method != null) {

				method.execute(securityContext, node, new Arguments(), new EvaluationHints());
			}

			tx.success();

		} catch(FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// first test without grant, expect no test object to be found using the user context
		try (final Tx tx = userApp.tx()) {

			assertEquals("Invalid grant() scripting result", 1, userApp.nodeQuery(sourceType).getAsList().size()); tx.success(); } catch(FrameworkException fex) {
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

		List<NodeInterface> testSixs = null;
		NodeInterface testOne1       = null;
		NodeInterface testOne2       = null;
		NodeInterface testTwo1       = null;
		NodeInterface testTwo2       = null;
		NodeInterface testThree1     = null;
		NodeInterface testThree2     = null;
		NodeInterface testFour1      = null;
		NodeInterface testFour2      = null;
		Date date1                   = null;
		Date date2                   = null;

		// setup phase
		try (final Tx tx = app.tx()) {

			testSixs             = createTestNodes("TestSix", 10);
			testOne1             = app.create("TestOne");
			testOne2             = app.create("TestOne");
			testTwo1             = app.create("TestTwo");
			testTwo2             = app.create("TestTwo");
			testThree1           = app.create("TestThree");
			testThree2           = app.create("TestThree");
			testFour1            = app.create("TestFour");
			testFour2            = app.create("TestFour");
			date1                = new Date(random.nextLong());
			date2                = new Date();

			testOne1.setProperty(Traits.of("TestOne").key("anInt")             , 42);
			testOne1.setProperty(Traits.of("TestOne").key("aLong")             , long1);
			testOne1.setProperty(Traits.of("TestOne").key("aDouble")           , double1);
			testOne1.setProperty(Traits.of("TestOne").key("aDate")             , date1);
			testOne1.setProperty(Traits.of("TestOne").key("anEnum")            , "One");
			testOne1.setProperty(Traits.of("TestOne").key("aString")           , "aString1");
			testOne1.setProperty(Traits.of("TestOne").key("aBoolean")          , true);
			testOne1.setProperty(Traits.of("TestOne").key("testTwo")           , testTwo1);
			testOne1.setProperty(Traits.of("TestOne").key("testThree")         , testThree1);
			testOne1.setProperty(Traits.of("TestOne").key("testFour")          , testFour1);
			testOne1.setProperty(Traits.of("TestOne").key("manyToManyTestSixs"), testSixs.subList(0, 5));

			testOne2.setProperty(Traits.of("TestOne").key("anInt")             , 33);
			testOne2.setProperty(Traits.of("TestOne").key("aLong")             , long2);
			testOne2.setProperty(Traits.of("TestOne").key("aDouble")           , double2);
			testOne2.setProperty(Traits.of("TestOne").key("aDate")             , date2);
			testOne2.setProperty(Traits.of("TestOne").key("anEnum")            , "Two");
			testOne2.setProperty(Traits.of("TestOne").key("aString")           , "aString2");
			testOne2.setProperty(Traits.of("TestOne").key("aBoolean")          , false);
			testOne2.setProperty(Traits.of("TestOne").key("testTwo")           , testTwo2);
			testOne2.setProperty(Traits.of("TestOne").key("testThree")         , testThree2);
			testOne2.setProperty(Traits.of("TestOne").key("testFour")          , testFour2);
			testOne2.setProperty(Traits.of("TestOne").key("manyToManyTestSixs"), testSixs.subList(5, 10));

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test phase, find all the things using scripting
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { anInt: 42 })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { anInt: 33 })[0]; }}", "test"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { aLong: " + long1 + " })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { aLong: " + long2 + " })[0]; }}", "test"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { aDouble: " + double1 + " })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { aDouble: " + double2 + " })[0]; }}", "test"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { anEnum: 'One' })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { anEnum: 'Two' })[0]; }}", "test"));

			assertEquals("Invalid scripted find() result", testOne1, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { aBoolean: true })[0]; }}", "test"));
			assertEquals("Invalid scripted find() result", testOne2, Scripting.evaluate(actionContext, testOne1, "${{ Structr.find('TestOne', { aBoolean: false })[0]; }}", "test"));


			tx.success();

		} catch(UnlicensedScriptException |FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testWrappingUnwrapping() {

		// setup phase
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);
			final NodeInterface context       = app.create("TestOne");

			Scripting.evaluate(actionContext, context, "${{ Structr.create('Group', { name: 'Group1' } ); }}", "test");
			Scripting.evaluate(actionContext, context, "${{ Structr.create('Group', 'name', 'Group2'); }}", "test");

			assertEquals("Invalid unwrapping result", 2, app.nodeQuery(StructrTraits.GROUP).getAsList().size());


			tx.success();

		} catch(UnlicensedScriptException |FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testEnumPropertyGet() {

		// setup phase
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);
			final NodeInterface context       = app.create("TestOne");

			Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); e.anEnum = 'One'; }}", "test");

			assertEquals("Invalid enum get result", "One", Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); e.anEnum; }}", "test"));

			assertEquals("Invalid Javascript enum comparison result", true, Scripting.evaluate(actionContext, context, "${{ var e = Structr.get('this'); e.anEnum == 'One'; }}", "test"));

			tx.success();

		} catch(UnlicensedScriptException |FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testCollectionOperations() {

		final PropertyKey<Iterable<NodeInterface>> members = Traits.of(StructrTraits.GROUP).key(GroupTraitDefinition.MEMBERS_PROPERTY);
		Group group                                        = null;
		Principal user1                                    = null;
		Principal user2                                    = null;
		NodeInterface testOne                              = null;

		// setup phase
		try (final Tx tx = app.tx()) {

			group = app.create(StructrTraits.GROUP, StructrTraits.GROUP).as(Group.class);
			user1  = app.create(StructrTraits.USER, "Tester1").as(Principal.class);
			user2  = app.create(StructrTraits.USER, "Tester2").as(Principal.class);

			group.setProperty(members, List.of(user1));


			testOne = app.create("TestOne");
			createTestNodes("TestSix", 10);

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test phase, find all the things using scripting
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			final Object result1 = Scripting.evaluate(actionContext, group, "${{ Structr.find('Principal', { name: 'Tester2' })[0]; }}", "test");

			System.out.println(result1);

			// test prerequisites
			assertEquals("Invalid prerequisite", 1, Iterables.count(group.getProperty(members)));
			assertEquals("Invalid prerequisite", user2, result1);

			// test scripting association
			Scripting.evaluate(actionContext, group, "${{ var group = Structr.find('Group')[0]; var users = group.members; users.push(Structr.find('Principal', { name: 'Tester2' })[0]); }}", "test");
			assertEquals("Invalid scripted array operation result", 2, Iterables.count(group.getProperty(members)));

			// reset group
			group.setProperty(members, List.of(user1));

			// test prerequisites
			assertEquals("Invalid prerequisite",     1, Iterables.count(group.getProperty(members)));

			// test direct push on member property
			Scripting.evaluate(actionContext, group, "${{ var group = Structr.find('Group')[0]; group.members.push(Structr.find('Principal', { name: 'Tester2' })[0]); }}", "test");
			assertEquals("Invalid scripted array operation result", 2, Iterables.count(group.getProperty(members)));



			// test scripting association
			Scripting.evaluate(actionContext, group, "${{ var test = Structr.find('TestOne')[0]; var testSixs = test.manyToManyTestSixs; testSixs.push(Structr.find('TestSix')[0]); }}", "test");
			assertEquals("Invalid scripted array operation result", 1, Iterables.count(testOne.getProperty(Traits.of("TestOne").key("manyToManyTestSixs"))));

			// test direct push on member property
			Scripting.evaluate(actionContext, group, "${{ var test = Structr.find('TestOne')[0]; var testSixs = test.manyToManyTestSixs.push(Structr.find('TestSix')[1]); }}", "test");
			assertEquals("Invalid scripted array operation result", 2, Iterables.count(testOne.getProperty(Traits.of("TestOne").key("manyToManyTestSixs"))));


			tx.success();

		} catch(UnlicensedScriptException |FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testPropertyConversion() {

		NodeInterface testOne = null;

		// setup phase
		try (final Tx tx = app.tx()) {

			testOne = app.create("TestOne");

			tx.success();

		} catch(FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test phase, check value conversion
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aString = 12; }}", "test");
			assertEquals("Invalid scripted property conversion result", "12", testOne.getProperty(Traits.of("TestOne").key("aString")));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.anInt = '12'; }}", "test");
			assertEquals("Invalid scripted property conversion result", (int)12, (int)testOne.getProperty(Traits.of("TestOne").key("anInt")));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aDouble = '12.2342'; }}", "test");
			assertEquals("Invalid scripted property conversion result", 12.2342, testOne.getProperty(Traits.of("TestOne").key("aDouble")), 0.0);

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aDouble = 2; }}", "test");
			assertEquals("Invalid scripted property conversion result", 2.0, testOne.getProperty(Traits.of("TestOne").key("aDouble")), 0.0);

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aLong = 2352343457252; }}", "test");
			assertEquals("Invalid scripted property conversion result", 2352343457252L, (long)testOne.getProperty(Traits.of("TestOne").key("aLong")));

			Scripting.evaluate(actionContext, testOne, "${{ var e = Structr.get('this'); e.aBoolean = true; }}", "test");
			assertEquals("Invalid scripted property conversion result", true, (boolean)testOne.getProperty(Traits.of("TestOne").key("aBoolean")));

			tx.success();

		} catch(UnlicensedScriptException |FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testQuotes() {

		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			Scripting.evaluate(actionContext, app.create("TestOne"), "${{\n // \"test\n}}", "test");

			tx.success();

		} catch(UnlicensedScriptException |FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

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
		final List<String> testSixNames   = new LinkedList<>();
		NodeInterface template            = null;
		NodeInterface template2           = null;
		NodeInterface testOne             = null;
		NodeInterface testTwo             = null;
		NodeInterface testThree           = null;
		NodeInterface testFour            = null;
		List<NodeInterface> testSixs      = null;
		int index                         = 0;

		try (final Tx tx = app.tx()) {

			testOne        = createTestNode("TestOne");
			testTwo        = createTestNode("TestTwo");
			testThree      = createTestNode("TestThree");
			testFour       = createTestNode("TestFour");
			testSixs       = createTestNodes("TestSix", 20, 1);

			// set string array on test four
			testFour.setProperty(Traits.of("TestFour").key("stringArrayProperty"), new String[] { "one", "two", "three", "four" } );

			final Calendar cal = GregorianCalendar.getInstance();

			// set calendar to 2018-01-01T00:00:00+0000
			cal.set(2018, 0, 1, 0, 0, 0);
			cal.set(Calendar.MILLISECOND, 0);

			for (final NodeInterface testSix : testSixs) {

				final String name = "TestSix" + StringUtils.leftPad(Integer.toString(index), 2, "0");

				testSix.setProperty(Traits.of("TestSix").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);
				testSix.setProperty(Traits.of("TestSix").key("index"), index);
				testSix.setProperty(Traits.of("TestSix").key("date"), cal.getTime());

				index++;
				cal.add(Calendar.DAY_OF_YEAR, 3);

				// build list of names
				testSixNames.add(name);
			}

			// create mail template
			template = createTestNode(StructrTraits.MAIL_TEMPLATE);
			template.setProperty(getKey(StructrTraits.MAIL_TEMPLATE, NodeInterfaceTraitDefinition.NAME_PROPERTY), "TEST");
			template.setProperty(getKey(StructrTraits.MAIL_TEMPLATE, MailTemplateTraitDefinition.LOCALE_PROPERTY), "en_EN");
			template.setProperty(getKey(StructrTraits.MAIL_TEMPLATE, MailTemplateTraitDefinition.TEXT_PROPERTY), "This is a template for ${this.name}");

			// create mail template
			template2 = createTestNode(StructrTraits.MAIL_TEMPLATE);
			template2.setProperty(getKey(StructrTraits.MAIL_TEMPLATE, NodeInterfaceTraitDefinition.NAME_PROPERTY), "TEST2");
			template2.setProperty(getKey(StructrTraits.MAIL_TEMPLATE, MailTemplateTraitDefinition.LOCALE_PROPERTY), "en_EN");
			template2.setProperty(getKey(StructrTraits.MAIL_TEMPLATE, MailTemplateTraitDefinition.TEXT_PROPERTY), "${this.aDouble}");

			// check existance
			assertNotNull(testOne);

			testOne.setProperty(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "A-nice-little-name-for-my-test-object");
			testOne.setProperty(Traits.of("TestOne").key("anInt"), 1);
			testOne.setProperty(Traits.of("TestOne").key("aString"), "String");
			testOne.setProperty(Traits.of("TestOne").key("anotherString"), "{\n\ttest: test,\n\tnum: 3\n}");
			testOne.setProperty(Traits.of("TestOne").key("replaceString"), "${this.name}");
			testOne.setProperty(Traits.of("TestOne").key("aLong"), 235242522552L);
			testOne.setProperty(Traits.of("TestOne").key("aDouble"), 2.234);
			testOne.setProperty(Traits.of("TestOne").key("aDate"), now);
			testOne.setProperty(Traits.of("TestOne").key("anEnum"), "One");
			testOne.setProperty(Traits.of("TestOne").key("aBoolean"), true);
			testOne.setProperty(Traits.of("TestOne").key("testTwo"), testTwo);
			testOne.setProperty(Traits.of("TestOne").key("testThree"), testThree);
			testOne.setProperty(Traits.of("TestOne").key("testFour"),  testFour);
			testOne.setProperty(Traits.of("TestOne").key("manyToManyTestSixs"), testSixs);
			testOne.setProperty(Traits.of("TestOne").key("cleanTestString"), "a<b>c.d'e?f(g)h{i}j[k]l+m/n–o\\p\\q|r's!t,u-v_w`x-y-zöäüßABCDEFGH");
			testOne.setProperty(Traits.of("TestOne").key("stringWithQuotes"), "A'B\"C");
			testOne.setProperty(Traits.of("TestOne").key("aStringArray"), new String[] { "a", "b", "c" });

			testTwo.setProperty(Traits.of("TestTwo").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testTwo_name");
			testThree.setProperty(Traits.of("TestThree").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testThree_name");

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

			assertEquals("Invalid size result", "", Scripting.replaceVariables(ctx, testOne, "${(this.alwaysNull.size}"));
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

			assertEquals("Invalid complement() result", "[two, two]", Scripting.replaceVariables(ctx, testOne, "${complement(merge('one', 'two', 'three', 'two'), merge('one', 'four', 'three'))}"));
			assertEquals("Invalid complement() result", "[one]", Scripting.replaceVariables(ctx, testOne, "${complement(merge('one', 'two', 'three', 'two'), merge('two', 'four', 'three'))}"));
			assertEquals("Invalid complement() result", "[one, three]", Scripting.replaceVariables(ctx, testOne, "${complement(merge('one', 'two', 'three', 'two'), 'two')}"));

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

			// starts_with
			assertEquals("Invalid starts_with() result", "true", Scripting.replaceVariables(ctx, testOne, "${starts_with(null, null)}"));
			assertEquals("Invalid starts_with() result", "false", Scripting.replaceVariables(ctx, testOne, "${starts_with(null, 'abc')}"));
			assertEquals("Invalid starts_with() result", "false", Scripting.replaceVariables(ctx, testOne, "${starts_with('abcdef', null)}"));
			assertEquals("Invalid starts_with() result", "true", Scripting.replaceVariables(ctx, testOne, "${starts_with('abcdef', 'abc')}"));
			assertEquals("Invalid starts_with() result", "false", Scripting.replaceVariables(ctx, testOne, "${starts_with('ABCDEF', 'abc')}"));
			assertEquals("Invalid starts_with() result", "true", Scripting.replaceVariables(ctx, testOne, "${starts_with(merge('a', 'b'), 'a')}"));
			assertEquals("Invalid starts_with() result", "false", Scripting.replaceVariables(ctx, testOne, "${starts_with(merge('c', 'a', 'b'), 'a')}"));
			assertEquals("Invalid starts_with() result", "false", Scripting.replaceVariables(ctx, testOne, "${starts_with(merge('abc', 'b'), 'a')}"));

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
			assertEquals("Invalid substring() result", "y-short", Scripting.replaceVariables(ctx, testOne, "${substring('very-short', 3, 200)}"));

			// length
			assertEquals("Invalid length() result", "37", Scripting.replaceVariables(ctx, testOne, "${length(this.name)}"));
			assertEquals("Invalid length() result", "37", Scripting.replaceVariables(ctx, testOne, "${length('a-nice-little-name-for-my-test-object')}"));
			assertEquals("Invalid length() result", "4", Scripting.replaceVariables(ctx, testOne, "${length('test')}"));
			assertEquals("Invalid length() result", "", Scripting.replaceVariables(ctx, testOne, "${length(this.alwaysNull)}"));

			// clean ("a<b>c.d'e?f(g)h{i}j[k]l+m/n–o\\p\\q|r's!t,u-v_w`x-y-zöäüßABCDEFGH")
			assertEquals("Invalid clean() result", "abcd-efghijkl-m-n-o-p-q-r-stu-v-w-x-y-zoeaeuessabcdefgh", Scripting.replaceVariables(ctx, testOne, "${clean(this.cleanTestString)}"));
			assertEquals("Invalid clean() result", "abcd-efghijkl-m-n-o-p-q-r-stu-v-w-x-y-zoeaeuessabcdefgh", Scripting.replaceVariables(ctx, testOne, "${clean(get(this, \"cleanTestString\"))}"));
			assertEquals("Invalid clean() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${clean(this.alwaysNull)}"));

			// trim
			assertEquals("Invalid trim() result", "test", Scripting.replaceVariables(ctx, testOne, "${trim('   \t\t\t\r\r\r\n\n\ntest')}"));
			assertEquals("Invalid trim() result", "test", Scripting.replaceVariables(ctx, testOne, "${trim('test   \t\t\t\r\r\r\n\n\n')}"));
			assertEquals("Invalid trim() result", "test", Scripting.replaceVariables(ctx, testOne, "${trim('   \t\t\t\r\r\r\n\n\ntest   \t\t\t\r\r\r\n\n\n')}"));
			assertEquals("Invalid trim() result", "test", Scripting.replaceVariables(ctx, testOne, "${trim('   test   ')}"));
			assertEquals("Invalid trim() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${trim(null)}"));
			assertEquals("Invalid trim() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${trim(this.alwaysNull)}"));

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

			// is
			assertEquals("Invalid is() result", "true",  Scripting.replaceVariables(ctx, testOne,  "${is(\"true\", \"true\")}"));
			assertEquals("Invalid is() result", "",      Scripting.replaceVariables(ctx, testOne,  "${is(\"false\", \"true\")}"));

			// is + equal
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(this.id, this.id), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(\"abc\", \"abc\"), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(3, 3), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(\"3\", \"3\"), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(3.1414, 3.1414), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(\"3.1414\", \"3.1414\"), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(23.44242222243633337234623462, 23.44242222243633337234623462), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(\"23.44242222243633337234623462\", \"23.44242222243633337234623462\"), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(13, 013), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${is(equal(13, \"013\"), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "",      Scripting.replaceVariables(ctx, testOne, "${is(equal(\"13\", \"013\"), \"true\")}"));
			assertEquals("Invalid is(equal()) result", "",      Scripting.replaceVariables(ctx, testOne, "${is(equal(\"13\", \"00013\"), \"true\")}"));

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

			// empty in JavaScript
			assertEquals("Invalid empty() result", "true",  Scripting.replaceVariables(ctx, testOne, "${{$.empty(\"\")}}"));
			assertEquals("Invalid empty() result", "false", Scripting.replaceVariables(ctx, testOne, "${{$.empty(\" \")}}"));
			assertEquals("Invalid empty() result", "false", Scripting.replaceVariables(ctx, testOne, "${{$.empty(\"   \")}}"));
			assertEquals("Invalid empty() result", "false", Scripting.replaceVariables(ctx, testOne, "${{$.empty(\"xyz\")}}"));
			assertEquals("Invalid empty() result", "true",  Scripting.replaceVariables(ctx, testOne, "${{$.empty([])}}"));
			assertEquals("Invalid empty() result", "true",  Scripting.replaceVariables(ctx, testOne, "${{$.empty({})}}"));

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
			//assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(23.4462, 2.34462e1)}"));
			//assertEquals("Invalid if(equal()) result", "true",  Scripting.replaceVariables(ctx, testOne, "${equal(0.00234462, 2.34462e-3)}"));
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

			// tests for eq(string, boolean) - only exact string matches for "true" and "false" should yield "is equal"
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(true, \"true\")}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(\"true\", true)}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(false, \"false\")}"));
			assertEquals("Invalid eq() result", "true",  Scripting.replaceVariables(ctx, testOne, "${eq(\"false\", false)}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(false, \"false \")}"));        // notice the space at the end - input strings are not trimmed!
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(true, \"true \")}"));          // notice the space at the end - input strings are not trimmed!
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(\"false \", false)}"));        // notice the space at the end - input strings are not trimmed!
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(\"true \", true)}"));          // notice the space at the end - input strings are not trimmed!
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(true, \"this is not true\")}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(true, \"anything\")}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(false, \"true\")}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(true, \"false\")}"));
			assertEquals("Invalid eq() result", "false",  Scripting.replaceVariables(ctx, testOne, "${eq(false, \"anything\")}"));

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
			assertEquals("Invalid round() result", "10",               Scripting.replaceVariables(ctx, testOne, "${round(\"10\")}"));
			assertEquals("Invalid round() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${round(this.alwaysNull)}"));
			assertEquals("Invalid round() result with null value", "", Scripting.replaceVariables(ctx, testOne, "${round(this.alwaysNull, this.alwaysNull)}"));

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

			// date_format with locale
			Locale locale = ctx.getLocale();
			assertEquals("Invalid set_locale() result", "", Scripting.replaceVariables(ctx, testOne, "${set_locale('us')}"));
			assertEquals("Invalid date_format() result", "01. Oct 2017", Scripting.replaceVariables(ctx, testOne, "${date_format(parse_date('01.10.2017', 'dd.MM.yyyy'), 'dd. MMM yyyy')}"));
			assertEquals("Invalid set_locale() result", "", Scripting.replaceVariables(ctx, testOne, "${set_locale('de')}"));
			assertEquals("Invalid date_format() result", "01. Okt. 2017", Scripting.replaceVariables(ctx, testOne, "${date_format(parse_date('01.10.2017', 'dd.MM.yyyy'), 'dd. MMM yyyy')}"));
			ctx.setLocale(locale);

			// date_format with null
			assertEquals("Invalid date_format() result with null value", "",                                           Scripting.replaceVariables(ctx, testOne, "${date_format(this.alwaysNull, \"yyyy\")}"));
			assertEquals("Invalid date_format() result with null value", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format(\"10\", this.alwaysNull)}"));
			assertEquals("Invalid date_format() result with null value", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format(this.alwaysNull, this.alwaysNull)}"));

			// date_format error messages
			assertEquals("Invalid date_format() result for wrong number of parameters", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format()}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format(this.aDouble)}"));
			assertEquals("Invalid date_format() result for wrong number of parameters", DateFormatFunction.ERROR_MESSAGE_DATE_FORMAT, Scripting.replaceVariables(ctx, testOne, "${date_format(this.aDouble, this.aDouble, this.aDouble)}"));

			// parse_date
			//assertEquals("Invalid parse_date() result", ParseDateFunction.ERROR_MESSAGE_PARSE_DATE, Scripting.replaceVariables(ctx, testOne, "${parse_date('2015-12-12')}"));
			//assertEquals("Invalid parse_date() result", "2015-12-12T00:00:00+0000", Scripting.replaceVariables(ctx, testOne, "${parse_date('2015-12-12', 'yyyy-MM-dd')}"));
			//assertEquals("Invalid parse_date() result", "2015-12-12T00:00:00+0000", Scripting.replaceVariables(ctx, testOne, "${parse_date('2015-12-12', 'yyyy-MM-dd')}"));
			//assertEquals("Invalid parse_date() result for wrong number of parameters", ParseDateFunction.ERROR_MESSAGE_PARSE_DATE, Scripting.replaceVariables(ctx, testOne, "${date_format(parse_date('2017-09-20T18:23:22+0200'), 'dd. MMM yyyy')}"));

			// to_date
			//assertEquals("Invalid to_date() result", ToDateFunction.ERROR_MESSAGE_TO_DATE, Scripting.replaceVariables(ctx, testOne, "${to_date()}"));
			//assertEquals("Invalid to_date() result", "2016-09-06T22:44:45+0000", Scripting.replaceVariables(ctx, testOne, "${to_date(1473201885000)}"));

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

			// parse_number
			final Locale oldLocale = ctx.getLocale();
			assertEquals("Invalid set_locale() result", "", Scripting.replaceVariables(ctx, testOne, "${set_locale('en')}"));
			assertEquals("Invalid parse_number() result", "123.456", Scripting.replaceVariables(ctx, testOne, "${parse_number('123.456')}"));
			ctx.setLocale(oldLocale);

			assertEquals("Invalid parse_number() result", "123.456", Scripting.replaceVariables(ctx, testOne, "${parse_number('123.456', 'en')}"));
			assertEquals("Invalid parse_number() result", "123.456", Scripting.replaceVariables(ctx, testOne, "${parse_number('123,456', 'de')}"));
			assertEquals("Invalid parse_number() result", "123456", Scripting.replaceVariables(ctx, testOne, "${parse_number('123456', 'de')}"));
			assertEquals("Invalid parse_number() result", "123456", Scripting.replaceVariables(ctx, testOne, "${parse_number('123.456', 'de')}"));
			assertEquals("Invalid parse_number() result", "123456", Scripting.replaceVariables(ctx, testOne, "${parse_number('123.456 €', 'de')}"));
			assertEquals("Invalid parse_number() result", "123456789", Scripting.replaceVariables(ctx, testOne, "${parse_number('£ 123,456,789.00 ', 'en')}"));
			assertEquals("Invalid parse_number() result", "123456789", Scripting.replaceVariables(ctx, testOne, "${parse_number('123,foo456,bar789.00 ', 'en')}"));
			assertEquals("Invalid parse_number() result", "123.456", Scripting.replaceVariables(ctx, testOne, "${parse_number('£ 123,456,789.00 ', 'de')}"));
			assertEquals("Invalid parse_number() result", "123.456", Scripting.replaceVariables(ctx, testOne, "${parse_number('123,foo456,bar789.00 ', 'de')}"));

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
			assertEquals("Invalid nth() result",   testSixs.get(12).toString(), Scripting.replaceVariables(ctx, testOne, "${nth(this.manyToManyTestSixs, 12)}"));
			assertEquals("Invalid nth() result",   "", Scripting.replaceVariables(ctx, testOne, "${nth(this.manyToManyTestSixs, 21)}"));

			// find with range
			assertEquals("Invalid find range result",  4, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   2,    5))}", "range test")).size());
			assertEquals("Invalid find range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   0,   20))}", "range test")).size());
			assertEquals("Invalid find range result", 19, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   1,   20))}", "range test")).size());
			assertEquals("Invalid find range result",  6, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(null,    5))}", "range test")).size());
			assertEquals("Invalid find range result", 12, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   8, null))}", "range test")).size());
			assertEquals("Invalid find range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(null, null))}", "range test")).size());

			// find with range
			assertEquals("Invalid find range result",  3, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   2,    5, true, false))}", "range test")).size());
			assertEquals("Invalid find range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   0,   20, true, false))}", "range test")).size());
			assertEquals("Invalid find range result", 19, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   1,   20, true, false))}", "range test")).size());
			assertEquals("Invalid find range result",  5, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(null,    5, true, false))}", "range test")).size());
			assertEquals("Invalid find range result", 12, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   8, null, true, false))}", "range test")).size());
			assertEquals("Invalid find range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(null, null, true, false))}", "range test")).size());

			// find with range
			assertEquals("Invalid find range result",  3, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   2,    5, false, true))}", "range test")).size());
			assertEquals("Invalid find range result", 19, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   0,   20, false, true))}", "range test")).size());
			assertEquals("Invalid find range result", 18, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   1,   20, false, true))}", "range test")).size());
			assertEquals("Invalid find range result",  6, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(null,    5, false, true))}", "range test")).size());
			assertEquals("Invalid find range result", 11, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   8, null, false, true))}", "range test")).size());
			assertEquals("Invalid find range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(null, null, false, true))}", "range test")).size());

			// find with range
			assertEquals("Invalid find range result",  2, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   2,    5, false, false))}", "range test")).size());
			assertEquals("Invalid find range result", 19, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   0,   20, false, false))}", "range test")).size());
			assertEquals("Invalid find range result", 18, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   1,   20, false, false))}", "range test")).size());
			assertEquals("Invalid find range result",  5, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(null,    5, false, false))}", "range test")).size());
			assertEquals("Invalid find range result", 11, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(   8, null, false, false))}", "range test")).size());
			assertEquals("Invalid find range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', range(null, null, false, false))}", "range test")).size());

			// find with date range
			assertEquals("Invalid find date range result", 11, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('01.01.2018', 'dd.MM.yyyy'), parse_date('01.02.2018', 'dd.MM.yyyy'), true, true))}", "range test")).size());
			assertEquals("Invalid find date range result", 15, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('16.01.2018', 'dd.MM.yyyy'), parse_date('01.04.2018', 'dd.MM.yyyy'), true, true))}", "range test")).size());
			assertEquals("Invalid find date range result",  7, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('16.01.2018', 'dd.MM.yyyy'), parse_date('03.02.2018', 'dd.MM.yyyy'), true, true))}", "range test")).size());
			assertEquals("Invalid find date range result", 12, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(                                  null, parse_date('03.02.2018', 'dd.MM.yyyy'), true, true))}", "range test")).size());
			assertEquals("Invalid find date range result",  8, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('06.02.2018', 'dd.MM.yyyy'),                                   null, true, true))}", "range test")).size());
			assertEquals("Invalid find date range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(                                  null,                                   null, true, true))}", "range test")).size());

			// find with date range
			assertEquals("Invalid find date range result", 11, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('01.01.2018', 'dd.MM.yyyy'), parse_date('01.02.2018', 'dd.MM.yyyy'), true, false))}", "range test")).size());
			assertEquals("Invalid find date range result", 15, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('16.01.2018', 'dd.MM.yyyy'), parse_date('01.04.2018', 'dd.MM.yyyy'), true, false))}", "range test")).size());
			assertEquals("Invalid find date range result",  6, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('16.01.2018', 'dd.MM.yyyy'), parse_date('03.02.2018', 'dd.MM.yyyy'), true, false))}", "range test")).size());
			assertEquals("Invalid find date range result", 11, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(                                  null, parse_date('03.02.2018', 'dd.MM.yyyy'), true, false))}", "range test")).size());
			assertEquals("Invalid find date range result",  8, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('06.02.2018', 'dd.MM.yyyy'),                                   null, true, false))}", "range test")).size());
			assertEquals("Invalid find date range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(                                  null,                                   null, true, false))}", "range test")).size());

			// find with date range
			assertEquals("Invalid find date range result", 10, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('01.01.2018', 'dd.MM.yyyy'), parse_date('01.02.2018', 'dd.MM.yyyy'), false, true))}", "range test")).size());
			assertEquals("Invalid find date range result", 14, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('16.01.2018', 'dd.MM.yyyy'), parse_date('01.04.2018', 'dd.MM.yyyy'), false, true))}", "range test")).size());
			assertEquals("Invalid find date range result",  6, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('16.01.2018', 'dd.MM.yyyy'), parse_date('03.02.2018', 'dd.MM.yyyy'), false, true))}", "range test")).size());
			assertEquals("Invalid find date range result", 12, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(                                  null, parse_date('03.02.2018', 'dd.MM.yyyy'), false, true))}", "range test")).size());
			assertEquals("Invalid find date range result",  7, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('06.02.2018', 'dd.MM.yyyy'),                                   null, false, true))}", "range test")).size());
			assertEquals("Invalid find date range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(                                  null,                                   null, false, true))}", "range test")).size());

			// find with date range
			assertEquals("Invalid find date range result", 10, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('01.01.2018', 'dd.MM.yyyy'), parse_date('01.02.2018', 'dd.MM.yyyy'), false, false))}", "range test")).size());
			assertEquals("Invalid find date range result", 14, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('16.01.2018', 'dd.MM.yyyy'), parse_date('01.04.2018', 'dd.MM.yyyy'), false, false))}", "range test")).size());
			assertEquals("Invalid find date range result",  5, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('16.01.2018', 'dd.MM.yyyy'), parse_date('03.02.2018', 'dd.MM.yyyy'), false, false))}", "range test")).size());
			assertEquals("Invalid find date range result", 11, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(                                  null, parse_date('03.02.2018', 'dd.MM.yyyy'), false, false))}", "range test")).size());
			assertEquals("Invalid find date range result",  7, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(parse_date('06.02.2018', 'dd.MM.yyyy'),                                   null, false, false))}", "range test")).size());
			assertEquals("Invalid find date range result", 20, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', range(                                  null,                                   null, false, false))}", "range test")).size());

			// find with lt,lte,gte,gt
			assertEquals("Invalid find lt result",    5, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index',  lt(5))}", "find lt test")).size());
			assertEquals("Invalid find lte result",   6, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', lte(5))}", "find lte test")).size());
			assertEquals("Invalid find gte result",  15, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index', gte(5))}", "find gte test")).size());
			assertEquals("Invalid find gt result",   14, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'index',  gt(5))}", "find gt test")).size());

			// find with lt,lte,gte,gt (date range)
			// starts with 01.01.2018... +3 per TestSix. second object has date "04.01.2018 00:00:00"
			assertEquals("Invalid find lt date result",    1, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date',  lt(parse_date('04.01.2018', 'dd.MM.yyyy')))}", "find lt date test")).size());
			assertEquals("Invalid find lte date result",   2, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', lte(parse_date('04.01.2018', 'dd.MM.yyyy')))}", "find lte date test")).size());
			assertEquals("Invalid find gte date result",  19, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date', gte(parse_date('04.01.2018', 'dd.MM.yyyy')))}", "find gte date test")).size());
			assertEquals("Invalid find gt date result",   18, ((List)Scripting.evaluate(ctx, testOne, "${find('TestSix', 'date',  gt(parse_date('04.01.2018', 'dd.MM.yyyy')))}", "find gt date test")).size());

			// slice with find
			final List sliceResult2 = (List)Scripting.evaluate(ctx, testOne, "${slice(sort(find('TestSix'), 'name'),  0,  5)}", "slice test");
			final List sliceResult3 = (List)Scripting.evaluate(ctx, testOne, "${slice(sort(find('TestSix'), 'name'),  5, 10)}", "slice test");
			final List sliceResult4 = (List)Scripting.evaluate(ctx, testOne, "${slice(sort(find('TestSix'), 'name'), 10, 15)}", "slice test");
			final List sliceResult5 = (List)Scripting.evaluate(ctx, testOne, "${slice(sort(find('TestSix'), 'name'), 15, 20)}", "slice test");

			assertEquals("Invalid slice() result, must return a list of 5 objects", 5, sliceResult2.size());
			assertEquals("Invalid slice() result, must return a list of 5 objects", 5, sliceResult3.size());
			assertEquals("Invalid slice() result, must return a list of 5 objects", 5, sliceResult4.size());
			assertEquals("Invalid slice() result, must return a list of 5 objects", 5, sliceResult5.size());
			assertEquals("Invalid slice() result", testSixs.get( 0), sliceResult2.get(0));
			assertEquals("Invalid slice() result", testSixs.get( 1), sliceResult2.get(1));
			assertEquals("Invalid slice() result", testSixs.get( 2), sliceResult2.get(2));
			assertEquals("Invalid slice() result", testSixs.get( 3), sliceResult2.get(3));

			assertEquals("Invalid slice() result", testSixs.get( 4), sliceResult2.get(4));
			assertEquals("Invalid slice() result", testSixs.get( 5), sliceResult3.get(0));
			assertEquals("Invalid slice() result", testSixs.get( 6), sliceResult3.get(1));
			assertEquals("Invalid slice() result", testSixs.get( 7), sliceResult3.get(2));
			assertEquals("Invalid slice() result", testSixs.get( 8), sliceResult3.get(3));
			assertEquals("Invalid slice() result", testSixs.get( 9), sliceResult3.get(4));
			assertEquals("Invalid slice() result", testSixs.get(10), sliceResult4.get(0));
			assertEquals("Invalid slice() result", testSixs.get(11), sliceResult4.get(1));
			assertEquals("Invalid slice() result", testSixs.get(12), sliceResult4.get(2));
			assertEquals("Invalid slice() result", testSixs.get(13), sliceResult4.get(3));
			assertEquals("Invalid slice() result", testSixs.get(14), sliceResult4.get(4));
			assertEquals("Invalid slice() result", testSixs.get(15), sliceResult5.get(0));
			assertEquals("Invalid slice() result", testSixs.get(16), sliceResult5.get(1));
			assertEquals("Invalid slice() result", testSixs.get(17), sliceResult5.get(2));
			assertEquals("Invalid slice() result", testSixs.get(18), sliceResult5.get(3));
			assertEquals("Invalid slice() result", testSixs.get(19), sliceResult5.get(4));

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
			assertNotNull("Setting the current date/time should not produce output (JS)", Scripting.replaceVariables(ctx, testOne, "${{var t = Structr.get('this'); (t.aDate = new Date());}}"));

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

			// each expression
			assertEquals("Invalid each() result", "abc", Scripting.replaceVariables(ctx, testOne, "${each(split('a,b,c', ','), print(data))}"));
			assertEquals("Invalid each() result", "abc", Scripting.replaceVariables(ctx, testOne, "${each(this.aStringArray, print(data))}"));

			// complex each expression, sets the value of "testString" to the concatenated IDs of all testSixs that are linked to "this"
			Scripting.replaceVariables(ctx, testOne, "${each(this.manyToManyTestSixs, set(this, \"testString\", concat(get(this, \"testString\"), data.id)))}");
			assertEquals("Invalid each() result", "640", Scripting.replaceVariables(ctx, testOne, "${length(this.testString)}"));

			assertEquals("Invalid if(equal()) result", "String",  Scripting.replaceVariables(ctx, testOne, "${if(empty(this.alwaysNull), titleize(this.aString, '-'), this.alwaysNull)}"));
			assertEquals("Invalid if(equal()) result", "String",  Scripting.replaceVariables(ctx, testOne, "${if(empty(this.aString), titleize(this.alwaysNull, '-'), this.aString)}"));

			assertEquals("Invalid result for special null value", "", Scripting.replaceVariables(ctx, testOne, "${null}"));
			assertEquals("Invalid result for special null value", "", Scripting.replaceVariables(ctx, testOne, "${if(equal(this.anInt, 15), \"selected\", null)}"));

			// tests from real-life examples
			assertEquals("Invalid replacement result", "tile plan ", Scripting.replaceVariables(ctx, testOne, "tile plan ${plan.bannerTag}"));

			// more tests with pre- and postfixes
			assertEquals("Invalid replacement result", "abcdefghijklmnop", Scripting.replaceVariables(ctx, testOne, "abcdefgh${blah}ijklmnop"));
			assertEquals("Invalid replacement result", "abcdefghStringijklmnop", Scripting.replaceVariables(ctx, testOne, "abcdefgh${this.aString}ijklmnop"));
			assertEquals("Invalid replacement result", "#String", Scripting.replaceVariables(ctx, testOne, "#${this.aString}"));
			assertEquals("Invalid replacement result", "doc_sections/"+ testOne.getUuid() + "/childSections?sort=pos", Scripting.replaceVariables(ctx, testOne, "doc_sections/${this.id}/childSections?sort=pos"));
			assertEquals("Invalid replacement result", "A Nice Little Name For My Test Object", Scripting.replaceVariables(ctx, testOne, "${titleize(this.name, '-')}"));
			assertEquals("Invalid replacement result", "STRINGtrueFALSE", Scripting.replaceVariables(ctx, testOne, "${upper(this.aString)}${lower(true)}${upper(false)}"));

			// null and NULL_STRING
			assertEquals("Invalid result for ___NULL___", "", Scripting.replaceVariables(ctx, testOne, "${null}"));
			assertEquals("Invalid result for ___NULL___", "", Scripting.replaceVariables(ctx, testOne, "${___NULL___}"));
			assertEquals("Invalid result for ___NULL___", "", Scripting.replaceVariables(ctx, testOne, "${is(true, ___NULL___)}"));
			assertEquals("Invalid result for ___NULL___", "", Scripting.replaceVariables(ctx, testOne, "${is(false, ___NULL___)}"));
			assertEquals("Invalid result for ___NULL___", "xy", Scripting.replaceVariables(ctx, testOne, "x${___NULL___}y"));
			assertEquals("Invalid result for ___NULL___", "xz", Scripting.replaceVariables(ctx, testOne, "x${is(true, ___NULL___)}z"));
			assertEquals("Invalid result for ___NULL___", "xz", Scripting.replaceVariables(ctx, testOne, "x${is(false, ___NULL___)}z"));

			// test store and retrieve
			assertEquals("Invalid store() result", "", Scripting.replaceVariables(ctx, testOne, "${store('tmp', this.name)}"));
			assertEquals("Invalid stored value", "A-nice-little-name-for-my-test-object", ctx.retrieve("tmp"));
			assertEquals("Invalid retrieve() result", "A-nice-little-name-for-my-test-object", Scripting.replaceVariables(ctx, testOne, "${retrieve('tmp')}"));
			assertEquals("Invalid retrieve() result", "", Scripting.replaceVariables(new ActionContext(SecurityContext.getSuperUserInstance()), testOne, "${retrieve('tmp')}"));

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

			} catch (UnlicensedScriptException |FrameworkException fex) { }

			try {
				Actions.execute(securityContext, testTwo, "${error(\"base\", \"test1\", \"test2\")}", "test");
				fail("error() should throw an exception.");

			} catch (UnlicensedScriptException |FrameworkException fex) { }

			// test multiline statements
			assertEquals("Invalid replace() result", "equal", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, 2),\n    (\"equal\"),\n    (\"not equal\")\n)}"));
			assertEquals("Invalid replace() result", "not equal", Scripting.replaceVariables(ctx, testOne, "${if(equal(2, 3),\n    (\"equal\"),\n    (\"not equal\")\n)}"));

			assertEquals("Invalid keys() / join() result", "createdBy,createdDate,hidden,id,lastModifiedDate,name,owner,type,visibleToAuthenticatedUsers,visibleToPublicUsers", Scripting.replaceVariables(ctx, testOne, "${join(keys(this, 'ui'), ',')}"));
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

			// test filter() with additional evaluation function
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
			assertEquals("Invalid dot notation result", testSixs.get(0).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)), Scripting.replaceVariables(ctx, testOne, "${this.manyToManyTestSixs[0].name}"));
			assertEquals("Invalid dot notation result", testSixs.get(0).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)), Scripting.replaceVariables(ctx, testOne, "${sort(find('TestSix'), 'name')[0].name}"));
			assertEquals("Invalid dot notation result", testSixs.get(15).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)), Scripting.replaceVariables(ctx, testOne, "${sort(find('TestSix'), 'name')[15].name}"));
			assertEquals("Invalid dot notation result", "20", Scripting.replaceVariables(ctx, testOne, "${this.manyToManyTestSixs.size}"));

			// test array property access
			assertEquals("Invalid string array access result", "one", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty[0]}"));
			assertEquals("Invalid string array access result", "two", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty[1]}"));
			assertEquals("Invalid string array access result", "three", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty[2]}"));
			assertEquals("Invalid string array access result", "four", Scripting.replaceVariables(ctx, testFour, "${this.stringArrayProperty[3]}"));

			// test string array property support in collection access methods
			assertEquals("Invalid string array access result with join()", "one,two,three,four", Scripting.replaceVariables(ctx, testFour, "${join(this.stringArrayProperty, ',')}"));
			assertEquals("Invalid string array access result with concat()", "onetwothreefour", Scripting.replaceVariables(ctx, testFour, "${concat(this.stringArrayProperty)}"));
			assertEquals("Invalid string array access result with slice()", "two,three", Scripting.replaceVariables(ctx, testFour, "${join(slice(this.stringArrayProperty, 1, 3), ',')}"));
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

			// sort on arrays in JS
			assertEquals("Invalid sort result", "[TestSix19, TestSix18, TestSix17, TestSix16, TestSix15, TestSix14, TestSix13, TestSix12, TestSix11, TestSix10, TestSix09, TestSix08, TestSix07, TestSix06, TestSix05, TestSix04, TestSix03, TestSix02, TestSix01, TestSix00]", Scripting.replaceVariables(ctx, null, "${{ $.extract($.sort($.find('TestOne')[0].manyToManyTestSixs, 'name', true), 'name'); }}"));

			// sort with extract
			assertEquals("Invalid sort result", "[b, a, c]", Scripting.replaceVariables(ctx, null, "${merge('b', 'a', 'c')}"));
			assertEquals("Invalid sort result", "[a, b, c]", Scripting.replaceVariables(ctx, null, "${sort(merge('b', 'a', 'c'))}"));
			assertEquals("Invalid sort result", "",          Scripting.replaceVariables(ctx, null, "${sort()}"));
			assertEquals("Invalid sort result", "[TestSix19, TestSix18, TestSix17, TestSix16, TestSix15, TestSix14, TestSix13, TestSix12, TestSix11, TestSix10, TestSix09, TestSix08, TestSix07, TestSix06, TestSix05, TestSix04, TestSix03, TestSix02, TestSix01, TestSix00]",          Scripting.replaceVariables(ctx, testOne, "${extract(sort(this.manyToManyTestSixs, 'index', true), 'name')}"));
			assertEquals("Invalid sort result", "[A-nice-little-name-for-my-test-object, testThree_name, testTwo_name]", Scripting.replaceVariables(ctx, testOne, "${extract(sort(merge(this, this.testTwo, this.testThree), 'name'), 'name')}"));
			assertEquals("Invalid sort result", "[A-nice-little-name-for-my-test-object, testThree_name, testTwo_name]", Scripting.replaceVariables(ctx, testOne, "${extract(sort(merge(this.testTwo, this, this.testThree), 'name'), 'name')}"));
			assertEquals("Invalid sort result", "[A-nice-little-name-for-my-test-object, testThree_name, testTwo_name]", Scripting.replaceVariables(ctx, testOne, "${extract(sort(merge(this.testTwo, this.testThree, this), 'name'), 'name')}"));

			// extract
			assertEquals("Invalid extract() result for relationship property", "[[" + StringUtils.join(testSixs, ", ") +  "]]", Scripting.replaceVariables(ctx, testOne, "${extract(find('TestOne'), 'manyToManyTestSixs')}"));
			assertEquals("Invalid extract() result for relationship property", "[" + StringUtils.join(testSixNames, ", ") +  "]", Scripting.replaceVariables(ctx, testOne, "${extract(unwind(extract(find('TestOne'), 'manyToManyTestSixs')), 'name')}"));

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
			assertEquals("Invalid search() result", testOne.getUuid(), Scripting.replaceVariables(ctx, testTwo, "${first(search('TestOne', 'name', 'little-name-for-my-test-object'))}"));
			assertEquals("Invalid search() result", testOne.getUuid(), Scripting.replaceVariables(ctx, testTwo, "${first(search('TestOne', 'name', 'A-nice-little-name-for'))}"));

			// negative test for find()
			assertEquals("Invalid find() result", "", Scripting.replaceVariables(ctx, testTwo, "${first(find('TestOne', 'name', 'little-name-for-my-test-object'))}"));
			assertEquals("Invalid find() result", "", Scripting.replaceVariables(ctx, testTwo, "${first(find('TestOne', 'name', 'A-nice-little-name-for'))}"));

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

			// test parser with different quote leves etc.
			assertEquals("Parser does not handle quotes correctly,", "test\"test", Scripting.replaceVariables(ctx, testOne, "${join(merge('test', 'test'), '\"')}"));
			assertEquals("Parser does not handle quotes correctly,", "test\'test", Scripting.replaceVariables(ctx, testOne, "${join(merge('test', 'test'), '\\'')}"));
			assertEquals("Parser does not handle quotes correctly,", "test\"test", Scripting.replaceVariables(ctx, testOne, "${join(merge(\"test\", \"test\"), \"\\\"\")}"));
			assertEquals("Parser does not handle quotes correctly,", "test\'test", Scripting.replaceVariables(ctx, testOne, "${join(merge(\"test\", \"test\"), \"\\'\")}"));

			// get_or_create()
			final String newUuid1 = Scripting.replaceVariables(ctx, null, "${get_or_create('TestOne', 'name', 'new-object-1')}");
			assertNotNull("Invalid get_or_create() result", newUuid1);
			assertEquals("Invalid get_or_create() result", newUuid1, Scripting.replaceVariables(ctx, null, "${get_or_create('TestOne', 'name', 'new-object-1')}"));
			assertEquals("Invalid get_or_create() result", newUuid1, Scripting.replaceVariables(ctx, null, "${get_or_create('TestOne', 'name', 'new-object-1')}"));
			assertEquals("Invalid get_or_create() result", newUuid1, Scripting.replaceVariables(ctx, null, "${get_or_create('TestOne', 'name', 'new-object-1')}"));

			// get_or_create()
			final String newUuid2 = Scripting.replaceVariables(ctx, null, "${{ Structr.getOrCreate('TestOne', { 'name': 'new-object-2', 'anInt': 13, 'aString': 'string' }) }}");
			assertNotNull("Invalid get_or_create() result", newUuid2);
			assertEquals("Invalid get_or_create() result", newUuid2, Scripting.replaceVariables(ctx, null, "${{ Structr.getOrCreate('TestOne', { 'name': 'new-object-2', 'anInt': 13, 'aString': 'string' }) }}"));
			assertEquals("Invalid get_or_create() result", newUuid2, Scripting.replaceVariables(ctx, null, "${{ Structr.getOrCreate('TestOne', { 'name': 'new-object-2', 'anInt': 13, 'aString': 'string' }) }}"));
			assertEquals("Invalid get_or_create() result", newUuid2, Scripting.replaceVariables(ctx, null, "${{ Structr.getOrCreate('TestOne', { 'name': 'new-object-2', 'anInt': 13, 'aString': 'string' }) }}"));

			// create_or_update()
			final String newUuid3 = Scripting.replaceVariables(ctx, null, "${create_or_update('User', 'eMail', 'tester@test.com', 'name', 'Some Name')}");
			assertNotNull("Invalid create_or_update() result: User object should have been created but hasn't.", newUuid3);
			assertEquals("Invalid create_or_update() result", "Some Name", Scripting.replaceVariables(ctx, null, "${get(find('Principal', '" + newUuid3 + "'), 'name')}"));
			assertEquals("Invalid create_or_update() result",        newUuid3, Scripting.replaceVariables(ctx, null, "${create_or_update('Principal', 'eMail', 'tester@test.com', 'name', 'New Name')}"));
			assertEquals("Invalid create_or_update() result",  "New Name", Scripting.replaceVariables(ctx, null, "${get(find('Principal', '" + newUuid3 + "'), 'name')}"));
			final String newUuid4 = Scripting.replaceVariables(ctx, null, "${create_or_update('User', 'eMail', 'tester@test.com', 'name', 'Some Name')}");
			assertNotNull("Invalid create_or_update() result: User object should have been created but hasn't.", newUuid4);
			final String newUuid5 = Scripting.replaceVariables(ctx, null, "${create_or_update('User', 'eMail', 'tester1@test.com', 'name', 'Some Name')}");
			assertNotNull("Invalid create_or_update() result: User object should have been created but hasn't.", newUuid5);
			assertEquals("Invalid create_or_update() result", "Some Name", Scripting.replaceVariables(ctx, null, "${get(find('Principal', '" + newUuid5 + "'), 'name')}"));
			final String newUuid6 = Scripting.replaceVariables(ctx, null, "${create_or_update('User', 'eMail', 'tester1@test.com', 'name', 'Some Name', 'locale', 'de_DE')}");
			assertNotNull("Invalid create_or_update() result: User object should have been created but hasn't.", newUuid6);
			assertEquals("Invalid create_or_update() result", "de_DE", Scripting.replaceVariables(ctx, null, "${get(find('Principal', '" + newUuid6 + "'), 'locale')}"));

			// sleep
			final long t0 = System.currentTimeMillis();
			Scripting.replaceVariables(ctx, null, "${sleep(1000)}");
			final long dt = System.currentTimeMillis() - t0;
			assertTrue("Sleep() function did not wait for the specified amount of time: " + dt, dt >= 1000);

			// random_uuid
			final String randomUuid = Scripting.replaceVariables(ctx, null, "${random_uuid()}");
			assertTrue("Invalid UUID returned by random_uuid(): " + randomUuid, Settings.isValidUuid(randomUuid));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail(fex.getMessage());
		}
	}

	@Test
	public void testSystemProperties () {
		try {

			final Principal user  = createTestNode(StructrTraits.USER).as(Principal.class);

			// create new node
			NodeInterface t1 = createTestNode("TestOne", user);

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
		NodeInterface testNodeOldScripting = null;

		try (final Tx tx = app.tx()) {

			testNodeOldScripting = createTestNode("TestOne");
			testNodeOldScripting.setProperty(Traits.of("TestOne").key("aString"), "InitialString");
			testNodeOldScripting.setProperty(Traits.of("TestOne").key("anInt"), 42);

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
		NodeInterface testNodeJavaScript = null;

		try (final Tx tx = app.tx()) {

			testNodeJavaScript = createTestNode("TestOne");
			testNodeJavaScript.setProperty(Traits.of("TestOne").key("aString"), "InitialString");
			testNodeJavaScript.setProperty(Traits.of("TestOne").key("anInt"), 42);

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

		NodeInterface testNode = null;
		String uuid ="";

		try (final Tx tx = app.tx()) {

			testNode = createTestNode("TestOne");
			testNode.setProperty(Traits.of("TestOne").key("aString"), "InitialString");
			testNode.setProperty(Traits.of("TestOne").key("anInt"), 42);
			uuid = testNode.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.ID_PROPERTY));

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
			NodeInterface testOne   = createTestNode("TestOne");
			NodeInterface testThree = createTestNode("TestThree");

			testOne.setProperty(Traits.of("TestOne").key("aDate"), now);
			Scripting.replaceVariables(ctx, testThree, "${set(this, 'aDateWithFormat', get(find('TestOne', '" + testOne.getUuid() + "'), 'aDate'))}");
			assertEquals("Copying a date (with default format) to a date (with custom format) failed [StructrScript]", isoDateFormat.format(testOne.getProperty(Traits.of("TestOne").key("aDate"))), isoDateFormat.format(testThree.getProperty(Traits.of("TestThree").key("aDateWithFormat"))));

			testThree.setProperty(Traits.of("TestThree").key("aDateWithFormat"), futureDate);
			Scripting.replaceVariables(ctx, testOne, "${set(this, 'aDate', get(find('TestThree', '" + testThree.getUuid() + "'), 'aDateWithFormat'))}");
			assertEquals("Copying a date (with custom format) to a date (with default format) failed [StructrScript]", isoDateFormat.format(testOne.getProperty(Traits.of("TestOne").key("aDate"))), isoDateFormat.format(testThree.getProperty(Traits.of("TestThree").key("aDateWithFormat"))));


			// Perform the same tests in JavaScript
			testOne.setProperty(Traits.of("TestOne").key("aDate"), null);
			testThree.setProperty(Traits.of("TestThree").key("aDateWithFormat"), null);

			testOne.setProperty(Traits.of("TestOne").key("aDate"), now);
			Scripting.replaceVariables(ctx, testThree, "${{ var testThree = Structr.this; var testOne = Structr.find('TestOne', '" + testOne.getUuid() + "');  testThree.aDateWithFormat = testOne.aDate; }}");
			assertEquals("Copying a date (with default format) to a date (with custom format) failed [JavaScript]", isoDateFormat.format(testOne.getProperty(Traits.of("TestOne").key("aDate"))), isoDateFormat.format(testThree.getProperty(Traits.of("TestThree").key("aDateWithFormat"))));

			testThree.setProperty(Traits.of("TestThree").key("aDateWithFormat"), futureDate);
			Scripting.replaceVariables(ctx, testOne, "${{ var testOne = Structr.this; var testThree = Structr.find('TestThree', '" + testThree.getUuid() + "');  testOne.aDate = testThree.aDateWithFormat; }}");
			assertEquals("Copying a date (with custom format) to a date (with default format) failed [JavaScript]", isoDateFormat.format(testOne.getProperty(Traits.of("TestOne").key("aDate"))), isoDateFormat.format(testThree.getProperty(Traits.of("TestThree").key("aDateWithFormat"))));

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
			NodeInterface testOne = createTestNode("TestOne");

			testOne.setProperty(Traits.of("TestOne").key("aDate"), now);

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

			final ActionContext ctx      = new ActionContext(securityContext, null);
			final String locationId      = Scripting.replaceVariables(ctx, null, "${create('Location')}");
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

	@Test
	public void testNonPrimitiveReturnValue() {

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   "testReturnValueOfGlobalSchemaMethod"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return { name: 'test', value: 123, me: Structr.me }; }")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Test"))),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),         "returnTest"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "{ return { name: 'test', value: 123, me: Structr.this }; }")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);
			final Map map           = (Map)Scripting.evaluate(ctx, null, "${{Structr.call('testReturnValueOfGlobalSchemaMethod')}}", "test");

			final Object name       = map.get("name");
			final Object value      = map.get("value");
			final Object me         = map.get("me");

			assertEquals("Invalid non-primitive scripting return value result, name should be of type string.",  "test", name);
			assertEquals("Invalid non-primitive scripting return value result, value should be of type integer", Integer.valueOf(123), value);
			assertTrue("Invalid non-primitive scripting return value result,   me should be of type SuperUser",    me instanceof SuperUser);

			tx.success();

		} catch (UnlicensedScriptException |FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final String type        = "Test";
			final NodeInterface obj = app.create(type, "test");
			final Map map           = (Map)obj.getProperty(Traits.of(type).key("returnTest"));
			final Object name       = map.get("name");
			final Object value      = map.get("value");
			final Object me         = map.get("me");

			assertEquals("Invalid non-primitive scripting return value result, name should be of type string.",  "test", name);
			assertEquals("Invalid non-primitive scripting return value result, value should be of type integer", Integer.valueOf(123), value);
			assertEquals("Invalid non-primitive scripting return value result, me should be the entity",         obj, me);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testJavascriptBatchFunction() {

		try (final Tx tx = app.tx()) {

			createTestNodes("TestOne", 1000);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);
			final StringBuilder func = new StringBuilder();

			func.append("${{\n");
			func.append("    Structr.doInNewTransaction(function() {\n");
			func.append("        var toDelete = Structr.find('TestOne').slice(0, 100);\n");
			func.append("        if (toDelete && toDelete.length) {\n");
			func.append("            Structr.log('Deleting ' + toDelete.length + ' nodes..');\n");
			func.append("            Structr.delete(toDelete);\n");
			func.append("            return true;\n");
			func.append("        } else {\n");
			func.append("            Structr.log('Finished');\n");
			func.append("            return false;\n");
			func.append("        }\n");
			func.append("    });\n");
			func.append("}}");


			final Object result = Scripting.evaluate(ctx, null, func.toString(), "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testStructrScriptBatchFunction() {

		try (final Tx tx = app.tx()) {

			createTestNodes("TestOne", 1000);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);
			Scripting.evaluate(ctx, null, "${batch(each(find('TestOne'), set(data, 'name', 'test')), 100)}", "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);
			Scripting.evaluate(ctx, null, "${batch(delete(find('TestOne')), 100)}", "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testBulkDeleteWithoutBatching() {

		try (final Tx tx = app.tx()) {

			createTestNodes("TestOne", 1000);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);
			Scripting.evaluate(ctx, null, "${delete(find('TestOne'))}", "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testQuotesInScriptComments() {

		final String script =  "${{\n" +
				"\n" +
				"	// test'\n" +
				"	Structr.print('test');\n" +
				"\n" +
				"}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);

			assertEquals("Single quotes in JavaScript comments should not prevent script evaluation.", "test", Scripting.evaluate(ctx, null, script, "test"));
			assertEquals("Single quotes in JavaScript comments should not prevent script evaluation.", "test", Scripting.replaceVariables(ctx, null, script));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testNewlineAtEndOfScriptingCode() {

		final String script =  "${{ 'test'; }}\n";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);

			assertEquals("Newline at end of JavaScript scripting should not prevent script evaluation.", "test", Scripting.evaluate(ctx, null, script, "test"));
			assertEquals("Newline at end of JavaScript scripting should not prevent script evaluation.", "test\n", Scripting.replaceVariables(ctx, null, script));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testIncludeJs() {

		final String script =  "${{ Structr.includeJs('test'); }}\n";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);

			// just run without an error, that's enough for this test
			Scripting.evaluate(ctx, null, script, "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testAfterCreateMethod() {

		final String expectedErrorToken = "create_not_allowed";

		// test setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema  = StructrSchema.createFromDatabase(app);
			final JsonType dummyType = schema.addType("DummyType");
			final JsonType newType   = schema.addType("MyDynamicType");

			newType.addMethod("onCreation",    "is(eq(this.name, 'forbiddenName'), error('myError', '" + expectedErrorToken + "', 'creating this object is not allowed'))");
			newType.addMethod("afterCreation", "create('DummyType', 'name', 'this should not be possible!')");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {
			logger.error("", t);
			fail("Unexpected exception during test setup.");
		}


		final String myDynamicType = "MyDynamicType";
		final String dummyType     = "DummyType";

		// test that afterCreate is called
		try (final Tx tx = app.tx()) {

			app.create(myDynamicType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "allowedName"));

			final Integer myDynamicTypeCount = app.nodeQuery(myDynamicType).getAsList().size();
			final Integer dummyTypeCount     = app.nodeQuery(dummyType).getAsList().size();

			final boolean correct = myDynamicTypeCount == 1 && dummyTypeCount == 0;

			assertTrue("Before tx.success() there should be exactly 1 node of type MyDynamicNode and 0 of type DummyType", correct);

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Integer myDynamicTypeCount = app.nodeQuery(myDynamicType).getAsList().size();
			final Integer dummyTypeCount     = app.nodeQuery(dummyType).getAsList().size();

			final boolean correct = myDynamicTypeCount == 1 && dummyTypeCount == 1;

			assertTrue("After tx.success() there should be exactly 1 node of type MyDynamicNode and 1 of type DummyType", correct);

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// delete nodes
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);
			Scripting.replaceVariables(ctx, null, "${delete(find('MyDynamicType'))}");
			Scripting.replaceVariables(ctx, null, "${delete(find('DummyType'))}");

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// test that afterCreate is not called if there was an error in onCreate
		try (final Tx tx = app.tx()) {

			app.create(myDynamicType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "forbiddenName"));

			final Integer myDynamicTypeCount = app.nodeQuery(myDynamicType).getAsList().size();
			final Integer dummyTypeCount     = app.nodeQuery(dummyType).getAsList().size();

			final boolean correct = myDynamicTypeCount == 1 && dummyTypeCount == 0;

			assertTrue("Before tx.success() there should be exactly 1 node of type MyDynamicNode and 0 of type DummyType", correct);

			tx.success();

		} catch (FrameworkException fex) {

			final boolean isExpectedErrorToken = fex.getErrorBuffer().getErrorTokens().get(0).getToken().equals(expectedErrorToken);

			assertTrue("Encountered unexpected error!", isExpectedErrorToken);

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Integer myDynamicTypeCount = app.nodeQuery(myDynamicType).getAsList().size();
			final Integer dummyTypeCount     = app.nodeQuery(dummyType).getAsList().size();

			final boolean correct = myDynamicTypeCount == 0 && dummyTypeCount == 0;

			assertTrue("After tx.success() there should be exactly 0 node of type MyDynamicNode and 0 of type DummyType (because we used a forbidden name)", correct);

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testFindNewlyCreatedObjectByOwner () {

		String userObjects        = "[";

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER, "testuser");

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// Create first object
		try (final Tx tx = app.tx()) {

			final Principal testUser = StructrApp.getInstance().nodeQuery(StructrTraits.USER).and(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testuser").getFirst().as(Principal.class);
			final ActionContext ctx = new ActionContext(SecurityContext.getInstance(testUser, AccessMode.Frontend));

			userObjects += Scripting.replaceVariables(ctx, null, "${ create('TestOne') }");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// find() it - this works because the cache is empty
		try (final Tx tx = app.tx()) {

			final Principal testUser = StructrApp.getInstance().nodeQuery(StructrTraits.USER).and(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testuser").getFirst().as(Principal.class);
			final ActionContext ctx = new ActionContext(SecurityContext.getInstance(testUser, AccessMode.Frontend));

			assertEquals("User should be able to find newly created object!", userObjects + "]", Scripting.replaceVariables(ctx, null, "${ find('TestOne', 'owner', me.id) }"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// create second object
		try (final Tx tx = app.tx()) {

			final Principal testUser = StructrApp.getInstance().nodeQuery(StructrTraits.USER).and(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testuser").getFirst().as(Principal.class);
			final ActionContext ctx = new ActionContext(SecurityContext.getInstance(testUser, AccessMode.Frontend));

			userObjects += ", " + Scripting.replaceVariables(ctx, null, "${ create('TestOne') }");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// find() it - this does not work because there is a cache entry already and it was not invalidated after creating the last relationship to it
		try (final Tx tx = app.tx()) {

			final Principal testUser = StructrApp.getInstance().nodeQuery(StructrTraits.USER).and(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testuser").getFirst().as(Principal.class);
			final ActionContext ctx = new ActionContext(SecurityContext.getInstance(testUser, AccessMode.Frontend));

			assertEquals("User should be able to find newly created object!", userObjects + "]", Scripting.replaceVariables(ctx, null, "${ find('TestOne', 'owner', me.id, sort('createdDate', 'desc')) }"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testConversionError() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();

			schema.addType("Test").addBooleanProperty("boolTest").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException | InvalidSchemaException | URISyntaxException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String script =  "${{ var test = Structr.create('Test'); test.boolTest = true; }}\n";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);

			// just run without an error, that's enough for this test
			Scripting.evaluate(ctx, null, script, "test");
			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testModifications() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType customer = schema.addType("Customer");
			final JsonObjectType project  = schema.addType("Project");
			final JsonObjectType task     = schema.addType("Task");

			customer.addStringProperty("log");
			project.addStringProperty("log");
			task.addStringProperty("log");

			// create relation
			final JsonReferenceType rel = project.relate(task, "has", Cardinality.OneToMany, "project", "tasks");
			rel.setName("ProjectTasks");

			customer.relate(project, "project", Cardinality.OneToOne, "customer", "project");

			customer.addMethod("onModification", "{ var mods = Structr.retrieve('modifications'); $.log(mods); Structr.this.log = JSON.stringify(mods); }");
			project.addMethod("onModification", "{ var mods = Structr.retrieve('modifications'); $.log(mods); Structr.this.log = JSON.stringify(mods); }");
			task.addMethod("onModification", "{ var mods = Structr.retrieve('modifications'); $.log(mods); Structr.this.log = JSON.stringify(mods); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String customer         = "Customer";
		final String project          = "Project";
		final String task             = "Task";
		final PropertyKey tasksKey    = Traits.of(project).key("tasks");
		final PropertyKey customerKey = Traits.of(project).key("customer");

		try (final Tx tx = app.tx()) {

			app.create(customer, "Testcustomer");
			app.create(project, "Testproject");
			app.create(task, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "task1"));
			app.create(task, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "task2"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Principal tester      = app.create(StructrTraits.USER, "modifications-tester").as(Principal.class);
			final NodeInterface c       = app.nodeQuery(customer).getFirst();
			final NodeInterface p       = app.nodeQuery(project).getFirst();
			final List<NodeInterface> t = app.nodeQuery(task).getAsList();

			p.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "newName");
			p.setProperty(tasksKey, t);
			p.setProperty(customerKey, c);

			c.as(AccessControllable.class).grant(Permission.write, tester);
			p.as(AccessControllable.class).grant(Permission.write, tester);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test modifications
		try (final Tx tx = app.tx()) {

			final Principal tester = app.nodeQuery(StructrTraits.USER).andName("modifications-tester").getFirst().as(Principal.class);
			final NodeInterface c = app.nodeQuery(customer).getFirst();
			final NodeInterface p = app.nodeQuery(project).getFirst();
			final NodeInterface t = app.nodeQuery(task).getFirst();

			final Map<String, Object> customerModifications = getLoggedModifications(c);
			final Map<String, Object> projectModifications  = getLoggedModifications(p);
			final Map<String, Object> taskModifications     = getLoggedModifications(t);

			assertMapPathValueIs(customerModifications, "added.project",   p.getUuid());
			assertMapPathValueIs(customerModifications, "removed",         new LinkedHashMap<>());
			assertMapPathValueIs(customerModifications, "added.grantees",  Arrays.asList(tester.getUuid()));

			assertMapPathValueIs(projectModifications, "before.name",     "Testproject");
			assertMapPathValueIs(projectModifications, "after.name",     "newName");
			assertMapPathValueIs(projectModifications, "added.customer", c.getUuid());
			assertMapPathValueIs(projectModifications, "removed",        new LinkedHashMap<>());

			final List<NodeInterface> tasks = app.nodeQuery(task).getAsList();
			final List<String> taskIds = new LinkedList();
			for (NodeInterface oneTask : tasks) {
				taskIds.add(oneTask.getUuid());
			}
			assertMapPathValueIs(projectModifications, "added.tasks",    taskIds);
			assertMapPathValueIs(projectModifications, "added.grantees",    Arrays.asList(tester.getUuid()));


			assertMapPathValueIs(taskModifications, "added.project",   p.getUuid());
			assertMapPathValueIs(taskModifications, "removed",         new LinkedHashMap<>());


			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface p = app.nodeQuery(project).getFirst();

			p.setProperty(customerKey, null);
			p.setProperty(tasksKey, Arrays.asList(app.nodeQuery(task).getFirst()));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test modifications
		try (final Tx tx = app.tx()) {

			final NodeInterface c = app.nodeQuery(customer).getFirst();
			final NodeInterface p = app.nodeQuery(project).getFirst();
			final NodeInterface t = app.nodeQuery(task).getFirst();

			final Map<String, Object> customerModifications = getLoggedModifications(c);
			final Map<String, Object> projectModifications  = getLoggedModifications(p);
			final Map<String, Object> taskModifications     = getLoggedModifications(t);

			assertMapPathValueIs(customerModifications, "added",           new LinkedHashMap<>());
			assertMapPathValueIs(customerModifications, "removed.project", p.getUuid());

			assertMapPathValueIs(projectModifications, "removed.customer", c.getUuid());
			assertMapPathValueIs(projectModifications, "added",            new LinkedHashMap<>());

			assertMapPathValueIs(taskModifications, "added.project",   p.getUuid());
			assertMapPathValueIs(taskModifications, "removed",         new LinkedHashMap<>());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testScriptCodeWithNewlines() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			createTestType(schema, "Test1", " 	 set(this, 'c', 'passed')  ",   "   \n 	set(this, 's', 'passed')\n	\n    \n  ");                 // "StructrScript with newlines"
			createTestType(schema, "Test2", "set(this, 'c', 'passed')",             "set(this, 's', 'passed')");                                  // "StructrScript without newlines"
			createTestType(schema, "Test3", "   { Structr.this.c = 'passed'; }   ", " 	 \n	  { Structr.this.s = 'passed'; }\n\n	\n    \n ");  // "JavaScript with newlines"
			createTestType(schema, "Test4", "{ Structr.this.c = 'passed'; }",       "{ Structr.this.s = 'passed'; }");                            // "JavaScript without newlines"

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type1 = "Test1";
		final String type2 = "Test2";
		final String type3 = "Test3";
		final String type4 = "Test4";

		// test onCreate
		try (final Tx tx = app.tx()) {

			app.create(type1, "test1");
			app.create(type2, "test2");
			app.create(type3, "test3");
			app.create(type4, "test4");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test onCreate
		try (final Tx tx = app.tx()) {

			final NodeInterface test1 = app.nodeQuery(type1).getFirst();
			final NodeInterface test2 = app.nodeQuery(type2).getFirst();
			final NodeInterface test3 = app.nodeQuery(type3).getFirst();
			final NodeInterface test4 = app.nodeQuery(type4).getFirst();

			assertEquals("Newlines in script code not trimmed correctly", "passed", test1.getProperty(Traits.of("Test1").key("c")));
			assertEquals("Newlines in script code not trimmed correctly", "passed", test2.getProperty(Traits.of("Test2").key("c")));
			assertEquals("Newlines in script code not trimmed correctly", "passed", test3.getProperty(Traits.of("Test3").key("c")));
			assertEquals("Newlines in script code not trimmed correctly", "passed", test4.getProperty(Traits.of("Test4").key("c")));

			assertNull("onSave method called for creation", test1.getProperty(Traits.of("Test1").key("s")));
			assertNull("onSave method called for creation", test2.getProperty(Traits.of("Test2").key("s")));
			assertNull("onSave method called for creation", test3.getProperty(Traits.of("Test3").key("s")));
			assertNull("onSave method called for creation", test4.getProperty(Traits.of("Test4").key("s")));

			test1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");
			test2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");
			test3.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");
			test4.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test onSave
		try (final Tx tx = app.tx()) {

			final NodeInterface test1 = app.nodeQuery(type1).getFirst();
			final NodeInterface test2 = app.nodeQuery(type2).getFirst();
			final NodeInterface test3 = app.nodeQuery(type3).getFirst();
			final NodeInterface test4 = app.nodeQuery(type4).getFirst();

			assertEquals("Newlines in script code not trimmed correctly", "passed", (String)test1.getProperty(Traits.of("Test1").key("s")));
			assertEquals("Newlines in script code not trimmed correctly", "passed", (String)test2.getProperty(Traits.of("Test2").key("s")));
			assertEquals("Newlines in script code not trimmed correctly", "passed", (String)test3.getProperty(Traits.of("Test3").key("s")));
			assertEquals("Newlines in script code not trimmed correctly", "passed", (String)test4.getProperty(Traits.of("Test4").key("s")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test actions
		try (final Tx tx = app.tx()) {

			assertEquals("Newlines in script code not trimmed correctly", "passed", Actions.execute(securityContext, null, "	 ${ 'passed' }	 ",    "StructrScript with whitespace"));
			assertEquals("Newlines in script code not trimmed correctly", "passed", Actions.execute(securityContext, null, "${ 'passed' }",                "StructrScript without whitespace"));
			assertEquals("Newlines in script code not trimmed correctly", "passed", Actions.execute(securityContext, null, "  ${{ 'passed'; }}   ", "JavaScript with whitespace"));
			assertEquals("Newlines in script code not trimmed correctly", "passed", Actions.execute(securityContext, null, "${{ 'passed'; }}",      "JavaScript without whitespace"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testScriptCodeWithWhitespace() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			createTestType(schema, "Test1", " 	 set(this, 'c', 'passed')  ", "    	set(this, 's', 'passed')	");          // "StructrScript with whitespace"
			createTestType(schema, "Test2", "set(this, 'c', 'passed')", "set(this, 's', 'passed')");                         // "StructrScript without whitespace"
			createTestType(schema, "Test3", "   { Structr.this.c = 'passed'; }   ", "   { Structr.this.s = 'passed'; }   "); // "JavaScript with whitespace"
			createTestType(schema, "Test4", "{ Structr.this.c = 'passed'; }", "{ Structr.this.s = 'passed'; }");             // "JavaScript without whitespace"

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type1 = "Test1";
		final String type2 = "Test2";
		final String type3 = "Test3";
		final String type4 = "Test4";

		// test onCreate
		try (final Tx tx = app.tx()) {

			app.create(type1, "test1");
			app.create(type2, "test2");
			app.create(type3, "test3");
			app.create(type4, "test4");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test onCreate
		try (final Tx tx = app.tx()) {

			final NodeInterface test1 = app.nodeQuery(type1).getFirst();
			final NodeInterface test2 = app.nodeQuery(type2).getFirst();
			final NodeInterface test3 = app.nodeQuery(type3).getFirst();
			final NodeInterface test4 = app.nodeQuery(type4).getFirst();

			assertEquals("Whitespace in script code not trimmed correctly", "passed", test1.getProperty(Traits.of("Test1").key("c")));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", test2.getProperty(Traits.of("Test2").key("c")));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", test3.getProperty(Traits.of("Test3").key("c")));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", test4.getProperty(Traits.of("Test4").key("c")));

			assertNull("onSave method called for creation", test1.getProperty(Traits.of("Test1").key("s")));
			assertNull("onSave method called for creation", test2.getProperty(Traits.of("Test2").key("s")));
			assertNull("onSave method called for creation", test3.getProperty(Traits.of("Test3").key("s")));
			assertNull("onSave method called for creation", test4.getProperty(Traits.of("Test4").key("s")));

			test1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");
			test2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");
			test3.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");
			test4.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test onSave
		try (final Tx tx = app.tx()) {

			final NodeInterface test1 = app.nodeQuery(type1).getFirst();
			final NodeInterface test2 = app.nodeQuery(type2).getFirst();
			final NodeInterface test3 = app.nodeQuery(type3).getFirst();
			final NodeInterface test4 = app.nodeQuery(type4).getFirst();

			assertEquals("Whitespace in script code not trimmed correctly", "passed", test1.getProperty(Traits.of("Test1").key("s")));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", test2.getProperty(Traits.of("Test2").key("s")));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", test3.getProperty(Traits.of("Test3").key("s")));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", test4.getProperty(Traits.of("Test4").key("s")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test actions
		try (final Tx tx = app.tx()) {

			assertEquals("Whitespace in script code not trimmed correctly", "passed", Actions.execute(securityContext, null, "	 ${ 'passed' }	 ",    "StructrScript with whitespace"));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", Actions.execute(securityContext, null, "${ 'passed' }",                "StructrScript without whitespace"));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", Actions.execute(securityContext, null, "  ${{ 'passed'; }}   ", "JavaScript with whitespace"));
			assertEquals("Whitespace in script code not trimmed correctly", "passed", Actions.execute(securityContext, null, "${{ 'passed'; }}",      "JavaScript without whitespace"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testContextStoreTransferToAndFromDoPrivileged() {

		final String storeKey        = "my-store-key";
		final String userValue       = "USER-value";
		final String privilegedValue = "PRIVILEGED-value";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx  = new ActionContext(securityContext, null);
			final StringBuilder func = new StringBuilder();

			func.append("${{\n");
			func.append("	Structr.store('").append(storeKey).append("', '").append(userValue).append("');\n");
			func.append("\n");
			func.append("	Structr.doPrivileged(function () {\n");
			func.append("		Structr.print(Structr.retrieve('").append(storeKey).append("'));\n");
			func.append("	});\n");
			func.append("}}");

			final String retrievedValue = Scripting.replaceVariables(ctx, null, func.toString());

			assertEquals("A value (that was stored outside doPrivilged) should be available in the privileged context", userValue, retrievedValue);


			final StringBuilder func2 = new StringBuilder();

			func2.append("${{\n");
			func2.append("	Structr.store('").append(storeKey).append("', '").append(userValue).append("');\n");
			func2.append("\n");
			func2.append("	Structr.doPrivileged(function () {\n");
			func2.append("		Structr.store('").append(storeKey).append("', '").append(privilegedValue).append("');\n");
			func2.append("	});\n");
			func2.append("\n");
			func2.append("	Structr.print(Structr.retrieve('").append(storeKey).append("'));\n");
			func2.append("}}");

			final String retrievedValue2 = Scripting.replaceVariables(ctx, null, func2.toString());

			assertEquals("A value (that was stored in doPrivilged) should be available in the surrounding context", privilegedValue, retrievedValue2);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testCryptoFunctions() {

		final ActionContext ctx = new ActionContext(securityContext);

		// test failures
		try {

			Scripting.replaceVariables(ctx, null, "${encrypt('plaintext')}");
			fail("Encrypt function should throw an exception when no initial encryption key is set.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		// test failures
		try {

			assertEquals("Decrypt function should return null when no initial encryption key is set.", "", Scripting.replaceVariables(ctx, null, "${decrypt('plaintext')}"));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// test functions without global encryption key
		try {

			assertEquals("Invalid encryption result", "ZuAM6SQ7GTc2KW55M/apUA==", Scripting.replaceVariables(ctx, null, "${encrypt('plaintext', 'structr')}"));
			assertEquals("Invalid encryption result", "b4bn2+w7yaEve3YGtn4IGA==", Scripting.replaceVariables(ctx, null, "${encrypt('plaintext', 'password')}"));

			assertEquals("Invalid decryption result", "ZuAM6SQ7GTc2KW55M/apUA==", Scripting.replaceVariables(ctx, null, "${encrypt('plaintext', 'structr')}"));
			assertEquals("Invalid decryption result", "b4bn2+w7yaEve3YGtn4IGA==", Scripting.replaceVariables(ctx, null, "${encrypt('plaintext', 'password')}"));

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		// test functions with global encryption key
		try {

			assertEquals("Invalid response when setting encryption key via scriptin", "", Scripting.replaceVariables(ctx, null, "${set_encryption_key('structr')}"));

			assertEquals("Invalid encryption result", "ZuAM6SQ7GTc2KW55M/apUA==", Scripting.replaceVariables(ctx, null, "${encrypt('plaintext')}"));
			assertEquals("Invalid encryption result", "ZuAM6SQ7GTc2KW55M/apUA==", Scripting.replaceVariables(ctx, null, "${encrypt('plaintext', 'structr')}"));
			assertEquals("Invalid encryption result", "b4bn2+w7yaEve3YGtn4IGA==", Scripting.replaceVariables(ctx, null, "${encrypt('plaintext', 'password')}"));

			assertEquals("Invalid encryption result", "plaintext", Scripting.replaceVariables(ctx, null, "${decrypt('ZuAM6SQ7GTc2KW55M/apUA==')}"));
			assertEquals("Invalid encryption result", "plaintext", Scripting.replaceVariables(ctx, null, "${decrypt('ZuAM6SQ7GTc2KW55M/apUA==', 'structr')}"));
			assertEquals("Invalid encryption result", "plaintext", Scripting.replaceVariables(ctx, null, "${decrypt('b4bn2+w7yaEve3YGtn4IGA==', 'password')}"));

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		// test resetting encryption key using the built-in function
		try {

			assertEquals("Invalid response when setting encryption key via scriptin", "", Scripting.replaceVariables(ctx, null, "${set_encryption_key(null)}"));

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		// test failures
		try {

			Scripting.replaceVariables(ctx, null, "${encrypt('plaintext')}");
			fail("Encrypt function should throw an exception when no initial encryption key is set.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}
	}

	@Test
	public void testFunctionPropertyTypeHint() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType project  = schema.addType("Project");

			project.addStringProperty("name1");
			project.addStringProperty("name2");

			final JsonFunctionProperty p1 = project.addFunctionProperty("functionProperty1");
			final JsonFunctionProperty p2 = project.addFunctionProperty("functionProperty2");

			p1.setTypeHint("String");
			p2.setTypeHint("String");

			p1.setWriteFunction("set(this, 'name1', concat('from StructrScript', value))");
			p2.setWriteFunction("{ Structr.this.name2 = 'from JavaScript' + Structr.get('value'); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type      = "Project";
		final PropertyKey key1 = Traits.of(type).key("functionProperty1");
		final PropertyKey key2 = Traits.of(type).key("functionProperty2");

		// test
		try (final Tx tx = app.tx()) {

			app.create(type,
					new NodeAttribute<>(key1, "test1"),
					new NodeAttribute<>(key2, "test2")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// check result
		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(type).getFirst();

			assertEquals("Write function has no access to 'this' object when creating a node", "from StructrScripttest1", node.getProperty(Traits.of(type).key("name1")));
			assertEquals("Write function has no access to 'this' object when creating a node", "from JavaScripttest2", node.getProperty(Traits.of(type).key("name2")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testStringConcatenationInJavaScript() {

		// setup
		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType project  = schema.addType("Project");

			project.addFunctionProperty("test1").setFormat("{ return Structr.this.name + 'test' + 123; }");
			project.addFunctionProperty("test2").setFormat("{ return 'test' + 123 + Structr.this.name; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create some test objects
			Scripting.evaluate(ctx, null, "${{ Structr.create('Group', { name: 'test' + 1231 + 'structr' }); }}", "test");
			Scripting.evaluate(ctx, null, "${{ var g = Structr.create('Group'); g.name = 'test' + 1232 + 'structr'; }}", "test");
			Scripting.evaluate(ctx, null, "${{ var g = Structr.create('Group'); Structr.set(g, 'name', 'test' + 1233 + 'structr'); }}", "test");
			Scripting.evaluate(ctx, null, "${{ Structr.create('Group', 'name', 'test' + 1234 + 'structr'); }}", "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String projectType = "Project";

		// check result
		try (final Tx tx = app.tx()) {

			int index = 1;

			for (final NodeInterface group : app.nodeQuery(StructrTraits.GROUP).sort(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				System.out.println(group.getName());

				assertEquals("Invalid JavaScript string concatenation result for script #" + index, "test123" + index++ + "structr", group.getName());
			}

			final NodeInterface project = app.create(projectType, "structr");
			final Traits traits         = project.getTraits();

			assertEquals("Invalid JavaScript string concatenation result in read function", "structrtest123", project.getProperty(traits.key("test1")));
			assertEquals("Invalid JavaScript string concatenation result in read function", "test123structr", project.getProperty(traits.key("test2")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testNewFindSyntaxInStructrScript() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("Test");

			final JsonType project  = schema.addType("Project");

			project.addStringProperty("name1").setIndexed(true);
			project.addStringProperty("name2").setIndexed(true);
			project.addStringProperty("name3").setIndexed(true);

			project.addIntegerProperty("age").setIndexed(true);
			project.addIntegerProperty("count").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String testType    = "Test";
		final String projectType = "Project";
		final PropertyKey name1  = Traits.of(projectType).key("name1");
		final PropertyKey name2  = Traits.of(projectType).key("name2");
		final PropertyKey name3  = Traits.of(projectType).key("name3");
		final PropertyKey age    = Traits.of(projectType).key("age");
		final PropertyKey count  = Traits.of(projectType).key("count");

		// setup
		try (final Tx tx = app.tx()) {

			app.create(projectType,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "group1"),
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "test"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    22),
					new NodeAttribute<>(count, 100)
			);

			app.create(projectType,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "group2"),
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "test"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    33),
					new NodeAttribute<>(count, 102)
			);

			app.create(projectType,
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "other"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    44),
					new NodeAttribute<>(count, 104)
			);

			for (int i=0; i<100; i++) {
				createTestNode(testType, "test" + StringUtils.leftPad(Integer.toString(i), 3, "0"));
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			assertEquals("Non-namespaced contains() returns wrong result", true, Scripting.evaluate(ctx, null, "${contains('name2', 'e')}", "testFindNewSyntax"));
			assertEquals("Non-namespaced empty() returns wrong result", true, Scripting.evaluate(ctx, null, "${empty('')}", "testFindNewSyntax"));
			assertEquals("Non-namespaced empty() returns wrong result", true, Scripting.evaluate(ctx, null, "${empty(null)}", "testFindNewSyntax"));
			assertEquals("Non-namespaced contains() returns wrong result", false, Scripting.evaluate(ctx, null, "${contains('name2', 'x')}", "testFindNewSyntax"));

			final List<NodeInterface> page1 = (List)Scripting.evaluate(ctx, null, "${find('Test', sort('name'), page(1, 10))}", "testFindNewSyntax");
			final List<NodeInterface> page2 = (List)Scripting.evaluate(ctx, null, "${find('Test', sort('name'), page(1, 5))}", "testFindNewSyntax");
			final List<NodeInterface> page3 = (List)Scripting.evaluate(ctx, null, "${find('Test', sort('name'), page(3, 5))}", "testFindNewSyntax");

			assertEquals("Advanced find() with sort() and page() returns wrong result", 10, page1.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test000", page1.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test001", page1.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test009", page1.get(9).getName());

			assertEquals("Advanced find() with sort() and page() returns wrong result", 5, page2.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test000", page2.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test001", page2.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test004", page2.get(4).getName());

			assertEquals("Advanced find() with sort() and page() returns wrong result", 5, page3.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test010", page3.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test011", page3.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test014", page3.get(4).getName());

			assertEquals("find() with namespaced contains() return wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${find('Project', contains('name2', 'e'))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced contains() return wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${find('Project', 'name', 'group1', 'name1', 'structr', 'name2', 'test')}", "testFindNewSyntax")).size());

			try {

				// test count assertion for simple parameters
				Scripting.evaluate(ctx, null, "${find('Project', 'name', 'group1', 'name1', 'structr', 'name2')}", "testFindNewSyntax");

				fail("Invalid number of parameters for find() should throw an exception.");

			} catch (FrameworkException fex) { }

			//assertEquals("find() with namespaced predicates return wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${find('Project', empty('name'))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${find('Project', or(empty('name'), equals('name', 'group2')))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${find('Project', contains('name2', 'e'), contains('name2', 'e'), contains('name2', 'e'))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${find('Project', and(equals('age', range(0, 35))))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${find('Project', equals('age', range(0, 35)), equals('name', 'group2'))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${find('Project', and(equals('age', range(0, 35)), equals('name', 'group2')))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${find('Project', and(contains('name2', 'e')))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${find('Project', and(equals('name', 'group1')))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${find('Project', and(equals('name', 'group1'), equals('name1', 'structr')))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${find('Project', and(equals('name1', 'structr'), equals('name2', 'test')))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 0, ((List)Scripting.evaluate(ctx, null, "${find('Project', and(equals('name1', 'structr'), equals('name2', 'structr')))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${find('Project', or(equals('age', 22), equals('age', 44)))}", "testFindNewSyntax")).size());
			assertEquals("find() with namespaced predicates return wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${find('Project', and(equals('name3', 'other'), or(equals('age', 22), equals('age', 44))))}", "testFindNewSyntax")).size());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testAdvancedFindPrivilegedInJavaScript() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("Test");

			final JsonType project  = schema.addType("Project");

			project.addStringProperty("name1").setIndexed(true);
			project.addStringProperty("name2").setIndexed(true);
			project.addStringProperty("name3").setIndexed(true);

			project.addIntegerProperty("age").setIndexed(true);
			project.addIntegerProperty("count").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final ActionContext ctx = new ActionContext(securityContext);
		final String testType   = "Test";
		final String type       = "Project";
		final PropertyKey name1 = Traits.of(type).key("name1");
		final PropertyKey name2 = Traits.of(type).key("name2");
		final PropertyKey name3 = Traits.of(type).key("name3");
		final PropertyKey age   = Traits.of(type).key("age");
		final PropertyKey count = Traits.of(type).key("count");

		String group1 = null;
		String group2 = null;
		String group3 = null;

		// setup
		try (final Tx tx = app.tx()) {

			group1 = app.create(type,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "group1"),
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "test"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    22),
					new NodeAttribute<>(count, 100)
			).getUuid();

			group2 = app.create(type,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "group2"),
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "test"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    33),
					new NodeAttribute<>(count, 102)
			).getUuid();

			group3 = app.create(type,
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "other"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    44),
					new NodeAttribute<>(count, 104)
			).getUuid();

			for (int i=0; i<100; i++) {
				createTestNode(testType, "test" + StringUtils.leftPad(Integer.toString(i), 3, "0"));
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> result1 = (List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', { 'name2': $.predicate.contains('s') }, $.predicate.sort('name', true)); }}", "testFindNewSyntax");
			final List<NodeInterface> result2 = (List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.sort('name', true)); }}", "testFindNewSyntax");
			final List<NodeInterface> result3 = (List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.sort('name')); }}", "testFindNewSyntax");

			final String testFunction = "${{\n" +
					"    let users = $.find('Project', {\n" +
					"            $and: {\n" +
					"                'name1': 'structr',\n" +
					"                'age': $.predicate.range(30, 50)\n" +
					"            }\n" +
					"        },\n" +
					"        $.predicate.sort('name', true),\n" +
					"        $.predicate.page(1, 10)\n" +
					"    );\n" +
					"    users;\n" +
					"}}";

			final Object result4Object        = Scripting.evaluate(ctx, null, testFunction, "testFindNewSyntax");
			final List<NodeInterface> result4 = (List)result4Object;

			assertEquals("Advanced find() does not filter correctly", 2, result1.size());
			assertEquals("Advanced find() does not filter correctly", result1.get(0).getUuid(), group2);
			assertEquals("Advanced find() does not filter correctly", result1.get(1).getUuid(), group1);

			assertEquals("sort() in advanced find() does not sort correctly", result2.get(0).getUuid(), group3);
			assertEquals("sort() in advanced find() does not sort correctly", result2.get(1).getUuid(), group2);
			assertEquals("sort() in advanced find() does not sort correctly", result2.get(2).getUuid(), group1);

			assertEquals("sort() in advanced find() does not sort correctly", result3.get(0).getUuid(), group1);
			assertEquals("sort() in advanced find() does not sort correctly", result3.get(1).getUuid(), group2);
			assertEquals("sort() in advanced find() does not sort correctly", result3.get(2).getUuid(), group3);

			assertEquals("Advanced find() does not filter correctly", 2, result4.size());
			assertEquals("Advanced find() does not filter correctly", result4.get(0).getUuid(), group3);
			assertEquals("Advanced find() does not filter correctly", result4.get(1).getUuid(), group2);

			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', { name: $.predicate.contains('2') }); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.contains('name2', 'e')); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', { name: 'group1', name1: 'structr', name2: 'test' }); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.empty('name')); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.or($.predicate.empty('name'), $.predicate.equals('name', 'group2'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.contains('name2', 'e'), $.predicate.contains('name2', 'e'), $.predicate.contains('name2', 'e')); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.and($.predicate.equals('age', $.predicate.range(0, 35)))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.equals('age', $.predicate.range(0, 35)), $.predicate.equals('name', 'group2')); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.and($.predicate.equals('age', $.predicate.range(0, 35)), $.predicate.equals('name', 'group2'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.and($.predicate.contains('name2', 'e'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.and($.predicate.equals('name', 'group1'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.and($.predicate.equals('name', 'group1'), $.predicate.equals('name1', 'structr'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.and($.predicate.equals('name1', 'structr'), $.predicate.equals('name2', 'test'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 0, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.and($.predicate.equals('name1', 'structr'), $.predicate.equals('name2', 'structr'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.or($.predicate.equals('age', 22), $.predicate.equals('age', 44))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Project', $.predicate.and($.predicate.equals('name3', 'other'), $.predicate.or($.predicate.equals('age', 22), $.predicate.equals('age', 44)))); }}", "testFindNewSyntax")).size());

			final List<NodeInterface> page1 = (List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Test', $.predicate.sort('name'), $.predicate.page(1, 10)); }}", "testFindNewSyntax");
			final List<NodeInterface> page2 = (List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Test', $.predicate.sort('name'), $.predicate.page(1, 5)); }}", "testFindNewSyntax");
			final List<NodeInterface> page3 = (List)Scripting.evaluate(ctx, null, "${{ $.findPrivileged('Test', $.predicate.sort('name'), $.predicate.page(3, 5)); }}", "testFindNewSyntax");

			assertEquals("Advanced find() with sort() and page() returns wrong result", 10, page1.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test000", page1.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test001", page1.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test009", page1.get(9).getName());

			assertEquals("Advanced find() with sort() and page() returns wrong result", 5, page2.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test000", page2.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test001", page2.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test004", page2.get(4).getName());

			assertEquals("Advanced find() with sort() and page() returns wrong result", 5, page3.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test010", page3.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test011", page3.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test014", page3.get(4).getName());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testAdvancedFindForRemoteProperties() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			// create relation
			final JsonReferenceType rel = project.relate(task, "has", Cardinality.ManyToMany, "projects", "tasks");
			rel.setName("ProjectTasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		final ActionContext ctx  = new ActionContext(securityContext);
		final String projectType = "Project";
		final String taskType    = "Task";

		final PropertyKey projectName  = Traits.of(projectType).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey projectTasks = Traits.of(projectType).key("tasks");

		final PropertyKey taskName     = Traits.of(taskType).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		try (final Tx tx = app.tx()) {

			final NodeInterface task1 = app.create(taskType, new NodeAttribute<>(taskName, "t1") );
			final NodeInterface task2 = app.create(taskType, new NodeAttribute<>(taskName, "t2") );
			final NodeInterface task3 = app.create(taskType, new NodeAttribute<>(taskName, "t3") );

			final NodeInterface task4 = app.create(taskType, new NodeAttribute<>(taskName, "t4") );
			final NodeInterface task5 = app.create(taskType, new NodeAttribute<>(taskName, "t5") );

			app.create(projectType, new NodeAttribute<>(projectName, "p1a"), new NodeAttribute<>(projectTasks, Arrays.asList(task1)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p2a"), new NodeAttribute<>(projectTasks, Arrays.asList(task2)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p3a"), new NodeAttribute<>(projectTasks, Arrays.asList(task3)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p4a"), new NodeAttribute<>(projectTasks, Arrays.asList(task1, task2)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p5a"), new NodeAttribute<>(projectTasks, Arrays.asList(task2, task3)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p6a"), new NodeAttribute<>(projectTasks, Arrays.asList(task1, task3)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p7a"), new NodeAttribute<>(projectTasks, Arrays.asList(task1, task2, task3)) );

			app.create(projectType, new NodeAttribute<>(projectName, "p1b"), new NodeAttribute<>(projectTasks, Arrays.asList(task1)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p2b"), new NodeAttribute<>(projectTasks, Arrays.asList(task2)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p3b"), new NodeAttribute<>(projectTasks, Arrays.asList(task3)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p4b"), new NodeAttribute<>(projectTasks, Arrays.asList(task1, task2)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p5b"), new NodeAttribute<>(projectTasks, Arrays.asList(task2, task3)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p6b"), new NodeAttribute<>(projectTasks, Arrays.asList(task1, task3)) );
			app.create(projectType, new NodeAttribute<>(projectName, "p7b"), new NodeAttribute<>(projectTasks, Arrays.asList(task1, task2, task3)) );

			app.create(projectType, new NodeAttribute<>(projectName, "p8a"), new NodeAttribute<>(projectTasks, Arrays.asList(task1, task2, task3, task4)) );

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Normal find() should use OR to search for remote properties", 9, ((List)Scripting.evaluate(ctx, null, "${{ let t1 = $.find('Task', 'name', 't1'); $.find('Project', 'tasks', t1); }}", "testFindOldSyntax")).size());
			assertEquals("Normal find() should use OR to search for remote properties", 9, ((List)Scripting.evaluate(ctx, null, "${{ let t2 = $.find('Task', 'name', 't2'); $.find('Project', 'tasks', t2); }}", "testFindOldSyntax")).size());
			assertEquals("Normal find() should use OR to search for remote properties", 9, ((List)Scripting.evaluate(ctx, null, "${{ let t3 = $.find('Task', 'name', 't3'); $.find('Project', 'tasks', t3); }}", "testFindOldSyntax")).size());

			assertEquals("Normal find() should use OR to search for remote properties", 13, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t2 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't2'))); $.find('Project', 'tasks', t1_t2); }}", "testFindOldSyntax")).size());
			assertEquals("Normal find() should use OR to search for remote properties", 13, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t3 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't3'))); $.find('Project', 'tasks', t1_t3); }}", "testFindOldSyntax")).size());
			assertEquals("Normal find() should use OR to search for remote properties", 13, ((List)Scripting.evaluate(ctx, null, "${{ let t2_t3 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't2'), $.predicate.equals('name', 't3'))); $.find('Project', 'tasks', t2_t3); }}", "testFindOldSyntax")).size());

			assertEquals("Normal find() should use OR to search for remote properties", 15, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t2_t3 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't2'), $.predicate.equals('name', 't3'))); $.find('Project', 'tasks', t1_t2_t3); }}", "testFindOldSyntax")).size());
			assertEquals("Normal find() should use OR to search for remote properties", 15, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', 'tasks', $.find('Task')); }}", "testFindOldSyntax")).size());

			assertEquals("Advanced find() should use EXACT search for $.equals predicate on remote properties", 2, ((List)Scripting.evaluate(ctx, null, "${{ let t1 = $.find('Task', 'name', 't1'); $.find('Project', 'tasks', $.predicate.equals(t1)); }}", "testFindNewSyntax")).size());

			assertEquals("Advanced find() should use EXACT search for $.equals predicate on remote properties", 2, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t2 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't2'))); $.find('Project', 'tasks', $.predicate.equals(t1_t2)); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() should use EXACT search for $.equals predicate on remote properties", 2, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t3 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't3'))); $.find('Project', 'tasks', $.predicate.equals(t1_t3)); }}", "testFindNewSyntax")).size());

			assertEquals("Advanced find() should use CONTAINS search for $.contains predicate on remote properties", 9, ((List)Scripting.evaluate(ctx, null, "${{ let t1 = $.find('Task', 'name', 't1'); $.find('Project', 'tasks', $.predicate.contains(t1)); }}", "testFindNewSyntax")).size());

			assertEquals("Advanced find() should use CONTAINS search for $.contains predicate on remote properties", 5, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t2 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't2'))); $.find('Project', 'tasks', $.predicate.contains(t1_t2)); }}", "testFindNewSyntax")).size());

			assertEquals("Advanced find() should use CONTAINS search for $.contains predicate on remote properties", 3, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t2_t3 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't2'), $.predicate.equals('name', 't3'))); $.find('Project', 'tasks', $.predicate.contains(t1_t2_t3)); }}", "testFindOldSyntax")).size());

			// test with unconnected Task
			assertEquals("Advanced find() should use EXACT search for $.equals predicate on remote properties", 0, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t5 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't5'))); $.find('Project', 'tasks', $.predicate.equals(t1_t5)); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() should use CONTAINS search for $.contains predicate on remote properties", 0, ((List)Scripting.evaluate(ctx, null, "${{ let t1_t5 = $.find('Task', 'name', $.predicate.or($.predicate.equals('name', 't1'), $.predicate.equals('name', 't5'))); $.find('Project', 'tasks', $.predicate.contains(t1_t5)); }}", "testFindNewSyntax")).size());

			// test unconnected Task
			assertEquals("Normal find() should use OR to search for remote properties", 0, ((List)Scripting.evaluate(ctx, null, "${{ let t5 = $.find('Task', 'name', 't5'); $.find('Project', 'tasks', t5); }}", "testFindOldSyntax")).size());
			assertEquals("Advanced find() should use EXACT search for $.equals predicate on remote properties", 0, ((List)Scripting.evaluate(ctx, null, "${{ let t5 = $.find('Task', 'name', 't5'); $.find('Project', 'tasks', $.predicate.equals(t5)); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() should use CONTAINS search for $.contains predicate on remote properties", 0, ((List)Scripting.evaluate(ctx, null, "${{ let t5 = $.find('Task', 'name', 't5'); $.find('Project', 'tasks', $.predicate.contains(t5)); }}", "testFindNewSyntax")).size());


			// ($.and and $.or with $.contains)
			assertEquals("Advanced find() should use CONTAINS search for $.contains predicate on remote properties", 9, ((List)Scripting.evaluate(ctx, null, "${{ let t1 = $.find('Task', 'name', 't1'); let t5 = $.find('Task', 'name', 't5'); $.find('Project', 'tasks', $.predicate.or($.predicate.contains(t1), $.predicate.contains(t5))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() should use CONTAINS search for $.contains predicate on remote properties", 0, ((List)Scripting.evaluate(ctx, null, "${{ let t1 = $.find('Task', 'name', 't1'); let t5 = $.find('Task', 'name', 't5'); $.find('Project', 'tasks', $.predicate.and($.predicate.contains(t1), $.predicate.contains(t5))); }}", "testFindNewSyntax")).size());

			// ($.not and $.empty)
			assertEquals("Advanced find() should understand $.not predicate for remote properties", 4, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Task', $.predicate.not($.predicate.empty('projects'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() should understand $.empty predicate for remote properties", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Task', $.predicate.empty('projects')); }}", "testFindNewSyntax")).size());

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testEmptyPredicateForRemoteProperties() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			// create relation
			project.relate(project, "SUB", Cardinality.OneToMany, "parent", "children");
			project.relate(task, "TASK", Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		final ActionContext ctx = new ActionContext(securityContext);
		final String projectType = "Project";
		final String taskType    = "Task";

		final PropertyKey projectChildren = Traits.of(projectType).key("children");
		final PropertyKey projectTasks    = Traits.of(projectType).key("tasks");

		final PropertyKey taskName     = Traits.of(taskType).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		try (final Tx tx = app.tx()) {

			final NodeInterface task1 = app.create(taskType, new NodeAttribute<>(taskName, "t1") );
			final NodeInterface task2 = app.create(taskType, new NodeAttribute<>(taskName, "t2") );
			final NodeInterface task3 = app.create(taskType, new NodeAttribute<>(taskName, "t3") );
			final NodeInterface task4 = app.create(taskType, new NodeAttribute<>(taskName, "t4") );
			final NodeInterface task5 = app.create(taskType, new NodeAttribute<>(taskName, "t5") );
			final NodeInterface task6 = app.create(taskType, new NodeAttribute<>(taskName, "t6") );
			final NodeInterface task7 = app.create(taskType, new NodeAttribute<>(taskName, "t7") );
			final NodeInterface task8 = app.create(taskType, new NodeAttribute<>(taskName, "t8") );

			final NodeInterface project1 = app.create(projectType,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Project #1"),
					new NodeAttribute<>(projectTasks, List.of(task1, task2))
			);

			final NodeInterface project2 = app.create(projectType,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Project #2"),
					new NodeAttribute<>(projectTasks, List.of(task3, task4)),
					new NodeAttribute<>(projectChildren, List.of(project1))
			);

			app.create(projectType,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Project #3"),
					new NodeAttribute<>(projectTasks, List.of(task5, task6)),
					new NodeAttribute<>(projectChildren, List.of(project2))
			);

			app.create(projectType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Project #4"));
			app.create(projectType, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Project #5"));

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final String errorMessage = "Advanced find() should understand $.empty predicate for remote properties";

			assertEquals(errorMessage, 3, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.empty('parent')); }}", "testFindNewSyntax1")).size());
			assertEquals(errorMessage, 3, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.empty('children')); }}", "testFindNewSyntax2")).size());
			assertEquals(errorMessage, 2, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Task', $.predicate.empty('project')); }}", "testFindNewSyntax3")).size());

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testNewFindSyntaxInJavaScript() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("Test");

			final JsonType project  = schema.addType("Project");

			project.addStringProperty("name1").setIndexed(true);
			project.addStringProperty("name2").setIndexed(true);
			project.addStringProperty("name3").setIndexed(true);

			project.addIntegerProperty("age").setIndexed(true);
			project.addIntegerProperty("count").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final ActionContext ctx = new ActionContext(securityContext);
		final String testType    = "Test";
		final String type        = "Project";
		final PropertyKey name1 = Traits.of(type).key("name1");
		final PropertyKey name2 = Traits.of(type).key("name2");
		final PropertyKey name3 = Traits.of(type).key("name3");
		final PropertyKey age   = Traits.of(type).key("age");
		final PropertyKey count = Traits.of(type).key("count");

		String group1 = null;
		String group2 = null;
		String group3 = null;

		// setup
		try (final Tx tx = app.tx()) {

			group1 = app.create(type,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "group1"),
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "test"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    22),
					new NodeAttribute<>(count, 100)
			).getUuid();

			group2 = app.create(type,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "group2"),
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "test"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    33),
					new NodeAttribute<>(count, 102)
			).getUuid();

			group3 = app.create(type,
					new NodeAttribute<>(name1, "structr"),
					new NodeAttribute<>(name2, "other"),
					new NodeAttribute<>(name3, "other"),
					new NodeAttribute<>(age,    44),
					new NodeAttribute<>(count, 104)
			).getUuid();

			for (int i=0; i<100; i++) {
				createTestNode(testType, "test" + StringUtils.leftPad(Integer.toString(i), 3, "0"));
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> result1 = (List)Scripting.evaluate(ctx, null, "${{ $.find('Project', { 'name2': $.predicate.contains('s') }, $.predicate.sort('name', true)); }}", "testFindNewSyntax");
			final List<NodeInterface> result2 = (List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.sort('name', true)); }}", "testFindNewSyntax");
			final List<NodeInterface> result3 = (List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.sort('name')); }}", "testFindNewSyntax");

			final String testFunction = "${{\n" +
					"    let users = $.find('Project', {\n" +
					"            $and: {\n" +
					"                'name1': 'structr',\n" +
					"                'age': $.predicate.range(30, 50)\n" +
					"            }\n" +
					"        },\n" +
					"        $.predicate.sort('name', true),\n" +
					"        $.predicate.page(1, 10)\n" +
					"    );\n" +
					"    users;\n" +
					"}}";

			final Object result4Object        = Scripting.evaluate(ctx, null, testFunction, "testFindNewSyntax");
			final List<NodeInterface> result4 = (List)result4Object;

			// make results visible in log file
			System.out.println("#### result1");
			result1.stream().forEach(n -> System.out.println((String)n.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY))));

			System.out.println("#### result2");
			result2.stream().forEach(n -> System.out.println((String)n.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY))));

			System.out.println("#### result3");
			result3.stream().forEach(n -> System.out.println((String)n.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY))));

			System.out.println("#### result4");
			result4.stream().forEach(n -> System.out.println((String)n.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY))));

			assertEquals("Advanced find() does not filter correctly", 2, result1.size());
			assertEquals("Advanced find() does not filter correctly", result1.get(0).getUuid(), group2);
			assertEquals("Advanced find() does not filter correctly", result1.get(1).getUuid(), group1);

			assertEquals("sort() in advanced find() does not sort correctly", result2.get(0).getUuid(), group3);
			assertEquals("sort() in advanced find() does not sort correctly", result2.get(1).getUuid(), group2);
			assertEquals("sort() in advanced find() does not sort correctly", result2.get(2).getUuid(), group1);

			assertEquals("sort() in advanced find() does not sort correctly", result3.get(0).getUuid(), group1);
			assertEquals("sort() in advanced find() does not sort correctly", result3.get(1).getUuid(), group2);
			assertEquals("sort() in advanced find() does not sort correctly", result3.get(2).getUuid(), group3);

			assertEquals("Advanced find() does not filter correctly", 2, result4.size());
			assertEquals("Advanced find() does not filter correctly", result4.get(0).getUuid(), group3);
			assertEquals("Advanced find() does not filter correctly", result4.get(1).getUuid(), group2);

			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', { name: $.predicate.contains('2') }); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.contains('name2', 'e')); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', { name: 'group1', name1: 'structr', name2: 'test' }); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.empty('name')); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.or($.predicate.empty('name'), $.predicate.equals('name', 'group2'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.contains('name2', 'e'), $.predicate.contains('name2', 'e'), $.predicate.contains('name2', 'e')); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.and($.predicate.equals('age', $.predicate.range(0, 35)))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.equals('age', $.predicate.range(0, 35)), $.predicate.equals('name', 'group2')); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.and($.predicate.equals('age', $.predicate.range(0, 35)), $.predicate.equals('name', 'group2'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 3, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.and($.predicate.contains('name2', 'e'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.and($.predicate.equals('name', 'group1'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.and($.predicate.equals('name', 'group1'), $.predicate.equals('name1', 'structr'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.and($.predicate.equals('name1', 'structr'), $.predicate.equals('name2', 'test'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 0, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.and($.predicate.equals('name1', 'structr'), $.predicate.equals('name2', 'structr'))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.or($.predicate.equals('age', 22), $.predicate.equals('age', 44))); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 2, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Project', $.predicate.and($.predicate.equals('name3', 'other'), $.predicate.or($.predicate.equals('age', 22), $.predicate.equals('age', 44)))); }}", "testFindNewSyntax")).size());

			// startsWith and endsWith
			assertEquals("Advanced find() returns wrong result", 10, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Test', { name: $.predicate.startsWith('test00') }); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.find('Test', { name: $.predicate.endsWith('21') }); }}", "testFindNewSyntax")).size());

			//case insensitive
			System.out.println("#################################");
			assertEquals("Advanced find() returns wrong result", 10, ((List)Scripting.evaluate(ctx, null, "${{ $.search('Test', { name: $.predicate.startsWith('TEST00') }); }}", "testFindNewSyntax")).size());
			assertEquals("Advanced find() returns wrong result", 1, ((List)Scripting.evaluate(ctx, null, "${{ $.search('Test', { name: $.predicate.endsWith('21') }); }}", "testFindNewSyntax")).size());

			final List<NodeInterface> page1 = (List)Scripting.evaluate(ctx, null, "${{ $.find('Test', $.predicate.sort('name'), $.predicate.page(1, 10)); }}", "testFindNewSyntax");
			final List<NodeInterface> page2 = (List)Scripting.evaluate(ctx, null, "${{ $.find('Test', $.predicate.sort('name'), $.predicate.page(1, 5)); }}", "testFindNewSyntax");
			final List<NodeInterface> page3 = (List)Scripting.evaluate(ctx, null, "${{ $.find('Test', $.predicate.sort('name'), $.predicate.page(3, 5)); }}", "testFindNewSyntax");

			assertEquals("Advanced find() with sort() and page() returns wrong result", 10, page1.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test000", page1.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test001", page1.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test009", page1.get(9).getName());

			assertEquals("Advanced find() with sort() and page() returns wrong result", 5, page2.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test000", page2.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test001", page2.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test004", page2.get(4).getName());

			assertEquals("Advanced find() with sort() and page() returns wrong result", 5, page3.size());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test010", page3.get(0).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test011", page3.get(1).getName());
			assertEquals("Advanced find() with sort() and page() returns wrong result", "test014", page3.get(4).getName());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testAdvancedFindWithRemotePropertySorting() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonObjectType test  = schema.addType("Test");
			final JsonObjectType test2  = schema.addType("Test2");
			final JsonObjectType test3  = schema.addType("Test3");

			test.relate(test2, "HAS_Test2", Cardinality.OneToOne, "test", "test2");
			test2.relate(test3, "HAS_Test3", Cardinality.OneToOne, "test2", "test3");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final ActionContext ctx = new ActionContext(securityContext);
		final String testType   = "Test";
		final String test2Type  = "Test2";
		final String test3Type  = "Test3";

		final PropertyKey test2_test = Traits.of(test2Type).key("test");
		final PropertyKey test3_test2 = Traits.of(test3Type).key("test2");

		// setup
		try (final Tx tx = app.tx()) {


			for (int i = 0; i < 10; i++) {

				final NodeInterface test = app.create(testType,
						new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test1_" + i)
				);

				final NodeInterface test2 = app.create(test2Type,
						new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test2_" + i),
						new NodeAttribute<>(test2_test, test)
				);

				final NodeInterface test3 = app.create(test3Type,
						new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test3_" + i),
						new NodeAttribute<>(test3_test2, test2)
				);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			List<NodeInterface> result = (List<NodeInterface>) Scripting.evaluate(ctx, null, "${{ $.find('Test', $.predicate.sort('test2.test3.name', false)); }}", "testFindNewSyntax");

			assertEquals("Advanced find() returns wrong result", 10, result.size());
			assertEquals("Advanced find() sorted incorrectly", "test1_0", result.get(0).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Advanced find() sorted incorrectly", "test1_1", result.get(1).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Advanced find() sorted incorrectly", "test2_0", ((NodeInterface)result.get(0).getProperty(Traits.of(testType).key("test2"))).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Advanced find() sorted incorrectly", "test3_0", ((NodeInterface)((NodeInterface)result.get(0).getProperty(Traits.of(testType).key("test2"))).getProperty(Traits.of(test2Type).key("test3"))).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));

			result = (List<NodeInterface>) Scripting.evaluate(ctx, null, "${{ $.find('Test', $.predicate.sort('test2.test3.name', true)); }}", "testFindNewSyntax");

			assertEquals("Advanced find() sorted incorrectly", "test1_9", result.get(0).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Advanced find() sorted incorrectly", "test1_8", result.get(1).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testAdvancedFindRangeQueryLeak() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("Test");

			final JsonType testType  = schema.addType("TestType");

			testType.addIntegerProperty("count").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final ActionContext ctx                = new ActionContext(securityContext);
		final String type                      = "TestType";
		final PropertyKey count                = Traits.of(type).key("count");
		final PropertyKey visibleToPublicUsers = Traits.of(type).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);

		// setup
		try (final Tx tx = app.tx()) {

			int cnt = 0;

			while (cnt < 10) {

				app.create(type,
						new NodeAttribute<>(visibleToPublicUsers, true),
						new NodeAttribute<>(count, cnt)
				);

				app.create(type,
						new NodeAttribute<>(visibleToPublicUsers, false),
						new NodeAttribute<>(count, cnt + 10)
				);

				cnt++;
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			// AND: works
			final String testRangeFunctionInANDGroup = "${{\n" +
					"    let nodes = $.find('TestType', {\n" +
					"            $and: {\n" +
					"                'visibleToPublicUsers': true,\n" +
					"                'count': $.predicate.range(5, 14)\n" +
					"            }\n" +
					"        }\n" +
					"    );\n" +
					"    nodes;\n" +
					"}}";

			final List<NodeInterface> res1 = (List)Scripting.evaluate(ctx, null, testRangeFunctionInANDGroup, "testAdvancedFindRangeQueryLeak");
			assertEquals("Advanced find range predicate does not filter correctly for surrounding AND", 5, res1.size());


			// OR with workaround AND around range: works
			final String testRangeFunctionORWrapRangeInAND = "${{\n" +
					"    let nodes = $.find('TestType', {\n" +
					"            $or: {\n" +
					"                'visibleToPublicUsers': true,\n" +
					"                $and: {\n" +
					"                    'count': $.predicate.range(5, 14)\n" +
					"                }\n" +
					"            }\n" +
					"        }\n" +
					"    );\n" +
					"    nodes;\n" +
					"}}";

			final List<NodeInterface> res2 = (List)Scripting.evaluate(ctx, null, testRangeFunctionORWrapRangeInAND, "testAdvancedFindRangeQueryLeak");
			assertEquals("Advanced find range predicate does not filter correctly for surrounding OR (even when wrapped in and() itself)", 15, res2.size());


			// Plain OR with structrscript syntax: does not work
			final String testRangeFunctionInORGroupStructrScriptSyntax = "${{\n" +
					"    let nodes = $.find('TestType', \n" +
					"            $.predicate.or(\n" +
					"                $.predicate.equals('visibleToPublicUsers', true),\n" +
					"                $.predicate.equals('count', $.predicate.range(5, 14))\n" +
					"            )\n" +
					"    );\n" +
					"    nodes;\n" +
					"}}";

			final List<NodeInterface> res3 = (List)Scripting.evaluate(ctx, null, testRangeFunctionInORGroupStructrScriptSyntax, "testAdvancedFindRangeQueryLeak");
			assertEquals("Advanced find range predicate does not filter correctly for surrounding OR (range() leaks outward and turns OR into AND) [StructrScript Syntax]", 15, res3.size());


			// Plain OR with JavaScript syntax: does not work
			final String testRangeFunctionInORGroupOtherSyntax = "${{\n" +
					"    let nodes = $.find('TestType', {\n" +
					"            $or: {\n" +
					"                'visibleToPublicUsers': true,\n" +
					"                'count': $.predicate.range(5, 14)\n" +
					"            }\n" +
					"        }\n" +
					"    );\n" +
					"    nodes;\n" +
					"}}";

			final List<NodeInterface> res4 = (List)Scripting.evaluate(ctx, null, testRangeFunctionInORGroupOtherSyntax, "testAdvancedFindRangeQueryLeak");
			assertEquals("Advanced find range predicate does not filter correctly for surrounding OR (range() leaks outward and turns OR into AND) [JavaScript Syntax]", 15, res4.size());


			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testAdvancedFindWithMultipleLevelsOfEmptyPredicates() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("TestType");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final String testType                  = "TestType";
		final PropertyKey visibleToPublicUsers = Traits.of(testType).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);

		// setup
		try (final Tx tx = app.tx()) {

			app.create(testType, new NodeAttribute<>(visibleToPublicUsers, true));
			app.create(testType, new NodeAttribute<>(visibleToPublicUsers, true));
			app.create(testType, new NodeAttribute<>(visibleToPublicUsers, true));
			app.create(testType, new NodeAttribute<>(visibleToPublicUsers, true));
			app.create(testType, new NodeAttribute<>(visibleToPublicUsers, true));
			app.create(testType, new NodeAttribute<>(visibleToPublicUsers, true));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final ActionContext ctx                = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			final int testNodeCount = StructrApp.getInstance().nodeQuery("TestType").getAsList().size();

			final String errorMessage = "all test nodes should be returned - no cypher exception should be triggered by empty clauses!";

			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType').length; }}", ""));

			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and()).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or()).length; }}", ""));

			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and(), $.predicate.and()).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and(), $.predicate.or ()).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or (), $.predicate.and()).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or (), $.predicate.or ()).length; }}", ""));

			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and($.predicate.and()), $.predicate.and($.predicate.and())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or ($.predicate.and()), $.predicate.and($.predicate.and())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and($.predicate.and()), $.predicate.and($.predicate.or ())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or ($.predicate.and()), $.predicate.and($.predicate.or ())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and($.predicate.or ()), $.predicate.and($.predicate.and())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or ($.predicate.or ()), $.predicate.and($.predicate.and())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and($.predicate.or ()), $.predicate.and($.predicate.or ())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or ($.predicate.or ()), $.predicate.and($.predicate.or ())).length; }}", ""));

			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or (), $.predicate.or (), $.predicate.or ()).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and(), $.predicate.and(), $.predicate.and()).length; }}", ""));

			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and($.predicate.or (), $.predicate.or (), $.predicate.or ())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or ($.predicate.or (), $.predicate.or (), $.predicate.or ())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and($.predicate.and(), $.predicate.and(), $.predicate.and())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or ($.predicate.and(), $.predicate.and(), $.predicate.and())).length; }}", ""));

			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or ($.predicate.or (), $.predicate.and(), $.predicate.or ())).length; }}", ""));
			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.or ($.predicate.and(), $.predicate.or (), $.predicate.and())).length; }}", ""));

			assertEquals(errorMessage, testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('TestType', $.predicate.and($.predicate.or($.predicate.or(), $.predicate.and($.predicate.or($.predicate.or()), $.predicate.or())), $.predicate.or())).length; }}", ""));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testAdvancedFindWithContainsPredicate() {

		try (final Tx tx = app.tx()) {

			int cnt = 0;

			while (cnt < 10) {

				app.create(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "node" + cnt));
				cnt++;
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final ActionContext ctx                = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			final int testNodeCount = StructrApp.getInstance().nodeQuery(StructrTraits.GROUP).getAsList().size();

			assertEquals("All groups should be returned", testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('Group').length; }}", ""));
			assertEquals("All groups should be returned with 'node' in their name", testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('Group', $.predicate.contains('name', 'node')).length; }}", ""));
			assertEquals("All groups should be returned because the empty string is always contained in any string", testNodeCount, Scripting.evaluate(ctx, null, "${{ $.find('Group', $.predicate.contains('name', '')).length; }}", ""));
			assertEquals("No groups should be found!", 0, Scripting.evaluate(ctx, null, "${{ $.find('Group', $.predicate.contains('name', 'notinthere')).length; }}", ""));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testJavascriptArrayWrapping() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType test  = schema.addType("Test");

			test.addMethod("doTest", "{ let arr = []; arr.push({ name: 'test1' }); arr.push({ name: 'test2' }); arr.push({ name: 'test2' }); return arr; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final ActionContext ctx = new ActionContext(securityContext);
		final String testType   = "Test";

		// setup
		try (final Tx tx = app.tx()) {

			app.create(testType, "test");

			final Object result = Scripting.evaluate(ctx, null, "${{ var test = $.find('Test')[0]; var arr = test.doTest(); for (let e of arr) { Structr.log(e); }; arr; }}", "test");

			assertTrue("Invalid wrapping of native Javascript array", result instanceof List);
			assertEquals("Invalid wrapping of native Javascript array", 3, ((List)result).size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testNamespaceHandling() {

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			// test that the empty() function is resolved to the non-namespaced one after exiting find()
			assertEquals("StructrScript: namespace is not exited correctly after entering find()", "[]true", (Scripting.evaluate(ctx, null, "${(find('Group'),empty(null))}", "test")));
			assertEquals("JavaScript: namespace is not exited correctly after entering find()", true, (Scripting.evaluate(ctx, null, "${{ $.find('Group'); $.empty(null); }}", "test")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testJavascriptFindDateRange() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("Test");

			final JsonType project  = schema.addType("Project");

			project.addDateProperty("date").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}


		final ActionContext ctx = new ActionContext(securityContext);
		final String type        = "Project";
		final PropertyKey date  = Traits.of(type).key("date");
		final Calendar calendar = GregorianCalendar.getInstance();

		// setup
		try (final Tx tx = app.tx()) {

			// 01.01.2019
			calendar.set(2019, 0, 1, 0, 0, 0);
			app.create(type, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "p1"), new NodeAttribute<>(date, calendar.getTime()));

			// 01.02.2019
			calendar.set(2019, 1, 1, 0, 0, 0);
			app.create(type, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "p2"), new NodeAttribute<>(date, calendar.getTime()));

			// 01.03.2019
			calendar.set(2019, 2, 1, 0, 0, 0);
			app.create(type, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "p3"), new NodeAttribute<>(date, calendar.getTime()));

			// 01.04.2019
			calendar.set(2019, 3, 1, 0, 0, 0);
			app.create(type, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "p4"), new NodeAttribute<>(date, calendar.getTime()));

			// 01.05.2019
			calendar.set(2019, 4, 1, 0, 0, 0);
			app.create(type, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "p5"), new NodeAttribute<>(date, calendar.getTime()));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final String errorMessage = "Advanced find() with date range returns wrong result";

			assertEquals(errorMessage, 5, ((List)Scripting.evaluate(ctx, null, formatDateTestScript("new Date",   "2000-01-01", "2100-01-01", GraphObjectTraitDefinition.CREATED_DATE_PROPERTY), "testFindNewSyntax")).size());
			assertEquals(errorMessage, 5, ((List)Scripting.evaluate(ctx, null, formatDateTestScript("Date.parse", "2000-01-01", "2100-01-01", GraphObjectTraitDefinition.CREATED_DATE_PROPERTY), "testFindNewSyntax")).size());

			assertEquals(errorMessage, 5, ((List)Scripting.evaluate(ctx, null, formatDateTestScript("new Date",   "2018-01-01", "2020-01-01", "date"), "testFindNewSyntax")).size());
			assertEquals(errorMessage, 5, ((List)Scripting.evaluate(ctx, null, formatDateTestScript("Date.parse", "2018-01-01", "2020-01-01", "date"), "testFindNewSyntax")).size());

			assertEquals(errorMessage, 2, ((List)Scripting.evaluate(ctx, null, formatDateTestScript("new Date",   "2018-12-31", "2019-02-15", "date"), "testFindNewSyntax")).size());
			assertEquals(errorMessage, 2, ((List)Scripting.evaluate(ctx, null, formatDateTestScript("Date.parse", "2018-12-31", "2019-02-15", "date"), "testFindNewSyntax")).size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testDatePropertyWithNonStandardFormatInScripting() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("Test");

			final JsonType project  = schema.addType("Project");
			project.addDateProperty("date").setIndexed(true).setFormat("yyyy");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final ActionContext ctx = new ActionContext(securityContext);
		final String type       = "Project";
		final PropertyKey date  = Traits.of(type).key("date");
		final Calendar calendar = GregorianCalendar.getInstance();

		// setup
		try (final Tx tx = app.tx()) {

			calendar.set(2019, 0, 1, 10, 20, 30);
			app.create(type, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "p1"), new NodeAttribute<>(date, calendar.getTime()));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Object value1 = Scripting.evaluate(ctx, null, "${{ $.find('Project', 'name', 'p1')[0].date; }}", "");
			final Object value2 = Scripting.evaluate(ctx, null, "${{ $.get($.find('Project', 'name', 'p1')[0], 'date'); }}", "");

			final Object value3 = Scripting.evaluate(ctx, null, "${find('Project', 'name', 'p1')[0].date}", "");
			final Object value4 = Scripting.evaluate(ctx, null, "${get(first(find('Project', 'name', 'p1')), 'date')}", "");

			assertTrue("dot notation should yield unformatted date object", value1 instanceof Date);
			assertTrue("get function should yield formatted date string", value2 instanceof String);
			assertTrue("dot notation should yield unformatted date object", value3 instanceof Date);
			assertTrue("get function should yield formatted date string", value4 instanceof String);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail(fex.getMessage());
		}
	}

	@Test
	public void testBatchErrorHandler() {

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			assertEquals("", "", (Scripting.evaluate(ctx, null, "${{ $.doInNewTransaction(function() { $.error('base', 'nope', 'detail'); }, function() { $.store('test-result', 'error_handled'); }); }}", "test")));
			assertEquals("Error handler in doInNewTransaction function was not called.", "error_handled", ctx.retrieve("test-result"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testNotEqual() {

		final ActionContext ctx = new ActionContext(securityContext);

		// setup
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.GROUP, "group1");
			app.create(StructrTraits.GROUP, "group2");
			app.create(StructrTraits.GROUP, "group3");
			app.create(StructrTraits.GROUP, "group4");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface group1 = app.nodeQuery(StructrTraits.GROUP).andName("group1").getFirst();
			final NodeInterface group2 = app.nodeQuery(StructrTraits.GROUP).andName("group2").getFirst();
			final String script1       = "${{ $.find('Group', { $and: { name: 'group1', id: $.predicate.not($.predicate.equals('" + group1.getUuid() + "')) }}); }}";
			final String script2       = "${{ $.find('Group', { $and: { name: 'group1', id: $.predicate.not($.predicate.equals('" + group2.getUuid() + "')) }}); }}";

			// test that not(equal()) works for the id property
			final Object result1 = Scripting.evaluate(ctx, null, script1, "test1");
			final Object result2 = Scripting.evaluate(ctx, null, script2, "test2");

			assertEquals("Invalid advanced find result for not(equals)) with ID", 0, ((List)result1).size());
			assertEquals("Invalid advanced find result for not(equals)) with ID", 1, ((List)result2).size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testFindNotEmpty() {

		final ActionContext ctx = new ActionContext(securityContext);

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema      = StructrSchema.createEmptySchema();
			final JsonObjectType contact = schema.addType("Contact");

			contact.addIntegerProperty("num").setIndexed(true);

			// add
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException | InvalidSchemaException | URISyntaxException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String contactClass = "Contact";
		final PropertyKey numKey  = Traits.of(contactClass).key("num");

		try (final Tx tx = app.tx()) {

			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact01"), new NodeAttribute<>(numKey,   12));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact02"), new NodeAttribute<>(numKey,   11));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact03"), new NodeAttribute<>(numKey, null));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          ""), new NodeAttribute<>(numKey,    9));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact05"), new NodeAttribute<>(numKey,    8));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact06"), new NodeAttribute<>(numKey,    7));
			app.create(contactClass,                                                      new NodeAttribute<>(numKey,    6));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact08"), new NodeAttribute<>(numKey,    5));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact09"), new NodeAttribute<>(numKey, null));
			app.create(contactClass,                                                      new NodeAttribute<>(numKey,    3));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact11"), new NodeAttribute<>(numKey,    2));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact12"), new NodeAttribute<>(numKey,    1));
			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final String query1               = "${find('Contact', not(empty('name')), sort('name'))}";
			final List<NodeInterface> result1 = (List)Scripting.evaluate(ctx, null, query1, "test1");

			final String query2               = "${find('Contact', not(empty('num')), sort('num'))}";
			final List<NodeInterface> result2 = (List)Scripting.evaluate(ctx, null, query2, "test2");

			// expected: 1, 2, 3, 5, 6, 8, 9, 11, 12
			assertEquals("Invalid result for advanced find with graph predicate", "contact01", result1.get(0).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid result for advanced find with graph predicate", "contact02", result1.get(1).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid result for advanced find with graph predicate", "contact03", result1.get(2).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid result for advanced find with graph predicate", "contact05", result1.get(3).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid result for advanced find with graph predicate", "contact06", result1.get(4).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid result for advanced find with graph predicate", "contact08", result1.get(5).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid result for advanced find with graph predicate", "contact09", result1.get(6).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid result for advanced find with graph predicate", "contact11", result1.get(7).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid result for advanced find with graph predicate", "contact12", result1.get(8).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));

			// expected: 12, 11, 9, 8, 7, 6, 5, 3, 2, 1
			assertEquals("Invalid result for advanced find with graph predicate",  1, result2.get(0).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  2, result2.get(1).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  3, result2.get(2).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  5, result2.get(3).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  6, result2.get(4).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  7, result2.get(5).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  8, result2.get(6).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  9, result2.get(7).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate", 11, result2.get(8).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate", 12, result2.get(9).getProperty(numKey));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testAdvancedFindNotEqualWithQuery() {

		final ActionContext ctx = new ActionContext(securityContext);

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema          = StructrSchema.createEmptySchema();
			final JsonObjectType contact     = schema.addType("Contact");
			final JsonObjectType contactType = schema.addType("ContactType");

			contactType.relate(contact, "has", Cardinality.OneToMany, "contactType", "contacts");

			contact.addIntegerProperty("num").setIndexed(true);

			// add
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException | InvalidSchemaException | URISyntaxException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String contactClass = "Contact";
		final String typeClass    = "ContactType";
		final PropertyKey typeKey = Traits.of(contactClass).key("contactType");
		final PropertyKey numKey  = Traits.of(contactClass).key("num");

		try (final Tx tx = app.tx()) {

			final NodeInterface type1 = app.create(typeClass, "type1");
			final NodeInterface type2 = app.create(typeClass, "type2");
			final NodeInterface type3 = app.create(typeClass, "type3");

			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact01"), new NodeAttribute<>(numKey,  1), new NodeAttribute<>(typeKey, type1));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact02"), new NodeAttribute<>(numKey,  2), new NodeAttribute<>(typeKey, type2)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact03"), new NodeAttribute<>(numKey,  3), new NodeAttribute<>(typeKey, type2)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact04"),                                  new NodeAttribute<>(typeKey, type1));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact05"),                                  new NodeAttribute<>(typeKey, type1));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact06"), new NodeAttribute<>(numKey,  6), new NodeAttribute<>(typeKey, type1));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact07"), new NodeAttribute<>(numKey,  7), new NodeAttribute<>(typeKey, type2)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact08"), new NodeAttribute<>(numKey,  8), new NodeAttribute<>(typeKey, type2)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact09"),                                  new NodeAttribute<>(typeKey, type3));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact10"),                                  new NodeAttribute<>(typeKey, type3));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact11"), new NodeAttribute<>(numKey, 12), new NodeAttribute<>(typeKey, type3)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact12"), new NodeAttribute<>(numKey, 13), new NodeAttribute<>(typeKey, type3)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact13"),                                  new NodeAttribute<>(typeKey, type1));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact14"), new NodeAttribute<>(numKey, 15), new NodeAttribute<>(typeKey, type2)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact15"), new NodeAttribute<>(numKey, 16), new NodeAttribute<>(typeKey, type3)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact16"), new NodeAttribute<>(numKey, 17), new NodeAttribute<>(typeKey, type1));
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact17"), new NodeAttribute<>(numKey, 18), new NodeAttribute<>(typeKey, type2)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact18"), new NodeAttribute<>(numKey, 19), new NodeAttribute<>(typeKey, type3)); // this
			app.create(contactClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "contact19"), new NodeAttribute<>(numKey, 20), new NodeAttribute<>(typeKey, type1));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final String query1               = "${find('Contact', and(not(empty('num')), not(equals('contactType', first(find('ContactType', 'name', 'type1'))))), sort('num', true), page(1, 20))}";
			final List<NodeInterface> result1 = (List)Scripting.evaluate(ctx, null, query1, "test1");

			// expected: 19, 18, 16, 15, 13, 12, 8, 7, 3, 2
			assertEquals("Invalid result for advanced find with graph predicate", 19, result1.get(0).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate", 18, result1.get(1).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate", 16, result1.get(2).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate", 15, result1.get(3).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate", 13, result1.get(4).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate", 12, result1.get(5).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  8, result1.get(6).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  7, result1.get(7).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  3, result1.get(8).getProperty(numKey));
			assertEquals("Invalid result for advanced find with graph predicate",  2, result1.get(9).getProperty(numKey));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void NodeInterface() {

		final ActionContext ctx = new ActionContext(securityContext);

		// setup
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.GROUP, "group1");
			app.create(StructrTraits.GROUP, "group2");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> groups = app.nodeQuery(StructrTraits.GROUP).getAsList();

			assertEquals("Invalid number of groups in test setup", 2, groups.size());

			final NodeInterface group1 = groups.get(0);
			final NodeInterface group2 = groups.get(1);
			final String expected      = "[" + group1.getUuid() + ", " + group2.getUuid() + "]";

			assertEquals("Invalid print output", expected,         Scripting.evaluate(ctx, group1, "${print(find('Group'))}", "test1"));
			assertEquals("Invalid print output", group1.getUuid(), Scripting.evaluate(ctx, group1, "${print(this)}", "test1"));
			assertEquals("Invalid print output", "", Scripting.evaluate(ctx, group2, "${log(this)}", "test2"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testAssignmentOfStringArrayProperties() {

		final ActionContext ctx = new ActionContext(securityContext);

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addStringArrayProperty("test");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// setup 2
		try (final Tx tx = app.tx()) {

			final String type      = "Test";
			final PropertyKey key = Traits.of(type).key("test");

			app.create(type,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "source"),
					new NodeAttribute<>(key, new String[] { "one", "two", "three" })
			);

			app.create(type,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "target")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test
		try (final Tx tx = app.tx()) {

			Scripting.evaluate(ctx, null, "${{ var source = $.find('Test', { name: 'source' })[0]; var target = $.find('Test', { name: 'target' })[0]; target.test = source.test; }}", "test");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testEmptyArrayProperty() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonType testType = schema.addType("TestEmptyArrayPropertyType");
			testType.addStringArrayProperty("arr");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}


		try (final Tx tx = app.tx()) {

			final String clazz       = "TestEmptyArrayPropertyType";
			final ActionContext ac   = new ActionContext(securityContext);
			final NodeInterface node = app.create(clazz);

			Scripting.evaluate(ac, node, "${{Structr.get('this').arr.push('test');}}", null);

			final String[] arr = node.getProperty(Traits.of(clazz).key("arr"));

			Assert.assertEquals(1, arr.length);

		} catch (FrameworkException ex) {

			fail();
		}
	}

	@Test
	public void testDifferentArrayPropertyAssignmentAndPush() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonType testType = schema.addType("ArrayPropertiesTest");

			testType.addStringArrayProperty("strings");
			testType.addIntegerArrayProperty("ints");
			testType.addDoubleArrayProperty("doubles");
			testType.addLongArrayProperty("longs");
			testType.addBooleanArrayProperty("bools");
			testType.addDateArrayProperty("dates");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}


		/**
		 * Test using .push() on the empty object
		 */
		try (final Tx tx = app.tx()) {

			final ActionContext ac = new ActionContext(securityContext);

			Scripting.evaluate(ac, null, "${{"
					+ "let n = $.create('ArrayPropertiesTest', {name: 'emptyTest'});"
					+ "}}", null);

			Scripting.evaluate(ac, null, "${{"
					+ "let n = $.create('ArrayPropertiesTest', {name: 'emptyPushTest'});"
					+ "n.strings.push('test');"
					+ "n.ints.push(42);"
					+ "n.doubles.push(42);"
					+ "n.longs.push(42);"
					+ "n.bools.push(true);"
					+ "n.dates.push(new Date());"
					+ "}}", null);

			Scripting.evaluate(ac, null, "${{"
					+ "let n = $.create('ArrayPropertiesTest', {name: 'emptySetTest'});"
					+ "n.strings = ['a', 'b', 'c'];"
					+ "n.ints = [3, 4, 5];"
					+ "n.doubles = [3.14, 4, 5.05];"
					+ "n.longs = [3, 4, 5];"
					+ "n.bools = [true, false, true];"
					+ "n.dates = [new Date(), $.get('now'), new Date()];"
					+ "}}", null);

			Scripting.evaluate(ac, null, "${{"
					+ "let n = $.create('ArrayPropertiesTest', {name: 'setAndPushTest'});"
					+ "n.strings = ['a', 'b', 'c'];"
					+ "n.strings.push('d');"
					+ "n.ints = [3, 4, 5];"
					+ "n.ints.push(6);"
					+ "n.doubles = [3.14, 4, 5.05];"
					+ "n.doubles.push(6);"
					+ "n.longs = [3, 4, 5];"
					+ "n.longs.push(6);"
					+ "n.bools = [true, false, true];"
					+ "n.bools.push(false);"
					+ "n.dates = [new Date(), $.get('now'), new Date()];"
					+ "n.dates.push(new Date());"
					+ "}}", null);

			tx.success();

		} catch (FrameworkException ex) {
			fail();
		}

		// test
		try (final Tx tx = app.tx()) {

			final ActionContext ac = new ActionContext(securityContext);
			assertEquals("All array properties should return 0 length", "000000",                                         Scripting.evaluate(ac, null, "${{ let n = $.find('ArrayPropertiesTest', { name: 'emptyTest' })[0]; $.print($.int(n.strings.length), $.int(n.ints.length), $.int(n.doubles.length), $.int(n.longs.length), $.int(n.bools.length), $.int(n.dates.length)); }}", "emptyPushTest"));
			assertEquals("All array properties should return 1 length after one push()", "111111",                        Scripting.evaluate(ac, null, "${{ let n = $.find('ArrayPropertiesTest', { name: 'emptyPushTest' })[0]; $.print($.int(n.strings.length), $.int(n.ints.length), $.int(n.doubles.length), $.int(n.longs.length), $.int(n.bools.length), $.int(n.dates.length)); }}", "emptyPushTest"));
			assertEquals("All array properties should return 3 length after assignment of [x,y,z]", "333333",             Scripting.evaluate(ac, null, "${{ let n = $.find('ArrayPropertiesTest', { name: 'emptySetTest' })[0]; $.print($.int(n.strings.length), $.int(n.ints.length), $.int(n.doubles.length), $.int(n.longs.length), $.int(n.bools.length), $.int(n.dates.length)); }}", "emptyPushTest"));
			assertEquals("All array properties should return 4 length after assignment of [x,y,z] plus push()", "444444", Scripting.evaluate(ac, null, "${{ let n = $.find('ArrayPropertiesTest', { name: 'setAndPushTest' })[0]; $.print($.int(n.strings.length), $.int(n.ints.length), $.int(n.doubles.length), $.int(n.longs.length), $.int(n.bools.length), $.int(n.dates.length)); }}", "emptyPushTest"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testAdvancedFindNamespaceHandlingWithException() {

		/*
		 * This test verifies that the find() namespace is correctly exited even when an exception
		 * occurs inside of a find() function call (e.g. caused by a wrongly typed search parameter).
		 */

		final ActionContext ctx = new ActionContext(securityContext);

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addIntegerProperty("test");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// test
		try (final Tx tx = app.tx()) {

			Scripting.evaluate(ctx, null, "${{ var source = $.find('Test', { test: 'error' });}}", "test");

			tx.success();

		} catch (FrameworkException fex) {

			assertEquals("Wrong error message for exception inside of advanced find() context", "Cannot parse input for property 'test'", fex.getMessage());
		}

	}

	@Test
	public void testStructrScriptArrayIndexingWithVariable() {

		final ActionContext ctx          = new ActionContext(securityContext);
		final List<NodeInterface> groups = new LinkedList<>();

		// setup
		try (final Tx tx = app.tx()) {

			groups.add(app.create(StructrTraits.GROUP, "group4"));
			groups.add(app.create(StructrTraits.GROUP, "group2"));
			groups.add(app.create(StructrTraits.GROUP, "group1"));
			groups.add(app.create(StructrTraits.GROUP, "group5"));
			groups.add(app.create(StructrTraits.GROUP, "group7"));
			groups.add(app.create(StructrTraits.GROUP, "group6"));
			groups.add(app.create(StructrTraits.GROUP, "group3"));
			groups.add(app.create(StructrTraits.GROUP, "group8"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface group1 = groups.get(2);

			ctx.setConstant("index1", 3);
			ctx.setConstant("index2", "3");


			assertEquals("StructrScript array indexing returns wrong result",   group1, Scripting.evaluate(ctx, null, "${find('Group', sort('name'))[0]}", "test1"));
			assertEquals("StructrScript array indexing returns wrong result", "group2", Scripting.evaluate(ctx, null, "${find('Group', sort('name'))[1].name}", "test1"));
			assertEquals("StructrScript array indexing returns wrong result", "group4", Scripting.evaluate(ctx, null, "${find('Group', sort('name'))[index1].name}", "test1"));
			assertEquals("StructrScript array indexing returns wrong result", "group4", Scripting.evaluate(ctx, null, "${find('Group', sort('name'))[index2].name}", "test1"));

			// FIXME: this test fails because [] binds to the wrong expression
			//final List<Group> check1 = groups.subList(3, 4);
			//assertEquals("StructrScript array indexing returns wrong result",   check1, Scripting.evaluate(ctx, null, "${merge(find('Group', sort('name'))[index2])}", "test1"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSortingByMultipleKeysInJavascript() {

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name7"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name5"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name2"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name1"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name3"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name4"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name9"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name8"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name6"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			//final List<TestOne> result1 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(TestOne.name).getAsList();
			final List<NodeInterface> result1 = (List)Scripting.evaluate(ctx, null, "${{ $.find('TestOne', $.predicate.sort('aLong'), $.predicate.sort('name'))}}", "test1");

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result1.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result1.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result1.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result1.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result1.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result1.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result1.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result1.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result1.get(8).getName());

			//final List<TestOne> result2 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong"), true).sort(TestOne.name).getAsList();
			final List<NodeInterface> result2 = (List)Scripting.evaluate(ctx, null, "${{ $.find('TestOne', $.predicate.sort('aLong', true), $.predicate.sort('name'))}}", "test2");

			assertEquals("Sorting by multiple keys returns wrong result", "name1", result2.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result2.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result2.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result2.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result2.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result2.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result2.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result2.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result2.get(8).getName());

			//final List<TestOne> result3 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(TestOne.name, true).getAsList();
			final List<NodeInterface> result3 = (List)Scripting.evaluate(ctx, null, "${{ $.find('TestOne', $.predicate.sort('aLong'), $.predicate.sort('name', true))}}", "test3");

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result3.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result3.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result3.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result3.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result3.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result3.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result3.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result3.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result3.get(8).getName());

			//final List<TestOne> result4 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(Traits.of("TestOne").key("anInt")).sort(TestOne.name).getAsList();
			final List<NodeInterface> result4 = (List)Scripting.evaluate(ctx, null, "${{ $.find('TestOne', $.predicate.sort('aLong'), $.predicate.sort('anInt'), $.predicate.sort('name'))}}", "test4");

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result4.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result4.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result4.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result4.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result4.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result4.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result4.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result4.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result4.get(8).getName());

			//final List<TestOne> result5 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(Traits.of("TestOne").key("anInt"), true).sort(TestOne.name).getAsList();
			final List<NodeInterface> result5 = (List)Scripting.evaluate(ctx, null, "${{ $.find('TestOne', $.predicate.sort('aLong'), $.predicate.sort('anInt', true), $.predicate.sort('name'))}}", "test5");

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result5.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result5.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result5.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result5.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result5.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result5.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result5.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result5.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result5.get(8).getName());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			System.out.println(fex.getMessage());
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSortingByMultipleKeysInStructrScriptAdvancedFind() {

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name7"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name5"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name2"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name1"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name3"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name4"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name9"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name8"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name6"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			//final List<TestOne> result1 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(TestOne.name).getAsList();
			final List<NodeInterface> result1 = (List)Scripting.evaluate(ctx, null, "${find('TestOne', sort('aLong'), sort('name'))}", "test1");

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result1.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result1.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result1.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result1.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result1.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result1.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result1.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result1.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result1.get(8).getName());

			//final List<TestOne> result2 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong"), true).sort(TestOne.name).getAsList();
			final List<NodeInterface> result2 = (List)Scripting.evaluate(ctx, null, "${find('TestOne', sort('aLong', true), sort('name'))}", "test2");

			assertEquals("Sorting by multiple keys returns wrong result", "name1", result2.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result2.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result2.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result2.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result2.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result2.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result2.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result2.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result2.get(8).getName());

			//final List<TestOne> result3 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(TestOne.name, true).getAsList();
			final List<NodeInterface> result3 = (List)Scripting.evaluate(ctx, null, "${find('TestOne', sort('aLong'), sort('name', true))}", "test3");

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result3.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result3.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result3.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result3.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result3.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result3.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result3.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result3.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result3.get(8).getName());

			//final List<TestOne> result4 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(Traits.of("TestOne").key("anInt")).sort(TestOne.name).getAsList();
			final List<NodeInterface> result4 = (List)Scripting.evaluate(ctx, null, "${find('TestOne', sort('aLong'), sort('anInt'), sort('name'))}", "test4");

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result4.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result4.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result4.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result4.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result4.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result4.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result4.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result4.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result4.get(8).getName());

			//final List<TestOne> result5 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(Traits.of("TestOne").key("anInt"), true).sort(TestOne.name).getAsList();
			final List<NodeInterface> result5 = (List)Scripting.evaluate(ctx, null, "${find('TestOne', sort('aLong'), sort('anInt', true), sort('name'))}", "test5");

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result5.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result5.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result5.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result5.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result5.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result5.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result5.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result5.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result5.get(8).getName());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			System.out.println(fex.getMessage());
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSortingByMultipleKeysInStructrScriptNormalFind() {

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name7"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name5"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name2"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name1"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name3"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 20L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name4"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name9"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 3), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name8"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 2), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));
			app.create("TestOne", new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name6"), new NodeAttribute<>(Traits.of("TestOne").key("anInt"), 1), new NodeAttribute<>(Traits.of("TestOne").key("aLong"), 10L));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			//final List<TestOne> result1 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(TestOne.name).getAsList();
			final List<NodeInterface> result1 = (List)Scripting.evaluate(ctx, null, "${sort(find('TestOne') 'aLong', false, 'name', false)}", "test1");

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result1.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result1.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result1.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result1.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result1.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result1.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result1.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result1.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result1.get(8).getName());

			//final List<TestOne> result2 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong"), true).sort(TestOne.name).getAsList();
			final List<NodeInterface> result2 = (List)Scripting.evaluate(ctx, null, "${sort(find('TestOne'), 'aLong', true, 'name')}", "test2");

			assertEquals("Sorting by multiple keys returns wrong result", "name1", result2.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result2.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result2.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result2.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result2.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result2.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result2.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result2.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result2.get(8).getName());

			//final List<TestOne> result3 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(TestOne.name, true).getAsList();
			final List<NodeInterface> result3 = (List)Scripting.evaluate(ctx, null, "${sort(find('TestOne'), 'aLong', false, 'name', true)}", "test3");

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result3.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result3.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result3.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result3.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result3.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result3.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result3.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result3.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result3.get(8).getName());

			//final List<TestOne> result4 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(Traits.of("TestOne").key("anInt")).sort(TestOne.name).getAsList();
			final List<NodeInterface> result4 = (List)Scripting.evaluate(ctx, null, "${sort(find('TestOne'), 'aLong', false, 'anInt', false, 'name', false)}", "test4");

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result4.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result4.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result4.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result4.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result4.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result4.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result4.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result4.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result4.get(8).getName());

			//final List<TestOne> result5 = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("aLong")).sort(Traits.of("TestOne").key("anInt"), true).sort(TestOne.name).getAsList();
			final List<NodeInterface> result5 = (List)Scripting.evaluate(ctx, null, "${sort(find('TestOne'), 'aLong', false, 'anInt', true, 'name')}", "test5");

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result5.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result5.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result5.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result5.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result5.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result5.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result5.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result5.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result5.get(8).getName());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			System.out.println(fex.getMessage());
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testComments() {

		/*
		 * This test verifies that comments in JavaScript blocks are detected and interpreded correctly.
		 */

		final ActionContext ctx = new ActionContext(securityContext);

		// test
		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(ctx, null, "${{\n\n\t$.log('Testing');\n\n\t/*}*/\n\n}}", "test");

			System.out.println("'" + result + "'");

			tx.success();

		} catch (FrameworkException fex) {

			assertEquals("Wrong error code for exception inside of advanced find() context.",   422, fex.getStatus());
			assertEquals("Wrong error message for exception inside of advanced find() context", "Cannot parse input error for property test", fex.getMessage());
		}

	}

	@Test
	public void testJavaScriptQuirksDuckTypingNumericalMapIndex () {

		/*
			This test makes sure that map access works even though javascript interprets numerical strings (e.g. "1", "25") as ints (after the map has undergone wrapping/unwrapping
		*/

		final ActionContext ctx = new ActionContext(securityContext);

		// test
		try (final Tx tx = app.tx()) {

			final Object result = ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testJavaScriptQuirksDuckTypingNumericalMapIndex.js"));

			assertEquals("Result should not be undefined! Access to maps at numerical indexes should work.", true, result);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

	}

	@Test
	public void testHMCAFunction () {
		/*
			This test ensures that the core function hmac() returns the correct HEX String for the given values.
		*/

		final ActionContext ctx = new ActionContext(securityContext);

		// test
		try (final Tx tx = app.tx()) {

			final String resultSHA256 = (String) ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testHMACFunctionSHA256.js"));
			assertEquals("Result does match the expected SHA256 hmac", "88cd2108b5347d973cf39cdf9053d7dd42704876d8c9a9bd8e2d168259d3ddf7", resultSHA256);

			final String resultMD5 = (String) ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testHMACFunctionMD5.js"));
			assertEquals("Result does match the expected MD5 hmac", "cd4b0dcbe0f4538b979fb73664f51abe", resultMD5);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);

			fail("Unexpected exception");
		}
	}

	@Test
	public void testSlice() {

		final ActionContext ctx         = new ActionContext(securityContext);
		final List<String> testSixNames = new LinkedList<>();
		NodeInterface testOne           = null;
		List<NodeInterface> testSixs    = null;
		int index                       = 0;

		try (final Tx tx = app.tx()) {

			testOne        = createTestNode("TestOne");
			testSixs       = createTestNodes("TestSix", 20, 1);

			final Calendar cal = GregorianCalendar.getInstance();

			// set calendar to 2018-01-01T00:00:00+0000
			cal.set(2018, 0, 1, 0, 0, 0);

			for (final NodeInterface testSix : testSixs) {

				final String name = "TestSix" + StringUtils.leftPad(Integer.toString(index), 2, "0");

				testSix.setProperty(Traits.of("TestSix").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);
				testSix.setProperty(Traits.of("TestSix").key("index"), index);
				testSix.setProperty(Traits.of("TestSix").key("date"), cal.getTime());

				index++;
				cal.add(Calendar.DAY_OF_YEAR, 3);

				// build list of names
				testSixNames.add(name);
			}

			testOne.setProperty(Traits.of("TestOne").key("manyToManyTestSixs"), testSixs);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			// slice
			final Object sliceResult = Scripting.evaluate(ctx, testOne, "${slice(this.manyToManyTestSixs, 0, 5)}", "slice test");
			assertTrue("Invalid slice() result, must return collection for valid results", sliceResult instanceof Collection);
			assertTrue("Invalid slice() result, must return list for valid results", sliceResult instanceof List);
			final List sliceResultList = (List)sliceResult;
			assertEquals("Invalid slice() result, must return a list of 5 objects", 5, sliceResultList.size());

			// slice with find
			final Object sliceWithFindResult = Scripting.evaluate(ctx, null, "${slice(find('TestSix'), 0, 2)}", "slice test with find");
			assertTrue("Invalid slice() result, must return collection for valid results", sliceWithFindResult instanceof Collection);
			assertTrue("Invalid slice() result, must return list for valid results", sliceWithFindResult instanceof List);
			final List sliceWithFindResultList = (List)sliceWithFindResult;
			assertEquals("Invalid slice() result, must return a list of 2 object", 2, sliceWithFindResultList.size());

			// slice with find JS
			//final Object sliceWithFindJSResult = Scripting.evaluate(ctx, null, "${{ return $.slice(function() { return $.find('TestSix') }, 0, 2); }}", "slice test with find in JS");
			//assertTrue("Invalid slice() result, must return collection for valid results", sliceWithFindJSResult instanceof Collection);
			//assertTrue("Invalid slice() result, must return list for valid results", sliceWithFindJSResult instanceof List);
			//final List sliceWithFindJSResultList = (List)sliceWithFindJSResult;
			//assertEquals("Invalid slice() result, must return a list of 2 objects", 2, sliceWithFindJSResultList.size());

			// test error cases
			assertEquals("Invalid slice() result for invalid inputs", "", Scripting.replaceVariables(ctx, testOne, "${slice(this.alwaysNull, 1, 2)}"));
			assertEquals("Invalid slice() result for invalid inputs", "", Scripting.replaceVariables(ctx, testOne, "${slice(this.manyToManyTestSixs, -1, 1)}"));
			assertEquals("Invalid slice() result for invalid inputs", "", Scripting.replaceVariables(ctx, testOne, "${slice(this.manyToManyTestSixs, 2, 1)}"));

			// test with interval larger than number of elements
			assertEquals("Invalid slice() result for invalid inputs",
					Iterables.toList((Iterable)testOne.getProperty(Traits.of("TestOne").key("manyToManyTestSixs"))).toString(),
					Scripting.replaceVariables(ctx, testOne, "${slice(this.manyToManyTestSixs, 0, 1000)}")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	@Test
	public void testPolyglotArraySorting () {

		/*
			Ensure that PolyglotProxyArray entries can be properly set by ensuring the proper function of sort on a proxy array.
		*/

		final ActionContext ctx = new ActionContext(securityContext);

		try (final Tx tx = app.tx()) {

			final Object result = ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testPolyglotArraySorting.js"));

			assertNotNull(result);
			assertTrue(result instanceof List);
			List resultList = (List)result;

			assertEquals(resultList.size(), 10);
			assertTrue(resultList.get(0) instanceof NodeInterface);
			assertEquals(((NodeInterface)resultList.get(0)).getName(), "TestOne9");

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

	}

	@Test
	public void testContextStorePollutionInSchemaMethodCall() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType project  = schema.addType("ContextTest");

			project.addIntegerProperty("result");

			final JsonFunctionProperty p1 = project.addFunctionProperty("input1");
			final JsonFunctionProperty p2 = project.addFunctionProperty("input2");

			p1.setTypeHint("int");
			p2.setTypeHint("int");

			// explanation: $.get('value') return the Map that is given as a parameter to the node creation below, so doTest is called with a map
			// (and it should be called with two different maps, that contain key1 OR key2, but not both)
			p1.setWriteFunction("{$.this.doTest($.get('value'));}");
			p2.setWriteFunction("{$.this.doTest($.get('value'));}");

			project.addMethod("doTest", "{ $.this.result =  $.retrieve('key1') + $.retrieve('key2'); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type      = "ContextTest";
		final PropertyKey key1 = Traits.of(type).key("input1");
		final PropertyKey key2 = Traits.of(type).key("input2");

		// test
		try (final Tx tx = app.tx()) {

			Map<String, Object> p1 = Map.ofEntries(new AbstractMap.SimpleEntry<>("key1", 1));
			Map<String, Object> p2 = Map.ofEntries(new AbstractMap.SimpleEntry<>("key2", 1));

			app.create(type,
					new NodeAttribute<>(key1, p1),
					new NodeAttribute<>(key2, p2)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// check result
		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(type).getFirst();
			final Traits traits      = node.getTraits();

			assertEquals(1, (int)node.getProperty(traits.key("result")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testStaticAndDynamicMethodCall() {

		/**
		 * 1. Call context tests
		 * 2. Reference this tests
		 * 3. Binding order test
		 * */

		// setup schema
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("StaticMethodTest");

			type.addMethod("doStaticTest", "{}").setIsStatic(true);
			type.addMethod("doDynamicTest", "{}");

			type.addMethod("doStaticTestWithThis", "{ return $.this.type; }").setIsStatic(true);
			type.addMethod("doDynamicTestWithThis", "{ return $.this.type; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");

		}

		/**
		 * 1. Call context tests
		 *
		 * Method  | Context | Expected Result
		 * --------+---------+----------------
		 * static  | static  | success
		 * static  | dynamic | failure
		 * dynamic | static  | failure
		 * dynamic | dynamic | success
		 *
		 * */

		final ActionContext ctx = new ActionContext(securityContext);
		final String testType   = "StaticMethodTest";

		// call static method from static context
		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(ctx, null, "${{ $.StaticMethodTest.doStaticTest(); }}", "doStaticTest");

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// call static method from dynamic context
		try (final Tx tx = app.tx()) {

			app.create(testType, "test");

			final Object result = Scripting.evaluate(ctx, null, "${{ const test = $.find('StaticMethodTest')[0]; test.doStaticTest(); }}", "test");

			fail("Calling static method from dynamic context should result in an Exception!");

		} catch (FrameworkException fex) {}

		// call dynamic method from static context
		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(ctx, null, "${{ $.StaticMethodTest.doDynamicTest(); }}", "doDynamicTest");
			fail("Calling dynamic method from static context should result in an Exception!");

		} catch (FrameworkException fex) {}

		// call dynamic method from dynamic context
		try (final Tx tx = app.tx()) {

			app.create(testType, "test");

			final Object result = Scripting.evaluate(ctx, null, "${{ const test = $.find('StaticMethodTest')[0]; test.doDynamicTest(); }}", "doDynamicTest");

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");

		}

		/**
		 * 2. Reference this tests
		 *
		 * Method  | Access $.this | Expected Result
		 * --------+---------------+----------------
		 * static  | true          | failure
		 * dynamic | true          | success
		 *
		 * */

		// reference $.this from static method
		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(ctx, null, "${{ $.StaticMethodTest.doStaticTestWithThis(); }}", "doStaticTestWithThis");
			fail("Referencing $.this from a static method should result in an Exception!");

		} catch (FrameworkException fex) {}

		// reference $.this from dynamic method
		try (final Tx tx = app.tx()) {

			app.create(testType, "test");

			final Object result = Scripting.evaluate(ctx, null, "${{ const test = $.find('StaticMethodTest')[0]; test.doDynamicTestWithThis(); }}", "doDynamicTestWithThis");

		} catch (FrameworkException fex) {

			fex.printStackTrace();

		}

		/**
		 * 3. Binding order test
		 * local variables bind stronger than class names
		 * */


		// reference $.this from static method
		try (final Tx tx = app.tx()) {

			ctx.setConstant("StaticMethodTest", Boolean.TRUE);
			final Object result = Scripting.evaluate(ctx, null, "${{ $.StaticMethodTest.doStaticTest(); }}", "doStaticTest");
			fail("Local variable or constant should overwrite the Class constant!");

		} catch (FrameworkException fex) {}

	}

	@Test
	public void testCacheFunction () {

		final ActionContext ctx = new ActionContext(securityContext);

		// test
		try (final Tx tx = app.tx()) {


			final Object cachedResult = ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testCacheFunction.js"));
			Object result = ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testCacheFunction.js"));
			assertEquals(cachedResult, result);

			tryWithTimeout(()-> {
				try {
					return !ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testCacheFunction.js")).equals(cachedResult);
				} catch (FrameworkException ex) {

					return false;
				}
			}, ()-> fail("Timeout reached while waiting for cached value to change after timeout"), 20000, 1000);

			final Object secondCachedResult = ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testCacheFunction.js"));

			assertFalse("Cached value didn't change after timeout.", cachedResult.equals(secondCachedResult));
			result = ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testCacheFunction.js"));
			assertEquals(secondCachedResult, result);

			tx.success();

		} catch (FrameworkException ex) {

			fail("Unexpected exception");
		}

	}

	@Test
	public void testVarsKeyword () {

		final ActionContext ctx = new ActionContext(securityContext);

		// test
		try (final Tx tx = app.tx()) {

			final Object result = ScriptTestHelper.testExternalScript(ctx, ScriptingTest.class.getResourceAsStream("/test/scripting/testVarsKeyword.js"));

			assertNotNull(result);
			assertTrue("Result is not a map", result instanceof Map);
			for (final Map.Entry<String, Integer> entry : Set.of(Map.entry("a", 0), Map.entry("b", 1), Map.entry("c", 2))) {
				assertEquals(entry.getValue(), ((Map) result).get(entry.getKey()));
			}

			tx.success();

		} catch (FrameworkException ex) {

			fail("Unexpected exception");
		}

	}

	@Test
	public void testMultilineStructrScriptExpression() {

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null,
					"${if (\n" +
							"	is_collection(request.param),\n" +
							"	(\n" +
							"		print('collection! '),\n" +
							"		each(request.param, print(data))\n" +
							"	),\n" +
							"	(\n" +
							"		print('single param!'),\n" +
							"		print(request.param)\n" +
							"	)\n" +
							")}", "test", null);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testDoubleBackslashEscaping() {

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> groups = new LinkedList<>();

			groups.add(app.create(StructrTraits.GROUP, "Group1"));
			groups.add(app.create(StructrTraits.GROUP, "Group2"));
			groups.add(app.create(StructrTraits.GROUP, "Group3"));

			final String result = Scripting.replaceVariables(new ActionContext(securityContext), null, "${concat(';', \"'\", '\\r\\n')}");

			assertEquals("Invalid StructrScript tokenizer result: ", ";'\r\n", result);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testThisKeywordAfterBatch() {

		try (final Tx tx = app.tx()) {

			final NodeInterface group = app.create(StructrTraits.GROUP, "Group1");

			Scripting.replaceVariables(new ActionContext(securityContext), group, "${{ $.log($.this.name); $.doInNewTransaction(function() { $.log('In doInNewTransaction()'); }); $.log($.this.name); }}");

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testStructrScriptFunctionWithParameters() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("createMap", "{ return { param1: 'Test', param2: 123 }; }");
			type.addMethod("test",      "concat('success',    this.name, retrieve('param1'))");
			type.addMethod("testOne",   "concat('successOne', this.name, retrieve('param1'), retrieve('param2'))");
			type.addMethod("test123",   "concat('success123', this.name, retrieve('param1'), retrieve('param2'), retrieve('param3'))");
			type.addMethod("test333",   "concat('success333', 'static', retrieve('param1'), retrieve('param2'), retrieve('param3'))").setIsStatic(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final String type        = "Test";
			final NodeInterface test = app.create(type, "test1");

			assertEquals("successtest1abc",              Scripting.replaceVariables(new ActionContext(securityContext), test, "${this.test('param1', 'abc')}"));
			assertEquals("successOnetest1abc123.0",      Scripting.replaceVariables(new ActionContext(securityContext), test, "${this.testOne('param1', 'abc', 'param2', 123)}"));
			assertEquals("success123test1abc123.0true",  Scripting.replaceVariables(new ActionContext(securityContext), test, "${this.test123('param1', 'abc', 'param2', 123, 'param3', true)}"));
			assertEquals("success333staticabc123.0true", Scripting.replaceVariables(new ActionContext(securityContext), test, "${Test.test333('param1', 'abc', 'param2', 123, 'param3', true)}"));

			// test mixed parameters and nested calls
			assertEquals("successtest1" + test.getUuid(), Scripting.replaceVariables(new ActionContext(securityContext), test, "${this.test('param1', first(find('Test')))}"));
			assertEquals("success123test1Test123",        Scripting.replaceVariables(new ActionContext(securityContext), test, "${this.test123(this.createMap())}"));
			assertEquals("success123test1Test123false",   Scripting.replaceVariables(new ActionContext(securityContext), test, "${this.test123(this.createMap(), 'param3', false)}"));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testStaticCallsBetweenMethods() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("createObject", "{ return { key1: 'value1' }; }").setIsStatic(true);
			type.addMethod("createDate",   "{ return new Date(); }").setIsStatic(true);
			type.addMethod("createMap",    "{ return new Map(); }").setIsStatic(true);
			type.addMethod("createSet",    "{ let mySet = new Set(); mySet.add('initialValue'); return mySet; }").setIsStatic(true);

			// native object created inline
			type.addMethod("test0",      "{ let map = { key1: 'value1' }; map.key2 = 123; return map; }").setIsStatic(true); // success
			type.addMethod("test1",      "{ let map = { key1: 'value1' }; map.set('key2', 123); return map; }").setIsStatic(true); // failure

			// native object created in method
			type.addMethod("test2",      "{ let map = $.Test.createObject(); map.key2 = 123; return map; }").setIsStatic(true); // success
			type.addMethod("test3",      "{ let map = $.Test.createObject(); map.set('key2', 123); return map; }").setIsStatic(true); // failure

			// map created inline
			type.addMethod("test4",      "{ let map = new Map(); map.test = 'ignore'; map.set('key1', 'value1'); return map; }").setIsStatic(true); // success
			type.addMethod("test5",      "{ let map = new Map(); map.test = 'ignore'; map.key1 = 'value1'; return map; }").setIsStatic(true); // no error but empty map

			// map created in method
			type.addMethod("test6",      "{ let map = $.Test.createMap(); map.test = 'ignore'; map.set('key1', 'value1'); map.set('key2', 123); return map; }").setIsStatic(true);  // success
			type.addMethod("test7",      "{ let map = $.Test.createMap(); map.test = 'ignore'; map.key1 = 'value1'; map.key2 = 123; return map; }").setIsStatic(true); // failure

			// date created inline
			type.addMethod("test8",      "{ let date = new Date(); let nextMonth = (date.getMonth() + 1).toFixed(0); return nextMonth; }").setIsStatic(true);

			// date created in method
			type.addMethod("test9",      "{ let date = $.Test.createDate(); let nextMonth = (date.getMonth() + 1).toFixed(0); return nextMonth; }").setIsStatic(true);

			// set created in method
			type.addMethod("test10",      "{ let mySet = $.Test.createSet(); mySet.test = 'ignore'; mySet.add('initialValue'); mySet.add('value1'); mySet.add('value2'); return mySet; }").setIsStatic(true);
			type.addMethod("test11",      "{ let mySet = $.Test.createSet(); mySet.test = 'ignore'; mySet[0] = 'ignore'; mySet.add('value1'); return mySet; }").setIsStatic(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			// create a native object inline and set some properties => success
			final Map<String, Object> value0 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test0(); }}", "test0");
			assertEquals("value1",  value0.get("key1"));
			assertEquals(123,       value0.get("key2"));

			try {

				// create a native object inline and use obj.set() => failure
				Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test1(); }}", "test1");
				//fail("Object access via set() should not work");

			} catch (FrameworkException fex) {
				assertEquals("Server-side scripting error", fex.getMessage());
			}



			// create a native object in a method, return it, and set a property => success
			final Map<String, Object> value2 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test2(); }}", "test0");
			assertEquals("value1",  value2.get("key1"));
			assertEquals(123,       value2.get("key2"));

			try {
				// create a native object in a method, return it, and use obj.set() => failure
				Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test3(); }}", "test1");
				//fail("Object access via set() should not work");

			} catch (FrameworkException fex) {
				assertEquals("Server-side scripting error", fex.getMessage());
			}



			// create a native Map inline and set a property via set() => success
			final Map<String, Object> value4 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test4(); }}", "test0");
			assertEquals("value1",  value4.get("key1"));

			// create a native Map inline and set a property directly => empty map but no error
			final Map<String, Object> value5 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test5(); }}", "test5");
			assertEquals(0, value5.size());



			// create a native Map in a method and set a property via set() => success
			final Map<String, Object> value6 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test6(); }}", "test6");
			assertEquals("value1",  value6.get("key1"));

			// create a native Map in a method and set a property directly => empty map but no error
			final Map<String, Object> value7 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test7(); }}", "test7");
			assertEquals(0, value7.size());



			// create a native date, get the month value, add one and convert it to a string => success
			final Object value8 = Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test8(); }}", "test8");
			System.out.println(value8);


			// create a native date in a method, get the month value, add one and convert it to a string => success
			final Object value9 = Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test9(); }}", "test9");
			System.out.println(value9);



			// - crate a native set in a method, and add values via add() => success
			// - also test for Set functionality where duplicates are not added
			// - also ignore direct property access (at least in size calculation)
			final Set<Object> value10 = (Set)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test10(); }}", "test10");
			assertEquals(3, value10.size());

			// - create a native set in a method, add values via add() => success
			// - ignore setting of a value via array-index lookup
			// - also ignore direct property access (at least in size calculation)
			final Set<Object> value11 = (Set)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test11(); }}", "test11");
			assertEquals(2, value11.size());

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testThisInMethodCalls() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("getName", "{ return $.this.name; }");

			// native object created inline
			type.addMethod("test",  "{ let names = []; for (let t of $.find('Test')) { names.push(t.getName()); } return names.join(''); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String testClass = "Test";

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> list = new LinkedList<>();

			for (int i=0; i<3; i++) {

				list.add(app.create(testClass, "Test" + StringUtils.leftPad(Integer.toString(i), 2, "0")));
			}

			// create a native object inline and set some properties => success
			final String value = (String)Scripting.evaluate(new ActionContext(securityContext), list.get(0), "${{ $.this.test(); }}", "test");

			assertEquals("Test00Test01Test02", value);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testThatMethodParametersStillExistAfterMethodCalls() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("method1", "{ $.log($.methodParameters); $.userMethod(); return $.methodParameters; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "userMethod"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ $.log('test'); }")
			);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String testClass = "Test";

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(testClass, "Test");

			final Map<String, Object> value = (Map)Scripting.evaluate(new ActionContext(securityContext), node, "${{ $.this.method1({ key1: 'value1', key2: 123 }); }}", "test");
			assertEquals("value1", value.get("key1"));
			assertEquals(123,      value.get("key2"));

			System.out.println(value);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMapWrapping() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("method1", "{ let map = new Map(); map.set('key1', 'value1'); return $.this.method2({ test: map }); }");
			type.addMethod("method2", "{ let map = $.methodParameters.test; return map.get('key1'); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String testClass = "Test";

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(testClass, "Test");

			Scripting.evaluate(new ActionContext(securityContext), node, "${{ $.this.method1({ key1: 'value1', key2: 123 }); }}", "test");

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testDateWrappingAndFormats() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("getDate",             "{ return new Date(); }").setIsStatic(true);
			type.addMethod("getNowJavascript",    "{ return $.now; }").setIsStatic(true);
			type.addMethod("getNowStructrscript", "now").setIsStatic(true);

			type.addMethod("test1", "{ return { test1: new Date(), test2: $.now, test3: $.Test.getDate(), test4: $.Test.getNowJavascript(), test5: $.Test.getNowStructrscript() }; }").setIsStatic(true);
			type.addMethod("test2", "{ return $.Test.test1(); }").setIsStatic(true);
			type.addMethod("test3", "{ return $.Test.test2(); }").setIsStatic(true);
			type.addMethod("test4", "{ return { test1: typeof new Date(), test2: typeof $.now, test3: typeof $.Test.getDate(), test4: typeof $.Test.getNowJavascript(), test5: typeof $.Test.getNowStructrscript() }; }").setIsStatic(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Map<String, Object> result1 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test1(); }}", "test");
			final Map<String, Object> result2 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test2(); }}", "test");
			final Map<String, Object> result3 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test3(); }}", "test");

			// check values (only need to check first result because we're checking for equality below)
			final Object value1 = result1.get("test1");
			final Object value2 = result1.get("test2");
			final Object value3 = result1.get("test3");
			final Object value4 = result1.get("test4");
			final Object value5 = result1.get("test5");

			assertEquals(ZonedDateTime.class, value1.getClass());
			assertEquals(ZonedDateTime.class, value2.getClass());
			assertEquals(ZonedDateTime.class, value3.getClass());
			assertEquals(ZonedDateTime.class, value4.getClass());
			assertEquals(String.class,        value5.getClass());

			final String zonedDateTimePattern = "[0-9]{4}\\-[0-9]{2}\\-[0-9]{2}T[0-9]{2}\\:[0-9]{2}\\:[0-9]{2}\\+[0-9]{4}";
			final String pattern              = ISO8601DateProperty.getDefaultFormat();
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

			System.out.println("FORMAT: " + pattern);

			final String formatted1 = formatter.format((ZonedDateTime)value1);
			final String formatted2 = formatter.format((ZonedDateTime)value2);
			final String formatted3 = formatter.format((ZonedDateTime)value3);
			final String formatted4 = formatter.format((ZonedDateTime)value4);


			System.out.println(formatted1);
			System.out.println(formatted2);
			System.out.println(formatted3);
			System.out.println(formatted4);

			assertTrue(formatted1.matches(zonedDateTimePattern));
			assertTrue(formatted2.matches(zonedDateTimePattern));
			assertTrue(formatted3.matches(zonedDateTimePattern));
			assertTrue(formatted4.matches(zonedDateTimePattern));
			assertTrue(value5.toString().matches(zonedDateTimePattern));

			// assert type (!) equality of all result entries (=> there should be no difference in the types dependinPg on their level in the object structure)
			assertEquals(result1.get("test1").getClass(), result2.get("test1").getClass());
			assertEquals(result1.get("test2").getClass(), result2.get("test2").getClass());
			assertEquals(result1.get("test3").getClass(), result2.get("test3").getClass());
			assertEquals(result1.get("test4").getClass(), result2.get("test4").getClass());
			assertEquals(result1.get("test5").getClass(), result2.get("test5").getClass());
			assertEquals(result2.get("test1").getClass(), result3.get("test1").getClass());
			assertEquals(result2.get("test2").getClass(), result3.get("test2").getClass());
			assertEquals(result2.get("test3").getClass(), result3.get("test3").getClass());
			assertEquals(result2.get("test4").getClass(), result3.get("test4").getClass());
			assertEquals(result2.get("test5").getClass(), result3.get("test5").getClass());

			// test Javascript typeof result
			final Map<String, Object> result4 = (Map)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.Test.test4(); }}", "test");

			assertEquals("object", result4.get("test1"));
			assertEquals("object", result4.get("test2"));
			assertEquals("object", result4.get("test3"));
			assertEquals("object", result4.get("test4"));
			assertEquals("string", result4.get("test5"));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testDateFormatWithZonedDateTime() {

		try (final Tx tx = app.tx()) {

			final String result1 = (String)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.dateFormat(new Date(), 'yyyy-MM-dd'); }}", "test1");
			final String result2 = (String)Scripting.evaluate(new ActionContext(securityContext), null, "${{ $.dateFormat($.now, 'yyyy-MM-dd'); }}", "test2");

			final String expected = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

			assertEquals(expected, result1);
			assertEquals(expected, result2);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testEntityBindingAcrossMultipleMethodCalls() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type = schema.addType("Test");

			type.addMethod("onCreate", "{ $.userMethod(); $.log($.this.id); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "userMethod"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ $.log('test'); }")
			);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String testClass = "Test";

		try (final Tx tx = app.tx()) {

			app.create(testClass, "Test");
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception. It is likely that entity in binding has been set incorrectly throughout the call chain.");
		}
	}

	@Test
	public void testInterScriptCallWithMap() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type = schema.addType("Test");

			type.addMethod("t1", "{let context = {map: new Map()}; context.map.set('a', 123); $.Test.t2({context});}").setIsStatic(true);
			// Explicitly add newline at the end of script to check trimming of source code when determining script language
			type.addMethod("t2", "{const migrationContext = $.methodParameters.context.map; migrationContext.set('b', 456); migrationContext.forEach((e) => {$.log(e);});}\n").setIsStatic(true);
			type.addMethod("onCreate", "{ $.Test.t1(); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String testClass = "Test";

		try (final Tx tx = app.tx()) {

			app.create(testClass, "Test");
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception. Map binding was not currently passed to second context. This could be due to incorrect engine parsing for the source.");
		}
	}

	@Test
	public void testDateConversions() {

		final String src = IOUtils.readFull(ScriptingTest.class.getResourceAsStream("/test/scripting/testDateConversions.js"));

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			final Object result1 = Scripting.evaluate(ctx, null, src, "test1");

			final ContextStore store = ctx.getContextStore();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testGetOwnPropertyNames() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("doTest1",             "{ return 'test1'; }");
			type.addMethod("doTest2",             "{ return 'test2'; }").setIsStatic(true);
			type.addMethod("doTest3",             "{ return 'test3'; }").setIsPrivate(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface test   = app.create("Test", "test");
			final List<String> result1 = (List) Scripting.evaluate(new ActionContext(securityContext), test, "${{ Object.getOwnPropertyNames($.this); }}", "test");
			final Set<String> expected = new LinkedHashSet<>();

			expected.add(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			expected.add(NodeInterfaceTraitDefinition.HIDDEN_PROPERTY);
			expected.add(NodeInterfaceTraitDefinition.OWNER_PROPERTY);
			expected.add(NodeInterfaceTraitDefinition.OWNER_ID_PROPERTY);
			expected.add(NodeInterfaceTraitDefinition.GRANTEES_PROPERTY);
			expected.add(GraphObjectTraitDefinition.BASE_PROPERTY);
			expected.add(GraphObjectTraitDefinition.TYPE_PROPERTY);
			expected.add(GraphObjectTraitDefinition.ID_PROPERTY);
			expected.add(GraphObjectTraitDefinition.CREATED_DATE_PROPERTY);
			expected.add(GraphObjectTraitDefinition.CREATED_BY_PROPERTY);
			expected.add(GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY);
			expected.add(GraphObjectTraitDefinition.LAST_MODIFIED_BY_PROPERTY);
			expected.add(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
			expected.add(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);

			// new: methods (non-lifecycle)
			expected.add("doTest1");
			expected.add("doTest2");

			expected.removeAll(result1);

			assertEquals("Invalid scripting reflection result", Set.of(), expected);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testFunctionInfoFunction() {


		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("doTest1", "{ $.log($.functionInfo()); return $.functionInfo(); }").addParameter("id", "string").addParameter("date", "date");
			type.addMethod("doTest2", "{ $.log($.functionInfo()); return $.functionInfo(); }").setIsStatic(true);
			type.addMethod("doTest3", "{ $.log($.functionInfo()); return $.functionInfo(); }").setIsPrivate(true);
			type.addMethod("doTest4", "{ $.log($.functionInfo()); return $.this.doTest1(); }");
			type.addMethod("doTest5", "{ $.log($.functionInfo()); return $.this.doTest4(); }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface test            = app.create("Test", "test");

			// test successful execution
			final Map<String, Object> result1 = (Map) Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.this.doTest1(); }}", "test");
			final Map<String, Object> result2 = (Map) Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.Test.doTest2(); }}", "test");
			final Map<String, Object> result3 = (Map) Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.this.doTest3(); }}", "test");
			final Map<String, Object> result4 = (Map) Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.this.doTest4(); }}", "test");
			final Map<String, Object> result5 = (Map) Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.this.doTest5(); }}", "test");
			final Map<String, Object> result6 = (Map) Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.functionInfo('Test', 'doTest1'); }}", "test");

			final String result7 = (String)Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.functionInfo('TestWrong', 'doTest1'); }}", "test");
			final String result8 = (String)Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.functionInfo('Test', 'wrongName'); }}", "test");
			final String result9 = (String)Scripting.evaluate(new ActionContext(securityContext), test, "${{ $.functionInfo('Test'); }}", "test");


			assertEquals("Invalid functionInfo() result", "doTest1", result1.get(NodeInterfaceTraitDefinition.NAME_PROPERTY));
			assertEquals("Invalid functionInfo() result", "doTest2", result2.get(NodeInterfaceTraitDefinition.NAME_PROPERTY));
			assertEquals("Invalid functionInfo() result", "doTest3", result3.get(NodeInterfaceTraitDefinition.NAME_PROPERTY));
			assertEquals("Invalid functionInfo() result", "doTest1", result4.get(NodeInterfaceTraitDefinition.NAME_PROPERTY));
			assertEquals("Invalid functionInfo() result", "doTest1", result5.get(NodeInterfaceTraitDefinition.NAME_PROPERTY));

			assertEquals("Invalid functionInfo() result", false, result1.get(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY));
			assertEquals("Invalid functionInfo() result", false, result1.get(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY));
			assertEquals("Invalid functionInfo() result", "POST", result1.get(SchemaMethodTraitDefinition.HTTP_VERB_PROPERTY));
			assertEquals("Invalid functionInfo() result", "string", ((Map)result1.get(SchemaMethodTraitDefinition.PARAMETERS_PROPERTY)).get("id"));
			assertEquals("Invalid functionInfo() result", "date", ((Map)result1.get(SchemaMethodTraitDefinition.PARAMETERS_PROPERTY)).get("date"));

			assertEquals("Invalid functionInfo() result", true,  result2.get(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY));
			assertEquals("Invalid functionInfo() result", false, result2.get(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY));

			assertEquals("Invalid functionInfo() result", false, result3.get(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY));
			assertEquals("Invalid functionInfo() result", true,  result3.get(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY));

			assertEquals("Invalid functionInfo() result", false, result4.get(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY));
			assertEquals("Invalid functionInfo() result", false, result4.get(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY));

			assertEquals("Invalid functionInfo() result", false, result5.get(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY));
			assertEquals("Invalid functionInfo() result", false, result5.get(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY));

			assertEquals("Invalid functionInfo() result", false,    result6.get(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY));
			assertEquals("Invalid functionInfo() result", false,    result6.get(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY));
			assertEquals("Invalid functionInfo() result", "POST",   result6.get(SchemaMethodTraitDefinition.HTTP_VERB_PROPERTY));
			assertEquals("Invalid functionInfo() result", "string", ((Map)result6.get(SchemaMethodTraitDefinition.PARAMETERS_PROPERTY)).get("id"));
			assertEquals("Invalid functionInfo() result", "date",   ((Map)result6.get(SchemaMethodTraitDefinition.PARAMETERS_PROPERTY)).get("date"));

			assertEquals("Invalid functionInfo() error result", FunctionInfoFunction.ERROR_MESSAGE_FUNCTION_INFO_JS,  result7);
			assertEquals("Invalid functionInfo() error result", FunctionInfoFunction.ERROR_MESSAGE_FUNCTION_INFO_JS,  result8);
			assertEquals("Invalid functionInfo() error result", FunctionInfoFunction.ERROR_MESSAGE_FUNCTION_INFO_JS,  result9);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	// ----- private methods ----
	private void createTestType(final JsonSchema schema, final String name, final String createSource, final String saveSource) {

		final JsonType test1    = schema.addType(name);

		test1.addStringProperty("c");
		test1.addStringProperty("s");

		test1.addMethod("onCreation",     createSource);
		test1.addMethod("onModification", saveSource);

	}

	private Map<String, Object> getLoggedModifications(final NodeInterface obj) {

		final String log = obj.getProperty(obj.getTraits().key("log"));

		return new GsonBuilder().create().fromJson(log, Map.class);
	}

	private void assertMapPathValueIs(final Map<String, Object> map, final String mapPath, final Object value) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof List) {

					final List list = (List)current;
					if (index >= list.size()) {

						// value for nonexisting fields must be null
						assertEquals("Invalid map path result for " + mapPath, value, null);

						// nothing more to check here
						return;

					} else {

						current = list.get(index);
					}
				}

			} else if ("#".equals(part) && current instanceof List) {

				assertEquals("Invalid collection size for " + mapPath, value, ((List)current).size());

				// nothing more to check here
				return;

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		// ignore type of value if numerical (GSON defaults to double...)
		if (value instanceof Number && current instanceof Number) {

			assertEquals("Invalid map path result for " + mapPath, ((Number)value).doubleValue(), ((Number)current).doubleValue(), 0.0);

		} else {

			assertEquals("Invalid map path result for " + mapPath, value, current);
		}
	}

	private String formatDateTestScript(final String parseMethod, final String start, final String end, final String fieldName) {

		final StringBuilder buf = new StringBuilder();

		buf.append("${{ let startDate = ");
		buf.append(parseMethod);
		buf.append("('");
		buf.append(start);
		buf.append("'); let endDate = ");
		buf.append(parseMethod);
		buf.append("('");
		buf.append(end);
		buf.append("'); $.find('Project', { ");
		buf.append(fieldName);
		buf.append(": $.predicate.range(startDate, endDate) }, $.predicate.sort('name')); }}");

		return buf.toString();
	}
}