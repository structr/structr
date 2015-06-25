/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.CaseHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaNodeLocalization;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.property.UuidProperty;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;
import org.structr.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
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

				try {

					SchemaNode schemaNode = StructrApp.getInstance().nodeQuery(SchemaNode.class).andName(type.getSimpleName()).getFirst();

					if (schemaNode != null) {

						final List<SchemaNodeLocalization> nodeLocalizations = schemaNode.localizations.getProperty(securityContext, schemaNode, false);
						final List<GraphObjectMap> localizationsMap = new ArrayList<>(nodeLocalizations.size());

						for (final SchemaNodeLocalization loc : nodeLocalizations) {

							final GraphObjectMap tmpMap = new GraphObjectMap();
							tmpMap.setProperty(new UuidProperty(), loc.getProperty(SchemaNodeLocalization.id));
							tmpMap.setProperty(new StringProperty("locale"), loc.getProperty(SchemaNodeLocalization.locale));
							tmpMap.setProperty(new StringProperty("name"), loc.getProperty(SchemaNodeLocalization.name));
							localizationsMap.add(tmpMap);

						}

						schema.setProperty(new GenericProperty("localizations"), localizationsMap);


					}

				} catch (FrameworkException ex) {

					Logger.getLogger(SchemaTypeResource.class.getName()).log(Level.SEVERE, "Error looking up SchemaNode - cannot display labels for properties!", ex);
				}
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
