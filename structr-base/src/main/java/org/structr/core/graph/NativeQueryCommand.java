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
package org.structr.core.graph;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.NativeQuery;
import org.structr.api.Transaction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Path;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Executes the given Cypher query and tries to convert the result in a List
 * of {@link GraphObject}s.
 *
 *
 */
public class NativeQueryCommand extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(NativeQueryCommand.class.getName());

	private boolean dontFlushCachesIfKeywordsInQuery = false;
	private boolean runInNewTransaction = false;

	public Iterable execute(String query) throws FrameworkException {
		return execute(query, null, false);
	}

	public Iterable execute(String query, Map<String, Object> parameters) throws FrameworkException {
		return execute(query, parameters, true);
	}

	public Iterable execute(String query, Map<String, Object> parameters, boolean includeHiddenAndDeleted) throws FrameworkException {
		return execute(query, parameters, includeHiddenAndDeleted, false);
	}

	public Iterable execute(String query, Map<String, Object> parameters, boolean includeHiddenAndDeleted, boolean publicOnly) throws FrameworkException {

		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");
		if (graphDb != null) {

			final Iterable extracted, result;

			final NativeQuery<Iterable> nativeQuery = graphDb.query(query, Iterable.class);

			if (parameters != null) {
				nativeQuery.configure(parameters);
			}

			if (runInNewTransaction) {
				// Run query in isolated tx
				final Transaction tx = graphDb.beginTx(true);

				result = graphDb.execute(nativeQuery, tx);
				tx.success();
				tx.close();
			} else {

				// Run query in current tx
				result = graphDb.execute(nativeQuery);
			}

			extracted = extractRows(result, includeHiddenAndDeleted, publicOnly);

			if (!dontFlushCachesIfKeywordsInQuery && query.matches("(?i)(?s)(?m).*\\s+(delete|set|remove)\\s+.*")) {
				logger.info("Clearing all caches due to DELETE, SET or REMOVE found in native query: " + query);
				FlushCachesCommand.flushAll();
			}

			return extracted;
		}

		return Collections.emptyList();
	}

	public void setRunInNewTransaction(final boolean runInNewTransaction) {
		this.runInNewTransaction = runInNewTransaction;
	}

	public void setDontFlushCachesIfKeywordsInQuery(final boolean dontFlushCachesIfKeywordsInQuery) {
		this.dontFlushCachesIfKeywordsInQuery = dontFlushCachesIfKeywordsInQuery;
	}

	private Iterable extractRows(final Iterable<Map<String, Object>> result, final boolean includeHiddenAndDeleted, final boolean publicOnly) {
		return Iterables.map(map -> { return extractColumns(map, includeHiddenAndDeleted, publicOnly); }, result);
	}

	private Object extractColumns(final Map<String, Object> map, final boolean includeHiddenAndDeleted, final boolean publicOnly) {

		final RelationshipFactory relFactory  = new RelationshipFactory(securityContext);
		final NodeFactory nodeFactory         = new NodeFactory(securityContext);

		if (map.size() == 1) {

			final Entry<String, Object> entry = map.entrySet().iterator().next();
			final String key                  = entry.getKey();
			final Object value                = entry.getValue();

			try {

				return handleObject(nodeFactory, relFactory, key, value, includeHiddenAndDeleted, publicOnly, 0);

			} catch (FrameworkException fex) {
				logger.error(ExceptionUtils.getStackTrace(fex));
			}

		} else {

			return Iterables.map(entry -> {

				final String key = entry.getKey();
				final Object val = entry.getValue();

				try {

					return handleObject(nodeFactory, relFactory, key, val, includeHiddenAndDeleted, publicOnly, 0);

				} catch (FrameworkException fex) {
					logger.error(ExceptionUtils.getStackTrace(fex));
				}

				return null;

			}, map.entrySet());
		}

		return null;
	}

	final Object handleObject(final NodeFactory nodeFactory, final RelationshipFactory relFactory, final String key, final Object value, boolean includeHiddenAndDeleted, boolean publicOnly, int level) throws FrameworkException {

		if (value instanceof Node) {

			return nodeFactory.instantiate((Node) value, includeHiddenAndDeleted, publicOnly);

		} else if (value instanceof Relationship) {

			final Relationship relationship = (Relationship)value;
			final GraphObject sourceNode    = nodeFactory.instantiate(relationship.getStartNode(), includeHiddenAndDeleted, publicOnly);
			final GraphObject targetNode    = nodeFactory.instantiate(relationship.getEndNode(), includeHiddenAndDeleted, publicOnly);

			if (sourceNode != null && targetNode != null) {

				return relFactory.instantiate((Relationship) value);
			}

			return null;

		} else if (value instanceof Path) {

			final List list = new LinkedList<>();
			final Path path = (Path)value;

			for (final PropertyContainer container : path) {

				final Object child = handleObject(nodeFactory, relFactory, null, container, includeHiddenAndDeleted, publicOnly, level + 1);
				if (child != null) {

					list.add(child);

				} else {

					// remove path from list if one of the children is null (=> permission)
					return null;
				}
			}

			return list;

		} else if (value instanceof Map) {

			final Map<String, Object> valueMap = (Map<String, Object>)value;
			final GraphObjectMap graphObject   = new GraphObjectMap();

			for (final Entry<String, Object> valueEntry : valueMap.entrySet()) {

				final String valueKey   = valueEntry.getKey();
				final Object valueValue = valueEntry.getValue();
				final Object result     = handleObject(nodeFactory, relFactory, valueKey, valueValue, includeHiddenAndDeleted, publicOnly, level + 1);

				if (result != null) {

					graphObject.setProperty(new GenericProperty(valueKey), result);
				}
			}

			return graphObject;

		} else if (value instanceof Iterable) {

			final Iterable<Object> valueCollection = (Iterable<Object>)value;
			final List<Object> collection          = new LinkedList<>();

			for (final Object valueEntry : valueCollection) {

				final Object result = handleObject(nodeFactory, relFactory, null, valueEntry, includeHiddenAndDeleted, publicOnly, level + 1);
				if (result != null) {

					collection.add(result);

				} else {

					// remove tuple from list if one of the children is null (=> permission)
					return null;
				}
			}

			return collection;

		} else if (level == 0) {

			final GraphObjectMap graphObject = new GraphObjectMap();
			graphObject.setProperty(new GenericProperty(key), value);

			return graphObject;
		}

		return value;
	}

}
