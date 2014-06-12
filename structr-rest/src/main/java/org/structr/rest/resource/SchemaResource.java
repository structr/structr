/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.CaseHelper;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.LongProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.*;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.PropertyDefinition;
import org.structr.schema.SchemaHelper;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;

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

			if (type == null) {

//				if (PropertyDefinition.exists(rawType)) {
//					type = PropertyDefinition.nodeExtender.getType(rawType);
//				}
			}

			if (type != null) {

				String url = "/".concat(CaseHelper.toUnderscore(rawType, true));

				schema.setProperty(new StringProperty("url"),   url);
				schema.setProperty(new StringProperty("type"),  rawType);
				schema.setProperty(new StringProperty("isRel"), AbstractRelationship.class.isAssignableFrom(type));
				schema.setProperty(new LongProperty("flags"), SecurityContext.getResourceFlags(rawType));

				// list property sets for all views
				Set<String> propertyViews              = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertyViews());
				Map<String, Map<String, Object>> views = new TreeMap();
				schema.setProperty(new StringProperty("views"), views);

				for (String view : propertyViews) {

					Set<PropertyKey> properties              = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(type, view));
					Map<String, Object> propertyConverterMap = new TreeMap<>();

					// augment property set with properties from PropertyDefinition
					if (PropertyDefinition.exists(type.getSimpleName())) {

						Iterable<PropertyDefinition> dynamicProperties = PropertyDefinition.getPropertiesForKind(type.getSimpleName());
						if (dynamicProperties != null) {

							for (PropertyDefinition property : dynamicProperties) {
								properties.add(property);
							}
						}

					}

					// ignore "all" and empty views
	//				if (!"all".equals(view) && !properties.isEmpty()) {
					if (!properties.isEmpty()) {

						for (PropertyKey property : properties) {

							Map<String, Object> propProperties    = new TreeMap();

							propProperties.put("dbName", property.dbName());
							propProperties.put("jsonName", property.jsonName());
							propProperties.put("className", property.getClass().getName());
							propProperties.put("defaultValue", property.defaultValue());

							propProperties.put("readOnly", property.isReadOnly());
							propProperties.put("system", property.isUnvalidated());

							PropertyConverter databaseConverter = property.databaseConverter(securityContext, null);
							PropertyConverter inputConverter    = property.inputConverter(securityContext);

							if (databaseConverter != null) {
								propProperties.put("databaseConverter", databaseConverter.getClass().getName());
							}

							if (inputConverter != null) {
								propProperties.put("inputConverter", inputConverter.getClass().getName());
							}


							propertyConverterMap.put(property.jsonName(), propProperties);
						}

						views.put(view, propertyConverterMap);
					}
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
