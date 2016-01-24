package org.structr.rest.transform;

import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;

/**
 *
 */
public interface StructrGraphObjectSource extends NodeInterface {

	public Iterable<GraphObject> createOutput(final TransformationContext context) throws FrameworkException;
	public GraphObject processInput(final SecurityContext securityContext, final Map<String, Object> propertyMap) throws FrameworkException;

	public Class<GraphObject> type();
}
