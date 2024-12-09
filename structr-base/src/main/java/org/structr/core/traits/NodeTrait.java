package org.structr.core.traits;

import org.structr.core.graph.NodeInterface;

public interface NodeTrait {

	NodeInterface getWrappedNode();

	String getUuid();
	String getType();
	String getName();

	boolean visibleToPublicUsers();
	boolean visibleToAuthenticatedUsers();
}
