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
package org.structr.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.export.StructrSchemaDefinition;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.export.StructrTypeDefinitions;
import org.structr.schema.openapi.common.OpenAPIAllOf;
import org.structr.schema.openapi.common.OpenAPISchemaReference;
import org.structr.schema.openapi.operation.*;
import org.structr.schema.openapi.operation.maintenance.*;
import org.structr.schema.openapi.parameter.OpenAPIQueryParameter;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.result.OpenAPICreateResponseSchema;
import org.structr.schema.openapi.result.OpenAPIExampleAnyResult;
import org.structr.schema.openapi.result.OpenAPISingleResponseSchema;
import org.structr.schema.openapi.result.OpenAPIWriteResponseSchema;
import org.structr.schema.openapi.schema.OpenAPIArraySchema;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;
import org.structr.schema.openapi.schema.OpenAPIStructrTypeSchemaOutput;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * A servlet that implements the OpenAPI endpoint.
 */
public class OpenAPIServlet extends AbstractDataServlet {

	private final Logger logger = LoggerFactory.getLogger(OpenAPIServlet.class);
	private final Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();

	private SecurityContext securityContext;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		String tag = getTagFromURLPath(request);

		// "schema" is the placeholder for "everything", all other values are used as a filter (sorry)
		if ("schema".equals(tag)) {
			tag = null;
		}

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");

		try {

			assertInitialized();

			Authenticator authenticator = null;

			// isolate request authentication in a transaction
			// Ensure CORS settings apply by letting the authenticator examine the request.
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = getConfig().getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			// isolate resource authentication
			final App app = StructrApp.getInstance(securityContext);
			try (final Tx tx = app.tx()) {

				authenticator.checkResourceAccess(securityContext, request, "_openapi", "");

				tx.success();
			}

			try (final Writer writer = response.getWriter()) {

				gson.toJson(createOpenAPIRoot(request, tag), writer);

				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Cache-Control", "no-cache");

				writer.append("\n");
				writer.flush();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}

		} catch (FrameworkException frameworkException) {

			// set status
			response.setStatus(frameworkException.getStatus());
		}
	}

	@Override
	public String getModuleName() {
		return "rest";
	}

	// ----- private methods -----
	private Map<String, Object> createOpenAPIRoot(final HttpServletRequest request, final String tag) throws FrameworkException {

		final StructrSchemaDefinition schema = (StructrSchemaDefinition)StructrSchema.createFromDatabase(StructrApp.getInstance());
		final Map<String, Object> root       = new LinkedHashMap<>();

		root.put("openapi",    "3.0.2");
		root.put("info",       createInfoObject());
		root.put("servers",    createServersObject(request));
		root.put("components", createComponentsObject(schema, tag));
		root.put("paths",      createPathsObject(schema, tag));
		root.put("tags",       createTagsObject(schema, tag));
		root.put("security",   createGlobalSecurityObject());

		return root;
	}

	private Map<String, Object> createInfoObject() {

		final Map<String, Object> info = new LinkedHashMap<>();

		final String serverDescription = Settings.OpenAPIServerTitle.getValue();
		final String serverVersion = Settings.OpenAPIServerVersion.getValue();

		info.put("title",   serverDescription);
		info.put("version", serverVersion);

		return info;
	}

	private List<Map<String, Object>> createServersObject(final HttpServletRequest request) {

		final List<Map<String, Object>> servers = new LinkedList<>();
		final Map<String, Object> server        = new LinkedHashMap<>();

		server.put("url",         getStructrUrl(request));

		// add server to list
		servers.add(server);

		return servers;
	}

	private Map<String, Object> createComponentsObject(final StructrSchemaDefinition schema, final String tag) {

		final Map<String, Object> components = new TreeMap<>();

		final Map<String, Object> schemas = createSchemasObject(schema, tag);

		components.put("securitySchemes", createSecuritySchemesObject());
		components.put("schemas",         schemas);
		components.put("responses",       createResponsesObject(schema, tag));
		components.put("parameters",      createParametersObject());

		return components;
	}

	private Map<String, Object> createSecuritySchemesObject() {

		final Map<String, Object> schemes    = new LinkedHashMap<>();
		final Map<String, Object> xUserAuth  = new LinkedHashMap<>();
		final Map<String, Object> xPassAuth  = new LinkedHashMap<>();
		final Map<String, Object> cookieAuth = new LinkedHashMap<>();
		final Map<String, Object> bearerAuth = new LinkedHashMap<>();

		xUserAuth.put("type", "apiKey");
		xUserAuth.put("in",   "header");
		xUserAuth.put("name", "X-User");

		xPassAuth.put("type", "apiKey");
		xPassAuth.put("in",   "header");
		xPassAuth.put("name", "X-Password");

		cookieAuth.put("type", "apiKey");
		cookieAuth.put("in",   "cookie");
		cookieAuth.put("name", "JSESSIONID");

		bearerAuth.put("type", "http");
		bearerAuth.put("scheme",   "bearer");
		bearerAuth.put("bearerFormat", "JWT");

		schemes.put("CookieAuth", cookieAuth);
		schemes.put("BearerAuth", bearerAuth);
		schemes.put("XUserAuth",  xUserAuth);
		schemes.put("XPassAuth",  xPassAuth);

		return schemes;
	}

	private List<Map<String, Object>> createGlobalSecurityObject() {

		final List<Map<String, Object>> security = new LinkedList<>();

		security.add(Map.of("CookieAuth", List.of()));
		security.add(Map.of("BearerAuth", List.of()));

		// must be used together
		security.add(Map.of("XUserAuth", List.of(), "XPassAuth", List.of()));

		return security;
	}

	private List<Map<String, Object>> createTagsObject(final StructrSchemaDefinition schema, final String tag) {

		final List<Map<String, Object>> tags = new LinkedList<>();

		for (final StructrTypeDefinition type : schema.getTypeDefinitions()) {

			if (type.isSelected(tag) && (StringUtils.isNotBlank(tag) && type.includeInOpenAPI() && !type.isServiceClass())) {

				String summary = type.getSummary();
				if (StringUtils.isBlank(summary)) {

					summary = "Operations for type " + type.getName();
				}

				tags.add(createTagObject(type.getName(), summary));
			}
		}

		return tags;
	}

	private Map<String, Object> createTagObject(final String name, final String description) {

		final Map<String, Object> tag = new LinkedHashMap<>();

		tag.put("name",        name);
		tag.put("description", description);

		return tag;
	}

	private Map<String, Object> createSchemasObject(final StructrSchemaDefinition schema, final String tag) {

		final Map<String, Object> map = new TreeMap<>();

		final StructrTypeDefinitions definitions = schema.getTypeDefinitionsObject();
		map.putAll(definitions.serializeOpenAPI(map, tag));

		// base classes
		map.put("AbstractNode", new OpenAPIStructrTypeSchemaOutput(AbstractNode.class, PropertyView.Public, 0));

		// base responses for GET and PUT,POST operations
		Map<String, Object> getResponseBaseSchema = new HashMap<>();
		getResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("query_time", "query_time", "number", null, "0.001659655", false));
		getResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("result_count", "result_count", "integer",null, "1", false));
		getResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("page_count", "page_count", "integer",null, "1", false));
		getResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("result_count_time", "result_count_time", "number", null, "0.000195496", false));
		getResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("serialization_time", "serialization_time", "number", null, "0.001270261", false));
		map.put("GetBaseResponse", new OpenAPIStructrTypeSchemaOutput("Response schema used by GET operations. Responses also contain the key 'result' as either an object or an array depending on the GET resource.", "object", getResponseBaseSchema));

		Map<String, Object> writeResponseBaseSchema = new HashMap<>();
		writeResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("result_count", "result_count", "integer",null, "1", false));
		writeResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("page_count", "page_count", "integer",null, "1", false));
		writeResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("result_count_time", "result_count_time", "number", null, "0.000195496", false));
		writeResponseBaseSchema.putAll(new OpenAPIPrimitiveSchema("serialization_time", "serialization_time", "number", null, "0.001270261", false));
		map.put("WriteBaseResponse", new OpenAPIStructrTypeSchemaOutput("Response schema used by PUT and POST operations.", "object", writeResponseBaseSchema));

		map.put("WriteResponse", new OpenAPIAllOf(
				new OpenAPISchemaReference("WriteBaseResponse"),
				new OpenAPIWriteResponseSchema()
		));

		map.put("CreateResponse", new OpenAPIAllOf(
				new OpenAPISchemaReference("WriteBaseResponse"),
				new OpenAPICreateResponseSchema()
		));

		map.put("LoginResponse", new OpenAPIAllOf(
				new OpenAPISchemaReference("WriteBaseResponse"),
				new OpenAPISingleResponseSchema(new OpenAPISchemaReference("#/components/schemas/User"))
		));

		map.put("TokenResponse", new OpenAPIAllOf(
				new OpenAPISchemaReference("WriteBaseResponse"),
				new OpenAPISingleResponseSchema(new OpenAPISchemaReference("#/components/schemas/TokenResponse"))
		));

		map.put("ok", new OpenAPIAllOf(
				new OpenAPISchemaReference("WriteResponse"),
				new OpenAPIWriteResponseSchema()
		));

		map.put("ErrorToken",  new OpenAPIObjectSchema("An error token used in semantic error messages returned by the REST server.",
			new OpenAPIPrimitiveSchema("The type that caused the error.", "type",     "string"),
			new OpenAPIPrimitiveSchema("The property that caused the error (if applicable).", "property", "string"),
			new OpenAPIPrimitiveSchema("The error token identifier.", "token",    "string"),
			new OpenAPIPrimitiveSchema("Optional detail information.", "detail",   "string")
		));

		map.put("RESTResponse", new OpenAPIObjectSchema("HTTP status code, message and optional error tokens used in semantic error messages returned by the REST server.",
			new OpenAPIPrimitiveSchema("The error code.",    "code",    "integer"),
			new OpenAPIPrimitiveSchema("The error message.", "message", "string"),
			Map.of("errors", new OpenAPIArraySchema("A list of error tokens.", new OpenAPISchemaReference("#/components/schemas/ErrorToken")))
		));

		map.put("TokenResponse", new OpenAPIObjectSchema("Contains the bearer token and refresh token that can be used to authenticate further calls to any other resources.",
			new OpenAPIPrimitiveSchema("The Bearer token.",                                                "access_token",    "string"),
			new OpenAPIPrimitiveSchema("The refresh token that can be used to optain more Bearer tokens.", "refresh_token",   "string"),
			new OpenAPIPrimitiveSchema("The expiration timestamp of the Bearer token.",                     "expiration_date", "integer"),
			new OpenAPIPrimitiveSchema("The token type.",                                                  "token_type",      "string")
		));

		map.put("UsernameLoginBody", new OpenAPIObjectSchema("Requestbody for login or token creation requests with username and password.",
				new OpenAPIPrimitiveSchema("Username of user to log in.", "name",     "string"),
				new OpenAPIPrimitiveSchema("Password of the user.",       "password", "string")
		));

		map.put("EMailLoginBody", new OpenAPIObjectSchema("Requestbody for login or token creation requests with eMail and password.",
				new OpenAPIPrimitiveSchema("eMail of user to log in.", "eMail",    "string"),
				new OpenAPIPrimitiveSchema("Password of the user.",    "password", "string")
		));

		map.put("RefreshTokenLoginBody", new OpenAPIObjectSchema("Requestbody for login or token creation requests with refresh_token.",
				new OpenAPIPrimitiveSchema("A refresh token from a previous call to the token resource.", "refresh_token",	"string")
		));

		return map;
	}

	private Map<String, Object> createPathsObject(final StructrSchemaDefinition schema, final String tag) {

		final Map<String, Object> paths = new TreeMap<>();

		// maintenance endpoints are only visible when there is no tag set
		if (StringUtils.isBlank(tag)) {

			// maintenance endpoints
			// Note: if you change / add something here, please also update the docs online!

			paths.putAll(new OpenAPIMaintenanceOperationChangeNodePropertyKey());
			paths.putAll(new OpenAPIMaintenanceOperationClearDatabase());
			paths.putAll(new OpenAPIMaintenanceOperationCopyRelationshipProperties());
			paths.putAll(new OpenAPIMaintenanceOperationCreateLabels());
			paths.putAll(new OpenAPIMaintenanceOperationDeploy());
			paths.putAll(new OpenAPIMaintenanceOperationDeployData());
			paths.putAll(new OpenAPIMaintenanceOperationDirectFileImport());
			paths.putAll(new OpenAPIMaintenanceOperationFixNodeProperties());
			paths.putAll(new OpenAPIMaintenanceOperationFlushCaches());
			paths.putAll(new OpenAPIMaintenanceOperationLetsencrypt());
			paths.putAll(new OpenAPIMaintenanceOperationRebuildIndex());
			paths.putAll(new OpenAPIMaintenanceOperationSetNodeProperties());
			paths.putAll(new OpenAPIMaintenanceOperationSetRelationshipProperties());
			paths.putAll(new OpenAPIMaintenanceOperationSetUuid());
			paths.putAll(new OpenAPIMaintenanceOperationSnapshot());
			paths.putAll(new OpenAPIMaintenanceOperationSync());

			// Note: if you change / add something here, please also update the docs online!
		}

		// session endpoints are only visible when there is no tag set

		// session handling and user management
		paths.putAll(new OpenAPIResetPasswordOperation());
		paths.putAll(new OpenAPIRegistrationOperation());
		paths.putAll(new OpenAPILoginOperation());
		paths.putAll(new OpenAPITokenOperation());
		paths.putAll(new OpenAPILogoutOperation());

		// add all other endpoints filtered by tag
		paths.putAll(schema.serializeOpenAPIOperations(tag));

		return paths;
	}

	private String getStructrUrl(final HttpServletRequest request) {

		final StringBuilder buf = new StringBuilder();

		buf.append(ActionContext.getBaseUrl(request));
		buf.append(Settings.RestPath.getValue());

		return buf.toString();
	}

	private Map<String, Object> createResponsesObject(final StructrSchemaDefinition schema, final String tag) {

		final Map<String, Object> responses = new LinkedHashMap<>();
		// 200 OK
		responses.put("ok", new OpenAPIRequestResponse("The request was executed successfully.",
			new OpenAPISchemaReference("ok"),
			new OpenAPIExampleAnyResult(List.of(), false)
		));

		// 201 Created
		responses.put("created", new OpenAPIRequestResponse("Created",
			new OpenAPISchemaReference("#/components/schemas/CreateResponse"),
			new OpenAPIExampleAnyResult(Arrays.asList(NodeServiceCommand.getNextUuid()), true)
		));

		// 400 Bad Request
		responses.put("badRequest", new OpenAPIRequestResponse("The request was not valid and should not be repeated without modifications.",
			new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
			Map.of("code", "400", "message", "Please specify sync file", "errors", List.of())
		));

		// 401 Unauthorized
		responses.put("unauthorized", new OpenAPIRequestResponse(
			"Access denied or wrong password.\n\nIf the error message is \"Access denied\", you need to configure a resource access grant for this endpoint."
			+ " otherwise the error message is \"Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!\".",
			new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
			Map.of("code", "401", "message", "Access denied", "errors", List.of())
		));

		responses.put("loginError", new OpenAPIRequestResponse("Wrong username or password, or user is blocked.",
			new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
			Map.of("code", "401", "message", "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!")
		));

		responses.put("loginResponse", new OpenAPIRequestResponse(
			"Login successful.",
			new OpenAPISchemaReference("LoginResponse")
		));

		responses.put("tokenError", new OpenAPIRequestResponse("The given access token or refresh token is invalid.",
			new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
			Map.of("code", "401", "message", "The given access_token or refresh_token is invalid!")
		));

		responses.put("tokenResponse", new OpenAPIRequestResponse(
			"The request was executed successfully.",
			new OpenAPISchemaReference("TokenResponse")
		));

		// 403 Forbidden
		responses.put("forbidden", new OpenAPIRequestResponse("The request was denied due to insufficient access rights to the object.",
			new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
			Map.of("code", "403", "message", "Forbidden", "errors", List.of())
		));

		// 404 Not Found
		responses.put("notFound", new OpenAPIRequestResponse("The desired object was not found.",
			new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
			Map.of("code", "404", "message", "Not Found", "errors", List.of())
		));

		// 422 Unprocessable Entity
		responses.put("validationError", new OpenAPIRequestResponse("The request entity was not valid, or validation failed.",
			new OpenAPISchemaReference("#/components/schemas/RESTResponse"),
			Map.of("code", "422", "message", "Unable to commit transaction, validation failed", "errors", List.of(
				Map.of("type", "ExampleType", "property", "name", "token", "must_not_be_empty"),
				Map.of("type", "ExampleType", "property", "name", "token", "must_match", "detail", "[^\\\\/\\\\x00]+")
			))
		));

		final StructrTypeDefinitions definitions = schema.getTypeDefinitionsObject();
		responses.putAll(definitions.serializeOpenAPIResponses(responses, tag));

		return responses;
	}

	private Map<String, Object> createParametersObject() {

		final Map<String, Object> parameters  = new LinkedHashMap<>();

		parameters.put("page",               new OpenAPIQueryParameter("_page",     "Page number of the results to fetch.", Map.of("type", "integer", "default", 1)));
		parameters.put("pageSize",           new OpenAPIQueryParameter("_pageSize", "Page size of result pages.",           Map.of("type", "integer")));
		parameters.put("inexactSearch",      new OpenAPIQueryParameter("_loose",    "Use inexact search",                   Map.of("type", "boolean", "default", false)));
		parameters.put("outputNestingDepth", new OpenAPIQueryParameter("_outputNestingDepth",     "Depth in the graph of the JSON output rendering. Does not work with the all view.", Map.of("type", "integer", "default", 3)));

		return parameters;
	}

	private String getTagFromURLPath(final HttpServletRequest request) {

		final String pathInfo = StringUtils.substringAfter(StringUtils.defaultIfBlank(request.getPathInfo(), ""), "/");

		if (StringUtils.isNotBlank(pathInfo) && pathInfo.endsWith(".json")) {

			return StringUtils.substringBeforeLast(pathInfo, ".");
		}

		return "schema";
	}
}