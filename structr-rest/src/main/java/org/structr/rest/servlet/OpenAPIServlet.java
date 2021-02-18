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
package org.structr.rest.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.export.StructrSchemaDefinition;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.openapi.common.OpenAPIOneOf;
import org.structr.schema.openapi.common.OpenAPIReference;
import org.structr.schema.openapi.operation.*;
import org.structr.schema.openapi.parameter.OpenAPIQueryParameter;
import org.structr.schema.openapi.request.OpenAPIRequestResponse;
import org.structr.schema.openapi.result.OpenAPIExampleAnyResult;
import org.structr.schema.openapi.schema.OpenAPIArraySchema;
import org.structr.schema.openapi.schema.OpenAPIObjectSchema;
import org.structr.schema.openapi.schema.OpenAPIPrimitiveSchema;
import org.structr.schema.openapi.schema.OpenAPIResultSchema;
import org.structr.schema.openapi.schema.OpenAPIStructrTypeSchemaOutput;

/**
 * A servlet that implements the OpenAPI endpoint.
 */
public class OpenAPIServlet extends AbstractDataServlet {

	private final Gson gson = new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		String tag = getTagFromURLPath(request);


		final String requestOrigin = request.getHeader("origin");

		if (requestOrigin != null) {

			String allowOrigin = Settings.OpenAPIAllowOrigin.getValue();

			if (StringUtils.equals(allowOrigin, "")) {
				allowOrigin = requestOrigin;
			}

			response.addHeader("Access-Control-Allow-Origin", allowOrigin);
		}


		if (StringUtils.isEmpty(tag)) {

			response.sendRedirect("/structr/openapi/schema.json");

		} else {

			// "schema" is the placeholder for "everything", all other values are used as a filter (sorry)
			if ("schema".equals(tag)) {
				tag = null;
			}

			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json");

			try (final Writer writer = response.getWriter()) {

				gson.toJson(createOpenAPIRoot(request, tag), writer);

				response.setStatus(HttpServletResponse.SC_OK);
				response.setHeader("Cache-Control", "no-cache");

				writer.append("\n");
				writer.flush();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
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

		components.put("securitySchemes", createSecuritySchemesObject());
		components.put("schemas",         createSchemasObject(schema, tag));
		components.put("responses",       createResponsesObject());
		components.put("parameters",      createParametersObject());

		return components;
	}

	private Map<String, Object> createSecuritySchemesObject() {

		final Map<String, Object> schemes = new LinkedHashMap<>();
		final Map<String, Object> cookieAuth = new LinkedHashMap<>();
		final Map<String, Object> bearerAuth = new LinkedHashMap<>();

		cookieAuth.put("type", "apiKey");
		cookieAuth.put("in",   "cookie");
		cookieAuth.put("name", "JSESSIONID");

		bearerAuth.put("type", "http");
		bearerAuth.put("scheme",   "bearer");
		bearerAuth.put("bearerFormat", "JWT");

		schemes.put("CookieAuth", cookieAuth);
		schemes.put("BearerAuth", bearerAuth);

		return schemes;
	}

	private List<Map<String, Object>> createGlobalSecurityObject() {

		final List<Map<String, Object>> security = new LinkedList<>();
		final Map<String, Object> securitySchemes = new LinkedHashMap<>();
		final Map<String, Object> auth    = new LinkedHashMap<>();

		securitySchemes.put("CookieAuth", List.of());
		securitySchemes.put("BearerAuth", List.of());

		security.add(securitySchemes);

		return security;
	}

	private List<Map<String, Object>> createTagsObject(final StructrSchemaDefinition schema, final String tag) {

		final List<Map<String, Object>> tags = new LinkedList<>();

		for (final StructrTypeDefinition type : schema.getTypeDefinitions()) {

			if (type.isSelected(tag)) {

				tags.add(createTagObject(type.getName(), "Operations for type " + type.getName()));
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

		// base classes
		map.put("AbstractNode", new OpenAPIStructrTypeSchemaOutput(AbstractNode.class, PropertyView.Public, 0));
		map.put("Principal",    new OpenAPIStructrTypeSchemaOutput(AbstractNode.class, PropertyView.Public, 0));

		map.put("ErrorToken",  new OpenAPIObjectSchema("An error token used in semantic error messages returned by the REST server.",
			new OpenAPIPrimitiveSchema("The type that caused the error.", "type",     "string"),
			new OpenAPIPrimitiveSchema("The property that caused the error (if applicable).", "property", "string"),
			new OpenAPIPrimitiveSchema("The error token identifier.", "token",    "string"),
			new OpenAPIPrimitiveSchema("Optional detail information.", "detail",   "string")
		));

		map.put("RESTResponse", new OpenAPIObjectSchema("HTTP status code, message and optional error tokens used in semantic error messages returned by the REST server.",
			new OpenAPIPrimitiveSchema("The error code.",    "code",    "integer"),
			new OpenAPIPrimitiveSchema("The error message.", "message", "string"),
			Map.of("errors", new OpenAPIArraySchema("A list of error tokens.", new OpenAPIReference("#/components/schemas/ErrorToken")))
		));

		map.put("TokenResponse", new OpenAPIObjectSchema("Contains the bearer token and refresh token that can be used to authenticate further calls to any other resources.",
			new OpenAPIPrimitiveSchema("The Bearer token.",    "access_token",    "string"),
			new OpenAPIPrimitiveSchema("The refresh token that can be used to optain more Bearer tokens.",    "refresh_token",    "string"),
			new OpenAPIPrimitiveSchema("The exiration timestamp of the Bearer token.",    "expiration_date",    "integer"),
			new OpenAPIPrimitiveSchema("The token type.",    "token_type",    "string")
		));

		return map;
	}

	private Map<String, Object> createPathsObject(final StructrSchemaDefinition schema, final String tag) {

		final Map<String, Object> paths = new LinkedHashMap<>();

		paths.putAll(new OpenAPIResetPasswordOperation());
		paths.putAll(new OpenAPIRegistrationOperation());
		paths.putAll(new OpenAPILoginOperation());
		paths.putAll(new OpenAPITokenOperation());
		paths.putAll(new OpenAPILogoutOperation());
		paths.putAll(schema.serializeOpenAPIOperations(tag));

		return paths;
	}

	private String getStructrUrl(final HttpServletRequest request) {

		final StringBuilder buf = new StringBuilder();

		buf.append(ActionContext.getBaseUrl(request));
		buf.append(Settings.RestPath.getValue());

		return buf.toString();
	}

	private Map<String, Object> createResponsesObject() {

		final Map<String, Object> responses = new LinkedHashMap<>();

		responses.put("created", new OpenAPIRequestResponse("Created",
			new OpenAPIResultSchema(new OpenAPIArraySchema("The UUID(s) of the created object(s).", Map.of("type", "string", "example", NodeServiceCommand.getNextUuid())), false),
			new OpenAPIExampleAnyResult(Arrays.asList("cf8b18f28b7c4dada3085656e78d9bd2"))
		));

		responses.put("loginError", new OpenAPIRequestResponse("Unauthorized",
			new OpenAPIReference("#/components/schemas/RESTResponse"),
			Map.of("code", "401", "message", "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!")
		));

		responses.put("tokenError", new OpenAPIRequestResponse("Unauthorized",
				new OpenAPIReference("#/components/schemas/RESTResponse"),
				new OpenAPIOneOf(
					Map.of("code", "401", "message", "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!"),
					Map.of("code", "401", "message", "The given access_token or refresh_token is invalid!")
				)
		));

		responses.put("grantError", new OpenAPIRequestResponse("Forbidden",
			new OpenAPIReference("#/components/schemas/RESTResponse"),
			Map.of("code", "401", "message", "Unauthorized", "errors", List.of())
		));

		responses.put("unauthorized", new OpenAPIRequestResponse("Unauthorized",
			new OpenAPIReference("#/components/schemas/RESTResponse"),
			Map.of("code", "401", "message", "Unauthorized", "errors", List.of())
		));

		responses.put("forbidden", new OpenAPIRequestResponse("Forbidden",
			new OpenAPIReference("#/components/schemas/RESTResponse"),
			Map.of("code", "403", "message", "Forbidden", "errors", List.of())
		));

		responses.put("notFound", new OpenAPIRequestResponse("Not Found",
			new OpenAPIReference("#/components/schemas/RESTResponse"),
			Map.of("code", "404", "message", "Not Found", "errors", List.of())
		));

		responses.put("validationError", new OpenAPIRequestResponse("Validation Error",
			new OpenAPIReference("#/components/schemas/RESTResponse"),
			Map.of("code", "422", "message", "Unable to commit transaction, validation failed", "errors", List.of(
				Map.of("type", "Folder", "property", "name", "token", "must_not_be_empty"),
				Map.of("type", "Folder", "property", "name", "token", "must_match", "detail", "[^\\\\/\\\\x00]+")
			))
		));

		return responses;
	}

	private Map<String, Object> createParametersObject() {

		final Map<String, Object> parameters  = new LinkedHashMap<>();

		parameters.put("page",          new OpenAPIQueryParameter("page",     "Page number of the results to fetch.", Map.of("type", "integer", "default", 1)));
		parameters.put("pageSize",      new OpenAPIQueryParameter("pageSize", "Page size of result pages.",           Map.of("type", "integer")));
		parameters.put("inexactSearch", new OpenAPIQueryParameter("loose",    "Use inexact search",                   Map.of("type", "boolean", "default", false)));

		return parameters;
	}

	private String getTagFromURLPath(final HttpServletRequest request) {

		final String pathInfo = StringUtils.substringAfter(StringUtils.defaultIfBlank(request.getPathInfo(), "").toLowerCase(), "/");

		if (StringUtils.isNotBlank(pathInfo) && pathInfo.endsWith(".json")) {

			return StringUtils.substringBeforeLast(pathInfo, ".");
		}

		return "schema";
	}
}