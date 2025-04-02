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
package org.structr.core.traits.definitions;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.SemanticErrorToken;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.core.traits.wrappers.SchemaRelationshipNodeTraitWrapper;
import org.structr.schema.ReloadSchema;
import org.structr.schema.SchemaHelper;

import java.util.*;

public class SchemaRelationshipNodeTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Logger logger                           = LoggerFactory.getLogger(SchemaRelationshipNodeTraitDefinition.class);
	private static final String SchemaRemoteAttributeNamePattern = "[a-zA-Z_][a-zA-Z0-9_]*";

	public static final String SOURCE_TYPE_PROPERTY                = "sourceType";
	public static final String TARGET_TYPE_PROPERTY                = "targetType";
	public static final String NAME_PROPERTY                       = "name";					// FIXME? Why does this type define "name"? (without adding a constraint or something like that)
	public static final String RELATIONSHIP_TYPE_PROPERTY          = "relationshipType";
	public static final String SOURCE_MULTIPLICITY_PROPERTY        = "sourceMultiplicity";
	public static final String TARGET_MULTIPLICITY_PROPERTY        = "targetMultiplicity";
	public static final String SOURCE_NOTION_PROPERTY              = "sourceNotion";
	public static final String TARGET_NOTION_PROPERTY              = "targetNotion";
	public static final String SOURCE_JSON_NAME_PROPERTY           = "sourceJsonName";
	public static final String TARGET_JSON_NAME_PROPERTY           = "targetJsonName";
	public static final String PREVIOUS_SOURCE_JSON_NAME_PROPERTY  = "oldSourceJsonName";
	public static final String PREVIOUS_TARGET_JSON_NAME_PROPERTY  = "oldTargetJsonName";
	public static final String CASCADING_DELETE_FLAG_PROPERTY      = "cascadingDeleteFlag";
	public static final String AUTOCREATION_FLAG_PROPERTY          = "autocreationFlag";
	public static final String IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY = "isPartOfBuiltInSchema";
	public static final String PERMISSION_PROPAGATION_PROPERTY     = "permissionPropagation";
	public static final String READ_PROPAGATION_PROPERTY           = "readPropagation";
	public static final String WRITE_PROPAGATION_PROPERTY          = "writePropagation";
	public static final String DELETE_PROPAGATION_PROPERTY         = "deletePropagation";
	public static final String ACCESS_CONTROL_PROPAGATION_PROPERTY = "accessControlPropagation";
	public static final String PROPERTY_MASK_PROPERTY              = "propertyMask";

	public SchemaRelationshipNodeTraitDefinition() {
		super(StructrTraits.SCHEMA_RELATIONSHIP_NODE);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {
				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Traits traits                         = obj.getTraits();
					final PropertyKey<NodeInterface> sourceNode = traits.key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY);
					final PropertyKey<NodeInterface> targetNode = traits.key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY);
					final PropertyKey<String> sourceType        = traits.key(SOURCE_TYPE_PROPERTY);
					final PropertyKey<String> targetType        = traits.key(TARGET_TYPE_PROPERTY);
					final PropertyKey<String> sourceJsonName    = traits.key(SOURCE_JSON_NAME_PROPERTY);
					final PropertyKey<String> targetJsonName    = traits.key(TARGET_JSON_NAME_PROPERTY);
					final PropertyKey<String> relationshipType  = traits.key(RELATIONSHIP_TYPE_PROPERTY);
					boolean valid                               = true;

					valid &= (obj.getProperty(sourceJsonName) == null || ValidationHelper.isValidStringMatchingRegex(obj, sourceJsonName, SchemaRemoteAttributeNamePattern, errorBuffer));
					valid &= (obj.getProperty(targetJsonName) == null || ValidationHelper.isValidStringMatchingRegex(obj, targetJsonName, SchemaRemoteAttributeNamePattern, errorBuffer));
					valid &= ValidationHelper.isValidStringNotBlank(obj, relationshipType, errorBuffer);

					// source and target node can be type names
					valid &= (ValidationHelper.isValidPropertyNotNull(obj, sourceNode, errorBuffer) || ValidationHelper.isValidStringNotBlank(obj, sourceType, errorBuffer));
					valid &= (ValidationHelper.isValidPropertyNotNull(obj, targetNode, errorBuffer) || ValidationHelper.isValidStringNotBlank(obj, targetType, errorBuffer));

					if (valid) {

						// clear error buffer so the schema build doesn't fail because of the above check
						errorBuffer.getErrorTokens().clear();

						// only if we are valid up to here, test for relationship uniqueness
						valid &= isRelationshipDefinitionUnique(obj.as(SchemaRelationshipNode.class), errorBuffer);
					}

					return valid;
				}
			},

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final Traits traits                              = obj.getTraits();
					final PropertyKey<String> previousSourceJsonName = traits.key(PREVIOUS_SOURCE_JSON_NAME_PROPERTY);
					final PropertyKey<String> previousTargetJsonName = traits.key(PREVIOUS_TARGET_JSON_NAME_PROPERTY);
					final PropertyKey<String> sourceJsonName         = traits.key(SOURCE_JSON_NAME_PROPERTY);
					final PropertyKey<String> targetJsonName         = traits.key(TARGET_JSON_NAME_PROPERTY);
					final PropertyMap map                            = new PropertyMap();

					// store old property names
					map.put(previousSourceJsonName, obj.getProperty(sourceJsonName));
					map.put(previousTargetJsonName, obj.getProperty(targetJsonName));

					obj.setProperties(securityContext, map);

					// register transaction postprocessing that recreates the schema information
					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(false));
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final SchemaRelationshipNode node                = obj.as(SchemaRelationshipNode.class);
					final Traits traits                              = obj.getTraits();
					final PropertyKey<String> previousSourceJsonName = traits.key(PREVIOUS_SOURCE_JSON_NAME_PROPERTY);
					final PropertyKey<String> previousTargetJsonName = traits.key(PREVIOUS_TARGET_JSON_NAME_PROPERTY);
					final PropertyKey<String> sourceJsonName         = traits.key(SOURCE_JSON_NAME_PROPERTY);
					final PropertyKey<String> targetJsonName         = traits.key(TARGET_JSON_NAME_PROPERTY);
					final PropertyMap map                            = new PropertyMap();

					checkClassName(node);
					checkAndRenameSourceAndTargetJsonNames(node);

					// store old property names
					map.put(previousSourceJsonName, obj.getProperty(sourceJsonName));
					map.put(previousTargetJsonName, obj.getProperty(targetJsonName));

					obj.setProperties(securityContext, map);

					// register transaction postprocessing that recreates the schema information
					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(false));
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

					// register transaction postprocessing that recreates the schema information
					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnNodeDeletion.class,
			new OnNodeDeletion() {

				@Override
				public void onNodeDeletion(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {

					try {

						removeSourceAndTargetJsonNames(nodeInterface.as(SchemaRelationshipNode.class));

					} catch (Throwable t) {

						// this method must not prevent
						// the deletion of a node
						logger.warn("", t);
					}
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of(
			SchemaRelationshipNode.class, (traits, node) -> new SchemaRelationshipNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NAME_PROPERTY, RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY, RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY,
					SOURCE_TYPE_PROPERTY, TARGET_TYPE_PROPERTY, SOURCE_MULTIPLICITY_PROPERTY, TARGET_MULTIPLICITY_PROPERTY, SOURCE_NOTION_PROPERTY, TARGET_NOTION_PROPERTY, RELATIONSHIP_TYPE_PROPERTY,
					SOURCE_JSON_NAME_PROPERTY, TARGET_JSON_NAME_PROPERTY, CASCADING_DELETE_FLAG_PROPERTY, AUTOCREATION_FLAG_PROPERTY, PREVIOUS_SOURCE_JSON_NAME_PROPERTY, PREVIOUS_TARGET_JSON_NAME_PROPERTY,
					PERMISSION_PROPAGATION_PROPERTY, READ_PROPAGATION_PROPERTY, WRITE_PROPAGATION_PROPERTY, DELETE_PROPAGATION_PROPERTY, ACCESS_CONTROL_PROPAGATION_PROPERTY, PROPERTY_MASK_PROPERTY, IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NAME_PROPERTY, RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY, RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY,
					SOURCE_TYPE_PROPERTY, TARGET_TYPE_PROPERTY, SOURCE_MULTIPLICITY_PROPERTY, TARGET_MULTIPLICITY_PROPERTY, SOURCE_NOTION_PROPERTY, TARGET_NOTION_PROPERTY, RELATIONSHIP_TYPE_PROPERTY,
					SOURCE_JSON_NAME_PROPERTY, TARGET_JSON_NAME_PROPERTY, CASCADING_DELETE_FLAG_PROPERTY, AUTOCREATION_FLAG_PROPERTY, PREVIOUS_SOURCE_JSON_NAME_PROPERTY, PREVIOUS_TARGET_JSON_NAME_PROPERTY,
					PERMISSION_PROPAGATION_PROPERTY, READ_PROPAGATION_PROPERTY, WRITE_PROPAGATION_PROPERTY, DELETE_PROPAGATION_PROPERTY, ACCESS_CONTROL_PROPAGATION_PROPERTY, PROPERTY_MASK_PROPERTY, IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY
			),
			"export",
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY, RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY,
					SOURCE_TYPE_PROPERTY, TARGET_TYPE_PROPERTY, SOURCE_MULTIPLICITY_PROPERTY, TARGET_MULTIPLICITY_PROPERTY, SOURCE_NOTION_PROPERTY, TARGET_NOTION_PROPERTY, RELATIONSHIP_TYPE_PROPERTY,
					SOURCE_JSON_NAME_PROPERTY, TARGET_JSON_NAME_PROPERTY, CASCADING_DELETE_FLAG_PROPERTY, AUTOCREATION_FLAG_PROPERTY,
					PERMISSION_PROPAGATION_PROPERTY, PROPERTY_MASK_PROPERTY, IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY
			),
			"schema",
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NAME_PROPERTY, RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY, RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY,
					SOURCE_TYPE_PROPERTY, TARGET_TYPE_PROPERTY, SOURCE_MULTIPLICITY_PROPERTY, TARGET_MULTIPLICITY_PROPERTY, SOURCE_NOTION_PROPERTY, TARGET_NOTION_PROPERTY, RELATIONSHIP_TYPE_PROPERTY,
					SOURCE_JSON_NAME_PROPERTY, TARGET_JSON_NAME_PROPERTY, CASCADING_DELETE_FLAG_PROPERTY, AUTOCREATION_FLAG_PROPERTY, PREVIOUS_SOURCE_JSON_NAME_PROPERTY, PREVIOUS_TARGET_JSON_NAME_PROPERTY,
					PERMISSION_PROPAGATION_PROPERTY, READ_PROPAGATION_PROPERTY, WRITE_PROPAGATION_PROPERTY, DELETE_PROPAGATION_PROPERTY, ACCESS_CONTROL_PROPAGATION_PROPERTY, PROPERTY_MASK_PROPERTY, IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY
			)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> sourceNode          = new StartNode(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_SOURCE_NODE);
		final Property<NodeInterface> targetNode          = new EndNode(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_TARGET_NODE);
		final Property<String>     sourceId               = new EntityNotionProperty<>(RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_NODE, RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY, StructrTraits.SCHEMA_NODE, new PropertyNotion(GraphObjectTraitDefinition.ID_PROPERTY));
		final Property<String>     targetId               = new EntityNotionProperty<>(RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_NODE, RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY, StructrTraits.SCHEMA_NODE, new PropertyNotion(GraphObjectTraitDefinition.ID_PROPERTY));
		final Property<String>     sourceType             = new StringProperty(SOURCE_TYPE_PROPERTY);
		final Property<String>     targetType             = new StringProperty(TARGET_TYPE_PROPERTY);
		final Property<String>     name                   = new StringProperty(NAME_PROPERTY).indexed();
		final Property<String>     relationshipType       = new StringProperty(RELATIONSHIP_TYPE_PROPERTY).indexed();
		final Property<String>     sourceMultiplicity     = new StringProperty(SOURCE_MULTIPLICITY_PROPERTY);
		final Property<String>     targetMultiplicity     = new StringProperty(TARGET_MULTIPLICITY_PROPERTY);
		final Property<String>     sourceNotion           = new StringProperty(SOURCE_NOTION_PROPERTY);
		final Property<String>     targetNotion           = new StringProperty(TARGET_NOTION_PROPERTY);
		final Property<String>     sourceJsonName         = new StringProperty(SOURCE_JSON_NAME_PROPERTY).indexed();
		final Property<String>     targetJsonName         = new StringProperty(TARGET_JSON_NAME_PROPERTY).indexed();
		final Property<String>     previousSourceJsonName = new StringProperty(PREVIOUS_SOURCE_JSON_NAME_PROPERTY).indexed();
		final Property<String>     previousTargetJsonName = new StringProperty(PREVIOUS_TARGET_JSON_NAME_PROPERTY).indexed();
		final Property<Long>       cascadingDeleteFlag    = new LongProperty(CASCADING_DELETE_FLAG_PROPERTY);
		final Property<Long>       autocreationFlag       = new LongProperty(AUTOCREATION_FLAG_PROPERTY);
		final Property<Boolean>    isPartOfBuiltInSchema  = new BooleanProperty(IS_PART_OF_BUILT_IN_SCHEMA_PROPERTY);

		// permission propagation via domain relationships
		final Property<String> permissionPropagation    = new EnumProperty(PERMISSION_PROPAGATION_PROPERTY,    PropagationDirection.class).defaultValue(PropagationDirection.None.name());
		final Property<String> readPropagation          = new EnumProperty(READ_PROPAGATION_PROPERTY,          PropagationMode.class).defaultValue(PropagationMode.Remove.name());
		final Property<String> writePropagation         = new EnumProperty(WRITE_PROPAGATION_PROPERTY,         PropagationMode.class).defaultValue(PropagationMode.Remove.name());
		final Property<String> deletePropagation        = new EnumProperty(DELETE_PROPAGATION_PROPERTY,        PropagationMode.class).defaultValue(PropagationMode.Remove.name());
		final Property<String> accessControlPropagation = new EnumProperty(ACCESS_CONTROL_PROPAGATION_PROPERTY, PropagationMode.class).defaultValue(PropagationMode.Remove.name());
		final Property<String> propertyMask             = new StringProperty(PROPERTY_MASK_PROPERTY);

		return newSet(
			sourceNode,
			targetNode,
			sourceId,
			targetId,
			sourceType,
			targetType,
			name,
			relationshipType,
			sourceMultiplicity,
			targetMultiplicity,
			sourceNotion,
			targetNotion,
			sourceJsonName,
			targetJsonName,
			previousSourceJsonName,
			previousTargetJsonName,
			cascadingDeleteFlag,
			autocreationFlag,
			isPartOfBuiltInSchema,
			permissionPropagation,
			readPropagation,
			writePropagation,
			deletePropagation,
			accessControlPropagation,
			propertyMask
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}


	// ----- private methods -----
	private boolean isRelationshipDefinitionUnique(final SchemaRelationshipNode node, final ErrorBuffer errorBuffer) {

		final Traits traits                            = node.getTraits();
		final PropertyKey<NodeInterface> sourceNodeKey = traits.key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY);
		final PropertyKey<NodeInterface> targetNodeKey = traits.key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY);
		final PropertyKey<String> sourceTypeKey        = traits.key(SOURCE_TYPE_PROPERTY);
		final PropertyKey<String> targetTypeKey        = traits.key(TARGET_TYPE_PROPERTY);
		final PropertyKey<String> relTypeKey           = traits.key(RELATIONSHIP_TYPE_PROPERTY);
		boolean allow                                  = true;

		try {

			final SchemaNode sourceNode = node.getSourceNode();
			final SchemaNode targetNode = node.getTargetNode();

			final Query<NodeInterface> query = StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).and(relTypeKey, node.getRelationshipType(), true);

			// source node or static type (string-based)
			if (sourceNode != null) query.and(sourceNodeKey, sourceNode); else query.and(sourceTypeKey, node.getSourceType());
			if (targetNode != null) query.and(targetNodeKey, targetNode); else query.and(targetTypeKey, node.getTargetType());

			for (final NodeInterface exRel : query.getResultStream()) {

				if (!exRel.getUuid().equals(node.getUuid())) {

					allow = false;
				}
			}

			if (!allow) {

				errorBuffer.add(new SemanticErrorToken(node.getType(), RELATIONSHIP_TYPE_PROPERTY, "duplicate_relationship_definition")
					.withDetail("Schema Relationship with same name between source and target node already exists. This is not allowed.")
				);
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return allow;
	}

	private void checkClassName(final SchemaRelationshipNode node) throws FrameworkException {

		final String className             = node.getClassName();
		final String potentialNewClassName = assembleNewClassName(node);

		if (!className.equals(potentialNewClassName)) {

			try {
				node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), potentialNewClassName);

			} catch (FrameworkException fex) {
				logger.warn("Unable to set relationship name to {}.", potentialNewClassName);
			}
		}
	}

	private String assembleNewClassName(final SchemaRelationshipNode node) {

		final String _sourceType = node.getSchemaNodeSourceType();
		final String _targetType = node.getSchemaNodeTargetType();
		final String _relType    = SchemaHelper.cleanPropertyName(node.getRelationshipType());

		return _sourceType + _relType + _targetType;
	}

	private void checkAndRenameSourceAndTargetJsonNames(final SchemaRelationshipNode node) throws FrameworkException {

		final Map<String, NodeInterface> schemaNodes = new LinkedHashMap<>();
		final String _previousSourceJsonName         = node.getPreviousSourceJsonName();
		final String _previousTargetJsonName         = node.getPreviousTargetJsonName();
		final String _currentSourceJsonName          = ((node.getSourceJsonName() != null) ? node.getSourceJsonName() : SchemaRelationshipNode.getPropertyName(node, new LinkedHashSet<>(), false));
		final String _currentTargetJsonName          = ((node.getTargetJsonName() != null) ? node.getTargetJsonName() : SchemaRelationshipNode.getPropertyName(node, new LinkedHashSet<>(), true));
		final SchemaNode _sourceNode                 = node.getSourceNode();
		final SchemaNode _targetNode                 = node.getTargetNode();

		// build schema node map
		StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_NODE).getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

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

	private void removeSourceAndTargetJsonNames(final SchemaRelationshipNode node) throws FrameworkException {

		final SchemaNode _sourceNode        = node.getSourceNode();
		final SchemaNode _targetNode        = node.getTargetNode();
		final String _currentSourceJsonName = ((node.getSourceJsonName() != null) ? node.getSourceJsonName() : SchemaRelationshipNode.getPropertyName(node, new LinkedHashSet<>(), false));
		final String _currentTargetJsonName = ((node.getTargetJsonName() != null) ? node.getTargetJsonName() : SchemaRelationshipNode.getPropertyName(node, new LinkedHashSet<>(), true));

		if (_sourceNode != null) {

			removeNameFromNonGraphProperties(_sourceNode, _currentSourceJsonName);
			removeNameFromNonGraphProperties(_sourceNode, _currentTargetJsonName);

		}

		if (_targetNode != null) {

			removeNameFromNonGraphProperties(_targetNode, _currentSourceJsonName);
			removeNameFromNonGraphProperties(_targetNode, _currentTargetJsonName);

		}

	}

	private void renameNotionPropertyReferences(final Map<String, NodeInterface> schemaNodes, final SchemaNode schemaNode, final String previousValue, final String currentValue) throws FrameworkException {

		final PropertyKey<String> formatKey = Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.FORMAT_PROPERTY);

		// examine properties of other node
		for (final SchemaProperty property : schemaNode.getSchemaProperties()) {

			if (SchemaHelper.Type.Notion.equals(property.getPropertyType()) || SchemaHelper.Type.IdNotion.equals(property.getPropertyType())) {

				// try to rename
				final String basePropertyName = property.getNotionBaseProperty();
				if (basePropertyName.equals(previousValue)) {

					property.setProperty(formatKey, property.getFormat().replace(previousValue, currentValue));
				}
			}
		}
	}

	private void renameNameInNonGraphProperties(final SchemaNode schemaNode, final String toRemove, final String newValue) throws FrameworkException {

		if (schemaNode.getTraits().hasKey(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY)) {

			final PropertyKey<String> nonGraphPropertiesKey = schemaNode.getTraits().key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY);

			// examine all views
			for (final SchemaView view : schemaNode.getSchemaViews()) {

				final String nonGraphProperties = view.getProperty(nonGraphPropertiesKey);
				if (nonGraphProperties != null) {

					final ArrayList<String> properties = new ArrayList<>(Arrays.asList(nonGraphProperties.split("[, ]+")));

					final int pos = properties.indexOf(toRemove);
					if (pos != -1) {
						properties.set(pos, newValue);
					}

					view.setProperty(nonGraphPropertiesKey, StringUtils.join(properties, ", "));
				}
			}
		}
	}

	private void removeNameFromNonGraphProperties(final SchemaNode schemaNode, final String toRemove) throws FrameworkException {

		if (schemaNode.getTraits().hasKey(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY)) {

			final PropertyKey<String> nonGraphPropertiesKey = schemaNode.getTraits().key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY);

			// examine all views
			for (final SchemaView view : schemaNode.getSchemaViews()) {

				final String nonGraphProperties = view.getProperty(nonGraphPropertiesKey);
				if (nonGraphProperties != null) {

					final ArrayList<String> properties = new ArrayList<>(Arrays.asList(nonGraphProperties.split("[, ]+")));

					properties.remove(toRemove);

					view.setProperty(nonGraphPropertiesKey, StringUtils.join(properties, ", "));
				}
			}
		}
	}

	/*
	public static void registerPropagatingRelationshipType(final Class type, final boolean isDynamic) {

		if (isDynamic) {

			dynamicPropagatingRelTypes.add(type);

		} else {

			staticPropagatingRelTypes.add(type);
		}
	}

	public static void clearPropagatingRelationshipTypes() {
		dynamicPropagatingRelTypes.clear();
	}

	public static Set<Class> getPropagatingRelationshipTypes() {
		return SetUtils.union(dynamicPropagatingRelTypes, staticPropagatingRelTypes);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {

		final Set<PropertyKey> propertyKeys = new LinkedHashSet<>(Iterables.toList(super.getPropertyKeys(propertyView)));

		// add "custom" property keys as String properties
		for (final String key : SchemaHelper.getProperties(getNode())) {

			final PropertyKey newKey = new StringProperty(key);
			newKey.setDeclaringClass(getClass());

			propertyKeys.add(newKey);
		}

		return propertyKeys;
	}

	// ----- nested classes -----
	private static class KeyMatcher implements Predicate<String> {

		@Override
		public boolean accept(String t) {

			if (ValidKeyPattern.matcher(t).matches()) {
				return true;
			}

			logger.warn("Invalid key name {} for notion.", t);

			return false;
		}
	}
	*/
}
