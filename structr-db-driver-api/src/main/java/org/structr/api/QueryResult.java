package org.structr.api;

/**
 *
 */
public interface QueryResult<T> extends Iterable<T>, AutoCloseable {

	int size();

	@Override
	void close();
}
