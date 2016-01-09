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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 *
 *
 */
public class TypeSearchAttribute<S extends GraphObject> extends PropertySearchAttribute<String> {

	public TypeSearchAttribute(Class<S> type, Occur occur, boolean isExactMatch) {
		this(type.getSimpleName(), occur, isExactMatch);
	}

	public TypeSearchAttribute(String type, Occur occur, boolean isExactMatch) {
		super(AbstractNode.type, type, occur, isExactMatch);
	}

	@Override
	public String toString() {
		return "TypeSearchAttribute(" + super.toString() + ")";
	}

	@Override
	public Query getQuery() {

		String value = getStringValue();
		if (isExactMatch()) {

			return new TermQuery(new Term(getKey().dbName(), value));

		} else {

			return new TermQuery(new Term(getKey().dbName(), value.toLowerCase()));
		}
	}

}
