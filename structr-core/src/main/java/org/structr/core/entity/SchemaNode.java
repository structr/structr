package org.structr.core.entity;

import java.util.EnumMap;
import java.util.Iterator;
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
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.Services;
import org.structr.core.entity.relationship.NodeIsRelatedToNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.schema.NodeSchema;

/**
 *
 * @author Christian Morgner
 */
public class SchemaNode extends AbstractSchemaNode implements NodeSchema {
	
	private enum Type {
		String, Integer, Long, Double, Boolean, Enum, Date
	}

	private static final Map<Type, String> typeMap                 = new EnumMap<>(Type.class);

	public static final Property<List<SchemaNode>>     outNodes    = new EndNodes<>("outNodes", NodeIsRelatedToNode.class);
	public static final Property<List<SchemaNode>>     inNodes     = new StartNodes<>("inNodes", NodeIsRelatedToNode.class);
	
	public static final View defaultView = new View(SchemaNode.class, PropertyView.Public,
		packageName, className, outNodes, inNodes
	);
	
	static {

		typeMap.put(Type.String,  "StringProperty");
		typeMap.put(Type.Integer, "IntProperty");
		typeMap.put(Type.Long,    "LongProperty");
		typeMap.put(Type.Double,  "DoubleProperty");
		typeMap.put(Type.Boolean, "BooleanProperty");
		typeMap.put(Type.Enum,    "EnumProperty");
		typeMap.put(Type.Date,    "ISO8601DateProperty");
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
	public String getNodeSource() throws FrameworkException {
		
		final Set<String> viewProperties = new LinkedHashSet<>();
		final Set<String> validators     = new LinkedHashSet<>();
		final StringBuilder src          = new StringBuilder();
		final Class baseType             = AbstractNode.class;
		final String _className          = getProperty(className);
		final Node rawNode               = getNode();
		
		src.append("package ").append(getProperty(packageName)).append(";\n\n");
		
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
		for (final String rawPropertyName : getProperties()) {
			
			if (rawNode.hasProperty(rawPropertyName)) {
				
				final String valueType    = rawNode.getProperty(rawPropertyName).toString();

				src.append(getPropertySource(_className, rawPropertyName, valueType, validators));
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
		
		error |= ValidationHelper.checkStringNotBlank(this, packageName, errorBuffer);
		error |= ValidationHelper.checkStringNotBlank(this, className,   errorBuffer);
		
		return !error && super.isValid(errorBuffer);
	}
	
	// ----- private methods -----
	private String getPropertySource(final String className, final String propertyName, final String rawValueType, final Set<String> globalValidators) throws FrameworkException {
		
		final StringBuilder buf            = new StringBuilder();
		final ValidationInfo info          = new ValidationInfo(className, propertyName, rawValueType);
		
		// collect global validators from property (can be more than one)
		globalValidators.addAll(info.getGlobalValidators());
		
		buf.append("\tpublic static final Property<").append(info.getValueType()).append("> ").append(propertyName).append("Property");
		buf.append(" = new ").append(info.getPropertyType()).append("(\"").append(propertyName).append("\"");
		buf.append(info.getLocalValidator());
		buf.append(").indexed();\n");
		
		return buf.toString();
	}
	
	private Iterable<String> getProperties() {
		
		final List<String> keys = new LinkedList<>();
		final Set<Type> types   = typeMap.keySet();
		final Node node         = getNode();

		for (final String key : node.getPropertyKeys()) {

			if (node.hasProperty(key)) {
				
				try {
					final String propertyType = node.getProperty(key).toString();
					final ValidationInfo info = new ValidationInfo(propertyType);
					
					if (types.contains(info.getValueType())) {
						keys.add(key);
					}
					
				} catch (Throwable t) {}
			}
		}
		
		return keys;
	}
	
	// ----- nested classes -----
	private static class ValidationInfo {
		
		private Set<String> globalValidators = new LinkedHashSet<>();
		private Type valueType               = null;
		private String uniquenessValidator   = null;
		private String enumTypeDefinition    = null;
		private String propertyName          = null;
		private String className             = null;
		private String localValidator        = "";
		
		public ValidationInfo(final String rawValueType) throws FrameworkException {
			this(null, null, rawValueType);
		}
		
		public ValidationInfo(final String className, final String propertyName, final String rawValueType) throws FrameworkException {
		
			int length         = rawValueType.length();
			int previousLength = 0;
			
			this.propertyName = propertyName;
			this.className    = className;
			
			// first: value type
			String parserSource = extractValueType(rawValueType);

			// second: uniqueness and/or non-null, check until the two methods to not
			//         change the length of the string any more
			while (previousLength != length) {
				
				parserSource = extractUniqueness(parserSource);
				parserSource = extractNonNull(parserSource);
				
				previousLength = length;
				length = parserSource.length();
			}
			
			// third: more complex type-dependent validation expressions
			extractComplexValidation(parserSource);
		}
		
		public Type getValueType() {
			return valueType;
		}
		
		public String getPropertyType() {	
			return typeMap.get(valueType);
		}
		
		public String getLocalValidator() {
			return localValidator;
		}
		
		public Set<String> getGlobalValidators() {
			return globalValidators;
		}
		
		public boolean hasUniquenessValidator() {
			return uniquenessValidator != null;
		}
		
		private String extractValueType(final String source) throws FrameworkException {
			
			for (final Type type : typeMap.keySet()) {
				
				if (source.startsWith(type.name())) {
					
					valueType = type;
					
					return source.substring(type.name().length());
				}
			}
			
			// invalid type!
			throw new FrameworkException(SchemaNode.class.getName(), new InvalidPropertySchemaToken(source));
		}
		
		private String extractUniqueness(final String source) {
			
			if (source.startsWith("!")) {

				localValidator = ", new GlobalPropertyUniquenessValidator()";

				return source.substring(1);
			}
			
			return source;
		}
		
		private String extractNonNull(final String source) {
			
			if (source.startsWith("+")) {

				StringBuilder buf = new StringBuilder();
				buf.append("ValidationHelper.checkPropertyNotNull(this, ");
				buf.append(className).append(".").append(propertyName).append("Property");
				buf.append(", errorBuffer)");

				globalValidators.add(buf.toString());

				return source.substring(1);
			}
			
			return source;
		}
		
		private void extractComplexValidation(final String source) throws FrameworkException {

			if (source.startsWith("(") && source.endsWith(")")) {

				final String expression = source.substring(1, source.length() - 1);
				
				switch (valueType) {

					case Boolean:
					case Date:
						return;

					case Double:
					case Long:
					case Integer:
						extractNumericValidation(expression);
						return;

					case String:
						localValidator = ", new SimpleRegexValidator(\""  + expression + "\")";
						return;

					case Enum:
						extractEnumValidation(expression);
						return;
				}
			}
			
			throw new FrameworkException(SchemaNode.class.getName(), new InvalidPropertySchemaToken(source));

		}
		
		private void extractNumericValidation(final String expression) throws FrameworkException {
			
			if (expression.contains(",") && (expression.startsWith("[") || expression.startsWith("]")) && (expression.endsWith("[") || expression.endsWith("]"))) {

				final StringBuilder buf = new StringBuilder();
				
				buf.append("ValidationHelper.check").append(valueType.name()).append("InRangeError(this, ");
				buf.append(className).append(".").append(propertyName).append("Property");
				buf.append(", \"").append(expression);
				buf.append("\", errorBuffer)");

				globalValidators.add(buf.toString());
				
			} else {
				
				throw new FrameworkException(SchemaNode.class.getName(), new InvalidPropertySchemaToken(expression));
			}
		}
		
		private void extractEnumValidation(final String expression) throws FrameworkException {
			
		}
	}
}
