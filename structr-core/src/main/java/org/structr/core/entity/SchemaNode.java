package org.structr.core.entity;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.neo4j.graphdb.Node;
import org.structr.common.PropertyView;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.Services;
import static org.structr.core.entity.AbstractSchemaNode.className;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.schema.NodeSchema;
import org.structr.schema.SchemaNotion;
import org.structr.schema.parser.BooleanPropertyParser;
import org.structr.schema.parser.DatePropertyParser;
import org.structr.schema.parser.DoublePropertyParser;
import org.structr.schema.parser.EnumPropertyParser;
import org.structr.schema.parser.IntPropertyParser;
import org.structr.schema.parser.LongPropertyParser;
import org.structr.schema.parser.PropertyParser;
import org.structr.schema.parser.StringPropertyParser;

/**
 *
 * @author Christian Morgner
 */
public class SchemaNode extends AbstractSchemaNode implements NodeSchema {
	
	public enum Type {
		String, Integer, Long, Double, Boolean, Enum, Date
	}

	private static final Map<Type, Class<? extends PropertyParser>> parserMap = new EnumMap<>(Type.class);

	public static final Property<List<SchemaNode>>  relatedTo   = new EndNodes<>("relatedTo", SchemaRelationship.class, new SchemaNotion(SchemaNode.class));
	public static final Property<List<SchemaNode>>  relatedFrom = new StartNodes<>("relatedFrom", SchemaRelationship.class, new SchemaNotion(SchemaNode.class));
	
	public static final View defaultView = new View(SchemaNode.class, PropertyView.Public,
		className, relatedTo, relatedFrom
	);
	
	static {

		parserMap.put(Type.String,  StringPropertyParser.class);
		parserMap.put(Type.Integer, IntPropertyParser.class);
		parserMap.put(Type.Long,    LongPropertyParser.class);
		parserMap.put(Type.Double,  DoublePropertyParser.class);
		parserMap.put(Type.Boolean, BooleanPropertyParser.class);
		parserMap.put(Type.Enum,    EnumPropertyParser.class);
		parserMap.put(Type.Date,    DatePropertyParser.class);
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
	public String getNodeSource(final ErrorBuffer errorBuffer) throws FrameworkException {
		
		final Set<String> viewProperties = new LinkedHashSet<>();
		final Set<String> validators     = new LinkedHashSet<>();
		final Set<String> enums          = new LinkedHashSet<>();
		final StringBuilder src          = new StringBuilder();
		final Class baseType             = AbstractNode.class;
		final String _className          = getProperty(className);
		final Node rawNode               = getNode();
		
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
		
		// output property source code and collect views
		for (final String propertyName : getProperties()) {
			
			if (rawNode.hasProperty(propertyName)) {
				
				final String rawType        = rawNode.getProperty(propertyName).toString();
				final PropertyParser parser = getParserForRawValue(errorBuffer, _className, propertyName, rawType);
			
				if (parser != null) {
					// append created source from parser
					src.append(parser.getPropertySource(errorBuffer));

					// register global elements created by parser
					validators.addAll(parser.getGlobalValidators());
					enums.addAll(parser.getEnumDefinitions());

					// register property in default view
					viewProperties.add(propertyName.substring(1));
				}
			}
		}
		
		// output related node definitions, collect property views
		for (final SchemaRelationship outRel : getOutgoingRelationships(SchemaRelationship.class)) {
			src.append(outRel.getPropertySource(_className));
			viewProperties.add(outRel.getPropertyName(_className));
		}
		
		// output related node definitions, collect property views
		for (final SchemaRelationship inRel : getIncomingRelationships(SchemaRelationship.class)) {
			src.append(inRel.getPropertySource(_className));
			viewProperties.add(inRel.getPropertyName(_className));
		}
		
		// output possible enum definitions
		for (final String enumDefition : enums) {
			src.append(enumDefition);
		}

		if (!viewProperties.isEmpty()) {
			formatView(src, _className, "default", "PropertyView.Public", viewProperties);
			formatView(src, _className, "ui", "PropertyView.Ui", viewProperties);
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
		
		error |= ValidationHelper.checkStringNotBlank(this, className, errorBuffer);
		
		return !error && super.isValid(errorBuffer);
	}
	
	private Iterable<String> getProperties() {
		
		final List<String> keys   = new LinkedList<>();
		final Node node           = getNode();

		for (final String key : node.getPropertyKeys()) {

			if (node.hasProperty(key) && key.startsWith("_")) {
				
				keys.add(key);
			}
		}
		
		return keys;
	}
	
	private PropertyParser getParserForRawValue(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String source) throws FrameworkException {
		
		for (final Entry<Type, Class<? extends PropertyParser>> entry : parserMap.entrySet()) {
			
			if (source.startsWith(entry.getKey().name())) {
				
				try {
					return entry.getValue().getConstructor(ErrorBuffer.class, String.class, String.class, String.class).newInstance(errorBuffer, className, propertyName, source);
					
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
		
		errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(source, "invalid_property_definition", "Unknow value type " + source + ", options are " + Arrays.asList(Type.values()) + "."));
		throw new FrameworkException(422, errorBuffer);
	}
	
	private void formatView(final StringBuilder src, final String _className, final String viewName, final String view, final Set<String> viewProperties) {

		// output default view
		src.append("\n\tpublic static final View ").append(viewName).append("View = new View(").append(_className).append(".class, ").append(view).append(",\n");
		src.append("\t\t");

		for (final Iterator<String> it = viewProperties.iterator(); it.hasNext();) {

			src.append(it.next()).append("Property");

			if (it.hasNext()) {
				src.append(", ");
			}
		}

		src.append("\n\t);\n");
		
	}
}
