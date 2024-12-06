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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SchemaRelationshipNodeTraitDefinition extends AbstractTraitDefinition {

	public static final String schemaRemoteAttributeNamePattern    = "[a-zA-Z_][a-zA-Z0-9_]*";

	private static final Logger logger                              = LoggerFactory.getLogger(SchemaRelationshipNode.class.getName());
	private static final Set<Class> dynamicPropagatingRelTypes      = new HashSet<>();
	private static final Set<Class> staticPropagatingRelTypes       = new HashSet<>();

	private static final Property<NodeInterface> sourceNode          = new StartNode("sourceNode", "SchemaRelationshipSourceNode");
	private static final Property<NodeInterface> targetNode          = new EndNode("targetNode", "SchemaRelationshipTargetNode");
	private static final Property<String>     sourceId               = new EntityNotionProperty<>("sourceId", sourceNode, new PropertyNotion(Traits.idProperty()));
	private static final Property<String>     targetId               = new EntityNotionProperty<>("targetId", targetNode, new PropertyNotion(Traits.idProperty()));
	private static final Property<String>     sourceType             = new StringProperty("sourceType");
	private static final Property<String>     targetType             = new StringProperty("targetType");
	private static final Property<String>     name                   = new StringProperty("name").indexed();
	private static final Property<String>     relationshipType       = new StringProperty("relationshipType").indexed();
	private static final Property<String>     sourceMultiplicity     = new StringProperty("sourceMultiplicity");
	private static final Property<String>     targetMultiplicity     = new StringProperty("targetMultiplicity");
	private static final Property<String>     sourceNotion           = new StringProperty("sourceNotion");
	private static final Property<String>     targetNotion           = new StringProperty("targetNotion");
	private static final Property<String>     sourceJsonName         = new StringProperty("sourceJsonName").indexed();
	private static final Property<String>     targetJsonName         = new StringProperty("targetJsonName").indexed();
	private static final Property<String>     previousSourceJsonName = new StringProperty("oldSourceJsonName").indexed();
	private static final Property<String>     previousTargetJsonName = new StringProperty("oldTargetJsonName").indexed();
	private static final Property<String>     extendsClass           = new StringProperty("extendsClass").indexed();
	private static final Property<Long>       cascadingDeleteFlag    = new LongProperty("cascadingDeleteFlag");
	private static final Property<Long>       autocreationFlag       = new LongProperty("autocreationFlag");
	private static final Property<Boolean>    isPartOfBuiltInSchema  = new BooleanProperty("isPartOfBuiltInSchema");

	// permission propagation via domain relationships
	private static final Property<PropagationDirection>   permissionPropagation = new EnumProperty("permissionPropagation", PropagationDirection.class, PropagationDirection.None);
	private static final Property<PropagationMode> readPropagation              = new EnumProperty<>("readPropagation", PropagationMode.class, PropagationMode.Remove);
	private static final Property<PropagationMode> writePropagation             = new EnumProperty<>("writePropagation", PropagationMode.class, PropagationMode.Remove);
	private static final Property<PropagationMode> deletePropagation            = new EnumProperty<>("deletePropagation", PropagationMode.class, PropagationMode.Remove);
	private static final Property<PropagationMode> accessControlPropagation     = new EnumProperty<>("accessControlPropagation", PropagationMode.class, PropagationMode.Remove);
	private static final Property<String>      propertyMask                     = new StringProperty("propertyMask");

	/*
	public static final View defaultView = new View(SchemaRelationshipNode.class, PropertyView.Public,
		name, sourceId, targetId, sourceType, targetType, sourceMultiplicity, targetMultiplicity, sourceNotion, targetNotion, relationshipType,
		sourceJsonName, targetJsonName, extendsClass, cascadingDeleteFlag, autocreationFlag, previousSourceJsonName, previousTargetJsonName,
		permissionPropagation, readPropagation, writePropagation, deletePropagation, accessControlPropagation, propertyMask, isPartOfBuiltInSchema
	);

	public static final View uiView = new View(SchemaRelationshipNode.class, PropertyView.Ui,
		name, sourceId, targetId, sourceType, targetType, sourceMultiplicity, targetMultiplicity, sourceNotion, targetNotion, relationshipType,
		sourceJsonName, targetJsonName, extendsClass, cascadingDeleteFlag, autocreationFlag, previousSourceJsonName, previousTargetJsonName,
		permissionPropagation, readPropagation, writePropagation, deletePropagation, accessControlPropagation, propertyMask, isPartOfBuiltInSchema
	);

	public static final View exportView = new View(SchemaRelationshipNode.class, "export",
		sourceId, targetId, sourceType, targetType, sourceMultiplicity, targetMultiplicity, sourceNotion, targetNotion, relationshipType,
		sourceJsonName, targetJsonName, extendsClass, cascadingDeleteFlag, autocreationFlag, permissionPropagation,
		propertyMask, isPartOfBuiltInSchema
	);

	public static final View schemaView = new View(SchemaNode.class, "schema",
		name, sourceId, targetId, sourceType, targetType, sourceMultiplicity, targetMultiplicity, sourceNotion, targetNotion, relationshipType,
		sourceJsonName, targetJsonName, extendsClass, cascadingDeleteFlag, autocreationFlag, previousSourceJsonName, previousTargetJsonName,
		permissionPropagation, readPropagation, writePropagation, deletePropagation, accessControlPropagation, propertyMask, isPartOfBuiltInSchema
	);
	*/

	public SchemaRelationshipNodeTraitDefinition() {
		super("SchemaRelationshipNode");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
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
			extendsClass,
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

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= (getProperty(sourceJsonName) == null || ValidationHelper.isValidStringMatchingRegex(this, sourceJsonName, schemaRemoteAttributeNamePattern, errorBuffer));
		valid &= (getProperty(targetJsonName) == null || ValidationHelper.isValidStringMatchingRegex(this, targetJsonName, schemaRemoteAttributeNamePattern, errorBuffer));
		valid &= ValidationHelper.isValidStringNotBlank(this, relationshipType, errorBuffer);

		// source and target node can be type names
		valid &= (ValidationHelper.isValidPropertyNotNull(this, sourceNode, errorBuffer) || ValidationHelper.isValidStringNotBlank(this, sourceType, errorBuffer));
		valid &= (ValidationHelper.isValidPropertyNotNull(this, targetNode, errorBuffer) || ValidationHelper.isValidStringNotBlank(this, targetType, errorBuffer));

		if (valid) {

			// clear error buffer so the schema build doesn't fail because of the above check
			errorBuffer.getErrorTokens().clear();

			// only if we are valid up to here, test for relationship uniqueness
			valid &= isRelationshipDefinitionUnique(errorBuffer);
		}

		return valid;
	}

	@Override
	public void onCreation(SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		final PropertyMap map = new PropertyMap();

		// store old property names
		map.put(previousSourceJsonName, getProperty(sourceJsonName));
		map.put(previousTargetJsonName, getProperty(targetJsonName));

		setProperties(securityContext, map);

		// register transaction post processing that recreates the schema information
		TransactionCommand.postProcess("reloadSchema", new ReloadSchema(false));
	}

	@Override
	public void onModification(SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		checkClassName();

		checkAndRenameSourceAndTargetJsonNames();

		final PropertyMap map = new PropertyMap();

		// store old property names
		map.put(previousSourceJsonName, getProperty(sourceJsonName));
		map.put(previousTargetJsonName, getProperty(targetJsonName));

		setProperties(securityContext, map);

		// register transaction post processing that recreates the schema information
		TransactionCommand.postProcess("reloadSchema", new ReloadSchema(false));
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {

		try {

			removeSourceAndTargetJsonNames();

		} catch (Throwable t) {

			// this method must not prevent
			// the deletion of a node
			logger.warn("", t);
		}
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

		super.onDeletion(securityContext, errorBuffer, properties);

		// register transaction post processing that recreates the schema information
		TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
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
