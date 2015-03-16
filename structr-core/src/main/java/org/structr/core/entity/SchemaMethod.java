package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.relationship.SchemaNodeMethod;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionEntry;

/**
 *
 * @author Christian Morgner
 */
public class SchemaMethod extends SchemaReloadingNode {

	public static final Property<AbstractSchemaNode> schemaNode  = new StartNode<>("schemaNode", SchemaNodeMethod.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));
	public static final Property<String>             source      = new StringProperty("source");

	public static final View defaultView = new View(SchemaMethod.class, PropertyView.Public,
		name, schemaNode, source
	);

	public static final View uiView = new View(SchemaMethod.class, PropertyView.Ui,
		name, schemaNode, source
	);

	public ActionEntry getActionEntry() {
		return new ActionEntry("___" + getProperty(AbstractNode.name), getProperty(SchemaMethod.source));
	}
}
