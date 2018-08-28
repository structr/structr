/**
 * Copyright (C) 2010-2018 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.structr.api.search.Occurrence;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.module.api.DeployableEntity;

import java.sql.Struct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowTypeQuery extends FlowBaseNode implements DataSource, DeployableEntity {

	public static final Property<List<FlowBaseNode>> dataTarget				= new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<String> dataType							= new StringProperty("dataType");
	public static final Property<String> query								= new StringProperty("query");

	public static final View defaultView 									= new View(FlowAction.class, PropertyView.Public, dataTarget, dataType, query);
	public static final View uiView      									= new View(FlowAction.class, PropertyView.Ui, dataTarget, dataType, query);


	@Override
	public Object get(Context context) {

		App app = StructrApp.getInstance(securityContext);

		try (Tx tx = app.tx()) {

			Class clazz = StructrApp.getConfiguration().getNodeEntityClass(getProperty(dataType));

			JSONObject jsonObject = null;

			final String queryString = getProperty(query);
			if (queryString != null) {
				jsonObject = new JSONObject(queryString);
			}

			Query query = app.nodeQuery(clazz);

			if (jsonObject != null && jsonObject.getJSONArray("operations").length() > 0) {
				resolveQueryObject(jsonObject, query);
			}

			return query.getAsList();

		} catch (FrameworkException ex) {

			logger.error("Exception in FlowTypeQuery: " + ex.getMessage());
		}

		return null;
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("dataType", getProperty(dataType));
		result.put("query", getProperty(query));

		return result;
	}

	private Query resolveQueryObject(final JSONObject object, final Query query) {
		final String type = object.getString("type");
		switch(type) {
			case "group":
				return resolveGroup(object, query);
			case "operation":
				return resolveOperation(object, query);
		}
		return query;
	}

	private Query resolveGroup(final JSONObject object, final Query query) {
		final String op = object.getString("op");
		final JSONArray operations = object.getJSONArray("operations");

		// Add group operator to wrap all added SearchAttributes in a new SearchAttributeGroup
		switch (op) {
			case "and":
				query.and();
				break;
			case "or":
				query.or();
				break;
		}

		// Resolve nested elements
		for (int i = 0; i < operations.length(); i++) {
			resolveQueryObject(operations.getJSONObject(i), query);
		}

		query.parent();

		return query;
	}

	private Query resolveOperation(final JSONObject object, final Query query) {
		final String key = object.getString("key");
		final String op = object.getString("op");
		final Object value = object.get("value");

		PropertyKey propKey = null;

		String queryType = getProperty(dataType);
		if (queryType != null) {

			Class queryTypeClass = StructrApp.getConfiguration().getNodeEntityClass(queryType);

			if (queryTypeClass != null) {
				propKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(queryTypeClass, key);
			}

		}

		if (propKey != null) {

			List<SearchAttribute> attributes = new ArrayList<>();

			switch (op) {
				case "eq":
					attributes.add(propKey.getSearchAttribute(securityContext, Occurrence.REQUIRED, value, true, query));
					break;
				case "neq":
					attributes.add(propKey.getSearchAttribute(securityContext, Occurrence.FORBIDDEN, value, true, query));
					break;
			}

			query.attributes(attributes);

		}

		return query;
	}

}
