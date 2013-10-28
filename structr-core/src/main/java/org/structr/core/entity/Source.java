package org.structr.core.entity;

import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public interface Source<R, S> {
	
	public S get(final SecurityContext securityContext, final NodeInterface node);
	
	public void set(final SecurityContext securityContext, final NodeInterface node, final S value) throws FrameworkException;

	public R getRaw(final Node dbNode);
}
