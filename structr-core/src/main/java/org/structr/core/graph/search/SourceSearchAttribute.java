package org.structr.core.graph.search;

import java.util.List;
import org.apache.lucene.search.BooleanClause.Occur;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;
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
