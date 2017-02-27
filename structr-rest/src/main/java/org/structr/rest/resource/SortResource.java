/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.GraphObjectComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSortKey;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.property.PropertyKey;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.servlet.JsonRestServlet;

/**
 *
 *
 */
public class SortResource extends WrappingResource {

	private String sortOrder = null;
	private String sortKey = null;

	public SortResource(SecurityContext securityContext, String sortKey, String sortOrder) {
		this.securityContext = securityContext;
		this.sortKey = sortKey;
		this.sortOrder = sortOrder;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.sortKey = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_KEY);
		this.sortOrder = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_ORDER);

		return sortKey != null;
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		if(wrappedResource != null) {

			Result result = wrappedResource.doGet(sortKey, sortDescending, pageSize, page, offsetId);

			try {
				Collections.sort(result.getResults(), new GraphObjectComparator(sortKey, sortOrder));

			} catch(Throwable t) {

				throw new FrameworkException(422, "Unable to sort results by " + sortKey.jsonName() + ": " + t.toString(), new InvalidSortKey(GraphObject.class.getSimpleName(), sortKey));
			}

			return result;
		}

		throw new IllegalPathException("Illegal path, sort resource needs a wrapped resource");
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return super.tryCombineWith(next);
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public String getSortKey() {
		return sortKey;
	}

	@Override
	public void postProcessResultSet(Result result) {
		result.setSortOrder(sortOrder);
		result.setSortKey(sortKey);
	}

        @Override
        public String getResourceSignature() {
                return wrappedResource.getResourceSignature();
        }
}
