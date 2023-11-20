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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.APICall;
import org.structr.api.APICallHandler;
import org.structr.api.APIEndpoint;
import org.structr.api.parameter.APIParameter;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotAllowedException;

/**
 *
 *
 */
public class GlobalSchemaMethodsResource extends APIEndpoint {

	private static final Logger logger              = LoggerFactory.getLogger(MaintenanceResource.class.getName());
	private static final APIParameter nameParameter = APIParameter.forPattern("name", "[a-z_A-Z][a-z_A-Z0-9]*");

	public GlobalSchemaMethodsResource() {

		super(
			APIParameter.forStaticString("maintenance"),
			APIParameter.forStaticString("globalSchemaMethods"),
			nameParameter
		);
	}

	@Override
	public APICallHandler accept(final SecurityContext securityContext, final APICall call) throws FrameworkException {

		final String typeName = call.get(nameParameter);
		if (typeName != null) {

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				final SchemaMethod method = app.nodeQuery(SchemaMethod.class).andName(typeName).and(SchemaMethod.schemaNode, null).getFirst();

				tx.success();

				if (method != null) {

					return new GlobalSchemaMethodHandler(securityContext, typeName, method);
				}

			} catch (FrameworkException fex) {
				logger.error(ExceptionUtils.getStackTrace(fex));
			}
		}

		return null;
	}

	private class GlobalSchemaMethodHandler extends APICallHandler {

		private SchemaMethod method = null;

		public GlobalSchemaMethodHandler(final SecurityContext securityContext, final String url, final SchemaMethod method) {

			super(securityContext, url);

			this.method = method;
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

			final App app           = StructrApp.getInstance(securityContext);
			RestMethodResult result = null;

			if (method != null) {

				try (final Tx tx = app.tx()) {

					final String source     = method.getProperty(SchemaMethod.source);
					final String methodName = method.getName();

					result = StaticMethodResource.invoke(securityContext, null, source, propertySet, methodName, method.getUuid());

					tx.success();
				}
			}

			if (result == null) {
				throw new IllegalPathException("Type and method name do not match the given path.");
			}

			return result;
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
