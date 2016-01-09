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
package org.structr.core.graph;

import org.neo4j.helpers.Predicate;
import org.structr.core.GraphObject;

/**
 *
 *
 */

public class TypePredicate<T extends GraphObject> implements Predicate<T> {

	private String desiredType = null;

	public TypePredicate(final String desiredType) {
		this.desiredType = desiredType;
	}

	@Override
	public boolean accept(final T arg) {
		return desiredType == null || desiredType.equals(arg.getType());
	}
}
