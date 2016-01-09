/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.rest.filter;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

/**
 *
 *
 */
public class OrFilter extends Filter {

	private List<Filter> filters = new LinkedList<Filter>();

	public OrFilter(Filter... filterList) {
		
		for(Filter filter : filterList) {
			filters.add(filter);
		}
	}

	@Override
	public boolean includeInResultSet(SecurityContext securityContext, GraphObject object) {
		
		for (Filter filter : filters) {
			
			// return on first positive result
			if (filter.includeInResultSet(securityContext, object)) {
				return true;
			}
		}

		return false;
	}
}
