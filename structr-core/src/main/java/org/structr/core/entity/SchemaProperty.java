package org.structr.core.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.relationship.SchemaNodeProperty;
import org.structr.core.entity.relationship.SchemaViewProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;

/**
 *
 * @author Christian Morgner
 */
public class SchemaProperty extends AbstractNode {

	public static final Property<SchemaNode>       schemaNode  = new StartNode<>("schemaNode", SchemaNodeProperty.class);
	public static final Property<List<SchemaView>> schemaViews = new StartNodes<>("schemaViews", SchemaViewProperty.class);
	


	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		name, schemaNode, schemaViews
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		name, schemaNode, schemaViews
	);
}
