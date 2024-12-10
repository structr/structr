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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.property.MethodProperty;
import org.structr.web.property.PathProperty;
import org.structr.web.traits.wrappers.AbstractFileTraitWrapper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

/**
 * Base class for filesystem objects in structr.
 */
public class AbstractFileTraitDefinition extends AbstractTraitDefinition {

	private static final Property<NodeInterface> storageConfigurationProperty = new EndNode("storageConfiguration", "AbstractFileCONFIGURED_BYStorageConfiguration").partOfBuiltInSchema();
	private static final Property<NodeInterface> parentProperty               = new StartNode("parent", "FolderCONTAINSAbstractFile").partOfBuiltInSchema().updateCallback(AbstractFileTraitDefinition::updateHasParent);
	private static final Property<String> parentIdProperty                    = new EntityIdProperty("parentId", AbstractFileTraitDefinition.parentProperty).format("parent, {},").partOfBuiltInSchema();
	private static final Property<Boolean> hasParentProperty                  = new BooleanProperty("hasParent").indexed().partOfBuiltInSchema().dynamic();
	private static final Property<Boolean> includeInFrontendExportProperty    = new BooleanProperty("includeInFrontendExport").indexed().partOfBuiltInSchema().dynamic();
	private static final Property<Boolean> isExternalProperty                 = new BooleanProperty("isExternal").indexed().partOfBuiltInSchema().dynamic();
	private static final Property<String> nameProperty                        = new StringProperty("name").format("[^\\\\/\\\\x00]+").notNull().indexed().partOfBuiltInSchema().dynamic();
	private static final Property<Long> lastSeenMountedProperty               = new LongProperty("lastSeenMounted").partOfBuiltInSchema().dynamic();
	private static final Property<Object> isMountedProperty                   = new MethodProperty("isMounted").format("org.structr.web.entity.AbstractFile, isMounted").typeHint("Boolean").partOfBuiltInSchema().dynamic();
	private static final Property<String> pathProperty                        = new PathProperty("path").typeHint("String").indexed().partOfBuiltInSchema().dynamic();

	public AbstractFileTraitDefinition() {
		super("AbstractFile");
	}

	/*
		type.addStringProperty("name", PropertyView.Public).setIndexed(true).setRequired(true).setFormat("[^\\\\/\\\\x00]+");

		type.addCustomProperty("isMounted", MethodProperty.class.getName(), PropertyView.Public, PropertyView.Ui).setTypeHint("Boolean").setFormat(AbstractFileTraitDefinition.class.getName() + ", isMounted");
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

						final AbstractFile file = ((NodeInterface) graphObject).as(AbstractFile.class);

						file.validateAndRenameFileOnce(securityContext, errorBuffer);
					}
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(GraphObject graphObject, SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {

					final AbstractFile file = ((NodeInterface) graphObject).as(AbstractFile.class);
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
	static java.io.File defaultGetFileOnDisk(final File fileBase, final boolean create) {

		final String uuid       = fileBase.getUuid();
		final String filePath   = Settings.FilesPath.getValue();
		final String uuidPath   = AbstractFileTraitDefinition.getDirectoryPath(uuid);
		final String finalPath  = filePath + "/" + uuidPath + "/" + uuid;
		final Path path         = Paths.get(finalPath);
		final java.io.File file = path.toFile();

		// create parent directory tree
		file.getParentFile().mkdirs();

		// create file only if requested
		if (!file.exists() && create && !fileBase.isExternal()) {

			try {

				file.createNewFile();

			} catch (IOException ioex) {

				final Logger logger = LoggerFactory.getLogger(AbstractFileTraitDefinition.class);
				logger.error("Unable to create file {}: {}", file, ioex.getMessage());
			}
		}

		return file;
	}

	static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	static void updateHasParent(final GraphObject obj, final NodeInterface value) throws FrameworkException {
		((AbstractFile) obj).setHasParent(value != null);
	}
}
