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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.files.external.DirectoryWatchService;
import org.structr.web.entity.Folder;
import org.structr.web.traits.wrappers.FolderTraitWrapper;

import java.util.Map;
import java.util.Set;

public class FolderTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String WORK_FOLDER_OF_USERS_PROPERTY       = "workFolderOfUsers";
	public static final String CHILDREN_PROPERTY                   = "children";
	public static final String FILES_PROPERTY                      = "files";
	public static final String FOLDERS_PROPERTY                    = "folders";
	public static final String IMAGES_PROPERTY                     = "images";
	public static final String FOLDER_PARENT_PROPERTY              = "folderParent";
	public static final String HOME_FOLDER_OF_USER_PROPERTY        = "homeFolderOfUser";
	public static final String IS_FOLDER_PROPERTY                  = "isFolder";
	public static final String MOUNT_DO_FULLTEXT_INDEXING_PROPERTY = "mountDoFulltextIndexing";
	public static final String MOUNT_WATCH_CONTENTS_PROPERTY       = "mountWatchContents";
	public static final String MOUNT_SCAN_INTERVAL_PROPERTY        = "mountScanInterval";
	public static final String POSITION_PROPERTY                   = "position";
	public static final String ENABLED_CHECKSUMS_PROPERTY          = "enabledChecksums";
	public static final String MOUNT_TARGET_PROPERTY               = "mountTarget";
	public static final String MOUNT_TARGET_FILE_TYPE_PROPERTY     = "mountTargetFileType";
	public static final String MOUNT_TARGET_FOLDER_TYPE_PROPERTY   = "mountTargetFolderType";
	public static final String MOUNT_LAST_SCANNED_PROPERTY         = "mountLastScanned";
	public static final String FILES_COUNT_PROPERTY                = "filesCount";
	public static final String FOLDERS_COUNT_PROPERTY              = "foldersCount";

	public FolderTraitDefinition() {
		super(StructrTraits.FOLDER);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final Folder thisFolder = graphObject.as(Folder.class);

					thisFolder.setHasParent();

					// only update watch service for root folder of mounted hierarchy
					if (thisFolder.getMountTarget() != null || !thisFolder.isMounted()) {

						FolderTraitDefinition.updateWatchService(thisFolder, true);
					}
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final Folder thisFolder = graphObject.as(Folder.class);

					thisFolder.setHasParent();

					// only update watch service for root folder of mounted hierarchy
					if (thisFolder.getMountTarget() != null || !thisFolder.isMounted()) {

						FolderTraitDefinition.updateWatchService(thisFolder, true);
					}
				}
			},

			OnDeletion.class,
			new OnDeletion() {
				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

					final Folder thisFolder = graphObject.as(Folder.class);

					FolderTraitDefinition.updateWatchService(thisFolder, false);
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

			Folder.class, (traits, node) -> new FolderTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> workFolderOfUsersProperty = new StartNodes(WORK_FOLDER_OF_USERS_PROPERTY, StructrTraits.USER_WORKING_DIR_FOLDER);
		final Property<Iterable<NodeInterface>> childrenProperty          = new EndNodes(CHILDREN_PROPERTY, StructrTraits.FOLDER_CONTAINS_ABSTRACT_FILE);
		final Property<Iterable<NodeInterface>> filesProperty             = new EndNodes(FILES_PROPERTY, StructrTraits.FOLDER_CONTAINS_FILE);
		final Property<Iterable<NodeInterface>> foldersProperty           = new EndNodes(FOLDERS_PROPERTY, StructrTraits.FOLDER_CONTAINS_FOLDER);
		final Property<Iterable<NodeInterface>> imagesProperty            = new EndNodes(IMAGES_PROPERTY, StructrTraits.FOLDER_CONTAINS_IMAGE);
		final Property<NodeInterface> folderParentProperty                = new StartNode(FOLDER_PARENT_PROPERTY, StructrTraits.FOLDER_CONTAINS_FOLDER);
		final Property<NodeInterface> homeFolderOfUserProperty            = new StartNode(HOME_FOLDER_OF_USER_PROPERTY, StructrTraits.USER_HOME_DIR_FOLDER);
		final Property<Boolean> isFolderProperty                          = new ConstantBooleanProperty(IS_FOLDER_PROPERTY, true).readOnly();
		final Property<Boolean> mountDoFulltextIndexingProperty           = new BooleanProperty(MOUNT_DO_FULLTEXT_INDEXING_PROPERTY);
		final Property<Boolean> mountWatchContentsProperty                = new BooleanProperty(MOUNT_WATCH_CONTENTS_PROPERTY);
		final Property<Integer> mountScanIntervalProperty                 = new IntProperty(MOUNT_SCAN_INTERVAL_PROPERTY);
		final Property<Integer> positionProperty                          = new IntProperty(POSITION_PROPERTY).indexed();		// FIXME: Is Folder.position ever used? sort order is alphabetically I think
		final Property<String> enabledChecksumsProperty                   = new StringProperty(ENABLED_CHECKSUMS_PROPERTY);
		final Property<String> mountTargetProperty                        = new StringProperty(MOUNT_TARGET_PROPERTY).indexed();
		final Property<String> mountTargetFileTypeProperty                = new StringProperty(MOUNT_TARGET_FILE_TYPE_PROPERTY);
		final Property<String> mountTargetFolderTypeProperty              = new StringProperty(MOUNT_TARGET_FOLDER_TYPE_PROPERTY);
		final Property<Long> mountLastScannedProperty                     = new LongProperty(MOUNT_LAST_SCANNED_PROPERTY);
		final Property<Object> filesCountProperty                         = new FunctionProperty(FILES_COUNT_PROPERTY).readFunction("size(this.files)").typeHint("int");
		final Property<Object> foldersCountProperty                       = new FunctionProperty(FOLDERS_COUNT_PROPERTY).readFunction("size(this.folders)").typeHint("int");

		return Set.of(
			childrenProperty,
			filesProperty,
			foldersProperty,
			imagesProperty,
			folderParentProperty,
			homeFolderOfUserProperty,
			workFolderOfUsersProperty,
			isFolderProperty,
			mountDoFulltextIndexingProperty,
			mountWatchContentsProperty,
			mountScanIntervalProperty,
			positionProperty,
			enabledChecksumsProperty,
			mountTargetProperty,
			mountTargetFileTypeProperty,
			mountTargetFolderTypeProperty,
			mountLastScannedProperty,
			filesCountProperty,
			foldersCountProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					FILES_PROPERTY, FOLDERS_PROPERTY, AbstractFileTraitDefinition.PARENT_ID_PROPERTY, ENABLED_CHECKSUMS_PROPERTY,
					FILES_COUNT_PROPERTY, FOLDERS_COUNT_PROPERTY, IS_FOLDER_PROPERTY, AbstractFileTraitDefinition.IS_MOUNTED_PROPERTY,
					MOUNT_DO_FULLTEXT_INDEXING_PROPERTY, MOUNT_LAST_SCANNED_PROPERTY, MOUNT_SCAN_INTERVAL_PROPERTY, MOUNT_TARGET_PROPERTY,
					MOUNT_TARGET_FILE_TYPE_PROPERTY, MOUNT_TARGET_FOLDER_TYPE_PROPERTY, MOUNT_WATCH_CONTENTS_PROPERTY, NodeInterfaceTraitDefinition.OWNER_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					FILES_PROPERTY, FOLDERS_PROPERTY, IMAGES_PROPERTY, ENABLED_CHECKSUMS_PROPERTY, IS_FOLDER_PROPERTY,
					MOUNT_DO_FULLTEXT_INDEXING_PROPERTY, MOUNT_LAST_SCANNED_PROPERTY, MOUNT_SCAN_INTERVAL_PROPERTY,
					MOUNT_TARGET_PROPERTY, MOUNT_TARGET_FILE_TYPE_PROPERTY, MOUNT_TARGET_FOLDER_TYPE_PROPERTY, MOUNT_WATCH_CONTENTS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- private static methods -----
	private static void updateWatchService(final Folder thisFolder, final boolean mount) {

		if (Services.getInstance().isConfigured(DirectoryWatchService.class)) {

			final DirectoryWatchService service = StructrApp.getInstance().getService(DirectoryWatchService.class);
			if (service != null && service.isRunning()) {

				if (mount) {

					service.mountFolder(thisFolder);

				} else {

					service.unmountFolder(thisFolder);
				}
			}
		}
	}
}
