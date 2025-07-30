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

import org.structr.api.config.Settings;
import org.structr.autocomplete.AbstractHint;
import org.structr.autocomplete.TypeNameHint;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.app.QueryGroup;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.function.search.AndPredicate;
import org.structr.core.function.search.SearchFunctionPredicate;
import org.structr.core.function.search.SearchParameter;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

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

		for (final String type : Traits.getAllTypes(t -> t.isNodeType() && !t.isServiceClass())) {

			hints.add(new TypeNameHint(quoteChar + type + quoteChar, type));
		}

		return hints;
	}

	public void applyQueryParameters(final SecurityContext securityContext, final Query<?> query) {

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

			final Traits traits = query.getTraits();
			if (traits != null) {

				final PropertyKey<?> key = traits.key(sortKey);
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

	protected boolean isAdvancedSearch(final SecurityContext securityContext, final Traits type, final PropertyKey key, final Object value, final QueryGroup query, final boolean exact) throws FrameworkException {

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

	protected Object handleQuerySources(final SecurityContext securityContext, final Traits traits, final QueryGroup query, final Object[] sources, final boolean exact, final String errorMessage) throws FrameworkException {

		// extension for native javascript objects
		if (sources.length == 2) {

			if (sources[1] instanceof Map) {

				handleObject(securityContext, traits, query, sources[1], exact);

			} else {

				if (sources[1] == null) {

					throw new IllegalArgumentException();
				}

				if (!isAdvancedSearch(securityContext, traits, null, sources[1], query, exact)) {

					final String uuid = sources[1].toString();

					if (Settings.isValidUuid(uuid)) {

						// special case: second parameter is a UUID
						final PropertyKey key = traits.key(GraphObjectTraitDefinition.ID_PROPERTY);

						query.and().key(key, sources[1].toString());

						return query.getFirst();

					} else {

						// probably an error case where migration to predicates was forgotten
						throw new FrameworkException(422, getReplacement() + ": Invalid parameter '" + uuid + "'. If a single parameter is given, it must be of type Map, UUID or Advanced Find predicate. Maybe a missing migration of Advanced Find to predicates?");
					}
				}
			}

		} else {

			final int parameter_count = sources.length;

			// the below loop must work for both simple parameters (key, value, key, value, key, value, ...)
			// and advanced ones (predicate, predicate, predicate, ...) so we increment the value
			// of c inside the loop if a non-advanced parameter is encountered.

			for (int c = 1; c < parameter_count; c++) {

				if (sources[c] == null) {
					throw new IllegalArgumentException();
				}

				if (!isAdvancedSearch(securityContext, traits, null, sources[c], query, exact)) {

					final String keyName = sources[c].toString();
					if (traits.hasKey(keyName)) {

						final PropertyKey key = traits.key(keyName);
						final PropertyConverter inputConverter = key.inputConverter(securityContext, false);

						// check number of parameters dynamically
						if (c + 1 >= sources.length) {

							throw new FrameworkException(400, "Invalid number of parameters, missing value for key " + key.jsonName() + ": " + errorMessage);
						}

						Object value = sources[++c]; // increment c to

						if (!isAdvancedSearch(securityContext, traits, key, value, query, exact)) {

							if (inputConverter != null) {

								value = inputConverter.convert(value);
							}

							// basic search is always AND
							query.and().key(key, value, exact);
						}

					} else {

						throw new FrameworkException(422, "Unknown key '" + keyName + "', returning null");
					}
				}
			}
		}

		return query.getAsList();
	}

	// ----- private methods -----
	private void handleObject(final SecurityContext securityContext, final Traits traits, final QueryGroup query, final Object source, final boolean exact) throws FrameworkException {

		if (source instanceof Map) {

			final Map<String, Object> queryData = (Map)source;
			for (final Entry<String, Object> entry : queryData.entrySet()) {

				final String keyName = entry.getKey();
				final Object value   = entry.getValue();

				if (keyName.startsWith("$")) {

					final String operator = keyName.substring(1).toLowerCase();
					switch (operator) {

						case "and":
							handleObject(securityContext, traits, query.and(), value, exact);
							break;

						case "or":
							handleObject(securityContext, traits, query.or(), value, exact);
							break;

						case "not":
							handleObject(securityContext, traits, query.not(), value, exact);
							break;
					}

				} else {

					final PropertyKey key = traits.key(keyName);

					if (!isAdvancedSearch(securityContext, traits, key, value, query, exact)) {

						Object convertedValue = value;

						final PropertyConverter inputConverter = key.inputConverter(securityContext, false);
						if (inputConverter != null) {

							convertedValue = inputConverter.convert(value);
						}

						query.key(key, convertedValue, exact);
					}
				}
			}

		} else if (source != null) {

			throw new FrameworkException(422, "Invalid type in advanced search query: expected object, got " + source.getClass().getSimpleName().toLowerCase());

		} else {

			throw new FrameworkException(422, "Invalid type in advanced search query: expected object, got null");
		}
	}
}
