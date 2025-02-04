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
package org.structr.web.traits.definitions;


import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
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

	public AbstractFileTraitDefinition() {
		super("AbstractFile");
	}

	/*
		type.addStringProperty("name", PropertyView.Public).setIndexed(true).setRequired(true).setFormat("[^\\\\/\\\\x00]+");

		type.addBooleanProperty("includeInFrontendExport",                  PropertyView.Ui).setIndexed(true);
		type.addBooleanProperty("isExternal",                               PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addLongProperty("lastSeenMounted",                             PropertyView.Public, PropertyView.Ui);

		type.addBooleanProperty("hasParent").setIndexed(true);

		type.addCustomProperty("path", PathProperty.class.getName(), PropertyView.Public, PropertyView.Ui).setTypeHint("String").setIndexed(true);
	*/

	/*
	View uiView = new View(AbstractFileTraitDefinition.class, PropertyView.Ui, parentProperty, storageConfigurationProperty);
	*/

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
						final GraphObjectMap beforeProps = modificationQueue.getModifications(file.getWrappedNode()).get(new GenericProperty<>("before"));
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

		final Property<NodeInterface> storageConfigurationProperty = new EndNode("storageConfiguration", "AbstractFileCONFIGURED_BYStorageConfiguration");
		final Property<NodeInterface> parentProperty               = new StartNode("parent", "FolderCONTAINSAbstractFile").updateCallback(AbstractFileTraitDefinition::updateHasParent);
		final Property<String> parentIdProperty                    = new EntityIdProperty("parentId", "AbstractFile", "parent", "Folder");
		final Property<Boolean> hasParentProperty                  = new BooleanProperty("hasParent").indexed().dynamic();
		final Property<Boolean> includeInFrontendExportProperty    = new BooleanProperty("includeInFrontendExport").indexed().dynamic();
		final Property<Boolean> isExternalProperty                 = new BooleanProperty("isExternal").indexed().dynamic();
		final Property<String> nameProperty                        = new StringProperty("name").format("[^\\\\/\\\\x00]+").notNull().indexed().dynamic();
		final Property<Long> lastSeenMountedProperty               = new LongProperty("lastSeenMounted").dynamic();
		final Property<Boolean> isMountedProperty                  = new AbstractFileIsMountedProperty();
		final Property<String> pathProperty                        = new PathProperty("path").typeHint("String").indexed().dynamic();

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
	public Relation getRelation() {
		return null;
	}

	/*
	// view configuration
	type.addViewProperty(PropertyView.Public, "visibleToAuthenticatedUsers");
	type.addViewProperty(PropertyView.Public, "visibleToPublicUsers");

	type.addViewProperty(PropertyView.Ui, "parent");
	type.addViewProperty(PropertyView.Ui, "storageConfiguration");
	*/

	// ----- protected methods -----

	static void updateHasParent(final GraphObject obj, final NodeInterface value) throws FrameworkException {
		((AbstractFile) obj).setHasParent(value != null);
	}
}
