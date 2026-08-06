/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.datasources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.structr.api.search.ComparisonQuery;
import org.structr.api.search.Operation;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.app.QueryGroup;
import org.structr.core.graph.search.ComparisonSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;

public class QueryBuilder {

	private final Traits traits;

	public QueryBuilder(final String type) {

		traits = Traits.of(type);
	}

	public QueryGroup resolveQueryObject(final ActionContext context, final JSONObject object, final QueryGroup query) {

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

	// ----- private methods -----
	private QueryGroup resolveSortOperation(final JSONObject object, final QueryGroup query) {

		final String queryType = object.getString("queryType");
		final String key       = object.getString("key");
		final String order  = object.getString("order");
		final Traits traits = Traits.of(queryType);

		if (queryType != null && queryType.length() > 0 && key != null && traits.hasKey(key)) {

			final PropertyKey propKey = traits.key(key);

			query.sort(propKey, "desc".equals(order));
		}

		return query;
	}

	private QueryGroup resolveGroup(final ActionContext context, final JSONObject object, final QueryGroup query) {

		final String    op         = object.getString("op");
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

		return query;
	}

	private QueryGroup resolveOperation(final ActionContext context, final JSONObject object, final QueryGroup query) {

		final String key = object.getString("key");
		final String op  = object.getString("op");
		Object value     = object.get("value");
		PropertyKey propKey = traits.key(key);

		if (value != null) {

			try {

				value = Scripting.replaceVariables(context, null, value, StructrTraits.FLOW_TYPE_QUERY);

			} catch (FrameworkException ex) {

				ex.printStackTrace();
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

			query.attributes(attributes, Operation.AND);
		}

		return query;
	}
}
