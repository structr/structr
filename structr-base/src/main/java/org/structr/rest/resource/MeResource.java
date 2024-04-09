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
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Principal;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotAllowedException;

import java.util.Arrays;
import java.util.Map;

/**
 *
 *
 */
public class MeResource extends TypedIdResource {

	private static final Logger logger = LoggerFactory.getLogger(MeResource.class.getName());

	public MeResource() {
		super(null);
	}

	public MeResource(SecurityContext securityContext) {
		super(securityContext);
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;

		if ("me".equalsIgnoreCase(part)) {

			this.typeResource = new TypeResource();
			this.typeResource.setSecurityContext(securityContext);
			this.typeResource.checkAndConfigure("user", securityContext, request);

			Principal user = securityContext.getUser(true);
			if (user != null) {

				// we need to create synthetic nested constraints
				this.idResource = new UuidResource();
				this.idResource.setSecurityContext(securityContext);
				this.idResource.checkAndConfigure(user.getProperty(GraphObject.id), securityContext, request);

			}
		}

		return true;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		Principal user = securityContext.getUser(true);
		if (user != null) {

			return new PagingIterable<>("/" + getUriPart(), Arrays.asList(user));
			//return new ResultStream(resultList, isCollectionResource(), isPrimitiveArray());

		} else {

			throw new NotAllowedException("No user");
		}
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if (typeResource != null) {
			return typeResource.doPost(propertySet);
		}

		throw new IllegalPathException(getResourceSignature() + " can only be applied to a non-empty resource");
	}

	@Override
	public String getUriPart() {
		return "me";
	}

}
