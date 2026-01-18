/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.core.traits.wrappers.SchemaNodeTraitWrapper;
import org.structr.schema.ReloadSchema;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class SchemaNodeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String RELATED_TO_PROPERTY                = "relatedTo";
	public static final String RELATED_FROM_PROPERTY              = "relatedFrom";
	public static final String SCHEMA_GRANTS_PROPERTY             = "schemaGrants";
	public static final String INHERITED_TRAITS_PROPERTY          = "inheritedTraits";
	public static final String NAME_PROPERTY                      = "name";
	public static final String DEFAULT_SORT_KEY_PROPERTY          = "defaultSortKey";
	public static final String DEFAULT_SORT_ORDER_PROPERTY        = "defaultSortOrder";
	public static final String DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY = "defaultVisibleToPublic";
	public static final String DEFAULT_VISIBLE_TO_AUTH_PROPERTY   = "defaultVisibleToAuth";
	public static final String HIERARCHY_LEVEL_PROPERTY           = "hierarchyLevel";
	public static final String REL_COUNT_PROPERTY                 = "relCount";
	public static final String IS_INTERFACE_PROPERTY              = "isInterface";
	public static final String IS_ABSTRACT_PROPERTY               = "isAbstract";
	public static final String CATEGORY_PROPERTY                  = "category";

	private static final Set<String> EntityNameBlacklist = new LinkedHashSet<>(Arrays.asList(new String[] {
		"Relation", "Property"
	}));

	public SchemaNodeTraitDefinition() {
		super(StructrTraits.SCHEMA_NODE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(GraphObject obj, ErrorBuffer errorBuffer) {

					boolean valid = true;

					valid &= ValidationHelper.isValidUniqueProperty(obj, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidStringMatchingRegex(obj, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), SchemaNode.schemaNodeNamePattern,
						"Type name must match the following pattern: '" + SchemaNode.schemaNodeNamePattern + "', which means it must begin with an uppercase letter and may only contain letters, numbers and underscores.",
						errorBuffer);

					return valid;
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					throwExceptionIfTypeAlreadyExists(graphObject);

					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					if (modificationQueue.isPropertyModified(graphObject, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY))) {
						throwExceptionIfTypeAlreadyExists(graphObject);
					}

					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnNodeDeletion.class,
			new OnNodeDeletion() {

				@Override
				public void onNodeDeletion(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {

					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SchemaNode.class, (traits, node) -> new SchemaNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>>          relatedTo              = new EndNodes(traitsInstance, RELATED_TO_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_SOURCE_NODE);
		final Property<Iterable<NodeInterface>>          relatedFrom            = new StartNodes(traitsInstance, RELATED_FROM_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_TARGET_NODE);
		final Property<Iterable<NodeInterface>>          schemaGrants           = new StartNodes(traitsInstance, SCHEMA_GRANTS_PROPERTY, StructrTraits.SCHEMA_GRANT_SCHEMA_NODE_RELATIONSHIP);
		final Property<String[]>                         inheritedTraits        = new ArrayProperty(INHERITED_TRAITS_PROPERTY, String.class);
		final Property<String>                           uniqueNameKey          = new StringProperty(NAME_PROPERTY).unique().indexed();
		final Property<String>                           defaultSortKey         = new StringProperty(DEFAULT_SORT_KEY_PROPERTY);
		final Property<String>                           defaultSortOrder       = new StringProperty(DEFAULT_SORT_ORDER_PROPERTY);
		final Property<Boolean>                          defaultVisibleToPublic = new BooleanProperty(DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY).readOnly().indexed();
		final Property<Boolean>                          defaultVisibleToAuth   = new BooleanProperty(DEFAULT_VISIBLE_TO_AUTH_PROPERTY).readOnly().indexed();
		final Property<Integer>                          hierarchyLevel         = new IntProperty(HIERARCHY_LEVEL_PROPERTY).indexed();
		final Property<Integer>                          relCount               = new IntProperty(REL_COUNT_PROPERTY).indexed();
		final Property<Boolean>                          isInterface            = new BooleanProperty(IS_INTERFACE_PROPERTY).indexed();
		final Property<Boolean>                          isAbstract             = new BooleanProperty(IS_ABSTRACT_PROPERTY).indexed();
		final Property<String>                           category               = new StringProperty(CATEGORY_PROPERTY).indexed();

		return newSet(
			relatedTo,
			relatedFrom,
			schemaGrants,
			inheritedTraits,
			defaultSortKey,
			defaultSortOrder,
			defaultVisibleToPublic,
			defaultVisibleToAuth,
			hierarchyLevel,
			relCount,
			isInterface,
			isAbstract,
			category,
			uniqueNameKey
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
					NAME_PROPERTY, INHERITED_TRAITS_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY,
					DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					NAME_PROPERTY, INHERITED_TRAITS_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY,
					DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY,
					CATEGORY_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY, AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY, AbstractSchemaNodeTraitDefinition.ICON_PROPERTY,
					AbstractSchemaNodeTraitDefinition.CHANGELOG_DISABLED_PROPERTY, AbstractSchemaNodeTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY
			),

			PropertyView.Schema,
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY,
					NAME_PROPERTY, INHERITED_TRAITS_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY,
					DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY,
					CATEGORY_PROPERTY,
					SCHEMA_GRANTS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY, AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY, AbstractSchemaNodeTraitDefinition.ICON_PROPERTY,
					AbstractSchemaNodeTraitDefinition.CHANGELOG_DISABLED_PROPERTY, AbstractSchemaNodeTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/**
	* If the system is fully initialized (and no schema replacement is currently active), we disallow overriding (known) existing types so we can prevent unwanted behavior.
	* If a user were to create a type 'Html', he could cripple Structrs Page rendering completely.
	* This is a fix for all types in the Structr context - this does not help if the user creates a type named 'String' or 'Object'.
	* That could still lead to unexpected behavior.
	*
	* @throws FrameworkException if a pre-existing type is encountered
	*/
	private void throwExceptionIfTypeAlreadyExists(final GraphObject graphObject) throws FrameworkException {

		if (Services.getInstance().isInitialized() && ! Services.getInstance().isOverridingSchemaTypesAllowed()) {

			final String typeName = graphObject.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

			// add type names to list of forbidden entity names
			if (EntityNameBlacklist.contains(typeName)) {
				throw new FrameworkException(422, "Type '" + typeName + "' already exists. To prevent unwanted/unexpected behavior this is forbidden.");
			}

			/*
			// add type names to list of forbidden entity names
			if (StructrApp.getConfiguration().getNodeEntities().containsKey(typeName)) {
				throw new FrameworkException(422, "Type '" + typeName + "' already exists. To prevent unwanted/unexpected behavior this is forbidden.");
			}

			// add interfaces to list of forbidden entity names
			if (StructrApp.getConfiguration().getInterfaces().containsKey(typeName)) {
				throw new FrameworkException(422, "Type '" + typeName + "' already exists. To prevent unwanted/unexpected behavior this is forbidden.");
			}
			*/
		}
	}
}
