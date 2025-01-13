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
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.*;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.TraitDefinition;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.DynamicNodeTraitDefinition;
import org.structr.schema.DynamicRelationshipTraitDefinition;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SourceFile;

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

			if (_sourceType.contains(".")) {

				// remove FQCN from class name (if present)
				buf.append(StringUtils.substringAfterLast(_sourceType, "."));

			} else {

				buf.append(_sourceType);
			}

			buf.append(_relType);

			if (_targetType.contains(".")) {

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

	@Override
	public String getMultiplicity(final String propertyNameToCheck) {
		return null;
	}

	@Override
	public String getRelatedType(final String propertyNameToCheck) {
		return null;
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
		return null;
	}

	@Override
	public String getTargetNotion() {
		return null;
	}

	@Override
	public String getMultiplicity(final boolean outgoing) {

		if (outgoing) {

			return getTargetMultiplicity();

		} else {

			return getSourceMultiplicity();
		}
	}

	@Override
	public String getPropertyName(final String relatedClassName, final Set<String> existingPropertyNames, final boolean outgoing) {

		final String relationshipTypeName = getRelationshipType().toLowerCase();
		final String _sourceType          = getSchemaNodeSourceType();
		final String _targetType          = getSchemaNodeTargetType();
		final String _targetJsonName      = getTargetJsonName();
		final String _targetMultiplicity  = getTargetMultiplicity();
		final String _sourceJsonName      = getSourceJsonName();
		final String _sourceMultiplicity  = getSourceMultiplicity();

		final String propertyName = SchemaProperty.getPropertyName(relatedClassName, existingPropertyNames, outgoing, relationshipTypeName, _sourceType, _targetType, _targetJsonName, _targetMultiplicity, _sourceJsonName, _sourceMultiplicity);

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
		return wrappedObject.getProperty(traits.key("targetJsonName"));
	}

	@Override
	public String getTargetJsonName() {
		return wrappedObject.getProperty(traits.key("targetJsonName"));
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
			src.line(this, "SchemaRelationshipNode.registerPropagatingRelationshipType(").append(_className).append(".class, true);");
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

	@Override
	public String getSourceType() {
		return null;
	}

	@Override
	public String getTargetType() {
		return null;
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

			final String _sourceType = getSchemaNodeSourceType().toUpperCase();
			final String _targetType = getSchemaNodeTargetType().toUpperCase();

			relType = _sourceType + "_" + _targetType;
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

						return new PropertyNotion(getNotionKey(_className, key), create);

					} else {

						logger.warn("Invalid key name {} for notion.", key);
					}

				} else {

					final Set<PropertyKey> keySet = new LinkedHashSet<>();

					// use only matching keys
					for (final Iterator<String> it = Iterables.filter(new KeyMatcher(), keys).iterator(); it.hasNext();) {

						buf.append(getNotionKey(_className, it.next()));
					}

					return new PropertySetNotion<>(keySet);
				}
			}
		}

		return null;
	}

	private PropertyKey getNotionKey(final String _className, final String key) {
		return Traits.of(_className).key(key);
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
		return null;
	}

	@Override
	public Long getCascadingDeleteFlag() {
		return null;
	}

	@Override
	public Iterable<SchemaGrant> getSchemaGrants() {
		return Collections.emptyList();
	}

	@Override
	public String getPreviousSourceJsonName() {
		return wrappedObject.getProperty(traits.key("oldSourceJsonName"));
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
	public String getPreviousTargetJsonName() {
		return wrappedObject.getProperty(traits.key("oldTargetJsonName"));
	}

	@Override
	public TraitDefinition[] getTraitDefinitions() {

		final List<TraitDefinition> definitions = new ArrayList<>();

		definitions.add(new DynamicRelationshipTraitDefinition(this));

		return definitions.toArray(new TraitDefinition[0]);
	}

	@Override
	public PropertyKey createKey(final SchemaNode entity, final boolean outgoing) throws FrameworkException {

		final String _sourceMultiplicity  = getMultiplicity(false);
		final String _targetMultiplicity  = getMultiplicity(true);
		final String _sourceNotion        = getSourceNotion();
		final String _targetNotion        = getTargetNotion();
		final String _sourceType          = getSchemaNodeSourceType();
		final String _targetType          = getSchemaNodeTargetType();
		final String _className           = getClassName();
		final String _propertyName        = SchemaHelper.cleanPropertyName(getPropertyName(entity.getName(), new LinkedHashSet<>(), outgoing));

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
	private void formatRelationshipFlags(final SourceFile src) {

		Long cascadingDelete = getCascadingDeleteFlag();
		if (cascadingDelete != null) {

			src.line(this, "@Override");
			src.begin(this, "public int getCascadingDeleteFlag() {");

			switch (cascadingDelete.intValue()) {

				case Relation.ALWAYS :
					src.line(this, "return Relation.ALWAYS;");
					break;

				case Relation.CONSTRAINT_BASED :
					src.line(this, "return Relation.CONSTRAINT_BASED;");
					break;

				case Relation.SOURCE_TO_TARGET :
					src.line(this, "return Relation.SOURCE_TO_TARGET;");
					break;

				case Relation.TARGET_TO_SOURCE :
					src.line(this, "return Relation.TARGET_TO_SOURCE;");
					break;

				case Relation.NONE :

				default :
					src.line(this, "return Relation.NONE;");

			}

			src.end();
		}

		Long autocreate = getAutocreationFlag();
		if (autocreate != null) {

			src.line(this, "@Override");
			src.begin(this, "public int getAutocreationFlag() {");

			switch (autocreate.intValue()) {

				case Relation.ALWAYS :
					src.line(this, "return Relation.ALWAYS;");
					break;

				case Relation.SOURCE_TO_TARGET :
					src.line(this, "return Relation.SOURCE_TO_TARGET;");
					break;

				case Relation.TARGET_TO_SOURCE :
					src.line(this, "return Relation.TARGET_TO_SOURCE;");
					break;

				default :
					src.line(this, "return Relation.NONE;");

			}

			src.end();
		}
	}

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

	private void checkClassName() throws FrameworkException {

		final String className = getClassName();
		final String potentialNewClassName = assembleNewClassName();

		if (!className.equals(potentialNewClassName)) {

			try {
				wrappedObject.setProperty(Traits.nameProperty(), potentialNewClassName);

			} catch (FrameworkException fex) {
				logger.warn("Unable to set relationship name to {}.", potentialNewClassName);
			}
		}
	}

	private String assembleNewClassName() {

		final String _sourceType = getSchemaNodeSourceType();
		final String _targetType = getSchemaNodeTargetType();
		final String _relType    = SchemaHelper.cleanPropertyName(getRelationshipType());

		return _sourceType + _relType + _targetType;
	}

	private void checkAndRenameSourceAndTargetJsonNames() throws FrameworkException {

		final Map<String, NodeInterface> schemaNodes = new LinkedHashMap<>();
		final String _previousSourceJsonName         = getPreviousSourceJsonName();
		final String _previousTargetJsonName         = getPreviousTargetJsonName();
		final String _currentSourceJsonName          = ((getSourceJsonName() != null) ? getSourceJsonName() : getPropertyName(getSchemaNodeTargetType(), new LinkedHashSet<>(), false));
		final String _currentTargetJsonName          = ((getTargetJsonName() != null) ? getTargetJsonName() : getPropertyName(getSchemaNodeSourceType(), new LinkedHashSet<>(), true));
		final NodeInterface _sourceNode              = getSourceNode();
		final NodeInterface _targetNode              = getTargetNode();

		// build schema node map
		StructrApp.getInstance().nodeQuery("SchemaNode").getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

		if (_previousSourceJsonName != null && _currentSourceJsonName != null && !_currentSourceJsonName.equals(_previousSourceJsonName)) {

			renameNameInNonGraphProperties(_targetNode, _previousSourceJsonName, _currentSourceJsonName);

			renameNotionPropertyReferences(schemaNodes, _sourceNode, _previousSourceJsonName, _currentSourceJsonName);
			renameNotionPropertyReferences(schemaNodes, _targetNode, _previousSourceJsonName, _currentSourceJsonName);
		}

		if (_previousTargetJsonName != null && _currentTargetJsonName != null && !_currentTargetJsonName.equals(_previousTargetJsonName)) {

			renameNameInNonGraphProperties(_sourceNode, _previousTargetJsonName, _currentTargetJsonName);

			renameNotionPropertyReferences(schemaNodes, _sourceNode, _previousTargetJsonName, _currentTargetJsonName);
			renameNotionPropertyReferences(schemaNodes, _targetNode, _previousTargetJsonName, _currentTargetJsonName);
		}
	}

	private void removeSourceAndTargetJsonNames() throws FrameworkException {

		final NodeInterface _sourceNode      = getSourceNode();
		final NodeInterface _targetNode      = getTargetNode();
		final String _currentSourceJsonName  = ((getSourceJsonName() != null) ? getSourceJsonName() : getPropertyName(getSchemaNodeTargetType(), new LinkedHashSet<>(), false));
		final String _currentTargetJsonName  = ((getTargetJsonName() != null) ? getTargetJsonName() : getPropertyName(getSchemaNodeSourceType(), new LinkedHashSet<>(), true));

		if (_sourceNode != null) {

			removeNameFromNonGraphProperties(_sourceNode, _currentSourceJsonName);
			removeNameFromNonGraphProperties(_sourceNode, _currentTargetJsonName);

		}

		if (_targetNode != null) {

			removeNameFromNonGraphProperties(_targetNode, _currentSourceJsonName);
			removeNameFromNonGraphProperties(_targetNode, _currentTargetJsonName);

		}

	}

	private void renameNotionPropertyReferences(final Map<String, NodeInterface> schemaNodes, final NodeInterface node, final String previousValue, final String currentValue) throws FrameworkException {

		final SchemaNode schemaNode = node.as(SchemaNode.class);

		// examine properties of other node
		for (final NodeInterface property : schemaNode.getSchemaProperties()) {

			if (SchemaHelper.Type.Notion.equals(property.getPropertyType()) || SchemaHelper.Type.IdNotion.equals(property.getPropertyType())) {

				// try to rename
				final String basePropertyName = property.getNotionBaseProperty(schemaNodes);
				if (basePropertyName.equals(previousValue)) {

					property.setProperty(SchemaProperty.format, property.getFormat().replace(previousValue, currentValue));
				}
			}

		}

	}

	private void renameNameInNonGraphProperties(final NodeInterface node, final String toRemove, final String newValue) throws FrameworkException {

		final SchemaNode schemaNode = node.as(SchemaNode.class);

		// examine all views
		for (final NodeInterface view : schemaNode.getSchemaViews()) {

			final String nonGraphProperties = view.getProperty(SchemaView.nonGraphProperties);
			if (nonGraphProperties != null) {

				final ArrayList<String> properties = new ArrayList<>(Arrays.asList(nonGraphProperties.split("[, ]+")));

				final int pos = properties.indexOf(toRemove);
				if (pos != -1) {
					properties.set(pos, newValue);
				}

				view.setProperty(SchemaView.nonGraphProperties, StringUtils.join(properties, ", "));
			}
		}
	}

	private void removeNameFromNonGraphProperties(final NodeInterface schemaNode, final String toRemove) throws FrameworkException {

		// examine all views
		for (final NodeInterface view : schemaNode.getSchemaViews()) {

			final String nonGraphProperties = view.getProperty(SchemaView.nonGraphProperties);
			if (nonGraphProperties != null) {

				final ArrayList<String> properties = new ArrayList<>(Arrays.asList(nonGraphProperties.split("[, ]+")));

				properties.remove(toRemove);

				view.setProperty(SchemaView.nonGraphProperties, StringUtils.join(properties, ", "));
			}
		}

	}

	private boolean isRelationshipDefinitionUnique(final ErrorBuffer errorBuffer) {

		boolean allow = true;

		try {

			final List<SchemaRelationshipNode> existingRelationships = StructrApp.getInstance().nodeQuery("SchemaRelationshipNode").and(relationshipType, this.getRelationshipType(), true).and(sourceNode, this.getSourceNode()).and(targetNode, this.getTargetNode()).getAsList();

			for (final SchemaRelationshipNode exRel : existingRelationships) {
				if (!exRel.getUuid().equals(this.getUuid())) {
					allow = false;
				}
			}

			if (!allow) {

				errorBuffer.add(new SemanticErrorToken(this.getType(), relationshipType.jsonName(), "duplicate_relationship_definition")
					.withDetail("Schema Relationship with same name between source and target node already exists. This is not allowed.")
				);
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}

		return allow;
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
