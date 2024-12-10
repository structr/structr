package org.structr.core.traits;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

public interface NodeTrait {

	NodeInterface getWrappedNode();

	String getUuid();
	String getType();
	String getName();

	void setName(final String name) throws FrameworkException;

	boolean visibleToPublicUsers();
	boolean visibleToAuthenticatedUsers();

	void setVisibleToAuthenticatedUsers(final boolean visible) throws FrameworkException;
	void setVisibleToPublicUsers(final boolean visible) throws FrameworkException;
}
