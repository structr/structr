/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.rest.resource;

import org.structr.core.Result;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.GraphObjectComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSortKey;
import org.structr.core.GraphObject;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.servlet.JsonRestServlet;

/**
 *
 * @author Christian Morgner
 */
public class SortResource extends WrappingResource {

	private static final Logger logger = Logger.getLogger(SortResource.class.getName());

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
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		if(wrappedResource != null) {
			
			Result result = wrappedResource.doGet(sortKey, sortDescending, pageSize, page, offsetId);

			try {
				Collections.sort(result.getResults(), new GraphObjectComparator(sortKey, sortOrder));
				
			} catch(Throwable t) {
				
				throw new FrameworkException("base", new InvalidSortKey(sortKey));
			}

			return result;
		}

		throw new IllegalPathException();
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
