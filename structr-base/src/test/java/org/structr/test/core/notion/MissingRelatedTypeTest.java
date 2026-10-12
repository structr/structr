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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseFeature;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.GraphDatabaseCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.DynamicRelationshipTraitDefinition;
import org.structr.schema.SchemaService;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * What a relationship property reports when the related node cannot be resolved.
 *
 * The related type of a relationship property comes from its schema relationship definition: the schema node it
 * is linked to, or the type named in sourceType/targetType. A definition that has neither still builds, and the
 * property then has no related type. IdDeserializationStrategy used to look up the given uuid without a type and
 * reject the node it found with a type mismatch naming the Java class of the node, which is AbstractNode for every
 * node since the traits migration:
 *
 *   {"code":422,"message":"Node type mismatch","errors":[{"token":"must_be_of_type","type":"AbstractNode"}]}
 *
 * That pointed at the input and at a type that does not exist, while the cause was in the schema. How a definition
 * of an existing installation got into that state is not known, so the state is produced here with Cypher.
 */
public class MissingRelatedTypeTest extends StructrTest {

	private static final String PROJECT = "Project";

	@Test
	public void testSchemaReloadWarnsAboutRelationshipWithoutTargetType() {

		if (!supportsCypher()) {
			return;
		}

		createTestSchema();
		removeTargetOfRelationship();

		final Logger logger                        = (Logger) LoggerFactory.getLogger(DynamicRelationshipTraitDefinition.class);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try {

			SchemaService.reloadSchema(new ErrorBuffer(), null, true, false);

		} finally {

			logger.detachAppender(appender);
			appender.stop();
		}

		final boolean warned = appender.list.stream().anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("ASSIGNED_TO") && e.getFormattedMessage().contains("has no target type"));

		assertTrue("The schema reload must warn about the relationship definition without a target type.", warned);
	}

	@Test
	public void testAssignmentWithoutRelatedTypeIsReportedAsSchemaProblem() {

		if (!supportsCypher()) {
			return;
		}

		createTestSchema();

		final String projectId = createNode(PROJECT, "project");
		final String userId    = createNode(StructrTraits.USER, "user");

		removeTargetOfRelationship();
		SchemaService.reloadSchema(new ErrorBuffer(), null, true, false);

		try (final Tx tx = app.tx()) {

			final NodeInterface project = app.getNodeById(PROJECT, projectId);

			assertNotNull("The project must be found.", project);

			// the route of an EAM update action: inputTypeToJavaType on the type of the target, then setProperties
			project.setProperties(securityContext, PropertyMap.inputTypeToJavaType(securityContext, PROJECT, Map.of("assignedUser", userId)));

			tx.success();

			fail("Assigning a related node through a relationship without a related type must fail.");

		} catch (FrameworkException fex) {

			assertEquals("A relationship without a related type is a schema problem, not a problem with the input: " + fex.toJSON(), 500, fex.getStatus());
			assertTrue("The message must name the property: " + fex.getMessage(), fex.getMessage().contains("assignedUser"));
			assertTrue("The message must say what is missing: " + fex.getMessage(), fex.getMessage().contains("has no related type"));
		}
	}

	@Test
	public void testTypeMismatchNamesTheTypeOfTheNode() {

		createTestSchema();

		final String projectId = createNode(PROJECT, "project");
		final String groupId   = createNode(StructrTraits.GROUP, "group");

		try (final Tx tx = app.tx()) {

			final NodeInterface project = app.getNodeById(PROJECT, projectId);
			final NodeInterface group   = app.getNodeById(StructrTraits.GROUP, groupId);

			project.setProperties(securityContext, PropertyMap.inputTypeToJavaType(securityContext, PROJECT, Map.of("assignedUser", group)));

			tx.success();

			fail("Assigning a Group to a property of type User must fail.");

		} catch (FrameworkException fex) {

			final List<ErrorToken> tokens = fex.getErrorBuffer().getErrorTokens();

			assertFalse("The rejection carries no error token.", tokens.isEmpty());

			final ErrorToken token = tokens.get(0);

			assertEquals("must_be_of_type", token.getToken());
			assertEquals("The token must name the type of the node, not its Java class: " + token.toJSON(), StructrTraits.GROUP, token.getType());
			assertEquals("assignedUser", token.getProperty());
			assertEquals(StructrTraits.USER, token.getDetail());
		}
	}

	// ----- private methods -----
	private boolean supportsCypher() {

		return Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query");
	}

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

	private String createNode(final String type, final String name) {

		try (final Tx tx = app.tx()) {

			final String uuid = app.create(type, name).getUuid();

			tx.success();

			return uuid;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while creating a node of type " + type + ".");

			return null;
		}
	}

	/**
	 * Unlinks the relationship definition from its target schema node and removes the targetType property, which the
	 * typed API does not allow: the validation of SchemaRelationshipNode requires one of them.
	 */
	private void removeTargetOfRelationship() {

		try (final Tx tx = app.tx()) {

			final DatabaseService<?> graphDb  = app.command(GraphDatabaseCommand.class).execute();
			final NativeQuery<Iterable> query = graphDb.query(
				"MATCH (r:SchemaRelationshipNode:" + randomTenantId + " { relationshipType: 'ASSIGNED_TO' }) "
				+ "OPTIONAL MATCH (r)-[e:IS_RELATED_TO]->(:SchemaNode) "
				+ "DELETE e REMOVE r.targetType RETURN DISTINCT r",
				Iterable.class
			);

			final int count = Iterables.count(graphDb.execute(query));

			assertEquals("Exactly one relationship definition must have been changed.", 1, count);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while removing the target of the relationship definition.");
		}

		// the definition was changed behind the framework's back, so anything cached about it is stale
		FlushCachesCommand.flushAll();
	}
}
