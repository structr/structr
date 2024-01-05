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
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;

import java.util.Arrays;

/**
 * Represents an exact UUID match.
 */
public class UuidResource extends FilterableResource {

	private String uuid = null;

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		GraphObject obj = getEntity();
		if (obj != null) {

			return new PagingIterable<>("/" + getUriPart(), Arrays.asList(obj));

		}

		throw new NotFoundException("Entity with ID " + getUuid() + " not found");
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		this.setUuid(part);

		return true;

	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		// do not allow nesting of "bare" uuid resource with type resource
		// as this will not do what the user expects to do.
		if (next instanceof TypeResource) {

			throw new IllegalPathException("Cannot resolve URL path, no type resource expected here");
		}

		return super.tryCombineWith(next);
	}

	public GraphObject getEntity() throws FrameworkException {

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

	public String getUuid() {

		return uuid;
	}

	@Override
	public String getUriPart() {

		return uuid;
	}

	@Override
	public String getResourceSignature() {

		return "/";
	}

	@Override
	public boolean isCollectionResource() {

		return false;
	}

	public void setUuid(String uuid) {

		this.uuid = uuid;
	}
}
