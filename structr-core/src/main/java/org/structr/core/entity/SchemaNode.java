package org.structr.core.entity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSchemaToken;
import org.structr.core.Services;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaNotion;

/**
 *
 * @author Christian Morgner
 */
public class SchemaNode extends AbstractSchemaNode implements Schema {

	public static final Property<List<SchemaNode>>  relatedTo   = new EndNodes<>("relatedTo", SchemaRelationship.class, new SchemaNotion(SchemaNode.class));
	public static final Property<List<SchemaNode>>  relatedFrom = new StartNodes<>("relatedFrom", SchemaRelationship.class, new SchemaNotion(SchemaNode.class));
	
	public static final View defaultView = new View(SchemaNode.class, PropertyView.Public,
		name, relatedTo, relatedFrom
	);
	
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {
		
		final Set<PropertyKey> propertyKeys = new LinkedHashSet<>(Services.getInstance().getConfigurationProvider().getPropertySet(getClass(), propertyView));
		
		// add "custom" property keys as String properties
		for (final String key : SchemaHelper.getProperties(getNode())) {
			
			final PropertyKey newKey = new StringProperty(key);
			newKey.setDeclaringClass(getClass());
			
			propertyKeys.add(newKey);
		}
			
		return propertyKeys;
	}
	
	@Override
	public String getSource(final ErrorBuffer errorBuffer) throws FrameworkException {
		
		final Set<String> viewProperties = new LinkedHashSet<>();
		final Set<String> validators     = new LinkedHashSet<>();
		final Set<String> enums          = new LinkedHashSet<>();
		final StringBuilder src          = new StringBuilder();
		final Class baseType             = AbstractNode.class;
		final String _className          = getProperty(name);
		
		src.append("package org.structr.dynamic;\n\n");
		
		src.append("import ").append(baseType.getName()).append(";\n");
		src.append("import ").append(PropertyView.class.getName()).append(";\n");
		src.append("import ").append(View.class.getName()).append(";\n");
		src.append("import ").append(ValidationHelper.class.getName()).append(";\n");
		src.append("import ").append(ErrorBuffer.class.getName()).append(";\n");
		src.append("import org.structr.core.validator.*;\n");
		src.append("import org.structr.core.property.*;\n");
		src.append("import org.structr.core.notion.*;\n");
		src.append("import java.util.List;\n\n");
		
		src.append("public class ").append(_className).append(" extends ").append(baseType.getSimpleName()).append(" {\n\n");

		// extract properties from node
		src.append(SchemaHelper.extractProperties(this, validators, enums, viewProperties, errorBuffer));
		
		// output related node definitions, collect property views
		for (final SchemaRelationship outRel : getOutgoingRelationships(SchemaRelationship.class)) {
			src.append(outRel.getPropertySource(_className));
			viewProperties.add(outRel.getPropertyName(_className) + "Property");
		}
		
		// output related node definitions, collect property views
		for (final SchemaRelationship inRel : getIncomingRelationships(SchemaRelationship.class)) {
			src.append(inRel.getPropertySource(_className));
			viewProperties.add(inRel.getPropertyName(_className) + "Property");
		}
		
		// output possible enum definitions
		for (final String enumDefition : enums) {
			src.append(enumDefition);
		}

		if (!viewProperties.isEmpty()) {
			SchemaHelper.formatView(src, _className, "default", "PropertyView.Public", viewProperties);
			SchemaHelper.formatView(src, _className, "ui", "PropertyView.Ui", viewProperties);
		}
		
		if (!validators.isEmpty()) {
			
			src.append("\n\t@Override\n");
			src.append("\tpublic boolean isValid(final ErrorBuffer errorBuffer) {\n\n");
			src.append("\t\tboolean error = false;\n\n");
			
			for (final String validator : validators) {
				src.append("\t\terror |= ").append(validator).append(";\n");
			}
			
			src.append("\n\t\treturn !error;\n");
			src.append("\t}\n");
		}
		
		src.append("}\n");
		
		return src.toString();
	}
	
	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {
		
		boolean error = false;
		
		error |= ValidationHelper.checkStringNotBlank(this, name, errorBuffer);

		if (!error && Services.getInstance().getConfigurationProvider().getNodeEntityClass(getProperty(name)) != null) {
			
			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidSchemaToken(getProperty(name), "type_already_exists"));
		}
		
		return !error && super.isValid(errorBuffer);
	}
}
