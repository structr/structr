/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.rest.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.NotFoundException;

/**
 */
public abstract class RESTEndpoint {

	private final Map<String, RESTParameter> parts = new LinkedHashMap<>();
	private final String pathSeparator            = "/";
	private Pattern pattern                       = null;

	public RESTEndpoint(final RESTParameter... parameters) {
		initialize(parameters);
	}

	/**
	 * Returns a handler that is capable of handling an API call accepted
	 * by this endpoint, or null of the call cannot be handled by this
	 * endpoint.
	 *
	 * @param securityContext
	 * @param call
	 *
	 * @return an APICallHandler, or null
	 *
	 * @throws FrameworkException
	 */
	public abstract RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException;

	public Matcher matcher(final String path) {
		return pattern.matcher(path);
	}

	public RESTCall initializeRESTCall(final Matcher matcher, final String viewName, final boolean isDefaultView) {

		final RESTCall call = new RESTCall(matcher.group(), viewName, isDefaultView);
		int group           = 1;

		for (final RESTParameter part : parts.values()) {

			final String value = matcher.group(group++);

			call.put(part.key(), value);
		}

		return call;
	}

	// ----- protected methods -----
	protected GraphObject getEntity(final SecurityContext securityContext, final Class entityClass, final String typeName, final String uuid) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		if (entityClass == null) {

			if (uuid != null) {

				throw new NotFoundException("Type " + typeName + " does not exist for request with entity ID " + uuid);

			} else {

				throw new NotFoundException("Request specifies no value for type and entity ID");
			}
		}

		GraphObject entity = app.nodeQuery(entityClass).uuid(uuid).getFirst();
		if (entity != null) {

			return entity;
		}

		entity = app.relationshipQuery(entityClass).uuid(uuid).getFirst();
		if (entity != null) {

			return entity;
		}

		throw new NotFoundException("Entity with ID " + uuid + " of type " +  typeName +  " does not exist");
	}

	// ----- private methods -----
	private void initialize(final RESTParameter... parameters) {

		final StringBuilder buf = new StringBuilder();

		for (final RESTParameter parameter : parameters) {

			parts.put(parameter.key(), parameter);

			buf.append(pathSeparator);
			buf.append("(");
			buf.append(parameter.urlPattern());
			buf.append(")");
		}

		this.pattern = Pattern.compile(buf.toString());
	}
}
