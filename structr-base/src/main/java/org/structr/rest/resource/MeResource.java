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
import org.structr.core.entity.Principal;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;

import java.util.Arrays;
import java.util.Map;
import org.structr.api.APICall;
import org.structr.api.APICallHandler;
import org.structr.api.APIEndpoint;
import org.structr.api.parameter.APIParameter;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.web.entity.User;

/**
 *
 *
 */
public class MeResource extends APIEndpoint {

	public MeResource() {
		super(APIParameter.forStaticString("me"));
	}

	@Override
	public APICallHandler accept(final SecurityContext securityContext, final APICall call) throws FrameworkException {

		final Principal user = securityContext.getUser(true);
		if (user != null) {

			return new MeResourceHandler(securityContext, call.getURL());
		}

		return null;
	}

	private class MeResourceHandler extends APICallHandler {

		public MeResourceHandler(final SecurityContext securityContext, final String url) {
			super(securityContext, url);
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			Principal user = securityContext.getUser(true);
			if (user != null) {

				return new PagingIterable<>(getURL(), Arrays.asList(user));

			} else {

				throw new NotAllowedException("No user");
			}
		}

		@Override
		public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("POST not allowed on " + url);
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public Class getEntityClass() {
			return User.class;
		}
	}

}
