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
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;
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
import java.util.stream.Collectors;

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
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

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

					checkInheritanceConstraints(graphObject.as(SchemaNode.class));
					throwExceptionIfTypeAlreadyExists(graphObject);

					TransactionCommand.postProcess("reloadSchema", new ReloadSchema(true));
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					checkInheritanceConstraints(graphObject.as(SchemaNode.class));

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
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>>          relatedTo              = new EndNodes(RELATED_TO_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_SOURCE_NODE);
		final Property<Iterable<NodeInterface>>          relatedFrom            = new StartNodes(RELATED_FROM_PROPERTY, StructrTraits.SCHEMA_RELATIONSHIP_TARGET_NODE);
		final Property<Iterable<NodeInterface>>          schemaGrants           = new StartNodes(SCHEMA_GRANTS_PROPERTY, StructrTraits.SCHEMA_GRANT_SCHEMA_NODE_RELATIONSHIP);
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
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NAME_PROPERTY,
					INHERITED_TRAITS_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY,
					DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NAME_PROPERTY,
					NodeInterfaceTraitDefinition.OWNER_PROPERTY, GraphObjectTraitDefinition.CREATED_BY_PROPERTY, NodeInterfaceTraitDefinition.HIDDEN_PROPERTY,
					GraphObjectTraitDefinition.CREATED_DATE_PROPERTY, GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY,
					GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY, AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY, AbstractSchemaNodeTraitDefinition.ICON_PROPERTY,
					AbstractSchemaNodeTraitDefinition.CHANGELOG_DISABLED_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY,
					DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					CATEGORY_PROPERTY, DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY, AbstractSchemaNodeTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY, INHERITED_TRAITS_PROPERTY
			),

			"schema",
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NAME_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY, AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY,
					AbstractSchemaNodeTraitDefinition.SCHEMA_METHODS_PROPERTY, AbstractSchemaNodeTraitDefinition.ICON_PROPERTY,
					AbstractSchemaNodeTraitDefinition.CHANGELOG_DISABLED_PROPERTY, RELATED_TO_PROPERTY, RELATED_FROM_PROPERTY, DEFAULT_SORT_KEY_PROPERTY, DEFAULT_SORT_ORDER_PROPERTY,
					HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY, CATEGORY_PROPERTY, SCHEMA_GRANTS_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY, AbstractSchemaNodeTraitDefinition.INCLUDE_IN_OPEN_API_PROPERTY, INHERITED_TRAITS_PROPERTY
			),

			"export",
			newSet(
					GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NAME_PROPERTY,
					DEFAULT_SORT_KEY_PROPERTY, DEFAULT_SORT_ORDER_PROPERTY, HIERARCHY_LEVEL_PROPERTY, REL_COUNT_PROPERTY, IS_INTERFACE_PROPERTY, IS_ABSTRACT_PROPERTY,
					DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY, DEFAULT_VISIBLE_TO_AUTH_PROPERTY, INHERITED_TRAITS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	public void handleMigration(final Map<String, SchemaNode> schemaNodes) throws FrameworkException {

		final Map<String, Class> staticTypes = new LinkedHashMap<>();

		staticTypes.put(StructrTraits.USER, User.class);
		staticTypes.put(StructrTraits.PAGE, Page.class);
		staticTypes.put(StructrTraits.MAIL_TEMPLATE, MailTemplate.class);
		staticTypes.put(StructrTraits.GROUP, Group.class);

		final String name = getName();

		if (staticTypes.keySet().contains(name)) {

			final Class type = staticTypes.get(name);

			// migrate fully dynamic types to static types
			setProperty(SchemaNode.extendsClass, null);
			setProperty(SchemaNode.implementsInterfaces, null);
			setProperty(SchemaNode.extendsClassInternal, type.getName());

		} else {

			final String extendsClassInternalValue = getProperty(SchemaNode.extendsClassInternal);
			if (extendsClassInternalValue != null && extendsClassInternalValue.startsWith("LinkedTreeNodeImpl<")) {

				setProperty(SchemaNode.extendsClassInternal, null);
			}

			final String previousExtendsClassValue = (String) this.getNode().getProperty("extendsClass");
			if (previousExtendsClassValue != null) {

				final String extendsClass = StringUtils.substringBefore(previousExtendsClassValue, "<"); // remove optional generic parts from class name
				final String className = StringUtils.substringAfterLast(extendsClass, ".");

				final SchemaNode baseType = schemaNodes.get(className);
				if (baseType != null) {

					setProperty(SchemaNode.extendsClass, baseType);
					this.getNode().setProperty("extendsClass", null);

				} else {

					setProperty(SchemaNode.extendsClassInternal, previousExtendsClassValue);
				}
			}

			// migrate dynamic classes that extend static types (that were dynamic before)
			final Set<String> prefixes    = Set.of("org.structr.core.entity.", "org.structr.web.entity.", "org.structr.mail.entity.");
			final String ifaces           = getProperty(SchemaNode.implementsInterfaces);
			final List<String> interfaces = new LinkedList<>();
			String extendsClass           = null;

			if (StringUtils.isNotBlank(ifaces) && !getProperty(isBuiltinType)) {

				final String[] parts = ifaces.split("[, ]+");
				for (final String part : parts) {

					for (final String prefix : prefixes) {

						if (part.startsWith(prefix)) {

							final String typeName = part.substring(prefix.length());
							final Class type = Traits.of(typeName);

							if (type != null) {

								extendsClass = type.getName();
								break;
							}
						}
					}

					// re-add interface if no extending class was found
					if (extendsClass == null) {
						interfaces.add(part);
					}
				}

				if (extendsClass != null) {
					setProperty(SchemaNode.extendsClassInternal, extendsClass);
				}

				if (interfaces.isEmpty()) {

					setProperty(SchemaNode.implementsInterfaces, null);

				} else {

					final String implementsInterfaces = StringUtils.join(interfaces, ", ");

					setProperty(SchemaNode.implementsInterfaces, implementsInterfaces);
				}
			}

			// migrate extendsClass relationship from dynamic to static
		}

		// remove "all" view since it is internal and shouldn't be updated explicitly
		for (final SchemaView view : getProperty(SchemaNode.schemaViews)) {

			if ("all".equals(view.getName())) {

				StructrApp.getInstance().delete(view);
			}
		}
	}

	@Export
	public String getGeneratedSourceCode(final SecurityContext securityContext) throws FrameworkException, UnlicensedTypeException {

		final SourceFile sourceFile               = new SourceFile("");
		final Map<String, SchemaNode> schemaNodes = new LinkedHashMap<>();

		// collect list of schema nodes
		StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_NODE).getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

		// return generated source code for this class
		SchemaHelper.getSource(sourceFile, this, schemaNodes, SchemaService.getBlacklist(), new ErrorBuffer());

		return sourceFile.getContent();
	}

	// ----- private methods -----
	private String addToList(final String source, final String value) {

		final List<String> list = new LinkedList<>();

		if (source != null) {

			list.addAll(Arrays.asList(source.split(",")));
		}

		list.add(value);

		return StringUtils.join(list, ",");
	}


	private String getRelatedType(final SchemaNode schemaNode, final String propertyNameToCheck) {

		final Set<String> existingPropertyNames = new LinkedHashSet<>();
		final String _className                 = schemaNode.getProperty(name);

		for (final SchemaRelationshipNode outRel : schemaNode.getProperty(SchemaNode.relatedTo)) {

			if (propertyNameToCheck.equals(outRel.getPropertyName(_className, existingPropertyNames, true))) {
				return outRel.getSchemaNodeTargetType();
			}
		}

		// output related node definitions, collect property views
		for (final SchemaRelationshipNode inRel : schemaNode.getProperty(SchemaNode.relatedFrom)) {

			if (propertyNameToCheck.equals(inRel.getPropertyName(_className, existingPropertyNames, false))) {
				return inRel.getSchemaNodeSourceType();
			}
		}

		return null;
	}
	*/

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

	private void checkInheritanceConstraints(final SchemaNode schemaNode) throws FrameworkException {

		final Set<String> traitNames = schemaNode.getInheritedTraits();
		final Set<Trait> traits      = traitNames.stream().map(name -> Traits.getTrait(name)).filter(t -> t != null).collect(Collectors.toSet());

		for (final Trait trait1 : traits) {

			for (final Trait trait2 : traits) {

				trait1.checkCompatibilityWith(trait2);

			}
		}

	}
}
