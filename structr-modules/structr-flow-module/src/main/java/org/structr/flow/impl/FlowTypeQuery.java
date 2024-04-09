/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.ComparisonQuery;
import org.structr.api.search.Occurrence;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.ComparisonSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.module.api.DeployableEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowTypeQuery extends FlowBaseNode implements DataSource, DeployableEntity {

	private static final Logger logger                                  = LoggerFactory.getLogger(FlowTypeQuery.class);
	public static final Property<DataSource> dataSource                 = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<Iterable<FlowBaseNode>> dataTarget     = new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<String> dataType                       = new StringProperty("dataType");
	public static final Property<String> query                          = new StringProperty("query");

	public static final View defaultView                                = new View(FlowAction.class, PropertyView.Public, dataTarget, dataType, query);
	public static final View uiView                                     = new View(FlowAction.class, PropertyView.Ui, dataTarget, dataType, query);

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

			final Query query = app.nodeQuery(clazz);

			if (jsonObject != null && jsonObject.getJSONArray("operations").length() > 0) {
				resolveQueryObject(context, jsonObject, query);
			}

			final List list = query.getAsList();

			tx.success();

			return list;

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
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

	private Query resolveQueryObject(final Context context, final JSONObject object, final Query query) {
		final String type = object.getString("type");
		switch(type) {
			case "group":
				return resolveGroup(context, object, query);
			case "operation":
				return resolveOperation(context, object, query);
			case "sort":
				return resolveSortOperation(object, query);
		}
		return query;
	}

	private Query resolveSortOperation(final JSONObject object, final Query query) {

		final String queryType = object.getString("queryType");
		final String key = object.getString("key");
		final String order = object.getString("order");

		if (queryType != null && queryType.length() > 0 && key != null && key.length() > 0) {

			final Class queryTypeClass = StructrApp.getConfiguration().getNodeEntityClass(queryType);
			if (queryTypeClass != null) {

				final PropertyKey propKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(queryTypeClass, key);

				query.sort(propKey, "desc".equals(order));
			}

		}

		return query;
	}

	private Query resolveGroup(final Context context, final JSONObject object, final Query query) {
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
			case "not":
				query.not();
				break;
		}

		// Resolve nested elements
		for (int i = 0; i < operations.length(); i++) {
			resolveQueryObject(context, operations.getJSONObject(i), query);
		}

		query.parent();

		return query;
	}

	private Query resolveOperation(final Context context, final JSONObject object, final Query query) {
		final String key = object.getString("key");
		final String op = object.getString("op");
		Object value = object.get("value");

		PropertyKey propKey = null;

		String queryType = getProperty(dataType);
		if (queryType != null) {

			Class queryTypeClass = StructrApp.getConfiguration().getNodeEntityClass(queryType);

			if (queryTypeClass != null) {
				propKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(queryTypeClass, key);
			}

		}

		if (value != null) {
			try {

				DataSource ds = getProperty(FlowTypeQuery.dataSource);

				if (ds != null) {

					Object data = ds.get(context);

					context.setData(getUuid(), data);
				}

				value = Scripting.replaceVariables(context.getActionContext(securityContext, this), null, value.toString(), "FlowTypeQuery");
			} catch (FrameworkException ex) {
				logger.warn("FlowTypeQuery: Could not evaluate given operation.", ex);
			}
		}

		if (propKey != null) {

			List<SearchAttribute> attributes = new ArrayList<>();

			switch (op) {
				case "eq":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.equal, value, Occurrence.REQUIRED));
					break;
				case "neq":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.notEqual, value, Occurrence.REQUIRED));
					break;
				case "gt":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.greater, value, Occurrence.REQUIRED));
					break;
				case "gteq":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.greaterOrEqual, value, Occurrence.REQUIRED));
					break;
				case "ls":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.less, value, Occurrence.REQUIRED));
					break;
				case "lseq":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.lessOrEqual, value, Occurrence.REQUIRED));
					break;
				case "null":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.isNull, value, Occurrence.REQUIRED));
					break;
				case "notNull":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.isNotNull, value, Occurrence.REQUIRED));
					break;
				case "startsWith":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.startsWith, value, Occurrence.REQUIRED));
					break;
				case "endsWith":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.endsWith, value, Occurrence.REQUIRED));
					break;
				case "contains":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.contains, value, Occurrence.REQUIRED));
					break;
				case "caseInsensitiveStartsWith":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.caseInsensitiveStartsWith, value, Occurrence.REQUIRED));
					break;
				case "caseInsensitiveEndsWith":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.caseInsensitiveEndsWith, value, Occurrence.REQUIRED));
					break;
				case "caseInsensitiveContains":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Operation.caseInsensitiveContains, value, Occurrence.REQUIRED));
					break;
			}

			query.attributes(attributes);

		}

		return query;
	}

}
