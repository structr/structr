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
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.PropertyKey;

import java.util.LinkedList;
import java.util.List;

/**
 */
public abstract class AbstractPredicate implements SearchFunctionPredicate {

	protected List<SearchFunctionPredicate> predicates = new LinkedList<>();
	protected List<SearchParameter> parameters         = new LinkedList<>();

	public void addPredicate(final SearchFunctionPredicate predicate) {

		predicates.add(predicate);
	}

	public void addParameter(final SearchParameter param) {

		parameters.add(param);
	}

	/**
	 * A predicate gets its value as it came from the script, but an array property can only search for an array
	 * of its component type (ticket 1417).
	 */
	protected Object searchValue(final SecurityContext securityContext, final PropertyKey key, final Object value) throws FrameworkException {

		if (key instanceof ArrayProperty arrayProperty) {

			return arrayProperty.convertScriptSearchValue(securityContext, value);
		}

		return value;
	}
}
