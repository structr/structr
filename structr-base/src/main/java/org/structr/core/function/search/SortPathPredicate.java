/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.function.search;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.graph.search.PathPropertySortOrder;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

public class SortPathPredicate extends AbstractPredicate {
	private boolean sortDescending = false;
	private String sortPath     = null;

	public SortPathPredicate(final String sortPath, final boolean sortDescending) {

		this.sortPath    = sortPath;
		this.sortDescending = sortDescending;
	}

	@Override
	public void configureQuery(final SecurityContext securityContext, final Traits type, final PropertyKey propertyKey, final QueryGroup query, final boolean exact) throws FrameworkException {
		query.sort(new PathPropertySortOrder(new ActionContext(securityContext), sortPath, sortDescending));
	}
}
