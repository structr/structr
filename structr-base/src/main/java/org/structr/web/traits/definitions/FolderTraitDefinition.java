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

		final Property<Iterable<NodeInterface>> workFolderOfUsersProperty = new StartNodes("workFolderOfUsers", "UserWORKING_DIRFolder");
		final Property<Iterable<NodeInterface>> childrenProperty          = new EndNodes("children", "FolderCONTAINSAbstractFile");
		final Property<Iterable<NodeInterface>> filesProperty             = new EndNodes("files", "FolderCONTAINSFile");
		final Property<Iterable<NodeInterface>> foldersProperty           = new EndNodes("folders", "FolderCONTAINSFolder");
		final Property<Iterable<NodeInterface>> imagesProperty            = new EndNodes("images", "FolderCONTAINSImage");
		final Property<NodeInterface> folderParentProperty                = new StartNode("folderParent", "FolderCONTAINSFolder");
		final Property<NodeInterface> homeFolderOfUserProperty            = new StartNode("homeFolderOfUser", "UserHOME_DIRFolder");
		final Property<Boolean> isFolderProperty                          = new ConstantBooleanProperty("isFolder", true).readOnly();
		final Property<Boolean> mountDoFulltextIndexingProperty           = new BooleanProperty("mountDoFulltextIndexing");
		final Property<Boolean> mountWatchContentsProperty                = new BooleanProperty("mountWatchContents");
		final Property<Integer> mountScanIntervalProperty                 = new IntProperty("mountScanInterval");
		final Property<Integer> positionProperty                          = new IntProperty("position").indexed();
		final Property<String> enabledChecksumsProperty                   = new StringProperty("enabledChecksums");
		final Property<String> mountTargetProperty                        = new StringProperty("mountTarget").indexed();
		final Property<String> mountTargetFileTypeProperty                = new StringProperty("mountTargetFileType");
		final Property<String> mountTargetFolderTypeProperty              = new StringProperty("mountTargetFolderType");
		final Property<Long> mountLastScannedProperty                     = new LongProperty("mountLastScanned");
		final Property<Object> filesCountProperty                         = new FunctionProperty("filesCount").readFunction("size(this.files)").typeHint("int");
		final Property<Object> foldersCountProperty                       = new FunctionProperty("foldersCount").readFunction("size(this.files)").typeHint("int");

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
				"files", "folders", "parentId", "enabledChecksums", "filesCount", "foldersCount", "isFolder",
				"isMounted", "mountDoFulltextIndexing", "mountLastScanned", "mountScanInterval", "mountTarget",
				"mountTargetFileType", "mountTargetFolderType", "mountWatchContents", "owner"
			),
			PropertyView.Ui,
			newSet(
				"files", "folders", "images", "enabledChecksums", "isFolder", "mountDoFulltextIndexing",
				"mountLastScanned", "mountScanInterval", "mountTarget", "mountTargetFileType", "mountTargetFolderType",
				"mountWatchContents"
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
