package org.structr.web.common;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;

/**
 * Defines an interface for graph database content retrieval.
 *
 * @author Christian Morgner
 */

public interface GraphDataSource<T> {

	public T getData(SecurityContext securityContext, RenderContext renderContext, AbstractNode referenceNode) throws FrameworkException;
}
