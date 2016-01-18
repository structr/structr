package org.structr.api;

/**
 *
 */
public interface Transaction extends AutoCloseable {

	void failure();
	void success();

	@Override
	void close();
}
