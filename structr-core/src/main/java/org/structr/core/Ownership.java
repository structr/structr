package org.structr.core;

import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;

/**
 *
 * @author Christian Morgner
 */
public interface Ownership {
	
	public Principal getSourceNode();
	public NodeInterface getTargetNode();
}
