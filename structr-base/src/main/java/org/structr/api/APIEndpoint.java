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
package org.structr.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.api.parameter.APIParameter;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 */
public abstract class APIEndpoint {

	private final Map<String, APIParameter> parts = new LinkedHashMap<>();
	private final String pathSeparator            = "/";
	private Pattern pattern                       = null;

	public APIEndpoint(final APIParameter... parameters) {
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
	public abstract APICallHandler accept(final SecurityContext securityContext, final APICall call) throws FrameworkException;

	public Matcher matcher(final String path) {
		return pattern.matcher(path);
	}

	public APICall initializeAPICall(final Matcher matcher, final String viewName) {

		final APICall call = new APICall(this, matcher.group(), viewName);
		int group          = 1;

		for (final APIParameter part : parts.values()) {

			final String value = matcher.group(group++);

			call.put(part, value);
		}

		return call;
	}

	// ----- private methods -----
	private void initialize(final APIParameter... parameters) {

		final StringBuilder buf = new StringBuilder();

		for (final APIParameter parameter : parameters) {

			parts.put(parameter.key(), parameter);

			buf.append(pathSeparator);
			buf.append("(");
			buf.append(parameter.urlPattern());
			buf.append(")");
		}

		this.pattern = Pattern.compile(buf.toString());
	}
}
