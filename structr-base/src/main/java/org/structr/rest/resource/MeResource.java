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
import org.structr.common.helper.CaseHelper;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoint;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.web.entity.User;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 *
 */
public class MeResource extends RESTEndpoint {

	public MeResource() {
		super(RESTParameter.forStaticString("me"));
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {
		return new MeResourceHandler(securityContext, call);
	}

	private class MeResourceHandler extends RESTCallHandler {

		public MeResourceHandler(final SecurityContext securityContext, final RESTCall call) {
			super(securityContext, call);
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
			throw new IllegalMethodException("POST not allowed on " + call.getURL());
		}

		@Override
		public boolean isCollection() {
			return false;
		}

		@Override
		public Class getEntityClass() {
			return User.class;
		}

		@Override
		public String getResourceSignature() {

			String signature = "User";

			// append requested view to resource signature
			if (!isDefaultView()) {

				signature += "/_" + CaseHelper.toUpperCamelCase(requestedView);
			}

			return signature;
		}
	}

}
