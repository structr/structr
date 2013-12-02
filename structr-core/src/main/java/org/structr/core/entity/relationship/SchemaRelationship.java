package org.structr.core.entity.relationship;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.CaseHelper;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.ValidationHelper;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import static org.structr.core.entity.AbstractSchemaNode.accessFlags;
import org.structr.core.entity.ManyToMany;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.Property;
import org.structr.core.property.SourceId;
import org.structr.core.property.StringProperty;
import org.structr.core.property.TargetId;
import org.structr.schema.ReloadSchema;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
 */
public class SchemaRelationship extends ManyToMany<SchemaNode, SchemaNode> implements Schema {

	private static final Logger logger                      = Logger.getLogger(SchemaRelationship.class.getName());
	private static final Pattern ValidKeyPattern            = Pattern.compile("[a-zA-Z_]+");
	
	public static final Property<String> sourceId           = new SourceId("sourceId");
	public static final Property<String> targetId           = new TargetId("targetId");
	public static final Property<String> relationshipType   = new StringProperty("relationshipType");
	public static final Property<String> sourceMultiplicity = new StringProperty("sourceMultiplicity");
	public static final Property<String> targetMultiplicity = new StringProperty("targetMultiplicity");
	public static final Property<String> sourceNotion       = new StringProperty("sourceNotion");
	public static final Property<String> targetNotion       = new StringProperty("targetNotion");
	
	
	public static final View defaultView = new View(SchemaRelationship.class, PropertyView.Public,
		AbstractNode.name, sourceId, targetId, sourceMultiplicity, targetMultiplicity, sourceNotion, targetNotion, relationshipType
	);

	@Override
	public Class<SchemaNode> getSourceType() {
		return SchemaNode.class;
	}

	@Override
	public Class<SchemaNode> getTargetType() {
		return SchemaNode.class;
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return sourceId;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return targetId;
	}

	@Override
	public String name() {
		return "IS_RELATED_TO";
	}
	
	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {
		
		boolean error = false;
		
		error |= ValidationHelper.checkStringNotBlank(this, relationshipType, errorBuffer);
		
		return !error && super.isValid(errorBuffer);
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		
		if (super.onCreation(securityContext, errorBuffer)) {
			
			final String signature = getResourceSignature();
			final Long flags       = getProperty(accessFlags);

			if (StringUtils.isNotBlank(signature)) {

				SchemaHelper.createGrant(signature, flags);

				// register transaction post processing that recreates the schema information
				TransactionCommand.postProcess("reloadSchema", new ReloadSchema());

				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer)) {

			final String signature = getResourceSignature();
			final Long flags       = getProperty(accessFlags);

			if (StringUtils.isNotBlank(signature)) {

				SchemaHelper.createGrant(signature, flags);

				// register transaction post processing that recreates the schema information
				TransactionCommand.postProcess("reloadSchema", new ReloadSchema());

				return true;
			}
		}
		
		return false;
	}

	@Override
	public void onRelationshipDeletion() {

		Services.getInstance().getConfigurationProvider().unregisterEntityType(getClassName());
		
		final String signature = getResourceSignature();
		if (StringUtils.isNotBlank(signature)) {
			
			SchemaHelper.removeGrant(getResourceSignature());
		}
	}

	@Override
	public String getClassName() {
	
		String name = getProperty(AbstractNode.name);
		if (name == null) {

			final String _sourceType = getSchemaNodeSourceType();
			final String _targetType = getSchemaNodeTargetType();

			name = _sourceType + _targetType;
		}
		
		return name;
	}
	
	// ----- interface PropertySchema -----
	public String getPropertySource(final String relatedClassName) {
		
		final StringBuilder buf          = new StringBuilder();
		final String _sourceMultiplicity = getProperty(sourceMultiplicity);
		final String _targetMultiplicity = getProperty(targetMultiplicity);
		final String _sourceNotion       = getProperty(sourceNotion);
		final String _targetNotion       = getProperty(targetNotion);
		final String _sourceType         = getSchemaNodeSourceType();
		final String _targetType         = getSchemaNodeTargetType();
		final String _className          = getClassName();

		if (_sourceType.equals(relatedClassName)) {

			if ("1".equals(_targetMultiplicity)) {

				final String propertyName = CaseHelper.toLowerCamelCase(_targetType);
				
				buf.append("\tpublic static final Property<").append(_targetType).append("> ").append(propertyName).append("Property");
				buf.append(" = new EndNode<>(\"").append(propertyName).append("\", ").append(_className).append(".class");
				buf.append(getNotion(_sourceType, _targetNotion));
				buf.append(");\n");
				
			} else {
				
				final String propertyName = CaseHelper.toLowerCamelCase(_targetType) + "s";
				
				buf.append("\tpublic static final Property<List<").append(_targetType).append(">> ").append(propertyName).append("Property");
				buf.append(" = new EndNodes<>(\"").append(propertyName).append("\", ").append(_className).append(".class");
				buf.append(getNotion(_sourceType, _targetNotion));
				buf.append(");\n");
			}
			
		} else if (_targetType.equals(relatedClassName)) {
			
			if ("1".equals(_sourceMultiplicity)) {

				final String propertyName = CaseHelper.toLowerCamelCase(_sourceType);
				
				buf.append("\tpublic static final Property<").append(_sourceType).append("> ").append(propertyName).append("Property");
				buf.append(" = new StartNode<>(\"").append(propertyName).append("\", ").append(_className).append(".class");
				buf.append(getNotion(_targetType, _sourceNotion));
				buf.append(");\n");
				
			} else {
				
				final String propertyName = CaseHelper.toLowerCamelCase(_sourceType) + "s";
				
				buf.append("\tpublic static final Property<List<").append(_sourceType).append(">> ").append(propertyName).append("Property");
				buf.append(" = new StartNodes<>(\"").append(propertyName).append("\", ").append(_className).append(".class");
				buf.append(getNotion(_targetType, _sourceNotion));
				buf.append(");\n");
			}
		}
		
		return buf.toString();
	}

	public String getPropertyName(final String relatedClassName) {
		
		final String _sourceMultiplicity = getProperty(sourceMultiplicity);
		final String _targetMultiplicity = getProperty(targetMultiplicity);
		final String _sourceType         = getSchemaNodeSourceType();
		final String _targetType         = getSchemaNodeTargetType();

		if (_sourceType.equals(relatedClassName)) {

			if ("1".equals(_targetMultiplicity)) {

				return CaseHelper.toLowerCamelCase(_targetType);
				
			} else {
				
				return CaseHelper.toLowerCamelCase(_targetType) + "s";
			}
			
		} else if (_targetType.equals(relatedClassName)) {
			
			if ("1".equals(_sourceMultiplicity)) {

				return CaseHelper.toLowerCamelCase(_sourceType);
				
			} else {
				
				return CaseHelper.toLowerCamelCase(_sourceType) + "s";
			}
		}
		
		return "";
	}

	@Override
	public String getSource(final ErrorBuffer errorBuffer) throws FrameworkException {

		final StringBuilder src          = new StringBuilder();
		final Class baseType             = AbstractRelationship.class;
		final String _className          = getClassName();
		final String _sourceNodeType     = getSchemaNodeSourceType();
		final String _targetNodeType     = getSchemaNodeTargetType();
		final Set<String> viewProperties = new LinkedHashSet<>();
		final Set<String> validators     = new LinkedHashSet<>();
		final Set<String> enums          = new LinkedHashSet<>();
		
		src.append("package org.structr.dynamic;\n\n");
		
		src.append("import ").append(baseType.getName()).append(";\n");
		src.append("import ").append(PropertyView.class.getName()).append(";\n");
		src.append("import ").append(View.class.getName()).append(";\n");
		src.append("import org.structr.core.property.*;\n");
		src.append("import org.structr.core.entity.*;\n\n");
		
		src.append("public class ").append(_className).append(" extends ").append(getBaseType()).append(" {\n\n");
		
		src.append(SchemaHelper.extractProperties(this, null, null, null, errorBuffer));
		
		// source and target id properties
		src.append("\tpublic static final Property<String> sourceId = new SourceId(\"sourceId\");\n");
		src.append("\tpublic static final Property<String> targetId = new SourceId(\"targetId\");\n\n");

		// add sourceId and targetId to view properties
		viewProperties.add("sourceId");
		viewProperties.add("targetId");
		
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
		
		// abstract method implementations
		src.append("\t@Override\n");
		src.append("\tpublic Class<").append(_sourceNodeType).append("> getSourceType() {\n");
		src.append("\t\treturn ").append(_sourceNodeType).append(".class;\n");
		src.append("\t}\n\n");
		src.append("\t@Override\n");
		src.append("\tpublic Class<").append(_targetNodeType).append("> getTargetType() {\n");
		src.append("\t\treturn ").append(_targetNodeType).append(".class;\n");
		src.append("\t}\n\n");
		src.append("\t@Override\n");
		src.append("\tpublic Property<String> getSourceIdProperty() {\n");
		src.append("\t\treturn sourceId;\n");
		src.append("\t}\n\n");
		src.append("\t@Override\n");
		src.append("\tpublic Property<String> getTargetIdProperty() {\n");
		src.append("\t\treturn targetId;\n");
		src.append("\t}\n\n");
		src.append("\t@Override\n");
		src.append("\tpublic String name() {\n");
		src.append("\t\treturn \"").append(getRelationshipType()).append("\";\n");
		src.append("\t}\n\n");
		
		src.append("}\n");
		
		return src.toString();
	}
	
	// ----- private methods -----
	private String getSchemaNodeSourceType() {
		return getSourceNode().getProperty(SchemaNode.name);
	}

	private String getSchemaNodeTargetType() {
		return getTargetNode().getProperty(SchemaNode.name);
	}

	private String getNotion(final String _className, final String notionSource) {
		
		final StringBuilder buf = new StringBuilder();
		
		if (StringUtils.isNotBlank(notionSource)) {
			
			final Set<String> keys = new LinkedHashSet<>(Arrays.asList(notionSource.split("[\\s,]+")));
			if (!keys.isEmpty()) {

				if (keys.size() == 1) {
					
					String key     = keys.iterator().next();
					boolean create = key.startsWith("+");

					if (create) {
						key = key.substring(1);
					}

					if (ValidKeyPattern.matcher(key).matches()) {

						buf.append(", new PropertyNotion(");
						buf.append(getNotionKey(_className, key));
						buf.append(", ").append(create);
						buf.append(")");
						
					} else {
			
						logger.log(Level.WARNING, "Invalid key name {0} for notion.", key);
					}
					
				} else {
					
					buf.append(", new PropertySetNotion(");
					
					// use only matching keys
					for (final Iterator<String> it = Iterables.filter(new KeyMatcher(), keys).iterator(); it.hasNext();) {

						buf.append(getNotionKey(_className, it.next()));

						if (it.hasNext()) {
							buf.append(", ");
						}
					}

					buf.append(")");
				}
			}
		}
		
		return buf.toString();
	}
	
	private String getNotionKey(final String _className, final String key) {
		return _className + "." + key;
	}
	
	private String getBaseType() {
		
		final String _sourceMultiplicity = getProperty(sourceMultiplicity);
		final String _targetMultiplicity = getProperty(targetMultiplicity);
		final String _sourceType         = getSchemaNodeSourceType();
		final String _targetType         = getSchemaNodeTargetType();
		final StringBuilder buf          = new StringBuilder();
		
		if ("1".equals(_sourceMultiplicity)) {

			if ("1".equals(_targetMultiplicity)) {

				buf.append("OneToOne");

			} else {

				buf.append("OneToMany");
			}

		} else {

			if ("1".equals(_targetMultiplicity)) {

				buf.append("ManyToOne");

			} else {

				buf.append("ManyToMany");
			}
		}
		
		buf.append("<");
		buf.append(_sourceType);
		buf.append(", ");
		buf.append(_targetType);
		buf.append(">");
		
		return buf.toString();
	}
	
	private String getResourceSignature() {
		
		final String _sourceType = getSchemaNodeSourceType();
		final String _targetType = getSchemaNodeTargetType();
		
		return _sourceType + "/" + _targetType;
	}
	
	private String getRelationshipType() {
		
		String relType = getProperty(relationshipType);
		if (relType == null) {
			
			final String _sourceType = getSchemaNodeSourceType().toUpperCase();
			final String _targetType = getSchemaNodeTargetType().toUpperCase();
			
			relType = _sourceType + "_" + _targetType;
		}
		
		return relType;
	}

	// ----- nested classes -----
	private static class KeyMatcher implements Predicate<String> {

		@Override
		public boolean accept(String t) {
			
			if (ValidKeyPattern.matcher(t).matches()) {
				return true;
			}
			
			logger.log(Level.WARNING, "Invalid key name {0} for notion.", t);
			
			return false;
		}
	}
}
