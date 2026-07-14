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
package org.structr.web.traits.definitions;

import org.apache.commons.collections.comparators.ComparatorChain;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.ChannelInput;
import org.structr.common.PathResolvingComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.SortInfo;
import org.structr.core.entity.DataSource;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.*;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.datasource.DataSourceOperations;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.files.sync.StorageSyncService;
import org.structr.schema.action.ActionContext;
import org.structr.web.datasource.FieldDefinition;
import org.structr.web.entity.Folder;
import org.structr.web.traits.wrappers.FolderTraitWrapper;

import java.util.*;

public class FolderTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String WORK_FOLDER_OF_USERS_PROPERTY        = "workFolderOfUsers";
	public static final String CHILDREN_PROPERTY                    = "children";
	public static final String FILES_PROPERTY                       = "files";
	public static final String FOLDERS_PROPERTY                     = "folders";
	public static final String IMAGES_PROPERTY                      = "images";
	public static final String FOLDER_PARENT_PROPERTY               = "folderParent";
	public static final String HOME_FOLDER_OF_USER_PROPERTY         = "homeFolderOfUser";
	public static final String IS_FOLDER_PROPERTY                   = "isFolder";
	public static final String MOUNT_DO_FULLTEXT_INDEXING_PROPERTY  = "mountDoFulltextIndexing";
	public static final String MOUNT_WATCH_CONTENTS_PROPERTY        = "mountWatchContents";
	public static final String MOUNT_SCAN_INTERVAL_PROPERTY         = "mountScanInterval";
	public static final String ENABLED_CHECKSUMS_PROPERTY           = "enabledChecksums";
	public static final String MOUNT_TARGET_PROPERTY                = "mountTarget";
	public static final String MOUNT_TARGET_FILE_TYPE_PROPERTY      = "mountTargetFileType";
	public static final String MOUNT_TARGET_FOLDER_TYPE_PROPERTY    = "mountTargetFolderType";
	public static final String MOUNT_LAST_SCANNED_PROPERTY          = "mountLastScanned";
	public static final String FILES_COUNT_PROPERTY                 = "filesCount";
	public static final String FOLDERS_COUNT_PROPERTY               = "foldersCount";
	public static final String EXCLUDE_SUBTREE_FROM_EXPORT_PROPERTY = "excludeSubtreeFromExport";

	public FolderTraitDefinition() {
		super(StructrTraits.FOLDER);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final Folder thisFolder = graphObject.as(Folder.class);

					thisFolder.setHasParent();

					// creates/updates the synchronizer if this folder directly carries a storage configuration
					StorageSyncService.handleNodeChanged(thisFolder);

					// physical directory materialization for outbound-governed subtrees
					if (!StorageSyncService.isSyncOrigin(securityContext)) {

						StorageSyncService.recordOutboundCreation(thisFolder);
					}
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final Folder thisFolder = graphObject.as(Folder.class);

					thisFolder.setHasParent();

					// creates, updates or removes the synchronizer depending on the folder's own storage configuration
					StorageSyncService.handleNodeChanged(thisFolder);
				}
			},

			OnDeletion.class,
			new OnDeletion() {
				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

					final Folder thisFolder = graphObject.as(Folder.class);

					// a deleted folder must no longer be watched
					StorageSyncService.handleNodeDeleted(thisFolder.getUuid());
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class, new DataSourceOperations<NodeInterface>() {

				@Override
				public ResultStream<NodeInterface> getValues(final ActionContext actionContext, final DataSource provider, final ChannelInput input) throws FrameworkException {

					final Folder               folder          = provider.as(Folder.class);
					final Traits               traits          = Traits.of(StructrTraits.FILE);
					final int                  pageSize        = input != null ? input.pageSize() : Integer.MAX_VALUE;
					final int                  page            = input != null ? input.page() : 1;
					final boolean              includeHidden   = provider.includeHidden();

					// start with all files
					Iterable<NodeInterface > result = folder.getProperty(folder.getTraits().key(FolderTraitDefinition.FILES_PROPERTY));

					// filter based on includeHidden flag
					result = Iterables.filter(f -> includeHidden || !f.isHidden(), result);

					// filter based on channel input
					if (input != null) {

						result = Iterables.filter(input, result);

						final List<SortInfo> sortKeys = input.sortKeys();
						if (sortKeys != null) {

							final ComparatorChain comparator         = new ComparatorChain();
							final DefaultSortOrder  sortOrder        = new DefaultSortOrder();

							for (final SortInfo sortInfo : sortKeys) {

								if (sortInfo.sortKey.contains(".")) {

									comparator.addComparator(new PathResolvingComparator(actionContext, sortInfo.sortKey, sortInfo.descending));

								} else {

									if (traits.hasKey(sortInfo.sortKey)) {

										final PropertyKey sortKey = traits.key(sortInfo.sortKey);
										if (sortKey != null) {

											sortOrder.addElement(sortKey, sortInfo.descending);
										}
									}
								}
							}

							if (!sortOrder.isEmpty()) {
								comparator.addComparator(sortOrder);
							}

							if (comparator.size() > 0) {

								// sort
								final List<NodeInterface> filteredResult = Iterables.toList(result);

								filteredResult.sort(comparator);

								result = filteredResult;
							}

						}
					}

					return new PagingIterable<>("Folder contents of " + folder.getUuid(), result, pageSize, page);
				}

				@Override
				public Map<String, FieldDefinition> getFields(final ActionContext actionContext, final DataSource provider) throws FrameworkException {

					final Map<String, FieldDefinition> output = new LinkedHashMap<>();
					final Traits traits                       = Traits.of(StructrTraits.FILE);

					// transform input
					for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

						// hide some internal properties
						if (!SchemaNodeTraitDefinition.PROPERTY_KEY_BLACKLIST_FOR_COMPONENTS.contains(key.jsonName())) {
							output.put(key.jsonName(), key.getFieldDefinition());
						}
					}

					return output;
				}

				@Override
				public String getDataType(final ActionContext actionContext, final DataSource provider) throws FrameworkException {
					return StructrTraits.FILE;
				}

				@Override
				public int getDimension(final DataSource provider) {
					return 1;
				}
			}
		);
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
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> workFolderOfUsersProperty = new StartNodes(traitsInstance, WORK_FOLDER_OF_USERS_PROPERTY, StructrTraits.USER_WORKING_DIR_FOLDER);
		final Property<Iterable<NodeInterface>> childrenProperty          = new EndNodes(traitsInstance, CHILDREN_PROPERTY, StructrTraits.FOLDER_CONTAINS_ABSTRACT_FILE);
		final Property<Iterable<NodeInterface>> filesProperty             = new EndNodes(traitsInstance, FILES_PROPERTY, StructrTraits.FOLDER_CONTAINS_FILE);
		final Property<Iterable<NodeInterface>> foldersProperty           = new EndNodes(traitsInstance, FOLDERS_PROPERTY, StructrTraits.FOLDER_CONTAINS_FOLDER);
		final Property<Iterable<NodeInterface>> imagesProperty            = new EndNodes(traitsInstance, IMAGES_PROPERTY, StructrTraits.FOLDER_CONTAINS_IMAGE);
		final Property<NodeInterface> folderParentProperty                = new StartNode(traitsInstance, FOLDER_PARENT_PROPERTY, StructrTraits.FOLDER_CONTAINS_FOLDER);
		final Property<NodeInterface> homeFolderOfUserProperty            = new StartNode(traitsInstance, HOME_FOLDER_OF_USER_PROPERTY, StructrTraits.USER_HOME_DIR_FOLDER);
		final Property<Boolean> isFolderProperty                          = new ConstantBooleanProperty(IS_FOLDER_PROPERTY, true).readOnly();
		final Property<Boolean> mountDoFulltextIndexingProperty           = new BooleanProperty(MOUNT_DO_FULLTEXT_INDEXING_PROPERTY);
		final Property<Boolean> mountWatchContentsProperty                = new BooleanProperty(MOUNT_WATCH_CONTENTS_PROPERTY);
		final Property<Integer> mountScanIntervalProperty                 = new IntProperty(MOUNT_SCAN_INTERVAL_PROPERTY);
		final Property<String> enabledChecksumsProperty                   = new StringProperty(ENABLED_CHECKSUMS_PROPERTY).description("Override for the global checksums setting, allows you to enable or disable individual checksums for all files in this folder (and sub-folders).");
		final Property<String> mountTargetProperty                        = new StringProperty(MOUNT_TARGET_PROPERTY).indexed();
		final Property<String> mountTargetFileTypeProperty                = new StringProperty(MOUNT_TARGET_FILE_TYPE_PROPERTY);
		final Property<String> mountTargetFolderTypeProperty              = new StringProperty(MOUNT_TARGET_FOLDER_TYPE_PROPERTY);
		final Property<Long> mountLastScannedProperty                     = new LongProperty(MOUNT_LAST_SCANNED_PROPERTY);
		final Property<Object> filesCountProperty                         = new FunctionProperty(FILES_COUNT_PROPERTY).readFunction("size(this.files)").typeHint("int");
		final Property<Object> foldersCountProperty                       = new FunctionProperty(FOLDERS_COUNT_PROPERTY).readFunction("size(this.folders)").typeHint("int");
		final Property<Boolean> excludeSubtreeFromExportProperty          = new BooleanProperty(EXCLUDE_SUBTREE_FROM_EXPORT_PROPERTY);

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
			enabledChecksumsProperty,
			mountTargetProperty,
			mountTargetFileTypeProperty,
			mountTargetFolderTypeProperty,
			mountLastScannedProperty,
			filesCountProperty,
			foldersCountProperty,
			excludeSubtreeFromExportProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					EXCLUDE_SUBTREE_FROM_EXPORT_PROPERTY, FILES_PROPERTY, FOLDERS_PROPERTY, IMAGES_PROPERTY, ENABLED_CHECKSUMS_PROPERTY, IS_FOLDER_PROPERTY,
					MOUNT_DO_FULLTEXT_INDEXING_PROPERTY, MOUNT_LAST_SCANNED_PROPERTY, MOUNT_SCAN_INTERVAL_PROPERTY, MOUNT_TARGET_PROPERTY,
					MOUNT_TARGET_FILE_TYPE_PROPERTY, MOUNT_TARGET_FOLDER_TYPE_PROPERTY, MOUNT_WATCH_CONTENTS_PROPERTY,
					FILES_COUNT_PROPERTY, FOLDERS_COUNT_PROPERTY, NodeInterfaceTraitDefinition.OWNER_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					EXCLUDE_SUBTREE_FROM_EXPORT_PROPERTY, FILES_PROPERTY, FOLDERS_PROPERTY, IMAGES_PROPERTY, ENABLED_CHECKSUMS_PROPERTY, IS_FOLDER_PROPERTY,
					MOUNT_DO_FULLTEXT_INDEXING_PROPERTY, MOUNT_LAST_SCANNED_PROPERTY, MOUNT_SCAN_INTERVAL_PROPERTY, MOUNT_TARGET_PROPERTY,
					MOUNT_TARGET_FILE_TYPE_PROPERTY, MOUNT_TARGET_FOLDER_TYPE_PROPERTY, MOUNT_WATCH_CONTENTS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public boolean includeInDocumentation() {
		return true;
	}

}
