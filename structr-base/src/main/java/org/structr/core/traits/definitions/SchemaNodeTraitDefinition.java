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

/**
 *
 *
 */
public class SchemaNodeTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Set<String> EntityNameBlacklist = new LinkedHashSet<>(Arrays.asList(new String[] {
		"Relation", "Property"
	}));

	public SchemaNodeTraitDefinition() {
		super("SchemaNode");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(GraphObject obj, ErrorBuffer errorBuffer) {

					boolean valid = true;

					valid &= ValidationHelper.isValidUniqueProperty(obj, Traits.of("NodeInterface").key("name"), errorBuffer);
					valid &= ValidationHelper.isValidStringMatchingRegex(obj, Traits.of("NodeInterface").key("name"), SchemaNode.schemaNodeNamePattern, errorBuffer);

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

					if (modificationQueue.isPropertyModified(graphObject, Traits.of("NodeInterface").key("name"))) {
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

		final Property<Iterable<NodeInterface>>          relatedTo              = new EndNodes("relatedTo", "SchemaRelationshipSourceNode");
		final Property<Iterable<NodeInterface>>          relatedFrom            = new StartNodes("relatedFrom", "SchemaRelationshipTargetNode");
		final Property<Iterable<NodeInterface>>          schemaGrants           = new StartNodes("schemaGrants", "SchemaGrantSchemaNodeRelationship");
		final Property<String[]>                         inheritedTraits        = new ArrayProperty("inheritedTraits", String.class);
		final Property<String>                           defaultSortKey         = new StringProperty("defaultSortKey");
		final Property<String>                           defaultSortOrder       = new StringProperty("defaultSortOrder");
		final Property<Boolean>                          defaultVisibleToPublic = new BooleanProperty("defaultVisibleToPublic").readOnly().indexed();
		final Property<Boolean>                          defaultVisibleToAuth   = new BooleanProperty("defaultVisibleToAuth").readOnly().indexed();
		final Property<Integer>                          hierarchyLevel         = new IntProperty("hierarchyLevel").indexed();
		final Property<Integer>                          relCount               = new IntProperty("relCount").indexed();
		final Property<Boolean>                          isInterface            = new BooleanProperty("isInterface").indexed();
		final Property<Boolean>                          isAbstract             = new BooleanProperty("isAbstract").indexed();
		final Property<String>                           category               = new StringProperty("category").indexed();

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
			category
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
				"id", "type", "name", "inheritedTraits", "relatedTo", "relatedFrom", "defaultSortKey",
				"defaultSortOrder", "hierarchyLevel", "relCount", "isInterface", "isAbstract", "defaultVisibleToPublic",
				"defaultVisibleToAuth"
			),

			PropertyView.Ui,
			newSet(
				"id", "type", "name", "owner", "createdBy", "hidden", "createdDate", "lastModifiedDate",
				"visibleToPublicUsers", "visibleToAuthenticatedUsers", "schemaProperties", "schemaViews",
				"schemaMethods", "icon", "changelogDisabled", "relatedTo", "relatedFrom", "defaultSortKey",
				"defaultSortOrder", "hierarchyLevel", "relCount", "isInterface", "isAbstract", "category",
				"defaultVisibleToPublic", "defaultVisibleToAuth", "includeInOpenAPI", "inheritedTraits"
			),

			"schema",
			newSet(
				"id", "type", "name", "schemaProperties", "schemaViews", "schemaMethods", "icon",
				"changelogDisabled", "relatedTo", "relatedFrom", "defaultSortKey", "defaultSortOrder",
				"hierarchyLevel", "relCount", "isInterface", "isAbstract", "category", "schemaGrants",
				"defaultVisibleToPublic", "defaultVisibleToAuth", "includeInOpenAPI", "inheritedTraits"
			),

			"export",
			newSet(
				"id", "type", "name", "defaultSortKey", "defaultSortOrder", "hierarchyLevel",
				"relCount", "isInterface", "isAbstract", "defaultVisibleToPublic", "defaultVisibleToAuth",
				"inheritedTraits"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {

		final List<PropertyKey> propertyKeys = Iterables.toList(super.getPropertyKeys(propertyView));

		// add "custom" property keys as String properties
		for (final String key : SchemaHelper.getProperties(getNode())) {

			final PropertyKey newKey = new StringProperty(key);
			newKey.setDeclaringClass(getClass());

			propertyKeys.add(newKey);
		}

		Collections.sort(propertyKeys, (o1, o2) -> { return o1.jsonName().compareTo(o2.jsonName()); });

		return new LinkedHashSet<>(propertyKeys);
	}

	@Override
	public String getMultiplicity(final Map<String, SchemaNode> schemaNodes, final String propertyNameToCheck) {

		String multiplicity = getMultiplicity(this, propertyNameToCheck);

		if (multiplicity == null) {

			// check if property is defined in parent class
			final SchemaNode parentSchemaNode = getProperty(SchemaNode.extendsClass);
			if (parentSchemaNode != null) {

				multiplicity = getMultiplicity(parentSchemaNode, propertyNameToCheck);
			}
		}

		if (multiplicity != null) {
			return multiplicity;
		}

		// fallback, search NodeInterface (this allows the owner relationship to be used in Notions!)
		final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(NodeInterface.class, propertyNameToCheck, false);
		if (key != null) {


			// return "extended" multiplicity when the falling back to a NodeInterface property
			// to signal the code generator that it must not append "Property" to the name of
			// the generated NotionProperty parameter, i.e. NotionProperty(owner, ...) instead
			// of NotionProperty(ownerProperty, ...)..

			if (key instanceof StartNode || key instanceof EndNode) {
				return "1X";
			}

			if (key instanceof StartNodes || key instanceof EndNodes) {
				return "*X";
			}
		}

		return null;
	}

	private String getMultiplicity(final SchemaNode schemaNode, final String propertyNameToCheck) {

		final Set<String> existingPropertyNames = new LinkedHashSet<>();
		final String _className                 = schemaNode.getProperty(name);

		for (final SchemaRelationshipNode outRel : schemaNode.getProperty(SchemaNode.relatedTo)) {

			if (propertyNameToCheck.equals(outRel.getPropertyName(_className, existingPropertyNames, true))) {
				return outRel.getMultiplicity(true);
			}
		}

		// output related node definitions, collect property views
		for (final SchemaRelationshipNode inRel : schemaNode.getProperty(SchemaNode.relatedFrom)) {

			if (propertyNameToCheck.equals(inRel.getPropertyName(_className, existingPropertyNames, false))) {
				return inRel.getMultiplicity(false);
			}
		}

		return null;
	}

	@Override
	public String getRelatedType(final Map<String, SchemaNode> schemaNodes, final String propertyNameToCheck) {

		String relatedType = getRelatedType(this, propertyNameToCheck);
		if (relatedType == null) {

			// check if property is defined in parent class
			final SchemaNode parentSchemaNode = getProperty(SchemaNode.extendsClass);
			if (parentSchemaNode != null) {

				relatedType = getRelatedType(parentSchemaNode, propertyNameToCheck);

			}
		}

		if (relatedType != null) {
			return relatedType;
		}

		// fallback, search NodeInterface (this allows the owner relationship to be used in Notions!)
		final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(NodeInterface.class, propertyNameToCheck, false);
		if (key != null) {

			final Class relatedTypeClass = key.relatedType();
			if (relatedTypeClass != null) {

				return relatedTypeClass.getSimpleName();
			}
		}

		return null;
	}

	public void handleMigration(final Map<String, SchemaNode> schemaNodes) throws FrameworkException {

		final Map<String, Class> staticTypes = new LinkedHashMap<>();

		staticTypes.put("User", User.class);
		staticTypes.put("Page", Page.class);
		staticTypes.put("MailTemplate", MailTemplate.class);
		staticTypes.put("Group", Group.class);

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
		StructrApp.getInstance().nodeQuery("SchemaNode").getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

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

			final String typeName = graphObject.getProperty(Traits.of("NodeInterface").key("name"));

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
