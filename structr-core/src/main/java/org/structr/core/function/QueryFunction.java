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
package org.structr.core.function;

import org.structr.common.SecurityContext;
import org.structr.core.app.Query;

/**
 * Interface to identify functions that execute database queries. This interface
 * allows built-in function evaluation to identify database query functions and
 * set query parameters like paging etc. BEFORE execution of the actual query.
 */
public interface QueryFunction {

	void setRangeStart(final int start);
	void setRangeEnd(final int end);

	void applyRange(final SecurityContext securityContext, final Query query);
	void resetRange();
}
