package org.structr.api.graph;

/**
 *
 */
public interface GraphProperties {

	void setProperty(final String name, final Object value);
	Object getProperty(final String name);
}
