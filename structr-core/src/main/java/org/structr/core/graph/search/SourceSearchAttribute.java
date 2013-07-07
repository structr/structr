package org.structr.core.graph.search;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class SourceSearchAttribute<T> extends SearchAttribute<T> {

	public SourceSearchAttribute(Occur occur) {
		super(occur);
	}
	
	@Override
	public Query getQuery() {
		return null;
	}

	@Override
	public boolean isExactMatch() {
		return true;
	}

	@Override
	public String getStringValue() {
		return null;
	}

	@Override
	public boolean includeInResult(GraphObject entity) {
		return true;
	}
}
