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
package org.structr.core.entity;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedTypeException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.Export;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.*;
import org.structr.core.entity.*;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaService;
import org.structr.schema.SourceFile;
import org.structr.web.entity.*;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.relationship.*;

import java.util.*;

/**
 *
 *
 */
public class SchemaNode extends AbstractSchemaNode {

	public static final String schemaNodeNamePattern    = "[A-Z][a-zA-Z0-9_]*";

	private static final Set<String> EntityNameBlacklist = new LinkedHashSet<>(Arrays.asList(new String[] {
		"Relation", "Property"
	}));

	public static final Property<Iterable<SchemaRelationshipNode>> relatedTo              = new EndNodes<>("relatedTo", SchemaRelationshipSourceNode.class);
	public static final Property<Iterable<SchemaRelationshipNode>> relatedFrom            = new StartNodes<>("relatedFrom", SchemaRelationshipTargetNode.class);
	public static final Property<Iterable<SchemaGrant>>            schemaGrants           = new StartNodes<>("schemaGrants", SchemaGrantSchemaNodeRelationship.class);
	public static final Property<SchemaNode>                       extendsClass           = new EndNode<>("extendsClass", SchemaNodeExtendsSchemaNode.class);
	public static final Property<Iterable<SchemaNode>>             extendedByClasses      = new StartNodes<>("extendedByClasses", SchemaNodeExtendsSchemaNode.class);
	public static final Property<String>                           extendsClassInternal   = new StringProperty("extendsClassInternal").indexed();
	public static final Property<String>                           implementsInterfaces   = new StringProperty("implementsInterfaces").indexed();
	public static final Property<String>                           defaultSortKey         = new StringProperty("defaultSortKey");
	public static final Property<String>                           defaultSortOrder       = new StringProperty("defaultSortOrder");
	public static final Property<Boolean>                          defaultVisibleToPublic = new BooleanProperty("defaultVisibleToPublic").readOnly().indexed();
	public static final Property<Boolean>                          defaultVisibleToAuth   = new BooleanProperty("defaultVisibleToAuth").readOnly().indexed();
	public static final Property<Boolean>                          isBuiltinType          = new BooleanProperty("isBuiltinType").readOnly().indexed();
	public static final Property<Integer>                          hierarchyLevel         = new IntProperty("hierarchyLevel").indexed();
	public static final Property<Integer>                          relCount               = new IntProperty("relCount").indexed();
	public static final Property<Boolean>                          isInterface            = new BooleanProperty("isInterface").indexed();
	public static final Property<Boolean>                          isAbstract             = new BooleanProperty("isAbstract").indexed();
	public static final Property<String>                           category               = new StringProperty("category").indexed();
	public static final Property<String[]>                         tags                   = new ArrayProperty("tags", String.class).indexed();
	public static final Property<Boolean>                          includeInOpenAPI       = new BooleanProperty("includeInOpenAPI").indexed();
	public static final Property<String>                           summary                = new StringProperty("summary").indexed();
	public static final Property<String>                           description            = new StringProperty("description").indexed();

	private static final Set<PropertyKey> PropertiesThatDoNotRequireRebuild = new LinkedHashSet<>(Arrays.asList(tags, summary, description, includeInOpenAPI));

	public static final View defaultView = new View(SchemaNode.class, PropertyView.Public,
		id, type, name, icon, changelogDisabled, extendsClass, implementsInterfaces, relatedTo, relatedFrom, defaultSortKey, defaultSortOrder, isBuiltinType, hierarchyLevel, relCount, isInterface, isAbstract, defaultVisibleToPublic, defaultVisibleToAuth, tags, summary, description
	);

	public static final View uiView = new View(SchemaNode.class, PropertyView.Ui,
		id, type, name, owner, createdBy, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, schemaProperties, schemaViews, schemaMethods, icon, description, changelogDisabled, extendsClass, implementsInterfaces, relatedTo, relatedFrom, defaultSortKey, defaultSortOrder, isBuiltinType, hierarchyLevel, relCount, isInterface, isAbstract, category, defaultVisibleToPublic, defaultVisibleToAuth, tags, summary, description, includeInOpenAPI
	);

	public static final View schemaView = new View(SchemaNode.class, "schema",
		id, type, name, schemaProperties, schemaViews, schemaMethods, icon, description, changelogDisabled, extendsClass, implementsInterfaces, relatedTo, relatedFrom, defaultSortKey, defaultSortOrder, isBuiltinType, hierarchyLevel, relCount, isInterface, isAbstract, category, schemaGrants, defaultVisibleToPublic, defaultVisibleToAuth, tags, summary, description, includeInOpenAPI
	);

	public static final View exportView = new View(SchemaNode.class, "export",
		id, type, name, icon, description, changelogDisabled, extendsClass, implementsInterfaces, defaultSortKey, defaultSortOrder, isBuiltinType, hierarchyLevel, relCount, isInterface, isAbstract, defaultVisibleToPublic, defaultVisibleToAuth, tags, summary, description
	);

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		throwExceptionIfTypeAlreadyExists();
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		if (modificationQueue.isPropertyModified(this, name)) {
			throwExceptionIfTypeAlreadyExists();
		}
	}

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
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidUniqueProperty(this, name, errorBuffer);
		valid &= ValidationHelper.isValidStringMatchingRegex(this, name, schemaNodeNamePattern, errorBuffer);

		return valid;
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
			final Set<String> prefixes    = Set.of("org.structr.core.entity.", "org.structr.web.entity.");
			final String ifaces           = getProperty(SchemaNode.implementsInterfaces);
			final List<String> interfaces = new LinkedList<>();
			String extendsClass           = null;

			if (StringUtils.isNotBlank(ifaces) && !getProperty(isBuiltinType)) {

				final String[] parts = ifaces.split("[, ]+");
				for (final String part : parts) {

					for (final String prefix : prefixes) {

						if (part.startsWith(prefix)) {

							final String typeName = part.substring(prefix.length());
							final Class type = StructrApp.getConfiguration().getNodeEntityClass(typeName);

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
		}
	}

	@Export
	public String getGeneratedSourceCode(final SecurityContext securityContext) throws FrameworkException, UnlicensedTypeException {

		final SourceFile sourceFile               = new SourceFile("");
		final Map<String, SchemaNode> schemaNodes = new LinkedHashMap<>();

		// collect list of schema nodes
		StructrApp.getInstance().nodeQuery(SchemaNode.class).getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

		// return generated source code for this class
		SchemaHelper.getSource(sourceFile, this, schemaNodes, SchemaService.getBlacklist(), new ErrorBuffer());

		return sourceFile.getContent();
	}

	@Override
	public boolean reloadSchemaOnCreate() {
		return true;
	}

	@Override
	public boolean reloadSchemaOnModify(final ModificationQueue modificationQueue) {

		final Set<PropertyKey> modifiedProperties = modificationQueue.getModifiedProperties();
		for (final PropertyKey modifiedProperty : modifiedProperties) {

			if (!PropertiesThatDoNotRequireRebuild.contains(modifiedProperty)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean reloadSchemaOnDelete() {
		return true;
	}

	@Override
	public Iterable<SchemaGrant> getSchemaGrants() {
		return getProperty(schemaGrants);
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

	/**
	* If the system is fully initialized (and no schema replacement is currently active), we disallow overriding (known) existing types so we can prevent unwanted behavior.
	* If a user were to create a type 'Html', he could cripple Structrs Page rendering completely.
	* This is a fix for all types in the Structr context - this does not help if the user creates a type named 'String' or 'Object'.
	* That could still lead to unexpected behavior.
	*
	* @throws FrameworkException if a pre-existing type is encountered
	*/
	private void throwExceptionIfTypeAlreadyExists() throws FrameworkException {

		if (Services.getInstance().isInitialized() && ! Services.getInstance().isOverridingSchemaTypesAllowed()) {

			final String typeName = getProperty(name);

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
