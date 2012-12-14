/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

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
import org.structr.core.converter.PropertyConverter;
import org.structr.core.module.ModuleService;
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
		
		List<GraphObjectMap> resultList = new LinkedList<GraphObjectMap>();
		
		// extract types from ModuleService
		ModuleService moduleService = (ModuleService)Services.getService(ModuleService.class);
		for (String rawType : moduleService.getCachedNodeEntityTypes()) {
			
			// create & add schema information
			Class type               = EntityContext.getEntityClassForRawType(rawType);
			GraphObjectMap schema = new GraphObjectMap();
			resultList.add(schema);
			
			String url = "/".concat(CaseHelper.toUnderscore(rawType, true));
			
			schema.setProperty(new StringProperty("url"),   url);
			schema.setProperty(new StringProperty("type"),  rawType);
			schema.setProperty(new LongProperty("flags"), SecurityContext.getResourceFlags(rawType));
			
			// list property sets for all views
			Map<String, Map<String, Object>> views = new TreeMap();
			Set<String> propertyViews              = EntityContext.getPropertyViews();
			schema.setProperty(new StringProperty("views"), views);
			
			for (String view : propertyViews) {
				
				Map<String, Object> propertyConverterMap = new TreeMap<String, Object>();
				Set<PropertyKey> properties              = EntityContext.getPropertySet(type, view);
				
				// ignore "all" and empty views
//				if (!"all".equals(view) && !properties.isEmpty()) {
				if (!properties.isEmpty()) {
					
					for (PropertyKey property : properties) {

						Map<String, Object> propProperties    = new TreeMap();
						
						propProperties.put("dbName", property.dbName());
						propProperties.put("jsonName", property.jsonName());
						propProperties.put("className", property.getClass().getName());
						propProperties.put("defaultValue", property.defaultValue());
						
						propProperties.put("readOnly", property.isReadOnlyProperty());
						propProperties.put("system", property.isSystemProperty());
						
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
		
		return new Result(resultList, resultList.size(), false, false);
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
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
