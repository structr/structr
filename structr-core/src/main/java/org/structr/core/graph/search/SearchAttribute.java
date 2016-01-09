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

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.neo4j.helpers.Predicate;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;

/**
 * Wrapper representing a part of a search query. All parts of a search query must have a search operator and a payload. The payload can be either a node attribute oder a group of serach attributes.
 *
 *
 * @param <T>
 */
public abstract class SearchAttribute<T> extends NodeAttribute<T> implements Predicate<GraphObject> {

	public static final String WILDCARD = "*";

	private Set<GraphObject> result = new LinkedHashSet<>();
	private Occur occur             = null;

	public abstract Query getQuery();
	public abstract boolean isExactMatch();
	public abstract boolean includeInResult(GraphObject entity);
	public abstract String getStringValue();
	public abstract String getInexactValue();
	public abstract String getValueForEmptyField();

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
		
		super(key, value);
		this.occur = occur;
	}
	
	public Occur getOccur() {
		return occur;
	}

	public void setResult(final Set<GraphObject> result) {
		this.result = result;
	}

	public Set<GraphObject> getResult() {
		return result;
	}

	public void addToResult(final GraphObject graphObject) {
		result.add(graphObject);
	}

	public void addToResult(final Set<GraphObject> list) {
		result.addAll(list);
	}

	public void setExactMatch(final boolean exact) {};
	
	// ----- interface Predicate<Node> -----
	@Override
	public boolean accept(final GraphObject obj) {
		return includeInResult(obj);
	}
}
