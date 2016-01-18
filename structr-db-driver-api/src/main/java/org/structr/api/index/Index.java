package org.structr.api.index;

import org.structr.api.QueryResult;
import org.structr.api.search.QueryPredicate;

/**
 *
 */
public interface Index<T> {

	void add(final T t, final String key, final Object value, final Class typeHint);

	void remove(final T t);
	void remove(final T t, final String key);

	QueryResult<T> query(final QueryPredicate predicate);
	QueryResult<T> query(final String key, final Object value, final Class typeHint);
	QueryResult<T> get(final String key, final Object value, final Class typeHint);
}
