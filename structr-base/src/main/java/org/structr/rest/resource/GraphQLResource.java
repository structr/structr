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


import jakarta.servlet.http.HttpServletRequest;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.schema.SchemaHelper;

import java.util.Map;

/**
 *
 */
public class GraphQLResource extends Resource {

	@Override
	public Resource tryCombineWith(final Resource next) throws FrameworkException {
		return null;
	}

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;

		return ("graphQL".equals(part));
	}

	@Override
	public String getUriPart() {
		return "graphQL";
	}

	@Override
	public Class<? extends GraphObject> getEntityClass() {
		return null;
	}

	@Override
	public String getResourceSignature() {
                return SchemaHelper.normalizeEntityName(getUriPart());
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {
		return true;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
		throw new IllegalMethodException("GraphQL endpoint supports POST only.");
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {
		throw new FrameworkException(500, "Not implemented");
	}
}
