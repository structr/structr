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
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.schema.SchemaHelper;

import java.util.Map;

/**
 *
 *
 */
public class GlobalSchemaMethodsResource extends Resource {

	private String uriPart = null;

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) {

		this.securityContext = securityContext;
		this.uriPart         = part;

		return ("globalSchemaMethods".equals(part));
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
		throw new NotAllowedException("GET not allowed, use POST to run maintenance commands");
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("PUT not allowed, use POST to run maintenance commands");
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalPathException("Cannot resolve URL path");
	}

	@Override
	public Resource tryCombineWith(final Resource next) throws FrameworkException {

		// only accept construction of paths with a valid schema method
		if (next instanceof GlobalSchemaMethodResource) {
			return next;
		}

		return null;
	}

	@Override
	public Class getEntityClass() {
		return null;
	}

	@Override
	public String getUriPart() {
		return uriPart;
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}

        @Override
        public String getResourceSignature() {
                return SchemaHelper.normalizeEntityName(getUriPart());
        }
}
