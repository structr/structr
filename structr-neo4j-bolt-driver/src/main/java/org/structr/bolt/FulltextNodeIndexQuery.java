/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.bolt;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.api.graph.Direction;
import org.structr.api.graph.RelationshipType;
import org.structr.api.search.Operation;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;
import org.structr.api.util.Iterables;
import org.structr.api.util.QueryHistogram;
import org.structr.api.util.QueryTimer;

import java.util.*;

/**
 *
 */
public class FulltextNodeIndexQuery extends AdvancedCypherQuery {

	private String key = null;
	private Object value = null;

	public FulltextNodeIndexQuery(QueryContext queryContext, AbstractCypherIndex<?> index, int requestedPageSize, int requestedPage) {
		super(queryContext, index, requestedPageSize, requestedPage);
	}

	@Override
	public void addSimpleParameter(final String identifier, final String key, final String operator, final Object value, final boolean isProperty, final boolean caseInsensitive) {
		this.key   = key;
		this.value = value;
	}

	@Override
	public String getStatement() {
		return "CALL db.index.fulltext.queryNodes(\"" + Iterables.first(typeLabels) + "_" + key + "\", \"" + value +"\") YIELD node RETURN node LIMIT 10";
	}

}