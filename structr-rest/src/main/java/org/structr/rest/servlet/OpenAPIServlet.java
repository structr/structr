/*
 * Copyright (C) 2010-2020 Structr GmbH
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
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.export.StructrSchemaDefinition;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.export.StructrTypeDefinitions;
import org.structr.schema.openapi.OpenAPIArraySchema;
import org.structr.schema.openapi.OpenAPIExampleAnyResult;
import org.structr.schema.openapi.OpenAPIObjectSchema;
import org.structr.schema.openapi.OpenAPIQueryParameter;
import org.structr.schema.openapi.OpenAPIRequestResponse;
import org.structr.schema.openapi.OpenAPIResultSchema;
import org.structr.schema.openapi.OpenAPIStructrTypeSchema;

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

				gson.toJson(createOpenAPIRoot(tag), writer);

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
	private Map<String, Object> createOpenAPIRoot(final String tag) throws FrameworkException {

		final StructrSchemaDefinition schema = (StructrSchemaDefinition)StructrSchema.createFromDatabase(StructrApp.getInstance());
		final Map<String, Object> root       = new LinkedHashMap<>();

		root.put("openapi",    "3.0.2");
		root.put("info",       createInfoObject());
		root.put("servers",    createServersObject());
		root.put("components", createComponentsObject(schema, tag));
		root.put("paths",      schema.serializeOpenAPIOperations(tag));
		root.put("tags",       createTagsObject(schema, tag));

		return root;
	}

	private Map<String, Object> createInfoObject() {

		final Map<String, Object> info = new LinkedHashMap<>();

		info.put("title",   "Structr REST API");
		info.put("version", "1.0.0");

		return info;
	}

	private List<Map<String, Object>> createServersObject() {

		final List<Map<String, Object>> servers = new LinkedList<>();
		final Map<String, Object> server        = new LinkedHashMap<>();

		server.put("url",         getStructrUrl());
		server.put("description", "The Structr REST Server");

		// add server to list
		servers.add(server);

		return servers;
	}

	private Map<String, Object> createComponentsObject(final StructrSchemaDefinition schema, final String tag) {

		final Map<String, Object> components = new TreeMap<>();

		components.put("schemas",    createSchemasObject(schema, tag));
		components.put("responses",  createResponsesObject());
		components.put("parameters", createParametersObject());

		return components;
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

		final StructrTypeDefinitions definitions = schema.getTypeDefinitionsObject();
		final Map<String, Object> map            = new TreeMap<>();

		// base classes
		map.put("AbstractNode", new OpenAPIStructrTypeSchema(AbstractNode.class, PropertyView.Public));
		map.put("Principal",    new OpenAPIStructrTypeSchema(Principal.class, PropertyView.Public));

		for (final String view : StructrApp.getConfiguration().getPropertyViews().stream().filter(name -> !StructrTypeDefinition.VIEW_BLACKLIST.contains(name)).collect(Collectors.toSet())) {

			map.putAll(definitions.serializeOpenAPI(tag, view));
		}

		return map;
	}

	private String getStructrUrl() {

		final StringBuilder buf = new StringBuilder();

		buf.append("http");
		buf.append("://");
		buf.append(Settings.ApplicationHost.getValue());
		buf.append(":");
		buf.append(Settings.HttpPort.getValue());
		buf.append(Settings.RestPath.getValue());

		return buf.toString();
	}

	private Map<String, Object> createResponsesObject() {

		final Map<String, Object> responses = new LinkedHashMap<>();
		final Map<String, Object> errors    = new LinkedHashMap<>();
		final Map<String, Object> items     = new LinkedHashMap<>();

		responses.put("created", new OpenAPIRequestResponse("Created",
			new OpenAPIResultSchema(new OpenAPIArraySchema(Map.of("type", "string")), false),
			new OpenAPIExampleAnyResult(Arrays.asList("cf8b18f28b7c4dada3085656e78d9bd2"))
		));

		responses.put("forbidden", new OpenAPIRequestResponse("Forbidden",
			new OpenAPIObjectSchema(Map.of(
				"code",    Map.of("type", "integer"),
				"message", Map.of("type", "string")
			)),
			Map.of("code", "401", "message", "Forbidden")
		));

		responses.put("notFound", new OpenAPIRequestResponse("Not Found",
			new OpenAPIObjectSchema(Map.of(
				"code",    Map.of("type", "integer"),
				"message", Map.of("type", "string")
			)),
			Map.of("code", "404", "message", "Not Found")
		));

		responses.put("validationError", new OpenAPIRequestResponse("Validation Error",
			new OpenAPIObjectSchema(Map.of(
				"code",    Map.of("type", "integer"),
				"message", Map.of("type", "string"),
				"errors",  errors
			)),
			Map.of("code", "422", "message", "Unable to commit transaction, validation failed", "errors", List.of(
				Map.of("type", "Folder", "property", "name", "token", "must_not_be_empty"),
				Map.of("type", "Folder", "property", "name", "token", "must_match", "detail", "[^\\\\/\\\\x00]+")
			))
		));

		errors.put("type", "array");
		errors.put("items", items);

		items.put("type",       "object");
		items.put("properties", Map.of(
			"type",     Map.of("type", "string"),
			"property", Map.of("type", "string"),
			"token",    Map.of("type", "string"),
			"detail",   Map.of("type", "string")
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