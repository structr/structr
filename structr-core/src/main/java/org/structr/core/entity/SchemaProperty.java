package org.structr.core.entity;

import java.util.List;
import java.util.Set;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.relationship.SchemaNodeProperty;
import org.structr.core.entity.relationship.SchemaViewProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.schema.Schema;
import org.structr.schema.parser.Validator;

/**
 *
 * @author Christian Morgner
 */
public class SchemaProperty extends AbstractNode {

	public static final Property<SchemaNode>       schemaNode   = new StartNode<>("schemaNode", SchemaNodeProperty.class);
	public static final Property<List<SchemaView>> schemaViews  = new StartNodes<>("schemaViews", SchemaViewProperty.class);

	public static final Property<String>           propertyType = new StringProperty("propertyType");
	public static final Property<String>           contentType  = new StringProperty("contentType");
	public static final Property<String>           format       = new StringProperty("format");
	public static final Property<Boolean>          notNull      = new BooleanProperty("notNull");
	public static final Property<Boolean>          unique       = new BooleanProperty("unique");

	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		name, schemaNode, schemaViews, propertyType, contentType, format, notNull, unique
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		name, schemaNode, schemaViews, propertyType, contentType, format, notNull, unique
	);

	public String getPropertySource(final Schema entity, final Set<String> enums, final Set<Validator> validators) {

		final StringBuilder buf = new StringBuilder();



//		PropertyParser parser = SchemaHelper.getParserForRawValue(errorBuffer, entity.getClassName(), propertyName, rawType);
//		if (parser != null) {
//
//			// append created source from parser
//			src.append(parser.getPropertySource(entity, errorBuffer));
//
//			// register global elements created by parser
//			validators.addAll(parser.getGlobalValidators());
//			enums.addAll(parser.getEnumDefinitions());
//
//			// register property in default view
//			addPropertyToView(PropertyView.Ui, propertyName, viewProperties);
//		}
//




		return buf.toString();
	}
}
