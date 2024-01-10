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
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NativeQueryCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.exception.NotFoundException;

import java.util.Collections;
import java.util.Map;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 */
public class CypherQueryResource extends ExactMatchEndpoint {

	public CypherQueryResource() {
		super(RESTParameter.forStaticString("cypher"));
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {
		return new CypherResourceHandler(securityContext, call);
	}

	private class CypherResourceHandler extends RESTCallHandler {

		public CypherResourceHandler(final SecurityContext securityContext, final RESTCall call) {
			super(securityContext, call);
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			// Admins only
			if (!securityContext.isSuperUser()) {

				throw new NotAllowedException("Use of the cypher endpoint is restricted to admin users");

			}

			try {

				Object queryObject = securityContext.getRequest().getParameter("query");
				if (queryObject != null) {

					String query                     = queryObject.toString();
					Iterable<GraphObject> resultList = StructrApp.getInstance(securityContext).command(NativeQueryCommand.class).execute(query, Collections.EMPTY_MAP);

					return new PagingIterable<>(getURL(), resultList);
				}

			} catch (org.structr.api.NotFoundException nfe) {

				throw new NotFoundException("Entity not found for the given query");
			}

			return PagingIterable.EMPTY_ITERABLE;
		}

		@Override
		public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

			// Admins only
			if (!securityContext.isSuperUser()) {

				throw new NotAllowedException("Use of the cypher endpoint is restricted to admin users");
			}

			try {

				RestMethodResult result = new RestMethodResult(200);
				Object queryObject      = propertySet.get("query");

				if (queryObject != null) {

					String query                     = queryObject.toString();
					Iterable<GraphObject> resultList = StructrApp.getInstance(securityContext).command(NativeQueryCommand.class).execute(query, propertySet);

					for (Object obj : resultList) {

						result.addContent(obj);
					}

				}

				return result;

			} catch (org.structr.api.NotFoundException nfe) {

				throw new NotFoundException("Entity not found for the given query");
			}
		}

		@Override
		public Class getEntityClass() {
			return null;
		}

		@Override
		public boolean isCollection() {
			return true;
		}
	}
}
