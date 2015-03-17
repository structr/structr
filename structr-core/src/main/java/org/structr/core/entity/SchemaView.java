package org.structr.core.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.relationship.SchemaNodeView;
import org.structr.core.entity.relationship.SchemaViewProperty;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class SchemaView extends SchemaReloadingNode {

	public static final Property<AbstractSchemaNode>   schemaNode         = new StartNode<>("schemaNode", SchemaNodeView.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));
	public static final Property<List<SchemaProperty>> schemaProperties   = new EndNodes<>("schemaProperties", SchemaViewProperty.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name, SchemaProperty.isBuiltinProperty));
	public static final Property<Boolean>              isBuiltinView      = new BooleanProperty("isBuiltinView");
	public static final Property<String>               nonGraphProperties = new StringProperty("nonGraphProperties");

	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		name, schemaNode, schemaProperties, nonGraphProperties
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		name, schemaNode, schemaProperties, nonGraphProperties
	);

	public static final View schemaView = new View(SchemaView.class, "schema",
		name, schemaNode, schemaProperties, nonGraphProperties, isBuiltinView
	);
}
