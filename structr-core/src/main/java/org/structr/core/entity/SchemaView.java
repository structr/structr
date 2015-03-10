package org.structr.core.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.relationship.SchemaNodeView;
import org.structr.core.entity.relationship.SchemaViewProperty;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;

/**
 *
 * @author Christian Morgner
 */
public class SchemaView extends AbstractNode {

	public static final Property<SchemaNode>           schemaNode       = new StartNode<>("schemaNode", SchemaNodeView.class);
	public static final Property<List<SchemaProperty>> schemaProperties = new EndNodes<>("schemaProperties", SchemaViewProperty.class);


	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		name, schemaNode, schemaProperties
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		name, schemaNode, schemaProperties
	);
}
