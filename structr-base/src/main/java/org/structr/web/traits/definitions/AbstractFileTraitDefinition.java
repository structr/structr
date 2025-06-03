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
package org.structr.web.traits.definitions;

import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.web.entity.AbstractFile;
import org.structr.web.property.AbstractFileIsMountedProperty;
import org.structr.web.property.PathProperty;
import org.structr.web.traits.wrappers.AbstractFileTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * Base class for filesystem objects in structr.
 */
public class AbstractFileTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String STORAGE_CONFIGURATION_PROPERTY      = "storageConfiguration";
	public static final String PARENT_PROPERTY                     = "parent";
	public static final String PARENT_ID_PROPERTY                  = "parentId";
	public static final String HAS_PARENT_PROPERTY                 = "hasParent";
	public static final String INCLUDE_IN_FRONTEND_EXPORT_PROPERTY = "includeInFrontendExport";
	public static final String IS_EXTERNAL_PROPERTY                = "isExternal";
	public static final String NAME_PROPERTY                       = "name";
	public static final String LAST_SEEN_MOUNTED_PROPERTY          = "lastSeenMounted";
	public static final String IS_MOUNTED_PROPERTY                 = "isMounted";
	public static final String PATH_PROPERTY                       = "path";

	public AbstractFileTraitDefinition() {
		super(StructrTraits.ABSTRACT_FILE);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					if (Settings.UniquePaths.getValue()) {

						final AbstractFile file = graphObject.as(AbstractFile.class);

						file.validateAndRenameFileOnce(securityContext, errorBuffer);
					}
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {

					final AbstractFile file = graphObject.as(AbstractFile.class);
					if (file.isExternal()) {

						// check if name changed
						final GraphObjectMap beforeProps = modificationQueue.getModifications(file).get(new GenericProperty<>("before"));
						if (beforeProps != null) {

							final String prevName = beforeProps.getProperty(new GenericProperty<>("name"));
							if (prevName != null) {

								throw new UnsupportedOperationException("Not implemented for new fs abstraction layer");

								/*
								final boolean renameSuccess = file.renameMountedAbstractFile(file.getParent(), file, "", prevName);

								if (!renameSuccess) {
									errorBuffer.add(new SemanticErrorToken("RenameFailed", AbstractFileTraitDefinition.name.jsonName(), "Renaming failed"));
								}
								*/
							}
						}

					} else if (Settings.UniquePaths.getValue()) {

						file.validateAndRenameFileOnce(securityContext, errorBuffer);
					}
				}
			},

			IsValid.class,
			new IsValid() {
				@Override

				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final PropertyKey<String> nameKey = obj.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

					boolean valid = true;

					// validations that are stored in the format attribute of a property must be implemented in IsValid, manually!
					valid &= ValidationHelper.isValidStringMatchingRegex(obj, nameKey, "[^\\/\\x00]+",
						"File and folder names may not contain the slash character (/).",
						errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(obj, nameKey, errorBuffer);

					return valid;
				}
			}
		);
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

		return Map.of(
			AbstractFile.class, (traits, node) -> new AbstractFileTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> storageConfigurationProperty = new EndNode(STORAGE_CONFIGURATION_PROPERTY, StructrTraits.ABSTRACT_FILE_CONFIGURED_BY_STORAGE_CONFIGURATION);
		final Property<NodeInterface> parentProperty               = new StartNode(PARENT_PROPERTY, StructrTraits.FOLDER_CONTAINS_ABSTRACT_FILE).updateCallback(AbstractFileTraitDefinition::updateHasParent);
		final Property<String> parentIdProperty                    = new EntityIdProperty(PARENT_ID_PROPERTY, StructrTraits.ABSTRACT_FILE, PARENT_PROPERTY, StructrTraits.FOLDER);
		final Property<Boolean> hasParentProperty                  = new BooleanProperty(HAS_PARENT_PROPERTY).indexed();
		final Property<Boolean> includeInFrontendExportProperty    = new BooleanProperty(INCLUDE_IN_FRONTEND_EXPORT_PROPERTY).indexed();
		final Property<Boolean> isExternalProperty                 = new BooleanProperty(IS_EXTERNAL_PROPERTY).indexed();
		final Property<String> nameProperty                        = new StringProperty(NAME_PROPERTY).notNull().indexed(); // fixme: not dynamic, but does it have any consequences?
		final Property<Long> lastSeenMountedProperty               = new LongProperty(LAST_SEEN_MOUNTED_PROPERTY);
		final Property<Boolean> isMountedProperty                  = new AbstractFileIsMountedProperty();
		final Property<String> pathProperty                        = new PathProperty(PATH_PROPERTY).typeHint("String").indexed();

		return Set.of(
			storageConfigurationProperty,
			parentProperty,
			parentIdProperty,
			hasParentProperty,
			includeInFrontendExportProperty,
			isExternalProperty,
			nameProperty,
			lastSeenMountedProperty,
			isMountedProperty,
			pathProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					NodeInterfaceTraitDefinition.NAME_PROPERTY, IS_EXTERNAL_PROPERTY, LAST_SEEN_MOUNTED_PROPERTY, PATH_PROPERTY,
					GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					IS_MOUNTED_PROPERTY, INCLUDE_IN_FRONTEND_EXPORT_PROPERTY, IS_EXTERNAL_PROPERTY, LAST_SEEN_MOUNTED_PROPERTY,
					PATH_PROPERTY, PARENT_PROPERTY, STORAGE_CONFIGURATION_PROPERTY
			)
		);
	}
	/*
	View uiView = new View(AbstractFileTraitDefinition.class, PropertyView.Ui, parentProperty, storageConfigurationProperty);
	*/

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- protected methods -----

	static void updateHasParent(final GraphObject obj, final NodeInterface value) throws FrameworkException {
		obj.as(AbstractFile.class).setHasParent(value != null);
	}
}
