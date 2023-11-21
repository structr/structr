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
import org.structr.core.GraphObjectMap;
import org.structr.core.function.StructrEnvFunction;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoint;
import org.structr.rest.api.parameter.RESTParameter;

/**
 *
 *
 */
public class EnvResource extends RESTEndpoint {

	public enum UriPart {
		_env
	}

	public EnvResource() {
		super(RESTParameter.forStaticString(UriPart._env.name()));
	}

	@Override
	public RESTCallHandler accept(final SecurityContext securityContext, final RESTCall call) throws FrameworkException {
		return new EnvResourceHandler(securityContext, call.getURL());
	}

	private class EnvResourceHandler extends RESTCallHandler {

		public EnvResourceHandler(final SecurityContext securityContext, final String url) {
			super(securityContext, url);
		}

		@Override
		public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

			final List<GraphObjectMap> resultList = new LinkedList<>();

			resultList.add(StructrEnvFunction.getStructrEnv());

			return new PagingIterable(getURL(), resultList);
		}

		@Override
		public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
			throw new IllegalMethodException("POST not allowed on " + getURL());
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
