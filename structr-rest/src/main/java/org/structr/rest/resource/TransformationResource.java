/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.driver.internal.util.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.PagingIterable;
import org.structr.common.PagingHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.ViewTransformation;
import org.structr.core.graph.NodeFactory;
import org.structr.core.property.PropertyKey;

/**
 *
 *
 */
public class TransformationResource extends WrappingResource {

	private static final Logger logger = LoggerFactory.getLogger(TransformationResource.class.getName());

	private ViewTransformation transformation = null;

	public TransformationResource(SecurityContext securityContext, ViewTransformation transformation) {
		this.securityContext = securityContext;
		this.transformation  = transformation;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		return false;	// no direct instantiation
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException {

		if(wrappedResource != null && transformation != null) {

			// allow view transformation to avoid evaluation of wrapped resource
			if (transformation.evaluateWrappedResource()) {

				final List list = wrappedResource.doGet(sortKey, sortDescending, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE).getAsList();

				try {

					transformation.apply(securityContext, list);

				} catch(Throwable t) {
					logger.warn("", t);
				}

				// apply paging later
				return new Result(PagingHelper.subList(Iterables.asList(list), pageSize, page), wrappedResource.isCollectionResource(), wrappedResource.isPrimitiveArray());

			} else {

				List<? extends GraphObject> list = new LinkedList<>();

				transformation.apply(securityContext, list);

				// apply paging later
				return new Result(PagingHelper.subList(Iterables.asList(list), pageSize, page), wrappedResource.isCollectionResource(), wrappedResource.isPrimitiveArray());

			}
		}

		return new Result(PagingIterable.EMPTY_ITERABLE, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public String getResourceSignature() {
		if(wrappedResource != null) {
			return wrappedResource.getResourceSignature();
		}

		return "";
	}
}
