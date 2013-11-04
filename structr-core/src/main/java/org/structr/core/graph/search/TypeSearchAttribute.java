package org.structr.core.graph.search;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class TypeSearchAttribute<S extends GraphObject> extends PropertySearchAttribute<String> {

	public TypeSearchAttribute(Class<S> type, Occur occur, boolean isExactMatch) {
		super(AbstractNode.type, type.getSimpleName(), occur, isExactMatch);
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
