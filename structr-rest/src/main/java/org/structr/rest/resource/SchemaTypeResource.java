/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
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
import org.structr.common.CaseHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.PropertyDefinition;
import org.structr.core.entity.Relation;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author Axel Morgner
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
		if (type == null) {

//			if (PropertyDefinition.exists(rawType)) {
//				type = PropertyDefinition.nodeExtender.getType(rawType);
//			}
		}

		if (type != null) {


			if (propertyView != null) {

				for (final Map.Entry<String, Object> entry : getPropertiesForView(type, propertyView).entrySet()) {

					final GraphObjectMap property = new GraphObjectMap();
					
					for (final Map.Entry<String, Object> prop : ((Map<String, Object>) entry.getValue()).entrySet()) {
					
						property.setProperty(new StringProperty(prop.getKey()), prop.getValue());
					}

					resultList.add(property);
				}

			} else {
		
				final GraphObjectMap schema = new GraphObjectMap();

				resultList.add(schema);

				String url = "/".concat(CaseHelper.toUnderscore(rawType, true));

				schema.setProperty(new StringProperty("url"), url);
				schema.setProperty(new StringProperty("type"), type.getSimpleName());
				schema.setProperty(new StringProperty("className"), type.getName());
				schema.setProperty(new StringProperty("isRel"), AbstractRelationship.class.isAssignableFrom(type));
				schema.setProperty(new LongProperty("flags"), SecurityContext.getResourceFlags(rawType));

				Set<String> propertyViews = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertyViews());

				// list property sets for all views
				Map<String, Map<String, Object>> views = new TreeMap();
				schema.setProperty(new StringProperty("views"), views);

				for (String view : propertyViews) {

					views.put(view, getPropertiesForView(type, view));

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

	private Map<String, Object> getPropertiesForView(final Class type, final String view) {

		final Set<PropertyKey> properties = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(type, view));
		final Map<String, Object> propertyConverterMap = new LinkedHashMap<>();

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
		//                      if (!"all".equals(view) && !properties.isEmpty()) {
		if (!properties.isEmpty()) {

			for (PropertyKey property : properties) {

				final Map<String, Object> propProperties = new LinkedHashMap();

				propProperties.put("dbName", property.dbName());
				propProperties.put("jsonName", property.jsonName());
				propProperties.put("className", property.getClass().getName());

				final Class declaringClass = property.getDeclaringClass();

				propProperties.put("declaringClass", declaringClass);
				propProperties.put("defaultValue", property.defaultValue());
				propProperties.put("readOnly", property.isReadOnly());
				propProperties.put("system", property.isUnvalidated());
				propProperties.put("indexed", property.isIndexed());
				propProperties.put("indexedWhenEmpty", property.isIndexedWhenEmpty());

				final Class<? extends GraphObject> relatedType = property.relatedType();
				if (relatedType != null) {
					propProperties.put("relatedType", relatedType.getName());
					propProperties.put("type", relatedType.getSimpleName());
				} else {
					propProperties.put("type", property.typeName());
				}
				propProperties.put("isCollection", property.isCollection());

				final PropertyConverter databaseConverter = property.databaseConverter(securityContext, null);
				final PropertyConverter inputConverter = property.inputConverter(securityContext);

				if (databaseConverter != null) {

					propProperties.put("databaseConverter", databaseConverter.getClass().getName());
				}

				if (inputConverter != null) {

					propProperties.put("inputConverter", inputConverter.getClass().getName());
				}

				//if (declaringClass != null && ("org.structr.dynamic".equals(declaringClass.getPackage().getName()))) {
				if (declaringClass != null && (declaringClass.getName().startsWith("org.structr.dynamic"))) {

					if (property instanceof RelationProperty) {

						Relation relation = ((RelationProperty) property).getRelation();

						if (relation != null) {
							propProperties.put("relationshipType", relation.name());
						}
					}

				}

				propertyConverterMap.put(property.jsonName(), propProperties);

			}
		}

		return propertyConverterMap;
	}
}
