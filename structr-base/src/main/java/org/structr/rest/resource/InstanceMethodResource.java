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
package org.structr.rest.resource;

import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;

import java.util.Map;
import org.structr.core.api.MethodCall;
import org.structr.core.api.MethodSignature;
import org.structr.core.api.Methods;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.EvaluationHints;

/**
 *
 */
public class InstanceMethodResource extends AbstractTypeIdNameResource {

	@Override
	public RESTCallHandler handleTypeIdName(final SecurityContext securityContext, final RESTCall call, final String typeName, final String uuid, final String name) throws FrameworkException {

		final Class entityClass = SchemaHelper.getEntityClassForRawType(typeName);
		if (entityClass != null) {

			final GraphObject entity        = getEntity(securityContext, entityClass, entityClass.getSimpleName(), uuid);
			final MethodSignature signature = Methods.getMethodSignatureOrNull(entityClass, entity, name);

			if (signature != null) {

				return new InstanceMethodResourceHandler(securityContext, call.getURL(), signature.createCall(call));
			}
		}

		return null;
	}

	private class InstanceMethodResourceHandler extends RESTCallHandler {

		private MethodCall call = null;

		public InstanceMethodResourceHandler(final SecurityContext securityContext, final String url, final MethodCall call) {

			super(securityContext, url);

			this.call = call;
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
			throw new IllegalMethodException("GET not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

			final App app = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx()) {

				final RestMethodResult result = wrapInResult(call.execute(securityContext, new EvaluationHints()));

				tx.success();

				return result;

			} catch (UnlicensedScriptException ex) {
				return new RestMethodResult(500, "Call to unlicensed function, see server log file for more details.");
			}
		}

		@Override
		public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("PUT not allowed on " + getURL());
		}

		@Override
		public RestMethodResult doDelete() throws FrameworkException {
			throw new IllegalMethodException("DELETE not allowed on " + getURL());
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public Class getEntityClass() {
			return null;
		}
	}
}
