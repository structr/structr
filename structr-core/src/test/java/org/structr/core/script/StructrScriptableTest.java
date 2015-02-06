package org.structr.core.script;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.TestUser;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.schema.ConfigurationProvider;


/**
 *
 * @author Christian Morgner
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

			// properties
			sourceNode.setProperty(new StringProperty("_testBoolean"), "Boolean");
			sourceNode.setProperty(new StringProperty("_testInteger"), "Integer");
			sourceNode.setProperty(new StringProperty("_testString"),  "String");
			sourceNode.setProperty(new StringProperty("_testDouble"),  "Double");
			sourceNode.setProperty(new StringProperty("_testEnum"),    "Enum(OPEN,CLOSED,TEST)");
			sourceNode.setProperty(new StringProperty("_testDate"),    "Date");

			// methods
			sourceNode.setProperty(new StringProperty("___onCreate"), "{ var e = Structr.get('this'); e.targets = Structr.find('Target'); }");
			sourceNode.setProperty(new StringProperty("___doTest01"), "{ var e = Structr.get('this'); e.testEnum = 'OPEN'; }");
			sourceNode.setProperty(new StringProperty("___doTest02"), "{ var e = Structr.get('this'); e.testEnum = 'CLOSED'; }");
			sourceNode.setProperty(new StringProperty("___doTest03"), "{ var e = Structr.get('this'); e.testEnum = 'TEST'; }");
			sourceNode.setProperty(new StringProperty("___doTest04"), "{ var e = Structr.get('this'); e.testEnum = 'INVALID'; }");
			sourceNode.setProperty(new StringProperty("___doTest05"), "{ var e = Structr.get('this'); e.testBoolean = true; e.testInteger = 123; e.testString = 'testing..'; e.testDouble = 1.2345; e.testDate = new Date(" + currentTimeMillis + "); }");

			final PropertyMap properties = new PropertyMap();

			properties.put(SchemaRelationship.sourceJsonName, "source");
			properties.put(SchemaRelationship.targetJsonName, "targets");
			properties.put(SchemaRelationship.sourceMultiplicity, "*");
			properties.put(SchemaRelationship.targetMultiplicity, "*");
			properties.put(SchemaRelationship.relationshipType, "HAS");

			app.create(sourceNode, targetNode, SchemaRelationship.class, properties);

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
			sourceNode.invokeMethod("doTest04", Collections.EMPTY_MAP, true);
			assertEquals("Invalid setProperty result for EnumProperty",    testEnumType.getEnumConstants()[2], sourceNode.getProperty(testEnumProperty));

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

		/**
		 * This test creates two connected SchemaNodes and tests the script-based
		 * association of one instance with several others in the onCreate method.
		 */


		// setup phase: create schema nodes
		try (final Tx tx = app.tx()) {

			// create two nodes and associate them with each other
			final SchemaNode sourceNode  = createTestNode(SchemaNode.class, "Source");

			sourceNode.setProperty(new StringProperty("___doTest01"), "{ var e = Structr.get('this'); e.grant(Structr.find('TestUser')[0], 'read', 'write'); }");

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
}
