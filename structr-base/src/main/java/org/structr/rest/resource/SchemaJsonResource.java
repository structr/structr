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
package org.structr.rest.resource;


import jakarta.servlet.http.HttpServletRequest;
import org.structr.api.schema.InvalidSchemaException;
import org.structr.api.schema.JsonSchema;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaHelper;
import org.structr.schema.export.StructrSchema;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;

public class SchemaJsonResource extends Resource {

	private static final String resourceIdentifier = "_schemaJson";
	private String uriPart                         = null;

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return null;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;

		if (part.equals(resourceIdentifier)) {
			this.uriPart = part;
			return true;
		}

		return false;
	}

	@Override
	public String getUriPart() {
		return this.uriPart;
	}

	@Override
	public Class<? extends GraphObject> getEntityClass() {
		return null;
	}

	@Override
	public String getResourceSignature() {
		return SchemaHelper.normalizeEntityName(getUriPart());
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {
		return false;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		String schema = null;

		final JsonSchema jsonSchema = StructrSchema.createFromDatabase(StructrApp.getInstance());
		schema                      = jsonSchema.toString();

		return new PagingIterable<>("/" + getUriPart(), Arrays.asList(schema));

	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if(propertySet != null && propertySet.containsKey("schema")) {

			try {
				final App app           = StructrApp.getInstance(securityContext);
				final String schemaJson = (String)propertySet.get("schema");

				StructrSchema.replaceDatabaseSchema(app, StructrSchema.createFromSource(schemaJson));

				return new RestMethodResult(200, "Schema imported successfully");

			} catch (InvalidSchemaException | URISyntaxException ex) {

				return new RestMethodResult(422, ex.getMessage());
			}
		}

		return new RestMethodResult(400, "Invalid request body. Specify schema json string as 'schema' in request body.");
	}

}
