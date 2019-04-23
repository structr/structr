/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.core.function;

import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;

/**
 * Abstract implementation of the basic functions of the Interface QueryFunction.
 */
public abstract class AbstractQueryFunction extends CoreFunction implements QueryFunction {

	public void applyQueryParameters(final SecurityContext securityContext, final Query query) {

		final ContextStore contextStore = securityContext.getContextStore();
		final String sortKey            = contextStore.getSortKey();
		final int start                 = contextStore.getRangeStart();
		final int end                   = contextStore.getRangeEnd();

		// paging applied by surrounding slice() function
		if (start >= 0 && end >= 0) {

			if (securityContext.getUser(false) != null && (securityContext.getUser(false).isAdmin() || securityContext.isSuperUser())) {

				query.getQueryContext().slice(start, end);

			} else {

				logger.warn("slice() can only be used by privileged users - not applying slice.");

			}
		}

		if (sortKey != null) {

			final Class type = query.getType();
			if (type != null) {

				final PropertyKey key = StructrApp.key(type, sortKey);
				if (key != null) {
					
					if (contextStore.getSortDescending()) {

						query.sortDescending(key);
						
					} else {

						query.sortAscending(key);
					}
				}

			} else {
				
				logger.warn("Cannot apply sort key, missing type in query object.");
			}
		}

	}

	protected void resetQueryParameters(final SecurityContext securityContext) {
		
		final ContextStore contextStore = securityContext.getContextStore();

		contextStore.resetQueryParameters();
	}
}
