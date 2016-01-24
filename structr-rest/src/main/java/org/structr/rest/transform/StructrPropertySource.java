package org.structr.rest.transform;

import java.util.Map;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;

/**
 *
 */
public interface StructrPropertySource extends NodeInterface {

	public static final Property<Integer> position = new IntProperty("position").indexed();

	public static final View defaultView = new View(StructrPropertySource.class, PropertyView.Public,
		position
	);

	public static final View uiView = new View(StructrPropertySource.class, PropertyView.Ui,
		position
	);

	Iterable<NamedValue> createOutput(final TransformationContext context) throws FrameworkException;
	GraphObject processInput(final SecurityContext securityContext, final Map<String, Object> propertyMap, final boolean commit) throws FrameworkException;

	String getSourceName();
	String getTargetName();
}
