package org.structr.core.entity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
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
	
	private Set<String> dynamicViews = new LinkedHashSet<>();
	
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {
		
		final Set<PropertyKey> propertyKeys = new LinkedHashSet<>(Iterables.toList(super.getPropertyKeys(propertyView)));
		
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
		
		final Map<String, Set<String>> viewProperties = new LinkedHashMap<>();
		final Set<String> validators                  = new LinkedHashSet<>();
		final Set<String> enums                       = new LinkedHashSet<>();
		final StringBuilder src                       = new StringBuilder();
		final Class baseType                          = AbstractNode.class;
		final String _className                       = getProperty(name);
		
		src.append("package org.structr.dynamic;\n\n");
		
		SchemaHelper.formatImportStatements(src, baseType);
		
		src.append("public class ").append(_className).append(" extends ").append(baseType.getSimpleName()).append(" {\n\n");
		
		// output related node definitions, collect property views
		for (final SchemaRelationship outRel : getOutgoingRelationships(SchemaRelationship.class)) {

			src.append(outRel.getPropertySource(_className));
			SchemaHelper.addPropertyToView(PropertyView.Public, outRel.getPropertyName(_className), viewProperties);
			SchemaHelper.addPropertyToView(PropertyView.Ui, outRel.getPropertyName(_className), viewProperties);
		}
		
		// output related node definitions, collect property views
		for (final SchemaRelationship inRel : getIncomingRelationships(SchemaRelationship.class)) {

			src.append(inRel.getPropertySource(_className));
			SchemaHelper.addPropertyToView(PropertyView.Public, inRel.getPropertyName(_className), viewProperties);
			SchemaHelper.addPropertyToView(PropertyView.Ui, inRel.getPropertyName(_className), viewProperties);
		}

		// extract properties from node
		src.append(SchemaHelper.extractProperties(this, validators, enums, viewProperties, errorBuffer));
		
		// output possible enum definitions
		for (final String enumDefition : enums) {
			src.append(enumDefition);
		}

		for (Entry<String, Set<String>> entry :viewProperties.entrySet()) {

			final String viewName  = entry.getKey();
			final Set<String> view = entry.getValue();
			
			if (!view.isEmpty()) {
				dynamicViews.add(viewName);
				SchemaHelper.formatView(src, _className, viewName, viewName, view);
			}
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
	public Set<String> getViews() {
		return dynamicViews;
	}
	
	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		return ValidationHelper.checkStringMatchesRegex(this, name, "[a-zA-Z_]+", errorBuffer);

	}

}
