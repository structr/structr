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

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.structr.api.search.SortOrder;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod.HttpVerb;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTMethodCallHandler;
import org.structr.rest.exception.IllegalMethodException;

/**
 *
 */
public class InstanceMethodResourceHandler extends RESTMethodCallHandler {

	private Class entityClass = null;
	private String typeName   = null;
	private String uuid       = null;

	public InstanceMethodResourceHandler(final RESTCall call, final Class entityClass, final String typeName, final String uuid, final AbstractMethod method) {

		super(call, method);

		this.entityClass = entityClass;
		this.typeName    = typeName;
		this.uuid        = uuid;
	}

	@Override
	public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		if (HttpVerb.GET.equals(method.getHttpVerb())) {

			final GraphObject entity      = getEntity(securityContext, entityClass, typeName, uuid);
			final RestMethodResult result = executeMethod(securityContext, entity, Arguments.fromPath(call.getPathParameters()));

			return new PagingIterable("GET " + getURL(), result.getContent());

		} else {

			throw new IllegalMethodException("GET not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
		}
	}

	@Override
	public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

		if (HttpVerb.POST.equals(method.getHttpVerb())) {

			final GraphObject entity = getEntity(securityContext, entityClass, typeName, uuid);

			return executeMethod(securityContext, entity, Arguments.fromMap(propertySet));

		} else {

			throw new IllegalMethodException("POST not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
		}
	}

	@Override
	public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

		if (HttpVerb.PUT.equals(method.getHttpVerb())) {

			final GraphObject entity = getEntity(securityContext, entityClass, typeName, uuid);

			return executeMethod(securityContext, entity, Arguments.fromMap(propertySet));

		} else {

			throw new IllegalMethodException("PUT not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
		}
	}

	@Override
	public RestMethodResult doPatch(final SecurityContext securityContext, final List<Map<String, Object>> propertySet) throws FrameworkException {

		if (HttpVerb.PATCH.equals(method.getHttpVerb())) {

			final GraphObject entity = getEntity(securityContext, entityClass, typeName, uuid);

			// FIXME, only the first property set is used, we need to test this
			return executeMethod(securityContext, entity, Arguments.fromMap(propertySet.get(0)));

		} else {

			throw new IllegalMethodException("PATCH not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
		}
	}

	@Override
	public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			if (!HttpVerb.DELETE.equals(method.getHttpVerb())) {

				throw new IllegalMethodException("DELETE not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());

			} else {

				final GraphObject entity      = getEntity(securityContext, entityClass, typeName, uuid);
				final RestMethodResult result = executeMethod(securityContext, entity, Arguments.fromPath(call.getPathParameters()));

				tx.success();

				return result;
			}
		}
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public String getTypeName(final SecurityContext securityContext) throws FrameworkException {

		final GraphObject entity = getEntity(securityContext, entityClass, typeName, uuid);
		if (entity != null) {

			return entity.getClass();
		}

		return null;
	}

	@Override
	public Set<String> getAllowedHttpMethodsForOptionsCall() {
		return Set.of(method.getHttpVerb().name());
	}
}