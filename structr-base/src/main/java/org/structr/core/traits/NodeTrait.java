package org.structr.core.traits;

import org.structr.core.graph.NodeInterface;

public interface NodeTrait {

	NodeInterface getWrappedNode();
	String getType();

	boolean visibleToPublicUsers();
	boolean visibleToAuthenticatedUsers();
}
