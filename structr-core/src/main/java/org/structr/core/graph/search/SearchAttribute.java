/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.graph.search;

import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

/**
 * Wrapper representing a part of a search query. All parts of a search query must have a search operator and a payload. The payload can be either a node attribute oder a group of serach attributes.
 *
 * @author Axel Morgner
 */
public abstract class SearchAttribute<T> {

	public static final String WILDCARD = "*";

	private List<GraphObject> result = new LinkedList<>();
	private PropertyKey<T> key       = null;
	private Occur occur              = null;
	private T value                  = null;

	public abstract Query getQuery();
	public abstract boolean isExactMatch();
	public abstract boolean includeInResult(GraphObject entity);
	public abstract String getStringValue();
	public abstract String getInexactValue();

	public SearchAttribute() {
		this(null, null);
	}
	
	public SearchAttribute(Occur occur) {
		this(occur, null, null);
	}
	
	public SearchAttribute(PropertyKey<T> key, T value) {
		this(null, key, value);
	}
	
	public SearchAttribute(Occur occur, PropertyKey<T> key, T value) {
		this.occur = occur;
		this.key   = key;
		this.value = value;
	}
	
	public Occur getOccur() {
		return occur;
	}

	public void setResult(final List<GraphObject> result) {
		this.result = result;
	}

	public List<GraphObject> getResult() {
		return result;
	}

	public void addToResult(final GraphObject graphObject) {
		result.add(graphObject);
	}

	public void addToResult(final List<GraphObject> list) {
		result.addAll(list);
	}

	public PropertyKey<T> getKey() {
		return key;
	}

	public T getValue() {
		return value;
	}
}
