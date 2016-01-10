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
package org.structr.core.graph.search;

import org.structr.api.search.ExactQuery;
import org.structr.api.search.Occurrence;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 *
 *
 */
public class TypeSearchAttribute<S extends GraphObject> extends PropertySearchAttribute<String> {

	public TypeSearchAttribute(Class<S> type, Occurrence occur, boolean isExactMatch) {
		this(type.getSimpleName(), occur, isExactMatch);
	}

	public TypeSearchAttribute(String type, Occurrence occur, boolean isExactMatch) {
		super(AbstractNode.type, type, occur, isExactMatch);
	}

	@Override
	public String toString() {
		return "TypeSearchAttribute(" + super.toString() + ")";
	}

	@Override
	public Class getQueryType() {
		return ExactQuery.class;
	}
}
