/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaRelationshipNodeTraitDefinition;
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

		final NodeInterface node = wrappedObject.getProperty(traits.key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY));
		if (node != null) {

			return node.as(SchemaNode.class);
		}

		return null;
	}

	@Override
	public SchemaNode getTargetNode() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY));
		if (node != null) {

			return node.as(SchemaNode.class);
		}

		return null;
	}

	// ----- interface Schema -----
	@Override
	public String getClassName() {

		String name = wrappedObject.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
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
				wrappedObject.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

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
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_NOTION_PROPERTY));
	}

	@Override
	public String getTargetNotion() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_NOTION_PROPERTY));
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
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.PREVIOUS_SOURCE_JSON_NAME_PROPERTY), propertyName);
	}

	@Override
	public void setPreviousTargetJsonName(final String propertyName) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.PREVIOUS_TARGET_JSON_NAME_PROPERTY), propertyName);
	}

	@Override
	public String getSourceMultiplicity() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY));
	}

	@Override
	public String getTargetMultiplicity() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY));
	}

	@Override
	public String getSourceJsonName() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_JSON_NAME_PROPERTY));
	}

	@Override
	public String getTargetJsonName() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_JSON_NAME_PROPERTY));
	}

	@Override
	public String getSourceType() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_TYPE_PROPERTY));
	}

	@Override
	public String getTargetType() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_TYPE_PROPERTY));
	}

	// ----- public methods -----
	@Override public String getSchemaNodeSourceType() {

		final SchemaNode sourceNode = getSourceNode();
		if (sourceNode != null) {

			return sourceNode.getName();
		}

		return getSourceType();
	}

	@Override
	public String getSchemaNodeTargetType() {

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

	@Override
	public String getInverseResourceSignature() {

		final String _sourceType = getSchemaNodeSourceType();
		final String _targetType = getSchemaNodeTargetType();

		return _targetType + "/" + _sourceType;
	}

	@Override
	public String getRelationshipType() {

		String relType = wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY));
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
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.AUTOCREATION_FLAG_PROPERTY), flag);
	}

	@Override
	public void setCascadingDeleteFlag(final Long flag) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.CASCADING_DELETE_FLAG_PROPERTY), flag);
	}

	@Override
	public void setRelationshipType(final String relType) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY), relType);
	}

	@Override
	public void setSourceMultiplicity(final String sourceMultiplicity) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY), sourceMultiplicity);
	}

	@Override
	public void setTargetMultiplicity(final String targetMultiplicity) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY), targetMultiplicity);
	}

	@Override
	public void setSourceJsonName(final String sourcePropertyName) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_JSON_NAME_PROPERTY), sourcePropertyName);
	}

	@Override
	public void setTargetJsonName(final String targetPropertyName) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_JSON_NAME_PROPERTY), targetPropertyName);
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
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.AUTOCREATION_FLAG_PROPERTY));
	}

	@Override
	public Long getCascadingDeleteFlag() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.CASCADING_DELETE_FLAG_PROPERTY));
	}

	@Override
	public String getPreviousSourceJsonName() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.PREVIOUS_SOURCE_JSON_NAME_PROPERTY));
	}

	@Override
	public String getPreviousTargetJsonName() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.PREVIOUS_TARGET_JSON_NAME_PROPERTY));
	}

	@Override
	public PropagationDirection getPermissionPropagation() {

		final String value = wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.PERMISSION_PROPAGATION_PROPERTY));
		if (value != null) {

			return PropagationDirection.valueOf(value);
		}

		return null;
	}

	@Override
	public PropagationMode getReadPropagation() {

		final String value = wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.READ_PROPAGATION_PROPERTY));
		if (value != null) {

			return PropagationMode.valueOf(value);
		}

		return null;
	}

	@Override
	public PropagationMode getWritePropagation() {

		final String value = wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.WRITE_PROPAGATION_PROPERTY));
		if (value != null) {

			return PropagationMode.valueOf(value);
		}

		return null;
	}

	@Override
	public PropagationMode getDeletePropagation() {

		final String value = wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.DELETE_PROPAGATION_PROPERTY));
		if (value != null) {

			return PropagationMode.valueOf(value);
		}

		return null;
	}

	@Override
	public PropagationMode getAccessControlPropagation() {

		final String value = wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.ACCESS_CONTROL_PROPAGATION_PROPERTY));
		if (value != null) {

			return PropagationMode.valueOf(value);
		}

		return null;
	}

	@Override
	public String getPropertyMask() {
		return wrappedObject.getProperty(traits.key(SchemaRelationshipNodeTraitDefinition.PROPERTY_MASK_PROPERTY));
	}

	@Override
	public void setSourceNode(final SchemaNode sourceSchemaNode) throws FrameworkException {
		wrappedObject.setProperty(traits.key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY), sourceSchemaNode);
	}

	@Override
	public void setTargetNode(final SchemaNode targetSchemaNode) throws FrameworkException {
		wrappedObject.setProperty(traits.key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY), targetSchemaNode);
	}

	@Override
	public void setSourceType(final String sourceType) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_TYPE_PROPERTY), sourceType);
	}

	@Override
	public void setTargetType(final String targetType) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_TYPE_PROPERTY), targetType);
	}

	@Override
	public TraitDefinition[] getTraitDefinitions(final TraitsInstance traitsInstance) {

		final List<TraitDefinition> definitions = new ArrayList<>();

		definitions.add(new DynamicRelationshipTraitDefinition(traitsInstance, this));

		return definitions.toArray(new TraitDefinition[0]);
	}

	@Override
	public PropertyKey createKey(final TraitsInstance traitsInstance, final SchemaNode entity, final boolean outgoing) throws FrameworkException {

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

				return new EndNode(traitsInstance, _propertyName, _className, getNotion(_sourceType, _targetNotion)).dynamic();

			} else {

				return new EndNodes(traitsInstance, _propertyName, _className, getNotion(_sourceType, _targetNotion)).dynamic();
			}

		} else {

			if ("1".equals(_sourceMultiplicity)) {

				return new StartNode(traitsInstance, _propertyName, _className, getNotion(_targetType, _sourceNotion)).dynamic();

			} else {

				return new StartNodes(traitsInstance, _propertyName, _className, getNotion(_targetType, _sourceNotion)).dynamic();
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
