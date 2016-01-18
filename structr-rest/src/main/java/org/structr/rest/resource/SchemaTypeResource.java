/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------
/**
 *
 *
 */
public class SchemaTypeResource extends Resource {

	protected Class entityClass = null;
	protected String rawType = null;
	protected HttpServletRequest request = null;
	protected TypeResource typeResource = null;
	private String propertyView = null;

	//~--- methods --------------------------------------------------------
	public SchemaTypeResource(SecurityContext securityContext, TypeResource typeResource) {
		this.securityContext = securityContext;
		this.typeResource = typeResource;
		this.rawType = typeResource.getRawType();
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		return true;

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<GraphObjectMap> resultList = new LinkedList<>();

		// create & add schema information
		Class type = typeResource.getEntityClass();
		if (type != null) {

			if (propertyView != null) {

				for (final Map.Entry<String, Object> entry : getPropertiesForView(type, propertyView).entrySet()) {

					final GraphObjectMap property = new GraphObjectMap();

					for (final Map.Entry<String, Object> prop : ((Map<String, Object>) entry.getValue()).entrySet()) {

						property.setProperty(new GenericProperty(prop.getKey()), prop.getValue());
					}

					resultList.add(property);
				}

			} else {

				final GraphObjectMap schema = new GraphObjectMap();

				resultList.add(schema);

				String url = "/".concat(rawType);

				schema.setProperty(new StringProperty("url"), url);
				schema.setProperty(new StringProperty("type"), type.getSimpleName());
				schema.setProperty(new StringProperty("className"), type.getName());
				schema.setProperty(new BooleanProperty("isRel"), AbstractRelationship.class.isAssignableFrom(type));
				schema.setProperty(new LongProperty("flags"), SecurityContext.getResourceFlags(rawType));

				Set<String> propertyViews = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertyViews());

				// list property sets for all views
				Map<String, Map<String, Object>> views = new TreeMap();
				schema.setProperty(new GenericProperty("views"), views);

				for (String view : propertyViews) {

					if (!View.INTERNAL_GRAPH_VIEW.equals(view)) {

						views.put(view, getPropertiesForView(type, view));
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

		if (next instanceof ViewFilterResource) {

			propertyView = ((ViewFilterResource) next).getPropertyView();
		}

		return this;
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getUriPart() {

		return rawType;

	}

	public String getRawType() {

		return rawType;

	}

	@Override
	public Class getEntityClass() {

		return entityClass;

	}

	@Override
	public String getResourceSignature() {

		return SchemaResource.UriPart._schema.name().concat("/").concat(SchemaHelper.normalizeEntityName(getUriPart()));

	}

	@Override
	public boolean isCollectionResource() {

		return true;

	}

	private Map<String, Object> getPropertiesForView(final Class type, final String view) throws FrameworkException {

		final Set<PropertyKey> properties = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(type, view));
		final Map<String, Object> propertyConverterMap = new LinkedHashMap<>();

		for (PropertyKey property : properties) {

			propertyConverterMap.put(property.jsonName(), SchemaHelper.getPropertyInfo(securityContext, property));
		}

		return propertyConverterMap;
	}
}
