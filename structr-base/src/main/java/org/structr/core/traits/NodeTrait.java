package org.structr.core.traits;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.web.common.RenderContext;

public interface NodeTrait {

	NodeInterface getWrappedNode();
	SecurityContext getSecurityContext();

	String getUuid();
	String getType();
	String getName();

	void setName(final String name) throws FrameworkException;

	boolean visibleToPublicUsers();
	boolean visibleToAuthenticatedUsers();

	void setVisibleToAuthenticatedUsers(final boolean visible) throws FrameworkException;
	void setVisibleToPublicUsers(final boolean visible) throws FrameworkException;

	default String getPropertyWithVariableReplacement(final RenderContext renderContext, final String key) throws FrameworkException {

		final NodeInterface node              = getWrappedNode();
		final Traits traits                   = node.getTraits();
		final PropertyKey<String> propertyKey = traits.key(key);

		return node.getPropertyWithVariableReplacement(renderContext, propertyKey);
	}

	default Traits getTraits() {
		return getWrappedNode().getTraits();
	}
}
