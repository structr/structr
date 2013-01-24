package org.structr.web.common;

import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 * Defines an interface for graph database content retrieval.
 *
 * @author Christian Morgner
 */

public interface GraphDataSource {

	public List<GraphObject> getData(SecurityContext securityContext, AbstractNode referenceNode) throws FrameworkException;
}
