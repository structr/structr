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

import org.structr.api.DatabaseFeature;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * Whether a relationship property still resolves its related node after the schema was reloaded at runtime.
 *
 * Adding a method creates a SchemaMethod, and that reloads the schema as a TransactionPostProcess. Post processes
 * run before the transaction is committed, so the reload runs in a nested transaction of the request that added
 * the method: its own prefetch hint is ignored, and it reads through the caches of that request. A reload at
 * startup runs in a transaction of its own.
 */
public class RuntimeSchemaReloadTest extends StructrTest {

	private static final String PROJECT = "Project";

	@Test
	public void testAssignmentAfterAddingMethodToOwnType() {

		createTestSchema();

		final Ids ids = createNodes();

		addMethod(PROJECT, "doSomething");
		assign(ids);
	}

	@Test
	public void testAssignmentAfterAddingMethodToUserType() {

		createTestSchema();

		final Ids ids = createNodes();

		addMethod(StructrTraits.USER, "doSomething");
		assign(ids);
	}

	@Test
	public void testAssignmentAfterAddingMethodsWithPrefetchingAndAbstractNodeLabels() {

		if (!Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			return;
		}

		final int threshold = Settings.PrefetchingThreshold.getValue();

		try {

			// the request hint learns its patterns from the reloads it contains, which a grown schema makes long enough
			Settings.PrefetchingThreshold.setValue(1);

			createTestSchema();

			final Ids ids = createNodes();

			addAbstractNodeLabelToAllNodes();

			for (int i = 0; i < 4; i++) {

				addMethod(i % 2 == 0 ? PROJECT : StructrTraits.USER, "doSomething" + i);
				assign(ids);
			}

		} finally {

			Settings.PrefetchingThreshold.setValue(threshold);
		}
	}

	// ----- private methods -----
	private void createTestSchema() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType project = schema.addType(PROJECT);
			final JsonObjectType user    = (JsonObjectType) schema.getType(StructrTraits.USER);

			project.relate(user, "ASSIGNED_TO", Cardinality.ManyToOne, "assignedProjects", "assignedUser");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception while creating the schema.");
		}
	}

	private Ids createNodes() {

		try (final Tx tx = app.tx()) {

			final Ids ids = new Ids(app.create(PROJECT, "project").getUuid(), app.create(StructrTraits.USER, "user").getUuid());

			tx.success();

			return ids;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while creating the nodes.");

			return null;
		}
	}

	/**
	 * Adds a method the way a request does, with a prefetch hint, so the schema reload of the post process runs
	 * inside it.
	 */
	private void addMethod(final String typeName, final String methodName) {

		try (final Tx tx = app.tx()) {

			tx.prefetchHint("HTTP POST /structr/rest/SchemaMethod");

			final Traits schemaNodeTraits = Traits.of(StructrTraits.SCHEMA_NODE);
			final Traits methodTraits     = Traits.of(StructrTraits.SCHEMA_METHOD);
			final NodeInterface type      = app.nodeQuery(StructrTraits.SCHEMA_NODE).key(schemaNodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), typeName).getFirst();

			assertNotNull("The schema node " + typeName + " must exist.", type);

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), type),
				new NodeAttribute<>(methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),      methodName),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),     "{ return 1; }")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while adding method " + methodName + " to " + typeName + ": " + fex.toJSON());
		}
	}

	private void assign(final Ids ids) {

		try (final Tx tx = app.tx()) {

			tx.prefetchHint("HTTP POST /structr/rest/DOMElement/<uuid>/event");

			final NodeInterface project = app.getNodeById(PROJECT, ids.project());

			assertNotNull("The project must be found.", project);

			// the route of an EAM update action: inputTypeToJavaType on the type of the target, then setProperties
			project.setProperties(securityContext, PropertyMap.inputTypeToJavaType(securityContext, PROJECT, Map.of("assignedUser", ids.user())));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Assigning the user by uuid failed after a runtime schema reload: " + fex.toJSON());
		}
	}

	private void addAbstractNodeLabelToAllNodes() {

		try (final Tx tx = app.tx()) {

			final DatabaseService<?> graphDb  = app.command(GraphDatabaseCommand.class).execute();
			final NativeQuery<Iterable> query = graphDb.query("MATCH (n:" + randomTenantId + ") SET n:AbstractNode RETURN n", Iterable.class);

			Iterables.count(graphDb.execute(query));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while adding the AbstractNode label.");
		}

		FlushCachesCommand.flushAll();
	}

	private record Ids(String project, String user) {
	}
}
