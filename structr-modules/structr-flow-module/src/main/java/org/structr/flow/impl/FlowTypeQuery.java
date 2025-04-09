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
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.ComparisonSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.flow.engine.Context;
import org.structr.flow.traits.definitions.FlowDataSourceTraitDefinition;
import org.structr.flow.traits.definitions.FlowTypeQueryTraitDefinition;
import org.structr.module.api.DeployableEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FlowTypeQuery extends FlowDataSource implements DeployableEntity {

	private static final Logger logger = LoggerFactory.getLogger(FlowTypeQuery.class);

	public FlowTypeQuery(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getDataType() {
		return wrappedObject.getProperty(traits.key(FlowTypeQueryTraitDefinition.DATA_TYPE_PROPERTY));
	}

	public String getQuery() {
		return wrappedObject.getProperty(traits.key(FlowDataSourceTraitDefinition.QUERY_PROPERTY));
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             getUuid());
		result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           getType());
		result.put(FlowTypeQueryTraitDefinition.DATA_TYPE_PROPERTY,                    getDataType());
		result.put(FlowDataSourceTraitDefinition.QUERY_PROPERTY,                       getQuery());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        isVisibleToPublicUsers());
		result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, isVisibleToAuthenticatedUsers());

		return result;
	}

	public Query resolveQueryObject(final Context context, final JSONObject object, final Query query) {
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

	public Query resolveSortOperation(final JSONObject object, final Query query) {

		final String queryType = object.getString("queryType");
		final String key       = object.getString("key");
		final String order     = object.getString("order");
		final Traits traits    = Traits.of(queryType);

		if (queryType != null && queryType.length() > 0 && key != null && traits.hasKey(key)) {

			final PropertyKey propKey = traits.key(key);

			query.sort(propKey, "desc".equals(order));
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

		final String queryType = getDataType();
		if (queryType != null && Traits.exists(queryType) && Traits.of(queryType).hasKey(key)) {

			propKey = Traits.of(queryType).key(key);
		}

		if (value != null) {

			try {

				final FlowDataSource ds = getDataSource();
				if (ds != null) {

					final Object data = ds.get(context);

					context.setData(getUuid(), data);
				}

				value = Scripting.replaceVariables(context.getActionContext(getSecurityContext(), this), null, value.toString(), StructrTraits.FLOW_TYPE_QUERY);

			} catch (FrameworkException ex) {
				logger.warn("FlowTypeQuery: Could not evaluate given operation.", ex);
			}
		}

		if (propKey != null) {

			List<SearchAttribute> attributes = new ArrayList<>();

			switch (op) {
				case "eq":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.equal, value));//, Operation.AND));
					break;
				case "neq":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.notEqual, value));//, Operation.AND));
					break;
				case "gt":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.greater, value));//, Operation.AND));
					break;
				case "gteq":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.greaterOrEqual, value));//, Operation.AND));
					break;
				case "ls":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.less, value));//, Operation.AND));
					break;
				case "lseq":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.lessOrEqual, value));//, Operation.AND));
					break;
				case "null":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.isNull, value));//, Operation.AND));
					break;
				case "notNull":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.isNotNull, value));//, Operation.AND));
					break;
				case "startsWith":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.startsWith, value));//, Operation.AND));
					break;
				case "endsWith":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.endsWith, value));//, Operation.AND));
					break;
				case "contains":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.contains, value));//, Operation.AND));
					break;
				case "caseInsensitiveStartsWith":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.caseInsensitiveStartsWith, value));//, Operation.AND));
					break;
				case "caseInsensitiveEndsWith":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.caseInsensitiveEndsWith, value));//, Operation.AND));
					break;
				case "caseInsensitiveContains":
					attributes.add(new ComparisonSearchAttribute(propKey, ComparisonQuery.Comparison.caseInsensitiveContains, value));//, Operation.AND));
					break;
			}

			query.attributes(attributes);

		}

		return query;
	}

}
