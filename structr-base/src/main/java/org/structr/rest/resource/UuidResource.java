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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.rest.exception.NotFoundException;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.ExactMatchEndpoint;
import org.structr.rest.api.parameter.RESTParameter;

/**
 * Represents an exact UUID match.
 */
public class UuidResource extends ExactMatchEndpoint {

	public UuidResource() {

		super(RESTParameter.forPattern("uuid", Settings.getValidUUIDRegexStringForURLParts()));
	}

	@Override
	public RESTCallHandler accept(final RESTCall call) throws FrameworkException {

		final String uuid = call.get("uuid");
		if (uuid != null) {

			return new UuidResourceHandler(call, uuid);
		}

		// only return a handler if there is actually a type with the requested name
		return null;
	}

	private class UuidResourceHandler extends RESTCallHandler {

		private String uuid = null;

		public UuidResourceHandler(final RESTCall call, final String uuid) {

			super(call);

			this.uuid = uuid;
		}

		@Override
		public ResultStream doGet(final SecurityContext securityContext, final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			GraphObject obj = getEntity(securityContext);
			if (obj != null) {

				return new PagingIterable<>("/" + getURL(), Arrays.asList(obj));

			}

			throw new NotFoundException("Entity with ID " + uuid + " not found");
		}

		@Override
		public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {
			return genericPut(securityContext, propertySet);
		}

		@Override
		public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {
			return genericDelete(securityContext);
		}

		public GraphObject getEntity(final SecurityContext securityContext) throws FrameworkException {

			final App app = StructrApp.getInstance(securityContext);

			GraphObject entity = app.nodeQuery().uuid(uuid).getFirst();
			if (entity == null) {

				entity = app.relationshipQuery().uuid(uuid).getFirst();
			}

			if (entity == null) {
				throw new NotFoundException("Entity with ID " + uuid + " not found");
			}

			return entity;
		}

		@Override
		public String getURL() {
			return uuid;
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public Class getEntityClass(final SecurityContext securityContext) {
			return null;
		}

		@Override
		public Set<String> getAllowedHttpMethodsForOptionsCall() {
			return Set.of("DELETE", "GET", "OPTIONS", "PUT");
		}
	}
}
