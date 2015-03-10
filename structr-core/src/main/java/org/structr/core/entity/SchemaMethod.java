package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.relationship.SchemaNodeMethod;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionEntry;

/**
 *
 * @author Christian Morgner
 */
public class SchemaMethod extends AbstractNode {

	public static final Property<SchemaNode> schemaNode  = new StartNode<>("schemaNode", SchemaNodeMethod.class);
	public static final Property<String> source          = new StringProperty("source");

	public static final View defaultView = new View(SchemaMethod.class, PropertyView.Public,
		name, schemaNode
	);

	public static final View uiView = new View(SchemaMethod.class, PropertyView.Ui,
		name, schemaNode
	);

	public ActionEntry getActionEntry() {
		return new ActionEntry("___" + getProperty(AbstractNode.name), getProperty(SchemaMethod.source));
	}
}
