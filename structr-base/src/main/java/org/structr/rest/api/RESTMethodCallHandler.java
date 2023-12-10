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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;

/**
 */
public abstract class RESTMethodCallHandler extends RESTCallHandler {

	protected AbstractMethod method = null;

	public RESTMethodCallHandler(final SecurityContext securityContext, final RESTCall call, final AbstractMethod method) {

		super(securityContext, call);

		this.method = method;
	}

	protected Map<String, Object> convertArguments(final Map<String, Object> restInput) throws FrameworkException {

		final Map<String, Object> convertedArguments = new LinkedHashMap<>();
		final Map<String, String> declaredParameters = method.getParameters();

		for (final String name : restInput.keySet()) {

			final String type  = declaredParameters.get(name);
			final Object input = restInput.get(name);

			convertedArguments.put(name, convert(input, type));
		}

		return convertedArguments;
	}

	private Object convert(final Object input, final String type) {

		// TODO: implement conversion...
		System.out.println("RESTMethodCallHandler: NOT converting " + input + " to " + type + ", implementation missing.");

		return input;
	}
}
