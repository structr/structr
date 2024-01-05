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
package org.structr.core.function;

import org.structr.api.config.Settings;
import org.structr.api.search.Occurrence;
import org.structr.autocomplete.AbstractHint;
import org.structr.autocomplete.TypeNameHint;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.function.search.AndPredicate;
import org.structr.core.function.search.SearchFunctionPredicate;
import org.structr.core.function.search.SearchParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Abstract implementation of the basic functions of the Interface QueryFunction.
 */
public abstract class AbstractQueryFunction extends CoreFunction implements QueryFunction {

	@Override
	public List<AbstractHint> getContextHints(final String lastToken) {

		final List<AbstractHint> hints = new LinkedList<>();
		final String quoteChar = lastToken.startsWith("'") ? "'" : lastToken.startsWith("\"") ? "\"" : "'";

		for (final Entry<String, Class<? extends NodeInterface>> entry : StructrApp.getConfiguration().getNodeEntities().entrySet()) {

			final String name = entry.getKey();
			final Class type  = entry.getValue();

			hints.add(new TypeNameHint(quoteChar + name + quoteChar, type.getSimpleName()));
		}

		return hints;
	}

	public void applyQueryParameters(final SecurityContext securityContext, final Query query) {

		final ContextStore contextStore = securityContext.getContextStore();
		final String sortKey            = contextStore.getSortKey();
		final int start                 = contextStore.getRangeStart();
		final int end                   = contextStore.getRangeEnd();

		// paging applied by surrounding slice() function
		if (start >= 0 && end >= 0) {

			if (securityContext.getUser(false) != null && (securityContext.getUser(false).isAdmin() || securityContext.isSuperUser())) {

				query.getQueryContext().slice(start, end);

			} else {

				logger.warn("slice() can only be used by privileged users - not applying slice.");

			}
		}

		if (sortKey != null) {

			final Class type = query.getType();
			if (type != null) {

				final PropertyKey key = StructrApp.key(type, sortKey);
				if (key != null) {

					query.sort(key, contextStore.getSortDescending());
				}

			} else {

				logger.warn("Cannot apply sort key, missing type in query object.");
			}
		}

	}

	protected void resetQueryParameters(final SecurityContext securityContext) {

		final ContextStore contextStore = securityContext.getContextStore();

		contextStore.resetQueryParameters();
	}

	protected boolean isAdvancedSearch(final SecurityContext securityContext, final Class type, final PropertyKey key, final Object value, final Query query, final boolean exact) throws FrameworkException {

		if (value instanceof Map) {

			handleObject(securityContext, type, query, value, exact);

			return true;
		}

		if (value instanceof SearchFunctionPredicate) {

			// allow predicate to modify query
			((SearchFunctionPredicate)value).configureQuery(securityContext, type, key, query, exact);

			return true;
		}

		if (value instanceof SearchParameter) {

			// default for simple search predicates is AND
			final AndPredicate and = new AndPredicate();

			and.addParameter((SearchParameter)value);
			and.configureQuery(securityContext, type, null, query, exact);

			return true;
		}

		return false;
	}

	protected Object handleQuerySources(final SecurityContext securityContext, final Class type, final Query query, final Object[] sources, final boolean exact, final String errorMessage) throws FrameworkException {

		// extension for native javascript objects
		if (sources.length == 2) {

			if (sources[1] instanceof Map) {

				handleObject(securityContext, type, query, sources[1], exact);

			} else {

				if (sources[1] == null) {

					throw new IllegalArgumentException();
				}

				if (!isAdvancedSearch(securityContext, type, null, sources[1], query, exact)) {

					final String uuid = sources[1].toString();

					if (Settings.isValidUuid(uuid)) {

						// special case: second parameter is a UUID
						final PropertyKey key = StructrApp.key(type, "id");

						query.and(key, sources[1].toString());

						return query.getFirst();

					} else {

						// probably an error case where migration to predicates was forgotten
						throw new FrameworkException(422, getReplacement() + ": Invalid parameter '" + uuid + "', returning null. If a single parameter is given, it must be of type Map, UUID or Advanced Find predicate. Maybe a missing migration of Advanced Find to predicates?");
					}
				}
			}

		} else {

			final int parameter_count = sources.length;

			// the below loop must work for both simple parameters (key, value, key, value, key, value, ...)
			// as well as advanced ones (predicate, predicate, predicate, ...) so we increment the value
			// of c inside the loop if a non-advanced parameter is encountered.

			for (int c = 1; c < parameter_count; c++) {

				if (sources[c] == null) {
					throw new IllegalArgumentException();
				}

				if (!isAdvancedSearch(securityContext, type, null, sources[c], query, exact)) {

					final PropertyKey key = StructrApp.key(type, sources[c].toString());
					if (key != null) {

						final PropertyConverter inputConverter = key.inputConverter(securityContext);

						// check number of parameters dynamically
						if (c + 1 >= sources.length) {

							throw new FrameworkException(400, "Invalid number of parameters, missing value for key " + key.jsonName() + ": " + errorMessage);
						}

						Object value = sources[++c]; // increment c to

						if (!isAdvancedSearch(securityContext, type, key, value, query, exact)) {

							if (inputConverter != null) {

								value = inputConverter.convert(value);
							}

							// basic search is always AND
							query.and(key, value, exact);
						}
					}
				}
			}
		}

		return query.getAsList();
	}

	// ----- private methods -----
	private void handleObject(final SecurityContext securityContext, final Class type, final Query query, final Object source, final boolean exact) throws FrameworkException {

		if (source instanceof Map) {

			final Map<String, Object> queryData = (Map)source;
			for (final Entry<String, Object> entry : queryData.entrySet()) {

				final String keyName = entry.getKey();
				final Object value   = entry.getValue();

				if (keyName.startsWith("$")) {

					final String operator = keyName.substring(1).toLowerCase();
					switch (operator) {

						case "and":
							handleAndObject(securityContext, type, query, value, exact);
							break;

						case "or":
							handleOrObject(securityContext, type, query, value, exact);
							break;

						case "not":
							handleNotObject(securityContext, type, query, value, exact);
							break;
					}

				} else {

					final PropertyKey key = StructrApp.key(type, keyName);

					if (!isAdvancedSearch(securityContext, type, key, value, query, exact)) {

						final PropertyConverter inputConverter = key.inputConverter(securityContext);
						if (inputConverter != null) {

							if (Occurrence.OPTIONAL.equals(query.getCurrentOccurrence())) {
								query.or(key, inputConverter.convert(value), exact);
							} else {
								query.and(key, inputConverter.convert(value), exact);
							}

						} else {

							if (Occurrence.OPTIONAL.equals(query.getCurrentOccurrence())) {
								query.or(key, value, exact);
							} else {
								query.and(key, value, exact);
							}
						}
					}
				}
			}

		} else if (source != null) {

			throw new FrameworkException(422, "Invalid type in advanced search query: expected object, got " + source.getClass().getSimpleName().toLowerCase());

		} else {

			throw new FrameworkException(422, "Invalid type in advanced search query: expected object, got null");
		}
	}

	private void handleAndObject(final SecurityContext securityContext, final Class type, final Query query, final Object source, final boolean exact) throws FrameworkException {

		query.and();
		handleObject(securityContext, type, query, source, exact);
		query.parent();
	}

	private void handleOrObject(final SecurityContext securityContext, final Class type, final Query query, final Object source, final boolean exact) throws FrameworkException {

		query.or();
		handleObject(securityContext, type, query, source, exact);
		query.parent();
	}

	private void handleNotObject(final SecurityContext securityContext, final Class type, final Query query, final Object source, final boolean exact) throws FrameworkException {

		query.not();
		handleObject(securityContext, type, query, source, exact);
		query.parent();
	}
}
