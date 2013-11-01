package org.structr.core.entity;

import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public interface Target<R, T> {

	public T get(final SecurityContext securityContext, final NodeInterface node);
	
	public void set(final SecurityContext securityContext, final NodeInterface node, final T value) throws FrameworkException;

	public R getRawSource(final SecurityContext securityContext, final Node dbNode);
	public boolean hasElements(final SecurityContext securityContext, final Node dbNode);
}
