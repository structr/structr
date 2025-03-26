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
package org.structr.rest.resource;

import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.entity.SchemaNode;
import org.structr.core.traits.Traits;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTMethodCallHandler;
import org.structr.rest.api.WildcardMatchEndpoint;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.rest.exception.IllegalMethodException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class StaticMethodResource extends WildcardMatchEndpoint {

	public StaticMethodResource() {

		super(
			RESTParameter.forPattern("type", SchemaNode.schemaNodeNamePattern, true),
			RESTParameter.forPattern("name", "[a-z][a-z_A-Z0-9]*", true)
		);
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		final String typeName = call.get("type");
		final String name     = call.get("name");

		if (typeName != null && name != null) {

			final Traits traits = Traits.of(typeName);
			if (traits != null) {

				final AbstractMethod method = Methods.resolveMethod(traits, name);
				if (method != null && method.isStatic() && !method.isPrivate()) {

					return new StaticMethodResourceHandler(call, method);
				}
			}
		}

		return null;
	}

	private class StaticMethodResourceHandler extends RESTMethodCallHandler {

		public StaticMethodResourceHandler(final RESTCall call, final AbstractMethod method) {
			super(call, method);
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			if ("GET".equals(method.getHttpVerb())) {

				final RestMethodResult result = executeMethod(securityContext, null, Arguments.fromPath(call.getPathParameters()));

				return new PagingIterable("GET " + getURL(), result.getContent());

			} else {

				throw new IllegalMethodException("GET not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
			}
		}

		@Override
		public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			if ("POST".equals(method.getHttpVerb())) {

				return executeMethod(securityContext, null, Arguments.fromMap(propertySet));

			} else {

				throw new IllegalMethodException("POST not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
			}
		}

		@Override
		public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

			if ("PUT".equals(method.getHttpVerb())) {

				return executeMethod(securityContext, null, Arguments.fromMap(propertySet));

			} else {

				throw new IllegalMethodException("PUT not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
			}
		}

		@Override
		public RestMethodResult doPatch(final SecurityContext securityContext, final List<Map<String, Object>> propertySet) throws FrameworkException {

			if ("PATCH".equals(method.getHttpVerb())) {

				// FIXME, only the first property set is used, we need to test this
				return executeMethod(securityContext, null, Arguments.fromMap(propertySet.get(0)));

			} else {

				throw new IllegalMethodException("PATCH not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
			}
		}

		@Override
		public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {

			if ("DELETE".equals(method.getHttpVerb())) {

				return executeMethod(securityContext, null, Arguments.fromPath(call.getPathParameters()));

			} else {

				throw new IllegalMethodException("DELETE not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
			}
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public String getTypeName(final SecurityContext securityContext) {
			return null;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of(method.getHttpVerb());
		}
	}
}
