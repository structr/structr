/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.function;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Path.Segment;
import org.neo4j.driver.types.Relationship;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.*;
import java.util.Map.Entry;

public class RemoteCypherFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_CYPHER    = "Usage: ${remote_cypher(url, username, password, query)}. Example ${remote_cypher('bolt://database.url', 'user', 'password', 'MATCH (n) RETURN n')}";
	public static final String ERROR_MESSAGE_CYPHER_JS = "Usage: ${{Structr.remoteCypher(url, username, password query)}}. Example ${{Structr.remoteCypher('bolt://database.url', 'user', 'password', 'MATCH (n) RETURN n')}}";

	private static final FixedSizeCache<String, Driver> driverCache = new FixedSizeCache<>("Driver Cache", 10);

	@Override
	public String getName() {
		return "remote_cypher";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("url, username, password, query [, parameterMap ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndTypes(sources, 4, String.class, String.class, String.class, String.class, Map.class);

			final Map<String, Object> params = new LinkedHashMap<>();
			final String url                 = sources[0].toString();
			final String username            = sources[1].toString();
			final String password            = sources[2].toString();
			final String query               = sources[3].toString();

			// parameters?
			if (sources.length > 4 && sources[4] != null && sources[4] instanceof Map) {
				params.putAll((Map)sources[4]);
			}

			final Driver driver = getDriver(url, username, password);
			if (driver != null) {

				try (final Session session = driver.session()) {

					final Result result                  = session.run(query, params);
					final List<Map<String, Object>> list = result.list(r -> {
						return r.asMap();
					});

					return extractRows(ctx.getSecurityContext(), list);
				}
			}

			return null;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CYPHER_JS : ERROR_MESSAGE_CYPHER);
	}

	@Override
	public String getShortDescription() {
		return "Returns the result of the given Cypher query against a remote instance";
	}


	// ----- private methods -----
	private Driver getDriver(final String url, final String username, final String password) {

		final String cacheKey = DigestUtils.md5Hex(url + username + password);

		Driver driver = driverCache.get(cacheKey);
		if (driver == null) {

			driver = GraphDatabase.driver(url, AuthTokens.basic(username, password), Config.builder().withEncryption().build());

			driverCache.put(cacheKey, driver);
		}

		return driver;
	}



	private Iterable extractRows(final SecurityContext securityContext, final Iterable<Map<String, Object>> result) {
		return Iterables.map(map -> { return extractColumns(securityContext, map); }, result);
	}

	private Object extractColumns(final SecurityContext securityContext, final Map<String, Object> map) {

		if (map.size() == 1) {

			final Entry<String, Object> entry = map.entrySet().iterator().next();
			final String key                  = entry.getKey();
			final Object value                = entry.getValue();

			try {

				return handleObject(securityContext, key, value, 0);

			} catch (FrameworkException fex) {
				logger.error(ExceptionUtils.getStackTrace(fex));
			}

		} else {

			return Iterables.map(entry -> {

				final String key = entry.getKey();
				final Object val = entry.getValue();

				try {

					return handleObject(securityContext, key, val, 0);

				} catch (FrameworkException fex) {
					logger.error(ExceptionUtils.getStackTrace(fex));
				}

				return null;

			}, map.entrySet());
		}

		return null;
	}

	private Object handleObject(final SecurityContext securityContext, final String key, final Object value, int level) throws FrameworkException {

		if (value instanceof Node) {

			return instantiateNode((Node) value);

		} else if (value instanceof Relationship) {

			return instantiateRelationship((Relationship) value);

		} else if (value instanceof Segment) {

			final Segment segment = (Segment)value;
			final Relationship rel = segment.relationship();
			final Node start       = segment.start();
			final Node end         = segment.end();

			final Map<String, Object> result = new HashMap<>();

			result.put("relationship", handleObject(securityContext, null, rel,   level + 1));
			result.put("start",        handleObject(securityContext, null, start, level + 1));
			result.put("end",          handleObject(securityContext, null, end,   level + 1));

			return result;

		} else if (value instanceof Path) {

			final List list = new LinkedList<>();
			final Path path = (Path)value;

			for (final Segment container : path) {

				final Object child = handleObject(securityContext, null, container, level + 1);
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
				final Object result     = handleObject(securityContext, valueKey, valueValue, level + 1);

				if (result != null) {

					graphObject.setProperty(new GenericProperty(valueKey), result);
				}
			}

			return graphObject;

		} else if (value instanceof Iterable) {

			final Iterable<Object> valueCollection = (Iterable<Object>)value;
			final List<Object> collection          = new LinkedList<>();

			for (final Object valueEntry : valueCollection) {

				final Object result = handleObject(securityContext, null, valueEntry, level + 1);
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

	private Map<String, Object> instantiateNode(final Node node) {
		return node.asMap();
	}

	private Map<String, Object> instantiateRelationship(final Relationship rel) {
		return rel.asMap();
	}
}
