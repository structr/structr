package org.structr.core.graph.search;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class EmptySearchAttribute<T> extends PropertySearchAttribute<T> {

	public EmptySearchAttribute(PropertyKey<T> key, T value) {
		super(key, value, BooleanClause.Occur.MUST, true);
	}
	
	@Override
	public Query getQuery() {
		return null;
	}
}
