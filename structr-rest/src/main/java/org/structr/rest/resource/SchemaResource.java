/**
 * Copyright (C) 2010-2015 Structr GmbH
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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;
import org.structr.schema.SchemaHelper;

/**
 *
 *
 */
public class SchemaResource extends Resource {

	public enum UriPart {
		_schema
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;

		if (UriPart._schema.name().equals(part)) {

			return true;
		}

		return false;
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<GraphObjectMap> resultList = new LinkedList<>();

		// extract types from ModuleService
		for (String rawType : StructrApp.getConfiguration().getNodeEntities().keySet()) {

			// create & add schema information
			Class type            = SchemaHelper.getEntityClassForRawType(rawType);
			GraphObjectMap schema = new GraphObjectMap();
			resultList.add(schema);

			if (type != null) {

				String url = "/".concat(rawType);

				schema.setProperty(new StringProperty("url"), url);
				schema.setProperty(new StringProperty("type"), type.getSimpleName());
				schema.setProperty(new StringProperty("className"), type.getName());
				schema.setProperty(new StringProperty("isRel"), AbstractRelationship.class.isAssignableFrom(type));
				schema.setProperty(new LongProperty("flags"), SecurityContext.getResourceFlags(rawType));
			}


		}

		return new Result(resultList, resultList.size(), false, false);
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof TypeResource) {

			SchemaTypeResource schemaTypeResource = new SchemaTypeResource(securityContext, (TypeResource) next);

			return schemaTypeResource;

		}

		throw new IllegalPathException();
	}

	@Override
	public String getUriPart() {
		return "";
	}

	@Override
	public Class getEntityClass() {
		return null;
	}

	@Override
	public String getResourceSignature() {
		return UriPart._schema.name();
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {
		return true;
	}
}
