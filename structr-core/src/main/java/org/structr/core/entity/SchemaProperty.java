package org.structr.core.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.relationship.SchemaNodeProperty;
import org.structr.core.entity.relationship.SchemaViewProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.parser.PropertyDefinition;

/**
 *
 * @author Christian Morgner
 */
public class SchemaProperty extends AbstractNode implements PropertyDefinition {

	public static final Property<SchemaNode>       schemaNode   = new StartNode<>("schemaNode", SchemaNodeProperty.class);
	public static final Property<List<SchemaView>> schemaViews  = new StartNodes<>("schemaViews", SchemaViewProperty.class);

	public static final Property<String>           defaultValue = new StringProperty("defaultValue");
	public static final Property<String>           propertyType = new StringProperty("propertyType");
	public static final Property<String>           contentType  = new StringProperty("contentType");
	public static final Property<String>           dbName       = new StringProperty("dbName");
	public static final Property<String>           format       = new StringProperty("format");
	public static final Property<Boolean>          notNull      = new BooleanProperty("notNull");
	public static final Property<Boolean>          unique       = new BooleanProperty("unique");

	public static final View defaultView = new View(SchemaProperty.class, PropertyView.Public,
		name, schemaNode, schemaViews, propertyType, contentType, format, notNull, unique
	);

	public static final View uiView = new View(SchemaProperty.class, PropertyView.Ui,
		name, schemaNode, schemaViews, propertyType, contentType, format, notNull, unique
	);

	@Override
	public String getPropertyName() {
		return getProperty(name);
	}

	public Type getPropertyType() {
		return Type.valueOf(getProperty(propertyType));
	}

	@Override
	public String getContentType() {
		return getProperty(contentType);
	}

	@Override
	public String getFormat() {
		return getProperty(format);
	}

	@Override
	public boolean isNotNull() {

		final Boolean isNotNull = getProperty(notNull);
		if (isNotNull != null && isNotNull) {

			return true;
		}

		return false;
	}

	@Override
	public boolean isUnique() {

		final Boolean isUnique = getProperty(unique);
		if (isUnique != null && isUnique) {

			return true;
		}

		return false;
	}

	@Override
	public String getRawSource() {
		return "";
	}

	@Override
	public String getSource() {
		return "";
	}

	@Override
	public String getDbName() {
		return getProperty(dbName);
	}

	@Override
	public String getDefaultValue() {
		return getProperty(defaultValue);
	}
}
