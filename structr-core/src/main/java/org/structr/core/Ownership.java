package org.structr.core;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;

/**
 *
 * @author Christian Morgner
 */
public interface Ownership {
	
	public Principal getSourceNode();
	public AbstractNode getTargetNode();
}
