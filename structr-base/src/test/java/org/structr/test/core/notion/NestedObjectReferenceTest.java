/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.core.notion;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaRelationshipNodeTraitDefinition;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * That writing to a node reached through a nested reference is opt-in, and gated on the
 * SecurityContext rather than on the input document.
 *
 * An input document may reference an existing node by a unique key ({"department": {"code": "ENG"}}),
 * and may carry further keys for that node alongside the one that resolves it. Whether those further
 * keys are applied depends on the "setNestedProperties" attribute of the SecurityContext:
 * DeserializationStrategy.setProperties writes nothing at all unless it is set. In this repository
 * JsonRestServlet, RESTCallHandler and XMLFileImportJob set it, but it is an ordinary attribute and
 * any caller may opt in: the Companion sets it before app.create so that a nested document behaves
 * in-process the way it does over REST.
 *
 * This is worth pinning down because it makes in-process code behave unlike a REST client for the
 * same document, which is a trap when reasoning about which writes a nested reference performs. The
 * REST side of the same question - whether a redundant value runs the modification chain - is covered
 * by NoOpModificationTest, which goes through HTTP so the flag is set the way production sets it.
 */
public class NestedObjectReferenceTest extends StructrTest {

	private static final String DEPARTMENT = "Department";
	private static final String EMPLOYEE   = "Employee";

	@Test
	public void testNestedPropertiesAreIgnoredWithoutTheSecurityContextFlag() {

		createTestSchema();

		final String departmentId = createDepartment("ENG");
		final Date before         = lastModified(departmentId);

		assertNotNull("no lastModifiedDate on the department", before);

		pause();
		createEmployee("Alice", Map.of("code", "ENG", "description", "changed"), false);

		assertNull("a nested property was applied although setNestedProperties was not set", description(departmentId));
	}

	@Test
	public void testNestedPropertiesAreAppliedWithTheSecurityContextFlag() {

		createTestSchema();

		final String departmentId = createDepartment("ENG");

		pause();
		createEmployee("Bob", Map.of("code", "ENG", "description", "changed"), true);

		assertEquals("a nested property was not applied although setNestedProperties was set", "changed", description(departmentId));
	}

	@Test
	public void testSetPropertyWithTheSameValueDoesNotModifyTheNode() {

		createTestSchema();

		final String departmentId = createDepartment("ENG");
		final Date before         = lastModified(departmentId);

		assertNotNull("no lastModifiedDate on the department", before);

		// the single-key route, bypassing the filter in setProperties
		pause();
		setDescription(departmentId, null);

		assertEquals("writing null over null modified the node", before, lastModified(departmentId));

		pause();
		setDescription(departmentId, "first");

		final Date afterRealChange = lastModified(departmentId);

		assertTrue("a real change was not detected", !before.equals(afterRealChange));

		// and now the same value again, through setProperty
		pause();
		setDescription(departmentId, "first");

		final Date afterNoOp = lastModified(departmentId);

		// documents the current behaviour: this is the write that has no equality guard
		assertEquals(
			"setProperty with an identical value no longer marks the node modified - if this fails, "
			+ "a no-op guard was added and GraphObjectModificationState.modify should be reviewed",
			afterRealChange,
			afterNoOp
		);
	}

	@Test
	public void testSetPropertyWithEqualArrayContentMustNotModifyTheNode() {

		createTestSchema();

		final String departmentId = createDepartment("ENG");

		pause();
		setTags(departmentId, new String[] { "a", "b" });

		final Date afterFirstWrite = lastModified(departmentId);

		// control first, so a broken signal cannot make the assertion below pass for the wrong reason:
		// changed content MUST still modify the node
		pause();
		setTags(departmentId, new String[] { "a", "c" });

		final Date afterRealChange = lastModified(departmentId);

		assertTrue(
			"writing different array content did not modify the node, so lastModifiedDate is not a "
			+ "usable signal here and the assertion below would prove nothing",
			!afterFirstWrite.equals(afterRealChange)
		);

		// and now the same content again
		pause();
		setTags(departmentId, new String[] { "a", "c" });

		final Date afterEqualWrite = lastModified(departmentId);

		// KNOWN TO FAIL. The two write routes in PropertyContainerTraitDefinition do not agree:
		//
		//   SetProperties (line 208, what a REST request uses)  Objects.deepEquals -> equal arrays filtered
		//   SetProperty   (line 172, a single key)              oldValue.equals    -> never equal, always writes
		//
		// equals on an array is identity, so two arrays of equal content are never equal and the write
		// goes through: lastModifiedDate moves, the changelog grows and onModification runs, for a write
		// that changed nothing. Any script or Java caller assigning an array-valued key one key at a
		// time is affected; clients are not, because REST takes the other route.
		//
		// The fix is one word - deepEquals at line 172 - but it changes when onModification fires for
		// array properties, so it belongs in a major release, and whether the asymmetry was deliberate
		// has not been established with the author.
		// compared as milliseconds on purpose: Date.toString() prints to the second, so a failure message
		// built from the Dates themselves shows two identical-looking timestamps and reads as nonsense
		assertEquals(
			"writing the SAME array content through setProperty modified the node (lastModifiedDate moved). "
			+ "SetProperty compares with oldValue.equals(value), and equals on an array is identity, so equal "
			+ "content is treated as a change. SetProperties, which a REST request uses, compares with "
			+ "Objects.deepEquals and gets this right.",
			afterRealChange.getTime(),
			afterEqualWrite.getTime()
		);
	}

	// ----- private methods -----
	private void createTestSchema() {

		try (final Tx tx = app.tx()) {

			final Traits schemaNode = Traits.of(StructrTraits.SCHEMA_NODE);
			final Traits schemaProp = Traits.of(StructrTraits.SCHEMA_PROPERTY);

			final NodeInterface department = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(schemaNode.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), DEPARTMENT)
			);

			final NodeInterface employee = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(schemaNode.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), EMPLOYEE)
			);

			// the key a reference resolves on. It has to be UNIQUE: IdDeserializationStrategy only
			// queries for keys that are unique or compound, or identifying, and identifying means
			// name/eMail on Principal types only, so a plain "name" resolves nothing at all.
			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(schemaProp.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   department),
				new NodeAttribute<>(schemaProp.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),           "code"),
				new NodeAttribute<>(schemaProp.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(schemaProp.key(SchemaPropertyTraitDefinition.UNIQUE_PROPERTY),        true)
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(schemaProp.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   department),
				new NodeAttribute<>(schemaProp.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),           "tags"),
				new NodeAttribute<>(schemaProp.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "StringArray")
			);

			// a non-identifying property, so a control case has something to change
			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(schemaProp.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   department),
				new NodeAttribute<>(schemaProp.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),           "description"),
				new NodeAttribute<>(schemaProp.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String")
			);

			final Traits rel = Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE);

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(rel.key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY),          employee),
				new NodeAttribute<>(rel.key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY),          department),
				new NodeAttribute<>(rel.key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY), "*"),
				new NodeAttribute<>(rel.key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY), "1"),
				new NodeAttribute<>(rel.key(SchemaRelationshipNodeTraitDefinition.SOURCE_JSON_NAME_PROPERTY),    "employees"),
				new NodeAttribute<>(rel.key(SchemaRelationshipNodeTraitDefinition.TARGET_JSON_NAME_PROPERTY),    "department"),
				new NodeAttribute<>(rel.key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY),   "WORKS_IN")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while creating the schema.");
		}
	}

	private String createDepartment(final String code) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(DEPARTMENT,
				new NodeAttribute<>(Traits.of(DEPARTMENT).key("code"), code)
			);

			tx.success();

			return node.getUuid();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while creating the department.");
		}

		return null;
	}

	/**
	 * Each case gets its own SecurityContext: the flag is an attribute of the context, and a context
	 * shared between test methods would carry it from one to the next and make the outcome depend on
	 * the order the methods happen to run in.
	 */
	private void createEmployee(final String name, final Map<String, Object> nestedDepartment, final boolean allowNestedWrites) {

		final SecurityContext context = SecurityContext.getSuperUserInstance();

		if (allowNestedWrites) {

			// what the REST layer does for every incoming request
			context.setAttribute("setNestedProperties", true);
		}

		final App localApp = StructrApp.getInstance(context);

		try (final Tx tx = localApp.tx()) {

			final Map<String, Object> input = new LinkedHashMap<>();

			input.put("name",       name);
			input.put("department", nestedDepartment);

			localApp.create(EMPLOYEE, PropertyMap.inputTypeToJavaType(context, EMPLOYEE, input));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while creating the employee.");
		}
	}

	/** Writes one key directly, the route that skips the deserialization strategies entirely. */
	private void setDescription(final String uuid, final String value) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = StructrApp.getInstance(securityContext).getNodeById(DEPARTMENT, uuid);

			assertNotNull("department " + uuid + " not found", node);

			node.setProperty(Traits.of(DEPARTMENT).key("description"), value);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while writing the description.");
		}
	}

	private void setTags(final String uuid, final String[] value) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = StructrApp.getInstance(securityContext).getNodeById(DEPARTMENT, uuid);

			assertNotNull("department " + uuid + " not found", node);

			node.setProperty(Traits.of(DEPARTMENT).<String[]>key("tags"), value);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while writing the tags.");
		}
	}

	private String description(final String uuid) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = StructrApp.getInstance(securityContext).getNodeById(DEPARTMENT, uuid);

			assertNotNull("department " + uuid + " not found", node);

			final String value = node.getProperty(Traits.of(DEPARTMENT).<String>key("description"));

			tx.success();

			return value;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while reading the description.");
		}

		return null;
	}

	private Date lastModified(final String uuid) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = StructrApp.getInstance(securityContext).getNodeById(DEPARTMENT, uuid);

			assertNotNull("department " + uuid + " not found", node);

			final PropertyKey<Date> key = Traits.of(DEPARTMENT).key(GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY);
			final Date value            = node.getProperty(key);

			tx.success();

			return value;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while reading lastModifiedDate.");
		}

		return null;
	}

	/**
	 * lastModifiedDate has millisecond resolution, so two writes in the same millisecond would be
	 * indistinguishable and an assertion of "not modified" would hold for the wrong reason.
	 */
	private void pause() {

		try {
			Thread.sleep(20);

		} catch (InterruptedException iex) {
			Thread.currentThread().interrupt();
		}
	}
}
