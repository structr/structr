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

import org.structr.common.error.FrameworkException;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;

import java.util.Map;

/**
 * A resource that implements the generic ability to
 * wrap another resource.
 */
public abstract class WrappingResource extends Resource {

	protected Resource wrappedResource = null;

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if (wrappedResource != null) {

			return wrappedResource.doPost(propertySet);
		}

		throw new IllegalPathException("PUT not allowed on " + getResourceSignature());
	}

	protected void wrapResource(final Resource wrappedResource) {
		this.wrappedResource = wrappedResource;
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof WrappingResource) {

			((WrappingResource) next).wrapResource(this);

			return next;
		}

		return null;
	}

	@Override
	public Class getEntityClass() {

		if (wrappedResource != null) {

			return wrappedResource.getEntityClass();
		}

		return null;
	}

	@Override
	public String getUriPart() {
		return wrappedResource.getUriPart();
	}

	@Override
	public boolean isPrimitiveArray() {

		if (wrappedResource != null) {

			return wrappedResource.isPrimitiveArray();
		}

		return false;
	}

	@Override
	public boolean isCollectionResource() throws FrameworkException {

		if (wrappedResource != null) {

			return wrappedResource.isCollectionResource();
		}

		return false;
	}
}
