package org.structr.api.graph;

import org.structr.api.NotInTransactionException;

/**
 *
 */
public interface PropertyContainer {

	long getId();

	boolean hasProperty(final String name);
	Object getProperty(final String name);
	Object getProperty(final String name, final Object defaultValue);
	void setProperty(final String name, final Object value);
	void removeProperty(final String name);

	Iterable<String> getPropertyKeys();

	void delete() throws NotInTransactionException;

	boolean isSpatialEntity();
}
