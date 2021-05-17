/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.test.rest.resource;

import com.jayway.restassured.RestAssured;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.graphql.GraphQLTest;
import org.structr.test.rest.common.StructrRestTestBase;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

/**
 *
 *
 */
public class OpenAPIEndpointTest extends StructrRestTestBase {

	@Test
	public void testUnfilteredDefaultSchema() {

		RestAssured.basePath = "/structr/openapi";

		final Map<String, Object> response = fetchOpenAPI("/");

		GraphQLTest.assertMapPathValueIs(response, "openapi",      "3.0.2");
		GraphQLTest.assertMapPathValueIs(response, "info.title",   "Structr REST Server");
		GraphQLTest.assertMapPathValueIs(response, "info.version", "1.0.1");

		GraphQLTest.assertMapPathValueIs(response, "servers.0.url", "http://" + host + ":" + httpPort + restUrl);

		// components
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.#",  3);

		GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.name",           "page");
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.in",             "query");
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.schema.type",    "integer");
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.schema.default", 1);

		GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.name",           "pageSize");
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.in",             "query");
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.schema.type",    "integer");

		GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.name",           "loose");
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.in",             "query");
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.schema.type",    "boolean");
		GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.schema.default", false);

		// responses

		// 200 OK
		assertNotNull("Missing response schema for 200 OK",       GraphQLTest.getMapPathValue(response, "components.responses.ok.content.application/json.schema"));
		assertNotNull("Missing response example for 200 Ok",      GraphQLTest.getMapPathValue(response, "components.responses.ok.content.application/json.example"));

		// 201 Created
		assertNotNull("Missing response schema for 201 Created",  GraphQLTest.getMapPathValue(response, "components.responses.created.content.application/json.schema"));
		assertNotNull("Missing response example for 201 Created", GraphQLTest.getMapPathValue(response, "components.responses.created.content.application/json.example"));

		// 400 Bad Request
		assertNotNull("Missing response schema for 400 Bad Request",  GraphQLTest.getMapPathValue(response, "components.responses.badRequest.content.application/json.schema"));
		assertNotNull("Missing response example for 400 Bad Request", GraphQLTest.getMapPathValue(response, "components.responses.badRequest.content.application/json.example"));

		// 401 Unauthorized
		assertNotNull("Missing response schema for 401 Unauthorized",  GraphQLTest.getMapPathValue(response, "components.responses.unauthorized.content.application/json.schema"));
		assertNotNull("Missing response example for 401 Unauthorized", GraphQLTest.getMapPathValue(response, "components.responses.unauthorized.content.application/json.example"));
		assertNotNull("Missing response schema for 401 Unauthorized  (login error)",  GraphQLTest.getMapPathValue(response, "components.responses.loginError.content.application/json.schema"));
		assertNotNull("Missing response example for 401 Unauthorized (login error)",  GraphQLTest.getMapPathValue(response, "components.responses.loginError.content.application/json.example"));
		assertNotNull("Missing response schema for 401 Unauthorized  (token error)",  GraphQLTest.getMapPathValue(response, "components.responses.tokenError.content.application/json.schema"));
		assertNotNull("Missing response example for 401 Unauthorized (token error)",  GraphQLTest.getMapPathValue(response, "components.responses.tokenError.content.application/json.example"));

		// 403 Forbidden
		assertNotNull("Missing response schema for 403 Forbidden",  GraphQLTest.getMapPathValue(response, "components.responses.forbidden.content.application/json.schema"));
		assertNotNull("Missing response example for 403 Forbidden", GraphQLTest.getMapPathValue(response, "components.responses.forbidden.content.application/json.example"));

		// 404 Not Found
		assertNotNull("Missing response schema for 404 Not Found",  GraphQLTest.getMapPathValue(response, "components.responses.notFound.content.application/json.schema"));
		assertNotNull("Missing response example for 404 Not Found", GraphQLTest.getMapPathValue(response, "components.responses.notFound.content.application/json.example"));

		// 422 Unprocessable Entity
		assertNotNull("Missing response schema for 422 Unprocessable Entity",  GraphQLTest.getMapPathValue(response, "components.responses.validationError.content.application/json.schema"));
		assertNotNull("Missing response example for 422 Unprocessable Entity", GraphQLTest.getMapPathValue(response, "components.responses.validationError.content.application/json.example"));

		// schemas
		GraphQLTest.assertMapPathValueIs(response, "components.schemas.#",  5);

		assertNotNull("Missing schema for AbstractNode",  GraphQLTest.getMapPathValue(response, "components.schemas.AbstractNode"));
		assertNotNull("Missing schema for ErrorToken",    GraphQLTest.getMapPathValue(response, "components.schemas.ErrorToken"));
		assertNotNull("Missing schema for User",          GraphQLTest.getMapPathValue(response, "components.schemas.User"));
		assertNotNull("Missing schema for RESTResponse",  GraphQLTest.getMapPathValue(response, "components.schemas.RESTResponse"));
		assertNotNull("Missing schema for TokenResponse", GraphQLTest.getMapPathValue(response, "components.schemas.TokenResponse"));

		// securitySchemes
		GraphQLTest.assertMapPathValueIs(response, "components.securitySchemes.#",  4);

		assertNotNull("Missing security scheme for cookie auth", GraphQLTest.getMapPathValue(response, "components.securitySchemes.CookieAuth"));
		assertNotNull("Missing security scheme for bearer auth", GraphQLTest.getMapPathValue(response, "components.securitySchemes.BearerAuth"));
		assertNotNull("Missing security scheme for x-user",      GraphQLTest.getMapPathValue(response, "components.securitySchemes.XUserAuth"));
		assertNotNull("Missing security scheme for x-password",  GraphQLTest.getMapPathValue(response, "components.securitySchemes.XPassAuth"));

		// check number of paths!
		GraphQLTest.assertMapPathValueIs(response, "paths.#",  21);

		// check paths
		final List<Map<String, Map<String, List<String>>>> paths = List.of(
			Map.of("/login",                                  Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/logout",                                 Map.of("post", List.of("description", "operationId",                "responses"))),
			Map.of("/maintenance/changeNodePropertyKey",      Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/clearDatabase",              Map.of("post", List.of("description", "operationId",                "responses"))),
			Map.of("/maintenance/copyRelationshipProperties", Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/createLabels",               Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/deploy",                     Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/deployData",                 Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/directFileImport",           Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/fixNodeProperties",          Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/flushCaches",                Map.of("post", List.of("description", "operationId",                "responses"))),
			Map.of("/maintenance/letsencrypt",                Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/rebuildIndex",               Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/setNodeProperties",          Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/setRelationshipProperties",  Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/setUuid",                    Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/snapshot",                   Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/maintenance/sync",                       Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/registration",                           Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/reset-password",                         Map.of("post", List.of("description", "operationId", "requestBody", "responses"))),
			Map.of("/token",                                  Map.of("post", List.of("description", "operationId", "requestBody", "responses")))
		);

		for (final Map<String, Map<String, List<String>>> entry : paths) {

			for (final Entry<String, Map<String, List<String>>> pathEntry : entry.entrySet()) {

				final String path = pathEntry.getKey();

				assertNotNull("Missing path for " + path, GraphQLTest.getMapPathValue(response,  "paths." + path));

				final Map<String, List<String>> map = pathEntry.getValue();

				for (final Entry<String, List<String>> mapEntry : map.entrySet()) {

					final String verb = mapEntry.getKey();

					for (final String field : mapEntry.getValue()) {

						assertNotNull("Missing " + field + " for " + verb + " on " + path, GraphQLTest.getMapPathValue(response,  "paths." + path + "." + verb + "." + field));
					}
				}
			}
		}

		// security
		GraphQLTest.assertMapPathValueIs(response, "security.#",  3);

		assertNotNull("Missing security entry for cookie auth", GraphQLTest.getMapPathValue(response, "security.0.CookieAuth"));
		assertNotNull("Missing security entry for bearer auth", GraphQLTest.getMapPathValue(response, "security.1.BearerAuth"));
		assertNotNull("Missing security entry for x-user",      GraphQLTest.getMapPathValue(response, "security.2.XUserAuth"));
		assertNotNull("Missing security entry for x-password",  GraphQLTest.getMapPathValue(response, "security.2.XPassAuth"));
	}

	@Test
	public void testFilteredSchema() {

		RestAssured.basePath = "/structr/openapi";

		// setup
		try (final Tx tx = app.tx()) {

			final String returnTypeJson = "{ \"type\": \"object\", \"properties\": { \"name\": \"string\" } }";

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			// add two types
			final JsonType contact  = schema.addType("Contact").setExtends(schema.getType("Principal"));
			final JsonType customer = schema.addType("Customer").setExtends(contact);

			contact.setIncludeInOpenAPI(true);
			contact.addTags("test", "contact");

			customer.setIncludeInOpenAPI(true);
			customer.addTags("test", "customer");

			// add lifecycle methods (should NOT appear in the OpenAPI output!)
			customer.addMethod("onCreate",              "log('Create')"      ).setIncludeInOpenAPI(true).addTags("test", "customer");
			customer.addMethod("afterCreate",           "log('After Create')").setIncludeInOpenAPI(true).addTags("test", "customer");
			customer.addMethod("onSave",                "log('Save')"        ).setIncludeInOpenAPI(true).addTags("test", "customer");
			customer.addMethod("onDelete",              "log('Delete')"      ).setIncludeInOpenAPI(true).addTags("test", "customer");

			// simple method
			customer.addMethod("doTest",                "log('Test')"        ).setIncludeInOpenAPI(true).addTags("test", "customer");

			// method with parameters
			final JsonMethod doTestParams = customer.addMethod("doTestParams", "log('Test')").setIncludeInOpenAPI(true).addTags("test", "customer");
			doTestParams.addParameter("id", "string").setDescription("The ID of the test object.");
			doTestParams.addParameter("index", "integer").setDescription("The index");

			// method with parameter and return type
			customer.addMethod("doTestParamsReturn", "log('Test')").setIncludeInOpenAPI(true).addTags("test", "customer")
				.setReturnType(returnTypeJson)
				.addParameter("id", "string").setDescription("The ID of the test object.");

			// static method
			customer.addMethod("doStatic", "log('Static')").setIncludeInOpenAPI(true).addTags("test", "customer")
				.setIsStatic(true);

			// static method with parameters
			final JsonMethod doStaticParams = customer.addMethod("doStaticParams", "log('Static')").setIncludeInOpenAPI(true).addTags("test", "customer");
			doStaticParams.setIsStatic(true);
			doStaticParams.addParameter("id", "string").setDescription("The ID of the test object.");
			doStaticParams.addParameter("index", "integer").setDescription("The index");

			// static method with parameter and return type
			customer.addMethod("doStaticParamsReturn",  "log('Static')").setIncludeInOpenAPI(true).addTags("test", "customer")
				.setIsStatic(true)
				.setReturnType(returnTypeJson)
				.addParameter("id", "string").setDescription("The ID of the test object.");

			// add global schema methods (no way of doing this with JsonSchema.. :'()
			app.create(SchemaMethod.class,
				new NodeAttribute<>(AbstractNode.name, "globalTest"),
				new NodeAttribute<>(SchemaMethod.source, "log('GlobalTest')"),
				new NodeAttribute<>(SchemaMethod.includeInOpenAPI, true),
				new NodeAttribute<>(SchemaMethod.tags, new String[] { "test", "customer" })
			);

			app.create(SchemaMethod.class,
				new NodeAttribute<>(AbstractNode.name, "globalTestParams"),
				new NodeAttribute<>(SchemaMethod.source, "log('GlobalTest')"),
				new NodeAttribute<>(SchemaMethod.includeInOpenAPI, true),
				new NodeAttribute<>(SchemaMethod.tags, new String[] { "test", "customer" }),
				new NodeAttribute<>(SchemaMethod.parameters, List.of(
					app.create(SchemaMethodParameter.class, new NodeAttribute<>(SchemaMethodParameter.name, "id"), new NodeAttribute<>(SchemaMethodParameter.parameterType, "string")),
					app.create(SchemaMethodParameter.class, new NodeAttribute<>(SchemaMethodParameter.name, "index"), new NodeAttribute<>(SchemaMethodParameter.parameterType, "integer"))
				))
			);

			app.create(SchemaMethod.class,
				new NodeAttribute<>(AbstractNode.name, "globalTestParamsReturn"),
				new NodeAttribute<>(SchemaMethod.source, "log('GlobalTest')"),
				new NodeAttribute<>(SchemaMethod.includeInOpenAPI, true),
				new NodeAttribute<>(SchemaMethod.tags, new String[] { "test", "customer" }),
				new NodeAttribute<>(SchemaMethod.parameters, List.of(
					app.create(SchemaMethodParameter.class, new NodeAttribute<>(SchemaMethodParameter.name, "id"), new NodeAttribute<>(SchemaMethodParameter.parameterType, "string")),
					app.create(SchemaMethodParameter.class, new NodeAttribute<>(SchemaMethodParameter.name, "index"), new NodeAttribute<>(SchemaMethodParameter.parameterType, "integer"))
				)),
				new NodeAttribute<>(SchemaMethod.returnType, returnTypeJson)
			);

			app.create(SchemaMethod.class,
				new NodeAttribute<>(AbstractNode.name, "globalOther"),
				new NodeAttribute<>(SchemaMethod.source, "log('GlobalOther')"),
				new NodeAttribute<>(SchemaMethod.includeInOpenAPI, true),
				new NodeAttribute<>(SchemaMethod.tags, new String[] { "test", "contact" })
			);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Exception t) {
			logger.warn("", t);
			fail("Unexpected exception.");
		}

		// check result for tag "test"
		{
			final Map<String, Object> response = fetchOpenAPI("/test.json");

			GraphQLTest.assertMapPathValueIs(response, "openapi",      "3.0.2");
			GraphQLTest.assertMapPathValueIs(response, "info.title",   "Structr REST Server");
			GraphQLTest.assertMapPathValueIs(response, "info.version", "1.0.1");

			GraphQLTest.assertMapPathValueIs(response, "servers.0.url", "http://" + host + ":" + httpPort + restUrl);

			// components
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.#",  3);

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.name",           "page");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.schema.type",    "integer");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.schema.default", 1);

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.name",           "pageSize");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.schema.type",    "integer");

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.name",           "loose");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.schema.type",    "boolean");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.schema.default", false);

			// responses

			// 200 OK
			assertNotNull("Missing response schema for 200 OK",       GraphQLTest.getMapPathValue(response, "components.responses.ok.content.application/json.schema"));
			assertNotNull("Missing response example for 200 Ok",      GraphQLTest.getMapPathValue(response, "components.responses.ok.content.application/json.example"));

			// 201 Created
			assertNotNull("Missing response schema for 201 Created",  GraphQLTest.getMapPathValue(response, "components.responses.created.content.application/json.schema"));
			assertNotNull("Missing response example for 201 Created", GraphQLTest.getMapPathValue(response, "components.responses.created.content.application/json.example"));

			// 400 Bad Request
			assertNotNull("Missing response schema for 400 Bad Request",  GraphQLTest.getMapPathValue(response, "components.responses.badRequest.content.application/json.schema"));
			assertNotNull("Missing response example for 400 Bad Request", GraphQLTest.getMapPathValue(response, "components.responses.badRequest.content.application/json.example"));

			// 401 Unauthorized
			assertNotNull("Missing response schema for 401 Unauthorized",  GraphQLTest.getMapPathValue(response, "components.responses.unauthorized.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized", GraphQLTest.getMapPathValue(response, "components.responses.unauthorized.content.application/json.example"));
			assertNotNull("Missing response schema for 401 Unauthorized  (login error)",  GraphQLTest.getMapPathValue(response, "components.responses.loginError.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized (login error)",  GraphQLTest.getMapPathValue(response, "components.responses.loginError.content.application/json.example"));
			assertNotNull("Missing response schema for 401 Unauthorized  (token error)",  GraphQLTest.getMapPathValue(response, "components.responses.tokenError.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized (token error)",  GraphQLTest.getMapPathValue(response, "components.responses.tokenError.content.application/json.example"));

			// 403 Forbidden
			assertNotNull("Missing response schema for 403 Forbidden",  GraphQLTest.getMapPathValue(response, "components.responses.forbidden.content.application/json.schema"));
			assertNotNull("Missing response example for 403 Forbidden", GraphQLTest.getMapPathValue(response, "components.responses.forbidden.content.application/json.example"));

			// 404 Not Found
			assertNotNull("Missing response schema for 404 Not Found",  GraphQLTest.getMapPathValue(response, "components.responses.notFound.content.application/json.schema"));
			assertNotNull("Missing response example for 404 Not Found", GraphQLTest.getMapPathValue(response, "components.responses.notFound.content.application/json.example"));

			// 422 Unprocessable Entity
			assertNotNull("Missing response schema for 422 Unprocessable Entity",  GraphQLTest.getMapPathValue(response, "components.responses.validationError.content.application/json.schema"));
			assertNotNull("Missing response example for 422 Unprocessable Entity", GraphQLTest.getMapPathValue(response, "components.responses.validationError.content.application/json.example"));

			// schemas
			GraphQLTest.assertMapPathValueIs(response, "components.schemas.#",  5);

			assertNotNull("Missing schema for AbstractNode",  GraphQLTest.getMapPathValue(response, "components.schemas.AbstractNode"));
			assertNotNull("Missing schema for ErrorToken",    GraphQLTest.getMapPathValue(response, "components.schemas.ErrorToken"));
			assertNotNull("Missing schema for User",          GraphQLTest.getMapPathValue(response, "components.schemas.User"));
			assertNotNull("Missing schema for RESTResponse",  GraphQLTest.getMapPathValue(response, "components.schemas.RESTResponse"));
			assertNotNull("Missing schema for TokenResponse", GraphQLTest.getMapPathValue(response, "components.schemas.TokenResponse"));

			// securitySchemes
			GraphQLTest.assertMapPathValueIs(response, "components.securitySchemes.#",  4);

			assertNotNull("Missing security scheme for cookie auth", GraphQLTest.getMapPathValue(response, "components.securitySchemes.CookieAuth"));
			assertNotNull("Missing security scheme for bearer auth", GraphQLTest.getMapPathValue(response, "components.securitySchemes.BearerAuth"));
			assertNotNull("Missing security scheme for x-user",      GraphQLTest.getMapPathValue(response, "components.securitySchemes.XUserAuth"));
			assertNotNull("Missing security scheme for x-password",  GraphQLTest.getMapPathValue(response, "components.securitySchemes.XPassAuth"));

			// check number of paths!
			GraphQLTest.assertMapPathValueIs(response, "paths.#",  14);

			// check number of operations (get, put, post, delete, ...)
			GraphQLTest.assertMapPathValueIs(response, "paths./Contact.#",  3);
			GraphQLTest.assertMapPathValueIs(response, "paths./Contact/{uuid}.#",  3);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer.#",  3);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}.#",  3);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}/doTest.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}/doTestParams.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}/doTestParamsReturn.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/doStatic.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/doStaticParams.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/doStaticParamsReturn.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTest.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTestParams.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTestParamsReturn.#",  1);

			// check paths
			checkPaths(response, List.of(
				Map.of("/Contact", Map.of(
					"get",    List.of("description", "operationId",                "parameters", "responses", "summary", "tags"),
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags"),
					"delete", List.of("description", "operationId",                "parameters", "responses", "summary", "tags")
				)),
				Map.of("/Contact/{uuid}", Map.of(
					"get",    List.of("description", "operationId",                "parameters", "responses", "summary", "tags"),
					"put",    List.of("description", "operationId", "requestBody",               "responses", "summary", "tags"),
					"delete", List.of("description", "operationId",                "parameters", "responses", "summary", "tags")
				)),
				Map.of("/Customer", Map.of(
					"get",    List.of("description", "operationId",                "parameters", "responses", "summary", "tags"),
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags"),
					"delete", List.of("description", "operationId",                "parameters", "responses", "summary", "tags")
				)),
				Map.of("/Customer/{uuid}", Map.of(
					"get",    List.of("description", "operationId",                "parameters", "responses", "summary", "tags"),
					"put",    List.of("description", "operationId", "requestBody",               "responses", "summary", "tags"),
					"delete", List.of("description", "operationId",                "parameters", "responses", "summary", "tags")
				)),
				Map.of("/Customer/{uuid}/doTest", Map.of(
					"post",   List.of("description", "operationId",                              "responses", "summary", "tags")
				)),
				Map.of("/Customer/{uuid}/doTestParams", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/Customer/{uuid}/doTestParamsReturn", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/Customer/doStatic", Map.of(
					"post",   List.of("description", "operationId",                              "responses", "summary", "tags")
				)),
				Map.of("/Customer/doStaticParams", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/Customer/doStaticParamsReturn", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/maintenance/globalSchemaMethods/globalOther", Map.of(
					"post",   List.of("description", "operationId",                              "responses", "summary", "tags")
				)),
				Map.of("/maintenance/globalSchemaMethods/globalTest", Map.of(
					"post",   List.of("description", "operationId",                              "responses", "summary", "tags")
				)),
				Map.of("/maintenance/globalSchemaMethods/globalTestParams", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/maintenance/globalSchemaMethods/globalTestParamsReturn", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				))
			));

			// check some special cases manually
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}/doTestParamsReturn.post.requestBody.content.application/json.schema.properties.id.type", "string");
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTestParams.post.requestBody.content.application/json.schema.properties.id.type",    "string");
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTestParams.post.requestBody.content.application/json.schema.properties.index.type", "integer");

			// security
			GraphQLTest.assertMapPathValueIs(response, "security.#",  3);

			assertNotNull("Missing security entry for cookie auth", GraphQLTest.getMapPathValue(response, "security.0.CookieAuth"));
			assertNotNull("Missing security entry for bearer auth", GraphQLTest.getMapPathValue(response, "security.1.BearerAuth"));
			assertNotNull("Missing security entry for x-user",      GraphQLTest.getMapPathValue(response, "security.2.XUserAuth"));
			assertNotNull("Missing security entry for x-password",  GraphQLTest.getMapPathValue(response, "security.2.XPassAuth"));
		}

		// check result for tag "customer"
		{
			final Map<String, Object> response = fetchOpenAPI("/customer.json");

			GraphQLTest.assertMapPathValueIs(response, "openapi",      "3.0.2");
			GraphQLTest.assertMapPathValueIs(response, "info.title",   "Structr REST Server");
			GraphQLTest.assertMapPathValueIs(response, "info.version", "1.0.1");

			GraphQLTest.assertMapPathValueIs(response, "servers.0.url", "http://" + host + ":" + httpPort + restUrl);

			// components
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.#",  3);

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.name",           "page");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.schema.type",    "integer");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.schema.default", 1);

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.name",           "pageSize");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.schema.type",    "integer");

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.name",           "loose");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.schema.type",    "boolean");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.schema.default", false);

			// responses

			// 200 OK
			assertNotNull("Missing response schema for 200 OK",       GraphQLTest.getMapPathValue(response, "components.responses.ok.content.application/json.schema"));
			assertNotNull("Missing response example for 200 Ok",      GraphQLTest.getMapPathValue(response, "components.responses.ok.content.application/json.example"));

			// 201 Created
			assertNotNull("Missing response schema for 201 Created",  GraphQLTest.getMapPathValue(response, "components.responses.created.content.application/json.schema"));
			assertNotNull("Missing response example for 201 Created", GraphQLTest.getMapPathValue(response, "components.responses.created.content.application/json.example"));

			// 400 Bad Request
			assertNotNull("Missing response schema for 400 Bad Request",  GraphQLTest.getMapPathValue(response, "components.responses.badRequest.content.application/json.schema"));
			assertNotNull("Missing response example for 400 Bad Request", GraphQLTest.getMapPathValue(response, "components.responses.badRequest.content.application/json.example"));

			// 401 Unauthorized
			assertNotNull("Missing response schema for 401 Unauthorized",  GraphQLTest.getMapPathValue(response, "components.responses.unauthorized.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized", GraphQLTest.getMapPathValue(response, "components.responses.unauthorized.content.application/json.example"));
			assertNotNull("Missing response schema for 401 Unauthorized  (login error)",  GraphQLTest.getMapPathValue(response, "components.responses.loginError.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized (login error)",  GraphQLTest.getMapPathValue(response, "components.responses.loginError.content.application/json.example"));
			assertNotNull("Missing response schema for 401 Unauthorized  (token error)",  GraphQLTest.getMapPathValue(response, "components.responses.tokenError.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized (token error)",  GraphQLTest.getMapPathValue(response, "components.responses.tokenError.content.application/json.example"));

			// 403 Forbidden
			assertNotNull("Missing response schema for 403 Forbidden",  GraphQLTest.getMapPathValue(response, "components.responses.forbidden.content.application/json.schema"));
			assertNotNull("Missing response example for 403 Forbidden", GraphQLTest.getMapPathValue(response, "components.responses.forbidden.content.application/json.example"));

			// 404 Not Found
			assertNotNull("Missing response schema for 404 Not Found",  GraphQLTest.getMapPathValue(response, "components.responses.notFound.content.application/json.schema"));
			assertNotNull("Missing response example for 404 Not Found", GraphQLTest.getMapPathValue(response, "components.responses.notFound.content.application/json.example"));

			// 422 Unprocessable Entity
			assertNotNull("Missing response schema for 422 Unprocessable Entity",  GraphQLTest.getMapPathValue(response, "components.responses.validationError.content.application/json.schema"));
			assertNotNull("Missing response example for 422 Unprocessable Entity", GraphQLTest.getMapPathValue(response, "components.responses.validationError.content.application/json.example"));

			// schemas
			GraphQLTest.assertMapPathValueIs(response, "components.schemas.#",  5);

			assertNotNull("Missing schema for AbstractNode",  GraphQLTest.getMapPathValue(response, "components.schemas.AbstractNode"));
			assertNotNull("Missing schema for ErrorToken",    GraphQLTest.getMapPathValue(response, "components.schemas.ErrorToken"));
			assertNotNull("Missing schema for User",          GraphQLTest.getMapPathValue(response, "components.schemas.User"));
			assertNotNull("Missing schema for RESTResponse",  GraphQLTest.getMapPathValue(response, "components.schemas.RESTResponse"));
			assertNotNull("Missing schema for TokenResponse", GraphQLTest.getMapPathValue(response, "components.schemas.TokenResponse"));

			// securitySchemes
			GraphQLTest.assertMapPathValueIs(response, "components.securitySchemes.#",  4);

			assertNotNull("Missing security scheme for cookie auth", GraphQLTest.getMapPathValue(response, "components.securitySchemes.CookieAuth"));
			assertNotNull("Missing security scheme for bearer auth", GraphQLTest.getMapPathValue(response, "components.securitySchemes.BearerAuth"));
			assertNotNull("Missing security scheme for x-user",      GraphQLTest.getMapPathValue(response, "components.securitySchemes.XUserAuth"));
			assertNotNull("Missing security scheme for x-password",  GraphQLTest.getMapPathValue(response, "components.securitySchemes.XPassAuth"));

			// check number of paths!
			GraphQLTest.assertMapPathValueIs(response, "paths.#",  11);

			// check number of operations (get, put, post, delete, ...)
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer.#",  3);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}.#",  3);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}/doTest.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}/doTestParams.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}/doTestParamsReturn.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/doStatic.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/doStaticParams.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/doStaticParamsReturn.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTest.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTestParams.#",  1);
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTestParamsReturn.#",  1);

			// check paths
			checkPaths(response, List.of(
				Map.of("/Customer", Map.of(
					"get",    List.of("description", "operationId",                "parameters", "responses", "summary", "tags"),
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags"),
					"delete", List.of("description", "operationId",                "parameters", "responses", "summary", "tags")
				)),
				Map.of("/Customer/{uuid}", Map.of(
					"get",    List.of("description", "operationId",                "parameters", "responses", "summary", "tags"),
					"put",    List.of("description", "operationId", "requestBody",               "responses", "summary", "tags"),
					"delete", List.of("description", "operationId",                "parameters", "responses", "summary", "tags")
				)),
				Map.of("/Customer/{uuid}/doTest", Map.of(
					"post",   List.of("description", "operationId",                              "responses", "summary", "tags")
				)),
				Map.of("/Customer/{uuid}/doTestParams", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/Customer/{uuid}/doTestParamsReturn", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/Customer/doStatic", Map.of(
					"post",   List.of("description", "operationId",                              "responses", "summary", "tags")
				)),
				Map.of("/Customer/doStaticParams", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/Customer/doStaticParamsReturn", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/maintenance/globalSchemaMethods/globalTest", Map.of(
					"post",   List.of("description", "operationId",                              "responses", "summary", "tags")
				)),
				Map.of("/maintenance/globalSchemaMethods/globalTestParams", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				)),
				Map.of("/maintenance/globalSchemaMethods/globalTestParamsReturn", Map.of(
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags")
				))
			));

			// check some special cases manually
			GraphQLTest.assertMapPathValueIs(response, "paths./Customer/{uuid}/doTestParamsReturn.post.requestBody.content.application/json.schema.properties.id.type", "string");
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTestParams.post.requestBody.content.application/json.schema.properties.id.type",    "string");
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalTestParams.post.requestBody.content.application/json.schema.properties.index.type", "integer");

			// security
			GraphQLTest.assertMapPathValueIs(response, "security.#",  3);

			assertNotNull("Missing security entry for cookie auth", GraphQLTest.getMapPathValue(response, "security.0.CookieAuth"));
			assertNotNull("Missing security entry for bearer auth", GraphQLTest.getMapPathValue(response, "security.1.BearerAuth"));
			assertNotNull("Missing security entry for x-user",      GraphQLTest.getMapPathValue(response, "security.2.XUserAuth"));
			assertNotNull("Missing security entry for x-password",  GraphQLTest.getMapPathValue(response, "security.2.XPassAuth"));
		}

		// check result for tag "contact"
		{
			final Map<String, Object> response = fetchOpenAPI("/contact.json");

			GraphQLTest.assertMapPathValueIs(response, "openapi",      "3.0.2");
			GraphQLTest.assertMapPathValueIs(response, "info.title",   "Structr REST Server");
			GraphQLTest.assertMapPathValueIs(response, "info.version", "1.0.1");

			GraphQLTest.assertMapPathValueIs(response, "servers.0.url", "http://" + host + ":" + httpPort + restUrl);

			// components
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.#",  3);

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.name",           "page");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.schema.type",    "integer");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.page.schema.default", 1);

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.name",           "pageSize");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.pageSize.schema.type",    "integer");

			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.name",           "loose");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.in",             "query");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.schema.type",    "boolean");
			GraphQLTest.assertMapPathValueIs(response, "components.parameters.inexactSearch.schema.default", false);

			// responses

			// 200 OK
			assertNotNull("Missing response schema for 200 OK",       GraphQLTest.getMapPathValue(response, "components.responses.ok.content.application/json.schema"));
			assertNotNull("Missing response example for 200 Ok",      GraphQLTest.getMapPathValue(response, "components.responses.ok.content.application/json.example"));

			// 201 Created
			assertNotNull("Missing response schema for 201 Created",  GraphQLTest.getMapPathValue(response, "components.responses.created.content.application/json.schema"));
			assertNotNull("Missing response example for 201 Created", GraphQLTest.getMapPathValue(response, "components.responses.created.content.application/json.example"));

			// 400 Bad Request
			assertNotNull("Missing response schema for 400 Bad Request",  GraphQLTest.getMapPathValue(response, "components.responses.badRequest.content.application/json.schema"));
			assertNotNull("Missing response example for 400 Bad Request", GraphQLTest.getMapPathValue(response, "components.responses.badRequest.content.application/json.example"));

			// 401 Unauthorized
			assertNotNull("Missing response schema for 401 Unauthorized",  GraphQLTest.getMapPathValue(response, "components.responses.unauthorized.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized", GraphQLTest.getMapPathValue(response, "components.responses.unauthorized.content.application/json.example"));
			assertNotNull("Missing response schema for 401 Unauthorized  (login error)",  GraphQLTest.getMapPathValue(response, "components.responses.loginError.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized (login error)",  GraphQLTest.getMapPathValue(response, "components.responses.loginError.content.application/json.example"));
			assertNotNull("Missing response schema for 401 Unauthorized  (token error)",  GraphQLTest.getMapPathValue(response, "components.responses.tokenError.content.application/json.schema"));
			assertNotNull("Missing response example for 401 Unauthorized (token error)",  GraphQLTest.getMapPathValue(response, "components.responses.tokenError.content.application/json.example"));

			// 403 Forbidden
			assertNotNull("Missing response schema for 403 Forbidden",  GraphQLTest.getMapPathValue(response, "components.responses.forbidden.content.application/json.schema"));
			assertNotNull("Missing response example for 403 Forbidden", GraphQLTest.getMapPathValue(response, "components.responses.forbidden.content.application/json.example"));

			// 404 Not Found
			assertNotNull("Missing response schema for 404 Not Found",  GraphQLTest.getMapPathValue(response, "components.responses.notFound.content.application/json.schema"));
			assertNotNull("Missing response example for 404 Not Found", GraphQLTest.getMapPathValue(response, "components.responses.notFound.content.application/json.example"));

			// 422 Unprocessable Entity
			assertNotNull("Missing response schema for 422 Unprocessable Entity",  GraphQLTest.getMapPathValue(response, "components.responses.validationError.content.application/json.schema"));
			assertNotNull("Missing response example for 422 Unprocessable Entity", GraphQLTest.getMapPathValue(response, "components.responses.validationError.content.application/json.example"));

			// schemas
			GraphQLTest.assertMapPathValueIs(response, "components.schemas.#",  5);

			assertNotNull("Missing schema for AbstractNode",  GraphQLTest.getMapPathValue(response, "components.schemas.AbstractNode"));
			assertNotNull("Missing schema for ErrorToken",    GraphQLTest.getMapPathValue(response, "components.schemas.ErrorToken"));
			assertNotNull("Missing schema for User",          GraphQLTest.getMapPathValue(response, "components.schemas.User"));
			assertNotNull("Missing schema for RESTResponse",  GraphQLTest.getMapPathValue(response, "components.schemas.RESTResponse"));
			assertNotNull("Missing schema for TokenResponse", GraphQLTest.getMapPathValue(response, "components.schemas.TokenResponse"));

			// securitySchemes
			GraphQLTest.assertMapPathValueIs(response, "components.securitySchemes.#",  4);

			assertNotNull("Missing security scheme for cookie auth", GraphQLTest.getMapPathValue(response, "components.securitySchemes.CookieAuth"));
			assertNotNull("Missing security scheme for bearer auth", GraphQLTest.getMapPathValue(response, "components.securitySchemes.BearerAuth"));
			assertNotNull("Missing security scheme for x-user",      GraphQLTest.getMapPathValue(response, "components.securitySchemes.XUserAuth"));
			assertNotNull("Missing security scheme for x-password",  GraphQLTest.getMapPathValue(response, "components.securitySchemes.XPassAuth"));

			// check number of paths!
			GraphQLTest.assertMapPathValueIs(response, "paths.#",  3);

			// check number of operations (get, put, post, delete, ...)
			GraphQLTest.assertMapPathValueIs(response, "paths./Contact.#",  3);
			GraphQLTest.assertMapPathValueIs(response, "paths./Contact/{uuid}.#",  3);
			GraphQLTest.assertMapPathValueIs(response, "paths./maintenance/globalSchemaMethods/globalOther.#",  1);

			// check paths
			checkPaths(response, List.of(
				Map.of("/Contact", Map.of(
					"get",    List.of("description", "operationId",                "parameters", "responses", "summary", "tags"),
					"post",   List.of("description", "operationId", "requestBody",               "responses", "summary", "tags"),
					"delete", List.of("description", "operationId",                "parameters", "responses", "summary", "tags")
				)),
				Map.of("/Contact/{uuid}", Map.of(
					"get",    List.of("description", "operationId",                "parameters", "responses", "summary", "tags"),
					"put",    List.of("description", "operationId", "requestBody",               "responses", "summary", "tags"),
					"delete", List.of("description", "operationId",                "parameters", "responses", "summary", "tags")
				)),
				Map.of("/maintenance/globalSchemaMethods/globalOther", Map.of(
					"post",   List.of("description", "operationId",                              "responses", "summary", "tags")
				))
			));


			// security
			GraphQLTest.assertMapPathValueIs(response, "security.#",  3);

			assertNotNull("Missing security entry for cookie auth", GraphQLTest.getMapPathValue(response, "security.0.CookieAuth"));
			assertNotNull("Missing security entry for bearer auth", GraphQLTest.getMapPathValue(response, "security.1.BearerAuth"));
			assertNotNull("Missing security entry for x-user",      GraphQLTest.getMapPathValue(response, "security.2.XUserAuth"));
			assertNotNull("Missing security entry for x-password",  GraphQLTest.getMapPathValue(response, "security.2.XPassAuth"));
		}
	}

	// ----- private methods -----
	private void checkPaths(final Map<String, Object> response, final List<Map<String, Map<String, List<String>>>> paths) {

		for (final Map<String, Map<String, List<String>>> entry : paths) {

			for (final Entry<String, Map<String, List<String>>> pathEntry : entry.entrySet()) {

				final String path = pathEntry.getKey();

				assertNotNull("Missing path for " + path, GraphQLTest.getMapPathValue(response,  "paths." + path));

				final Map<String, List<String>> map = pathEntry.getValue();

				for (final Entry<String, List<String>> mapEntry : map.entrySet()) {

					final String verb = mapEntry.getKey();

					for (final String field : mapEntry.getValue()) {

						assertNotNull("Missing " + field + " for " + verb + " on " + path, GraphQLTest.getMapPathValue(response,  "paths." + path + "." + verb + "." + field));
					}
				}
			}
		}
	}

	private Map<String, Object> fetchOpenAPI(final String url) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")

			.expect()
				.statusCode(200)

			.when()
				.get(url)

			.andReturn()
			.as(Map.class);
	}
}
