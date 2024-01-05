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


import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoint;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTMethodCallHandler;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.api.parameter.RESTParameter;
import org.structr.schema.action.EvaluationHints;

/**
 *
 *
 */
public class GlobalSchemaMethodsResource extends RESTEndpoint {

	public GlobalSchemaMethodsResource() {

		super(
			RESTParameter.forPattern("name", "[a-z][a-z_A-Z0-9]*")
		);
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {

		final String methodName = call.get("name");
		if (methodName != null) {

			final AbstractMethod method = Methods.resolveMethod(null, methodName);
			if (method != null) {

				return new GlobalSchemaMethodResourceHandler(securityContext, call, method);
			}
		}

		return null;
	}

	private class GlobalSchemaMethodResourceHandler extends RESTMethodCallHandler {

		public GlobalSchemaMethodResourceHandler(final SecurityContext securityContext, final RESTCall call, final AbstractMethod method) {
			super(securityContext, call, method);
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			throw new NotAllowedException("GET not allowed, use POST to execute schema methods.");
		}

		@Override
		public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
			throw new NotAllowedException("PUT not allowed, use POST to execute schema methods.");
		}

		@Override
		public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

			final App app = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx()) {

				final Arguments arguments     = Arguments.fromMap(propertySet);
				final RestMethodResult result = wrapInResult(method.execute(securityContext, null, arguments, new EvaluationHints()));

				tx.success();

				return result;

			} catch (UnlicensedScriptException ex) {
				return new RestMethodResult(500, "Call to unlicensed function, see server log file for more details.");
			}
		}

		@Override
		public Class getEntityClass() {
			return null;
		}

		@Override
		public boolean isCollection() {
			return false;
		}
	}
}
