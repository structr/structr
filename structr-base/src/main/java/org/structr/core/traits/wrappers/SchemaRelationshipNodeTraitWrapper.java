/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.traits.wrappers;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.TraitDefinition;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.DynamicRelationshipTraitDefinition;
import org.structr.schema.SchemaHelper;

import java.util.*;
import java.util.regex.Pattern;

public class SchemaRelationshipNodeTraitWrapper extends AbstractSchemaNodeTraitWrapper implements SchemaRelationshipNode {

	private static final Logger logger           = LoggerFactory.getLogger(SchemaRelationshipNodeTraitWrapper.class);
	private static final Pattern ValidKeyPattern = Pattern.compile("[a-zA-Z_]+");

	public SchemaRelationshipNodeTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public SchemaNode getSourceNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("sourceNode"));
		if (node != null) {

			return node.as(SchemaNode.class);
		}

		return null;
	}

	@Override public SchemaNode getTargetNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("targetNode"));
		if (node != null) {

			return node.as(SchemaNode.class);
		}

		return null;
	}

	// ----- interface Schema -----
	@Override
	public String getClassName() {

		String name = wrappedObject.getProperty(Traits.nameProperty());
		if (name == null) {

			final String _sourceType = getSchemaNodeSourceType();
			final String _targetType = getSchemaNodeTargetType();
			final String _relType    = SchemaHelper.cleanPropertyName(getRelationshipType());
			final StringBuilder buf  = new StringBuilder();

			if (_sourceType != null &&_sourceType.contains(".")) {

				// remove FQCN from class name (if present)
				buf.append(StringUtils.substringAfterLast(_sourceType, "."));

			} else {

				buf.append(_sourceType);
			}

			buf.append(_relType);

			if (_targetType != null && _targetType.contains(".")) {

				// remove FQCN from class name (if present)
				buf.append(StringUtils.substringAfterLast(_targetType, "."));
			} else {

				buf.append(_targetType);
			}

			name = buf.toString();

			try {
				wrappedObject.setProperty(Traits.nameProperty(), name);

			} catch (FrameworkException fex) {
				logger.warn("Unable to set relationship name to {}.", name);
			}
		}

		return name;
	}

	/*
	public void getPropertySource(final SourceFile src, final String propertyName, final boolean outgoing) {
		getPropertySource(src, propertyName, outgoing, false);
	}

	public void getPropertySource(final SourceFile src, final String propertyName, final boolean outgoing, final boolean newStatementOnly) {

		final Boolean partOfBuiltInSchema = isPartOfBuiltInSchema();
		final String _sourceMultiplicity  = getMultiplicity(false);
		final String _targetMultiplicity  = getMultiplicity(true);
		final String _sourceNotion        = getSourceNotion();
		final String _targetNotion        = getTargetNotion();
		final String _sourceType          = getSchemaNodeSourceType();
		final String _targetType          = getSchemaNodeTargetType();
		final String _className           = getClassName();
		final SourceLine line             = src.line(this);

		if (outgoing) {


			if ("1".equals(_targetMultiplicity)) {

				if (!newStatementOnly) {

					line.append("public static final Property<");
					//line.append("org.structr.dynamic.");
					line.append(_targetType);
					line.append("> ");
					line.append(SchemaHelper.cleanPropertyName(propertyName));
					line.append("Property");
					line.append(" = ");

				}

				line.append("new EndNode<>(\"");
				line.append(propertyName);
				line.append("\", ");
				line.append(_className);
				line.append(".class");
				line.append(getNotion(_sourceType, _targetNotion));

				if (newStatementOnly) {

					line.append(")");

				} else {

					line.append(").dynamic()");
					line.append(".setSourceUuid(\"");
					line.append(getUuid());
					line.append("\")");
					line.append(partOfBuiltInSchema ? "" : "");
					line.append(";");
				}

			} else {

				if (!newStatementOnly) {

					line.append("public static final Property<java.lang.Iterable<");
					//line.append("org.structr.dynamic.");
					line.append(_targetType);
					line.append(">> ");
					line.append(SchemaHelper.cleanPropertyName(propertyName));
					line.append("Property");
					line.append(" = ");
				}

				line.append("new EndNodes<>(\"");
				line.append(propertyName);
				line.append("\", ");
				line.append(_className);
				line.append(".class");
				line.append(getNotion(_sourceType, _targetNotion));

				if (newStatementOnly) {

					line.append(")");

				} else {

					line.append(").dynamic()");
					line.append(".setSourceUuid(\"");
					line.append(getUuid()).append("\")");
					line.append(partOfBuiltInSchema ? "" : "");
					line.append(";");
				}
			}

		} else {

			if ("1".equals(_sourceMultiplicity)) {

				if (!newStatementOnly) {

					line.append("public static final Property<");
					//line.append("org.structr.dynamic.");
					line.append(_sourceType);
					line.append("> ");
					line.append(SchemaHelper.cleanPropertyName(propertyName));
					line.append("Property");
					line.append(" = ");
				}

				line.append("new StartNode<>(\"");
				line.append(propertyName);
				line.append("\", ");
				line.append(_className);
				line.append(".class");
				line.append(getNotion(_targetType, _sourceNotion));

				if (newStatementOnly) {

					line.append(")");

				} else {

					line.append(").dynamic()");
					line.append(".setSourceUuid(\"");
					line.append(getUuid());
					line.append("\")");
					line.append(partOfBuiltInSchema ? "" : "");
					line.append(";");
				}

			} else {

				if (!newStatementOnly) {

					line.append("public static final Property<java.lang.Iterable<");
					line.append("org.structr.dynamic.");
					line.append(_sourceType);
					line.append(">> ");
					line.append(SchemaHelper.cleanPropertyName(propertyName));
					line.append("Property");
					line.append(" = ");
				}

				line.append("new StartNodes<>(\"");
				line.append(propertyName);
				line.append("\", ");
				line.append(_className);
				line.append(".class");
				line.append(getNotion(_targetType, _sourceNotion));

				if (newStatementOnly) {

					line.append(")");

				} else {

					line.append(").dynamic()");
					line.append(".setSourceUuid(\"");
					line.append(getUuid()).append("\")");
					line.append(partOfBuiltInSchema ? "" : "");
					line.append(";");
				}
			}
		}
	}
	*/

	@Override
	public String getSourceNotion() {
		return wrappedObject.getProperty(traits.key("sourceNotion"));
	}

	@Override
	public String getTargetNotion() {
		return wrappedObject.getProperty(traits.key("targetNotion"));
	}

	@Override
	public String getPropertyName(final Set<String> existingPropertyNames, final boolean outgoing) {

		final String relationshipTypeName = getRelationshipType().toLowerCase();
		final String _sourceType          = getSchemaNodeSourceType();
		final String _targetType          = getSchemaNodeTargetType();
		final String _targetJsonName      = getTargetJsonName();
		final String _targetMultiplicity  = getTargetMultiplicity();
		final String _sourceJsonName      = getSourceJsonName();
		final String _sourceMultiplicity  = getSourceMultiplicity();

		final String propertyName = SchemaProperty.getPropertyName(existingPropertyNames, outgoing, relationshipTypeName, _sourceType, _targetType, _targetJsonName, _targetMultiplicity, _sourceJsonName, _sourceMultiplicity);

		try {
			if (outgoing) {

				if (_targetJsonName == null) {

					setPreviousTargetJsonName(propertyName);
				}

			} else {

				if (_sourceJsonName == null) {

					setPreviousSourceJsonName(propertyName);
				}
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}

		return propertyName;
	}

	@Override
	public void setPreviousSourceJsonName(final String propertyName) throws FrameworkException {
		wrappedObject.setProperty(traits.key("oldSourceJsonName"), propertyName);
	}

	@Override
	public void setPreviousTargetJsonName(final String propertyName) throws FrameworkException {
		wrappedObject.setProperty(traits.key("oldTargetJsonName"), propertyName);
	}

	@Override
	public String getSourceMultiplicity() {
		return wrappedObject.getProperty(traits.key("sourceMultiplicity"));
	}

	@Override
	public String getTargetMultiplicity() {
		return wrappedObject.getProperty(traits.key("targetMultiplicity"));
	}

	@Override
	public String getSourceJsonName() {
		return wrappedObject.getProperty(traits.key("sourceJsonName"));
	}

	@Override
	public String getTargetJsonName() {
		return wrappedObject.getProperty(traits.key("targetJsonName"));
	}

	@Override
	public String getSourceType() {
		return wrappedObject.getProperty(traits.key("sourceType"));
	}

	@Override
	public String getTargetType() {
		return wrappedObject.getProperty(traits.key("targetType"));
	}

	/*
	public void getSource(final SourceFile src, final Map<String, SchemaNode> schemaNodes, final ErrorBuffer errorBuffer) throws FrameworkException {

		final Map<String, List<ActionEntry>> actions        = new LinkedHashMap<>();
		final Map<String, CodeSourceViewSet> viewProperties = new LinkedHashMap<>();
		final Class baseType                                = AbstractRelationship.class;
		final String _className                             = getClassName();
		final String _sourceNodeType                        = getSchemaNodeSourceType();
		final String _targetNodeType                        = getSchemaNodeTargetType();
		final List<String> propertyValidators               = new LinkedList<>();
		final Set<String> compoundIndexKeys                 = new LinkedHashSet<>();
		final Set<String> propertyNames                     = new LinkedHashSet<>();
		final Set<Validator> validators                     = new LinkedHashSet<>();
		final Set<String> enums                             = new LinkedHashSet<>();
		final Set<String> interfaces                        = new LinkedHashSet<>();

		src.line(this, "package org.structr.dynamic;");

		SchemaHelper.formatImportStatements(src, this, baseType);

		final SourceLine classDefinition = src.begin(this, "public class ").append(_className).append(" extends ").append(getBaseType());

		if ("OWNS".equals(getRelationshipType())) {
			interfaces.add(Ownership.class.getName());
		}

		if (!PropagationDirection.None.equals(wrappedObject.getProperty(permissionPropagation))) {
			interfaces.add(PermissionPropagation.class.getName());
		}

		// append interfaces if present
		if (!interfaces.isEmpty()) {

			classDefinition.append(" implements ");
			classDefinition.append(StringUtils.join(interfaces, ", "));
		}

		classDefinition.append(" {");

		if (!PropagationDirection.None.equals(getPermissionPropagation())) {

			src.begin(this, "static {");
			src.line(this, "Traits.of("SchemaRelationshipNode").key("registerPropagatingRelationshipType")(").append(_className).append(".class, true);");
			src.end();
		}

		SchemaHelper.extractProperties(src, schemaNodes, this, propertyNames, validators, compoundIndexKeys, enums, viewProperties, propertyValidators, errorBuffer);
		SchemaHelper.extractViews(schemaNodes, this, viewProperties, Collections.EMPTY_SET, errorBuffer);
		SchemaHelper.extractMethods(schemaNodes, this, actions);

		// source and target id properties
		src.line(this, "public static final Property<java.lang.String> sourceIdProperty = new SourceId(\"sourceId\");");
		src.line(this, "public static final Property<java.lang.String> targetIdProperty = new TargetId(\"targetId\");");

		SchemaHelper.addPropertyToView(PropertyView.Ui, "sourceId", viewProperties);
		SchemaHelper.addPropertyToView(PropertyView.Ui, "targetId", viewProperties);

		// output possible enum definitions
		for (final String enumDefition : enums) {
			src.line(this, enumDefition);
		}

		for (Map.Entry<String, CodeSourceViewSet> entry : viewProperties.entrySet()) {

			final CodeSourceViewSet view = entry.getValue();
			final String viewName        = entry.getKey();

			if (!view.isEmpty()) {
				dynamicViews.add(viewName);
				SchemaHelper.formatView(src, view.getSource(), _className, viewName, viewName, view);
			}
		}

		// abstract method implementations
		src.line(this, "@Override");
		src.begin(this, "public Class<").append(_sourceNodeType).append("> getSourceType() {");
		src.line(this, "return ").append(_sourceNodeType).append(".class;");
		src.end();

		src.line(this, "@Override");
		src.begin(this, "public Class<").append(_targetNodeType).append("> getTargetType() {");
		src.line(this, "return ").append(_targetNodeType).append(".class;");
		src.end();

		src.line(this, "@Override");
		src.begin(this, "public Property<java.lang.String> getSourceIdProperty() {");
		src.line(this, "return sourceId;");
		src.end();

		src.line(this, "@Override");
		src.begin(this, "public Property<java.lang.String> getTargetIdProperty() {");
		src.line(this, "return targetId;");
		src.end();

		src.line(this, "@Override");
		src.begin(this, "public java.lang.String name() {");
		src.line(this, "return \"").append(getRelationshipType()).append("\";");
		src.end();

		SchemaHelper.formatValidators(src, this, validators, compoundIndexKeys, false, propertyValidators);
		SchemaHelper.formatMethods(src, this, actions, Collections.emptySet());

		formatRelationshipFlags(src);
		formatPermissionPropagation(src);

		src.end();
	}
	*/

	// ----- public methods -----
	@Override public String getSchemaNodeSourceType() {

		final SchemaNode sourceNode = getSourceNode();
		if (sourceNode != null) {

			return sourceNode.getName();
		}

		return getSourceType();
	}

	@Override public String getSchemaNodeTargetType() {

		final SchemaNode targetNode = getTargetNode();
		if (targetNode != null) {

			return targetNode.getName();
		}

		return getTargetType();
	}

	@Override
	public String getResourceSignature() {

		final String _sourceType = getSchemaNodeSourceType();
		final String _targetType = getSchemaNodeTargetType();

		return _sourceType + "/" + _targetType;
	}

	@Override public String getInverseResourceSignature() {

		final String _sourceType = getSchemaNodeSourceType();
		final String _targetType = getSchemaNodeTargetType();

		return _targetType + "/" + _sourceType;
	}

	@Override
	public String getRelationshipType() {

		String relType = wrappedObject.getProperty(traits.key("relationshipType"));
		if (relType == null) {

			final String _sourceType = getSchemaNodeSourceType();
			final String _targetType = getSchemaNodeTargetType();

			if (_sourceType != null && _targetType != null) {

				relType = _sourceType.toUpperCase() + "_" + _targetType.toUpperCase();
			}
		}

		return relType;
	}

	private Notion getNotion(final String _className, final String notionSource) {

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

						return new PropertyNotion(key, create);

					} else {

						logger.warn("Invalid key name {} for notion.", key);
					}

				} else {

					return new PropertySetNotion<>(keys);
				}
			}
		}

		return new ObjectNotion();
	}

	private String getBaseType() {

		final String _sourceMultiplicity = getSourceMultiplicity();
		final String _targetMultiplicity = getTargetMultiplicity();
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

	@Override
	public void resolveCascadingEnums(final JsonSchema.Cascade delete, final JsonSchema.Cascade autoCreate) throws FrameworkException {

		if (delete != null) {

			switch (delete) {

				case sourceToTarget:
					setCascadingDeleteFlag(Long.valueOf(Relation.SOURCE_TO_TARGET));
					break;

				case targetToSource:
					setCascadingDeleteFlag(Long.valueOf(Relation.TARGET_TO_SOURCE));
					break;

				case always:
					setCascadingDeleteFlag(Long.valueOf(Relation.ALWAYS));
					break;

				case constraintBased:
					setCascadingDeleteFlag(Long.valueOf(Relation.CONSTRAINT_BASED));
					break;
			}
		}

		if (autoCreate != null) {

			switch (autoCreate) {

				case sourceToTarget:
					setAutocreationFlag(Long.valueOf(Relation.SOURCE_TO_TARGET));
					break;

				case targetToSource:
					setAutocreationFlag(Long.valueOf(Relation.TARGET_TO_SOURCE));
					break;

				case always:
					setAutocreationFlag(Long.valueOf(Relation.ALWAYS));
					break;

				case constraintBased:
					setAutocreationFlag(Long.valueOf(Relation.CONSTRAINT_BASED));
					break;
			}
		}

	}

	@Override
	public void setAutocreationFlag(final Long flag) throws FrameworkException {
		wrappedObject.setProperty(traits.key("autocreationFlag"), flag);
	}

	@Override
	public void setCascadingDeleteFlag(final Long flag) throws FrameworkException {
		wrappedObject.setProperty(traits.key("cascadingDeleteFlag"), flag);
	}

	@Override
	public Map<String, Object> resolveCascadingFlags() {

		final Long cascadingDelete        = getCascadingDeleteFlag();
		final Long autoCreate             = getAutocreationFlag();
		final Map<String, Object> cascade = new TreeMap<>();

		if (cascadingDelete != null) {

			switch (cascadingDelete.intValue()) {

				case Relation.SOURCE_TO_TARGET:
					cascade.put(JsonSchema.KEY_DELETE, JsonSchema.Cascade.sourceToTarget.name());
					break;

				case Relation.TARGET_TO_SOURCE:
					cascade.put(JsonSchema.KEY_DELETE, JsonSchema.Cascade.targetToSource.name());
					break;

				case Relation.ALWAYS:
					cascade.put(JsonSchema.KEY_DELETE, JsonSchema.Cascade.always.name());
					break;

				case Relation.CONSTRAINT_BASED:
					cascade.put(JsonSchema.KEY_DELETE, JsonSchema.Cascade.constraintBased.name());
					break;
			}
		}

		if (autoCreate != null) {

			switch (autoCreate.intValue()) {

				case Relation.SOURCE_TO_TARGET:
					cascade.put(JsonSchema.KEY_CREATE, JsonSchema.Cascade.sourceToTarget.name());
					break;

				case Relation.TARGET_TO_SOURCE:
					cascade.put(JsonSchema.KEY_CREATE, JsonSchema.Cascade.targetToSource.name());
					break;

				case Relation.ALWAYS:
					cascade.put(JsonSchema.KEY_CREATE, JsonSchema.Cascade.always.name());
					break;

				case Relation.CONSTRAINT_BASED:
					cascade.put(JsonSchema.KEY_CREATE, JsonSchema.Cascade.constraintBased.name());
					break;
			}
		}

		return cascade;
	}

	@Override
	public Long getAutocreationFlag() {
		return wrappedObject.getProperty(traits.key("autocreationFlag"));
	}

	@Override
	public Long getCascadingDeleteFlag() {
		return wrappedObject.getProperty(traits.key("cascadingDeleteFlag"));
	}

	@Override
	public String getPreviousSourceJsonName() {
		return wrappedObject.getProperty(traits.key("oldSourceJsonName"));
	}

	@Override
	public String getPreviousTargetJsonName() {
		return wrappedObject.getProperty(traits.key("oldTargetJsonName"));
	}

	@Override
	public PropagationDirection getPermissionPropagation() {

		final String value = wrappedObject.getProperty(traits.key("permissionPropagation"));
		if (value != null) {

			return PropagationDirection.valueOf(value);
		}

		return null;
	}

	@Override
	public PropagationMode getReadPropagation() {

		final String value = wrappedObject.getProperty(traits.key("readPropagation"));
		if (value != null) {

			return PropagationMode.valueOf(value);
		}

		return null;
	}

	@Override
	public PropagationMode getWritePropagation() {

		final String value = wrappedObject.getProperty(traits.key("writePropagation"));
		if (value != null) {

			return PropagationMode.valueOf(value);
		}

		return null;
	}

	@Override
	public PropagationMode getDeletePropagation() {

		final String value = wrappedObject.getProperty(traits.key("deletePropagation"));
		if (value != null) {

			return PropagationMode.valueOf(value);
		}

		return null;
	}

	@Override
	public PropagationMode getAccessControlPropagation() {

		final String value = wrappedObject.getProperty(traits.key("accessControlPropagation"));
		if (value != null) {

			return PropagationMode.valueOf(value);
		}

		return null;
	}

	@Override
	public String getPropertyMask() {
		return wrappedObject.getProperty(traits.key("propertyMask"));
	}

	@Override
	public void setSourceNode(final SchemaNode sourceSchemaNode) throws FrameworkException {
		wrappedObject.setProperty(traits.key("sourceNode"), sourceSchemaNode.getWrappedNode());
	}

	@Override
	public void setTargetNode(final SchemaNode targetSchemaNode) throws FrameworkException {
		wrappedObject.setProperty(traits.key("targetNode"), targetSchemaNode.getWrappedNode());
	}

	@Override
	public void setSourceType(final String sourceType) throws FrameworkException {
		wrappedObject.setProperty(traits.key("sourceType"), sourceType);
	}

	@Override
	public void setTargetType(final String targetType) throws FrameworkException {
		wrappedObject.setProperty(traits.key("targetType"), targetType);
	}

	@Override
	public TraitDefinition[] getTraitDefinitions() {

		final List<TraitDefinition> definitions = new ArrayList<>();

		definitions.add(new DynamicRelationshipTraitDefinition(this));

		return definitions.toArray(new TraitDefinition[0]);
	}

	@Override
	public PropertyKey createKey(final SchemaNode entity, final boolean outgoing) throws FrameworkException {

		final String _sourceMultiplicity  = getSourceMultiplicity();
		final String _targetMultiplicity  = getTargetMultiplicity();
		final String _sourceNotion        = getSourceNotion();
		final String _targetNotion        = getTargetNotion();
		final String _sourceType          = getSchemaNodeSourceType();
		final String _targetType          = getSchemaNodeTargetType();
		final String _className           = getClassName();
		final String _propertyName        = SchemaHelper.cleanPropertyName(getPropertyName(new LinkedHashSet<>(), outgoing));

		if (outgoing) {

			if ("1".equals(_targetMultiplicity)) {

				return new EndNode(_propertyName, _className, getNotion(_sourceType, _targetNotion)).dynamic();

			} else {

				return new EndNodes(_propertyName, _className, getNotion(_sourceType, _targetNotion)).dynamic();
			}

		} else {

			if ("1".equals(_sourceMultiplicity)) {

				return new StartNode(_propertyName, _className, getNotion(_targetType, _sourceNotion)).dynamic();

			} else {

				return new StartNodes(_propertyName, _className, getNotion(_targetType, _sourceNotion)).dynamic();
			}
		}
	}

	@Override
	public List<IsValid> createValidators(SchemaNode entity) throws FrameworkException {
		return List.of();
	}

	// ----- private methods -----
	/*
	private void formatPermissionPropagation(final SourceFile buf) {

		if (!PropagationDirection.None.equals(getPermissionPropagation())) {

			buf.line(this, "@Override");
			buf.begin(this, "public PropagationDirection getPropagationDirection() {");
			buf.line(this, "return PropagationDirection.").append(wrappedObject.getProperty(permissionPropagation)).append(";");
			buf.end();


			buf.line(this, "@Override");
			buf.begin(this, "public PropagationMode getReadPropagation() {");
			buf.line(this, "return PropagationMode.").append(wrappedObject.getProperty(readPropagation)).append(";");
			buf.end();


			buf.line(this, "@Override");
			buf.begin(this, "public PropagationMode getWritePropagation() {");
			buf.line(this, "return PropagationMode.").append(wrappedObject.getProperty(writePropagation)).append(";");
			buf.end();


			buf.line(this, "@Override");
			buf.begin(this, "public PropagationMode getDeletePropagation() {");
			buf.line(this, "return PropagationMode.").append(wrappedObject.getProperty(deletePropagation)).append(";");
			buf.end();


			buf.line(this, "@Override");
			buf.begin(this, "public PropagationMode getAccessControlPropagation() {");
			buf.line(this, "return PropagationMode.").append(wrappedObject.getProperty(accessControlPropagation)).append(";");
			buf.end();


			buf.line(this, "@Override");
			buf.begin(this, "public String getDeltaProperties() {");
			final String _propertyMask = wrappedObject.getProperty(propertyMask);
			if (_propertyMask != null) {

				buf.line(this, "return \"").append(_propertyMask).append("\";");

			} else {

				buf.line(this, "return null;");
			}

			buf.end();
		}
	}
	*/

	// ----- nested classes -----
	private static class KeyMatcher implements Predicate<String> {

		@Override
		public boolean accept(final String t) {

			if (ValidKeyPattern.matcher(t).matches()) {
				return true;
			}

			logger.warn("Invalid key name {} for notion.", t);

			return false;
		}
	}
}
