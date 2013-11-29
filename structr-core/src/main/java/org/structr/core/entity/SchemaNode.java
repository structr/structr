package org.structr.core.entity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.Node;
import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.Services;
import org.structr.core.entity.relationship.NodeIsRelatedToNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.schema.NodeSchema;
import org.structr.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
 */
public class SchemaNode extends AbstractSchemaNode implements NodeSchema {

	private static final Map<String, String> typeMap               = new LinkedHashMap<>();

	public static final Property<List<SchemaNode>>     outNodes    = new EndNodes<>("outNodes", NodeIsRelatedToNode.class);
	public static final Property<List<SchemaNode>>     inNodes     = new StartNodes<>("inNodes", NodeIsRelatedToNode.class);
	
	public static final View defaultView = new View(SchemaNode.class, PropertyView.Public,
		packageName, className, outNodes, inNodes
	);
	
	static {

		typeMap.put("String",  "StringProperty");
		typeMap.put("Integer", "IntProperty");
		typeMap.put("Long",    "LongProperty");
		typeMap.put("Double",  "DoubleProperty");
		typeMap.put("Boolean", "BooleanProperty");
		typeMap.put("Date",    "ISO8601DateProperty");

	}
	
	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {
		
		final Set<PropertyKey> propertyKeys = new LinkedHashSet<>(Services.getInstance().getConfigurationProvider().getPropertySet(getClass(), propertyView));
		
		// add "custom" property keys as String properties
		for (final String key : getProperties()) {
			
			final PropertyKey newKey = new StringProperty(key);
			newKey.setDeclaringClass(getClass());
			
			propertyKeys.add(newKey);
		}
			
		return propertyKeys;
	}
	
	@Override
	public String getPackageName() {
		return getProperty(packageName);
	}

	@Override
	public String getClassName() {
		return getProperty(className);
	}
	
	@Override
	public String getNodeSource() {
		
		final Set<String> viewProperties = new LinkedHashSet<>();
		final StringBuilder src          = new StringBuilder();
		final Class baseType             = AbstractNode.class;
		final String _className          = getProperty(className);
		final Node rawNode               = getNode();
		
		src.append("package ").append(getProperty(packageName)).append(";\n\n");
		
		src.append("import ").append(baseType.getName()).append(";\n");
		src.append("import ").append(PropertyView.class.getName()).append(";\n");
		src.append("import ").append(View.class.getName()).append(";\n");
		src.append("import org.structr.core.property.*;\n");
		src.append("import org.structr.core.notion.*;\n");
		src.append("import java.util.List;\n\n");
		
		src.append("public class ").append(_className).append(" extends ").append(baseType.getSimpleName()).append(" {\n\n");
		
		// output property source code and collect views
		for (final String rawPropertyName : getProperties()) {
			
			if (rawNode.hasProperty(rawPropertyName)) {
				
				final String valueType    = rawNode.getProperty(rawPropertyName).toString();
				final String propertyType = typeMap.get(valueType);

				src.append(getPropertySource(rawPropertyName, propertyType, valueType));
				viewProperties.add(rawPropertyName);
			}
		}
		
		// output related node definitions, collect property views
		for (final NodeIsRelatedToNode outRel : getOutgoingRelationships(NodeIsRelatedToNode.class)) {
			src.append(outRel.getPropertySource(_className));
			viewProperties.add(outRel.getPropertyName(_className));
		}
		
		// output related node definitions, collect property views
		for (final NodeIsRelatedToNode inRel : getIncomingRelationships(NodeIsRelatedToNode.class)) {
			src.append(inRel.getPropertySource(_className));
			viewProperties.add(inRel.getPropertyName(_className));
		}

		if (!viewProperties.isEmpty()) {

			// output default view
			src.append("\n\tpublic static final View defaultView = new View(").append(_className).append(".class, PropertyView.Public,\n");
			src.append("\t\t");

			for (final Iterator<String> it = viewProperties.iterator(); it.hasNext();) {

				src.append(it.next()).append("Property");

				if (it.hasNext()) {
					src.append(", ");
				}
			}

			src.append("\n\t);\n");
		}
		
		src.append("}\n");
		
		return src.toString();
	}
	
	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {
		
		boolean error = false;
		
		error |= ValidationHelper.checkStringNotBlank(this, packageName, errorBuffer);
		error |= ValidationHelper.checkStringNotBlank(this, className,   errorBuffer);
		
		return !error && super.isValid(errorBuffer);
	}
	
	// ----- private methods -----
	private String getPropertySource(final String propertyName, final String propertyType, final String valueType) {
		
		final StringBuilder buf    = new StringBuilder();
		
		buf.append("\tpublic static final Property<").append(valueType).append("> ").append(propertyName).append("Property");
		buf.append(" = new ").append(propertyType).append("(\"").append(propertyName).append("\");");
		
		// TODO: add notions
		
		buf.append("\n");
		
		return buf.toString();
	}
	
	private Iterable<String> getProperties() {
		
		final List<String> keys = new LinkedList<>();
		final Set<String> types = typeMap.keySet();
		final Node node         = getNode();

		for (final String key : node.getPropertyKeys()) {
			
			if (node.hasProperty(key) && types.contains(node.getProperty(key).toString())) {
				
				keys.add(key);
			}
		}
		
		return keys;
	}
}
