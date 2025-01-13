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
import org.structr.core.traits.definitions.AbstractTraitDefinition;
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

public class FolderTraitDefinition extends AbstractTraitDefinition {

	/*
		type.addBooleanProperty("isFolder", PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		type.addStringProperty("mountTarget", PropertyView.Public).setIndexed(true);
		type.addIntegerProperty("position").setIndexed(true);

		type.addStringProperty("enabledChecksums",         PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("mountTarget",              PropertyView.Ui);
		type.addBooleanProperty("mountDoFulltextIndexing", PropertyView.Public, PropertyView.Ui);
		type.addBooleanProperty("mountWatchContents",      PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("mountScanInterval",       PropertyView.Public, PropertyView.Ui);
		type.addLongProperty("mountLastScanned",           PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("mountTargetFileType",      PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("mountTargetFolderType",    PropertyView.Public, PropertyView.Ui);

		type.addFunctionProperty("filesCount", "public").setReadFunction("size(this.files)").setTypeHint("int");
		type.addFunctionProperty("foldersCount", "public").setReadFunction("size(this.folders)").setTypeHint("int");
	*/


	public FolderTraitDefinition() {
		super("Folder");
	}

	/*
	View defaultView = new View(Folder.class, PropertyView.Public, filesProperty, foldersProperty, parentIdProperty);
	View uiView      = new View(Folder.class, PropertyView.Ui,     filesProperty, foldersProperty, imagesProperty);
	*/

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final Folder thisFolder = ((NodeInterface) graphObject).as(Folder.class);

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

					final Folder thisFolder = ((NodeInterface) graphObject).as(Folder.class);

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

					final Folder thisFolder = ((NodeInterface) graphObject).as(Folder.class);

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
		final Property<Boolean> isFolderProperty                           = new BooleanProperty("isFolder").readOnly().transformators("org.structr.common.ConstantBooleanTrue").dynamic();
		final Property<Boolean> mountDoFulltextIndexingProperty            = new BooleanProperty("mountDoFulltextIndexing").dynamic();
		final Property<Boolean> mountWatchContentsProperty                 = new BooleanProperty("mountWatchContents").dynamic();
		final Property<Integer> mountScanIntervalProperty                  = new IntProperty("mountScanInterval").dynamic();
		final Property<Integer> positionProperty                           = new IntProperty("position").indexed().dynamic();
		final Property<String> enabledChecksumsProperty                    = new StringProperty("enabledChecksums").dynamic();
		final Property<String> mountTargetProperty                         = new StringProperty("mountTarget").indexed().dynamic();
		final Property<String> mountTargetFileTypeProperty                 = new StringProperty("mountTargetFileType").dynamic();
		final Property<String> mountTargetFolderTypeProperty               = new StringProperty("mountTargetFolderType").dynamic();
		final Property<Long> mountLastScannedProperty                      = new LongProperty("mountLastScanned").dynamic();
		final Property<Object> filesCountProperty                          = new FunctionProperty("filesCount").typeHint("int").dynamic();
		final Property<Object> foldersCountProperty                        = new FunctionProperty("foldersCount").typeHint("int").dynamic();

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
