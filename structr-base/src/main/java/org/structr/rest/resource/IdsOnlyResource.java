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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Value;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;

import java.util.Map;


/**
 *
 *
 */
public class IdsOnlyResource extends ViewFilterResource {

	private static final Logger logger = LoggerFactory.getLogger(IdsOnlyResource.class.getName());

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		this.securityContext = securityContext;
		return true;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		if (wrappedResource != null) {
			return wrappedResource.doGet(sortOrder, pageSize, page);
		}

		throw new IllegalPathException("GET not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if (wrappedResource != null) {
			return wrappedResource.doPost(propertySet);
		}

		throw new IllegalPathException("POST not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		if (wrappedResource != null) {
			return wrappedResource.doPut(propertySet);
		}

		throw new IllegalPathException("PUT not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {

		if (wrappedResource != null) {
			return wrappedResource.doDelete();
		}

		throw new IllegalPathException("DELETE not allowed on " + getResourceSignature());
	}

	@Override
	public String getUriPart() {
		return "ids";
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

	@Override
	public boolean isPrimitiveArray() {
		return true;
	}

	@Override
	public void configurePropertyView(Value<String> propertyView) {

		try {
			propertyView.set(securityContext, "ids");

		} catch(FrameworkException fex) {

			logger.warn("Unable to configure property view", fex);
		}
	}

}
