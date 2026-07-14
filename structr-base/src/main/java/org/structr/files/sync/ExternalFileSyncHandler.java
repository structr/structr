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
package org.structr.files.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.SyncTarget;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FolderTraitDefinition;

/**
 * Maps external change events and enumerated entries for one sync target
 * onto graph operations: creates, updates, moves and deletes File and Folder
 * nodes (honouring mountTargetFileType/mountTargetFolderType), creates
 * intermediate folders, marks nodes isExternal, maintains lastSeenMounted
 * and triggers fulltext indexing and metadata refresh where content actually
 * changed. All methods require an active transaction.
 *
 * Node resolution: entries are resolved by node UUID first; entries without
 * a (known) UUID are resolved by their virtual path relative to the sync
 * root. Entries that resolve into a subtree governed by a different
 * StorageConfiguration (nested sync roots) are skipped - that subtree
 * belongs to another synchronizer.
 */
public class ExternalFileSyncHandler {

	private static final Logger logger = LoggerFactory.getLogger(ExternalFileSyncHandler.class);

	private final SyncTarget target;
	private final Stats stats = new Stats();

	// all graph access happens with a sync-originated superuser context, so
	// lifecycle guards can distinguish external changes from user-initiated
	// ones (see StorageSyncService.SYNC_ORIGIN_ATTRIBUTE)
	private final App app = StructrApp.getInstance(StorageSyncService.createSyncContext());

	public ExternalFileSyncHandler(final SyncTarget target) {
		this.target = target;
	}

	public Stats getStats() {
		return stats;
	}

	/**
	 * Pull side: ensure a node exists and is current for the enumerated
	 * entry. Refreshes metadata only when the entry's size/lastModified
	 * differ from the node's stored values, but always stamps
	 * lastSeenMounted so deletion reconciliation can identify stale nodes.
	 */
	public NodeInterface handleDiscovered(final ExternalEntry entry, final long scanTime) throws FrameworkException {

		final NodeInterface node = resolve(entry, true);
		if (node == null) {
			return null;
		}

		if (node.is(StructrTraits.FILE) && !hasChanged(node.as(File.class), entry)) {

			// unchanged: only stamp lastSeenMounted for reconciliation
			node.setProperty(lastSeenMountedKey(), scanTime);
			stats.unchanged++;

		} else {

			updateNode(node, scanTime);
			stats.updated++;
		}

		return node;
	}

	/**
	 * Push side: CREATED or MODIFIED. Creates or updates the node and
	 * refreshes its metadata unconditionally.
	 */
	public NodeInterface handleCreatedOrModified(final ExternalEntry entry) throws FrameworkException {

		final NodeInterface node = resolve(entry, true);
		if (node != null) {

			updateNode(node, System.currentTimeMillis());
			stats.updated++;
		}

		return node;
	}

	/**
	 * Push side: DELETED. Resolves by UUID first, else by path, and deletes
	 * the node if present. The sync root itself is never deleted.
	 */
	public void handleDeleted(final ExternalEntry entryReference) throws FrameworkException {

		final NodeInterface node = resolve(entryReference, false);
		if (node != null) {

			if (target.syncRootUuid().equals(node.getUuid())) {

				logger.warn("External storage of sync target {} reports deletion of the sync root itself, not deleting node {}", target.syncRootPath(), node.getUuid());
				return;
			}

			app.delete(node);
			stats.deleted++;
		}
	}

	/**
	 * Push side: MOVED (path-addressed). Renames/re-parents the node found
	 * at the previous path, or creates the node at the new location if no
	 * node exists at the previous path. UUID-addressed backends achieve the
	 * same with a MODIFIED event carrying a new relativePath.
	 */
	public void handleMoved(final String fromRelativePath, final ExternalEntry to) throws FrameworkException {

		final NodeInterface node = resolveByPath(new ExternalEntry(fromRelativePath, null, to.directory(), null, null, null), false);
		if (node != null) {

			if (moveTo(node, to)) {

				updateNode(node, System.currentTimeMillis());
				stats.updated++;
			}

		} else {

			handleCreatedOrModified(to);
		}
	}

	// ----- private methods -----
	private NodeInterface resolve(final ExternalEntry entry, final boolean create) throws FrameworkException {

		if (entry.hasUuid()) {

			final NodeInterface node = app.getNodeById(StructrTraits.ABSTRACT_FILE, entry.nodeUuid());
			if (node != null) {

				if (!isGovernedByThisTarget(node.as(AbstractFile.class))) {

					logger.debug("Ignoring entry {} of sync target {}: node belongs to a different storage configuration", entry.nodeUuid(), target.syncRootPath());
					stats.ignored++;

					return null;
				}

				// the node dictates its own name/path unless the provider explicitly reports a differing path
				if (entry.hasPath()) {
					moveTo(node, entry);
				}

				return node;
			}

			if (!entry.hasPath()) {

				// a uuid-only entry without a node cannot be created: it has no name and no parent path
				logger.warn("Ignoring unknown uuid-only entry {} (native key {}) of sync target {}: no node with this UUID exists and no path was supplied", entry.nodeUuid(), entry.nativeKey(), target.syncRootPath());
				stats.ignored++;

				return null;
			}
		}

		return resolveByPath(entry, create);
	}

	private NodeInterface resolveByPath(final ExternalEntry entry, final boolean create) throws FrameworkException {

		final NodeInterface syncRoot = getSyncRoot();
		if (syncRoot == null || !entry.hasPath()) {
			return null;
		}

		// the empty relative path addresses the sync root itself
		if (entry.relativePath().isEmpty()) {
			return syncRoot;
		}

		if (!target.syncRootIsFolder()) {

			logger.warn("Ignoring entry {} of sync target {}: sync root is a single file and cannot contain other entries", entry.relativePath(), target.syncRootPath());
			stats.ignored++;

			return null;
		}

		final NodeInterface parentFolder = resolveParentFolder(syncRoot, entry, create);
		if (parentFolder == null) {
			return null;
		}

		return getOrCreate(syncRoot, parentFolder, entry, create);
	}

	private NodeInterface resolveParentFolder(final NodeInterface syncRoot, final ExternalEntry entry, final boolean create) throws FrameworkException {

		final String parentPath = entry.parentPath();
		if (parentPath.isEmpty()) {
			return syncRoot;
		}

		// walk existing folders segment by segment so nested sync roots are detected
		// before any intermediate folders are created
		final Traits traits                        = Traits.of(StructrTraits.ABSTRACT_FILE);
		final PropertyKey<NodeInterface> parentKey = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);
		final PropertyKey<String> nameKey          = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		NodeInterface current = syncRoot;

		for (final String segment : parentPath.split("/")) {

			final NodeInterface child = app.nodeQuery(StructrTraits.FOLDER).key(nameKey, segment).key(parentKey, current).getFirst();
			if (child == null) {

				if (!create) {
					return null;
				}

				// remaining folders do not exist yet and will inherit this target's
				// configuration, so no boundary can be crossed below this point.
				// read the sync root's path fresh from the node - ancestors may have
				// been renamed since the target snapshot was taken
				final String virtualParentPath = syncRoot.as(AbstractFile.class).getPath() + "/" + parentPath;

				return FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), virtualParentPath);
			}

			if (child.as(AbstractFile.class).getStorageConfiguration() != null) {

				logger.debug("Ignoring entry {} of sync target {}: it lies below nested sync root {}, which is governed by a different storage configuration", entry.relativePath(), target.syncRootPath(), child.getUuid());
				stats.ignored++;

				return null;
			}

			current = child;
		}

		return current;
	}

	private NodeInterface getOrCreate(final NodeInterface syncRoot, final NodeInterface parentFolder, final ExternalEntry entry, final boolean doCreate) throws FrameworkException {

		final Traits traits                        = Traits.of(StructrTraits.ABSTRACT_FILE);
		final PropertyKey<Boolean> isExternalKey   = traits.key(AbstractFileTraitDefinition.IS_EXTERNAL_PROPERTY);
		final PropertyKey<NodeInterface> parentKey = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);
		final PropertyKey<String> nameKey          = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		// kind-agnostic lookup: deletions cannot know whether the vanished entry
		// was a file or a folder, and existing nodes may have a subtype
		NodeInterface node = app.nodeQuery(StructrTraits.ABSTRACT_FILE).key(nameKey, entry.name()).key(parentKey, parentFolder).getFirst();
		if (node == null && doCreate) {

			final String type            = getTargetType(syncRoot, entry.directory());

			final PropertyMap properties = new PropertyMap();

			properties.put(nameKey,       entry.name());
			properties.put(parentKey,     parentFolder);
			properties.put(isExternalKey, true);

			if (entry.hasUuid()) {

				// keep uuid-keyed backends bound to their key
				properties.put(Traits.of(type).key(GraphObjectTraitDefinition.ID_PROPERTY), entry.nodeUuid());
			}

			node = app.create(type, properties);
			stats.created++;
		}

		return node;
	}

	private String getTargetType(final NodeInterface syncRoot, final boolean directory) {

		String fileType   = null;
		String folderType = null;

		if (syncRoot.is(StructrTraits.FOLDER)) {

			final Folder rootFolder = syncRoot.as(Folder.class);

			fileType   = rootFolder.getMountTargetFileType();
			folderType = rootFolder.getMountTargetFolderType();

			if (fileType != null) {

				final Traits traits = Traits.of(fileType);
				if (traits == null || !traits.contains(StructrTraits.FILE)) {

					logger.error("Given target type {} for synchronized files does not extend File.", fileType);
					fileType = null;
				}
			}

			if (folderType != null) {

				final Traits traits = Traits.of(folderType);
				if (traits == null || !traits.contains(StructrTraits.FOLDER)) {

					logger.error("Given target type {} for synchronized folders does not extend Folder.", folderType);
					folderType = null;
				}
			}
		}

		if (directory) {

			return folderType != null ? folderType : StructrTraits.FOLDER;
		}

		return fileType != null ? fileType : StructrTraits.FILE;
	}

	/**
	 * Re-parents/renames the given node to match the entry's relative path,
	 * if it differs from the node's current virtual location.
	 *
	 * @return true if the node was actually moved or renamed
	 */
	private boolean moveTo(final NodeInterface node, final ExternalEntry entry) throws FrameworkException {

		final NodeInterface syncRoot = getSyncRoot();
		if (syncRoot == null) {
			return false;
		}

		final AbstractFile abstractFile = node.as(AbstractFile.class);
		final String syncRootPath       = syncRoot.as(AbstractFile.class).getPath();
		final String expectedPath       = entry.relativePath().isEmpty() ? syncRootPath : syncRootPath + "/" + entry.relativePath();
		final String currentPath        = abstractFile.getPath();

		if (expectedPath.equals(currentPath)) {
			return false;
		}

		if (target.syncRootUuid().equals(node.getUuid())) {

			logger.warn("External storage of sync target {} reports a differing path {} for the sync root itself, not moving node {}", target.syncRootPath(), expectedPath, node.getUuid());
			return false;
		}

		final NodeInterface newParent = resolveParentFolder(syncRoot, entry, true);
		if (newParent == null) {
			return false;
		}

		final Traits traits = Traits.of(StructrTraits.ABSTRACT_FILE);

		node.setProperty(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY), newParent);
		node.setProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), entry.name());

		return true;
	}

	/**
	 * Ported from the legacy FileSyncWatchEventListener: fulltext indexing
	 * and full metadata refresh for files, lastSeenMounted for folders.
	 */
	private void updateNode(final NodeInterface node, final long timestamp) throws FrameworkException {

		if (node.is(StructrTraits.FILE)) {

			final NodeInterface syncRoot = getSyncRoot();

			if (syncRoot != null && syncRoot.is(StructrTraits.FOLDER)) {

				final PropertyKey<Boolean> doFulltextIndexing = Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_DO_FULLTEXT_INDEXING_PROPERTY);

				if (Boolean.TRUE.equals(syncRoot.getProperty(doFulltextIndexing))) {
					StructrApp.getInstance().getFulltextIndexer().addToFulltextIndex(node);
				}
			}

			FileHelper.updateMetadata(node.as(File.class), new PropertyMap(lastSeenMountedKey(), timestamp), true);

		} else if (node.is(StructrTraits.ABSTRACT_FILE)) {

			node.setProperty(lastSeenMountedKey(), timestamp);
		}
	}

	private boolean hasChanged(final File file, final ExternalEntry entry) {

		if (entry.size() == null || entry.lastModified() == null) {

			// the backend cannot provide cheap change detection
			return true;
		}

		final Long nodeSize = file.getSize();
		final Long nodeDate = file.getFileModificationDate();

		return nodeSize == null || nodeDate == null || !entry.size().equals(nodeSize) || !entry.lastModified().equals(nodeDate);
	}

	private boolean isGovernedByThisTarget(final AbstractFile file) {

		if (target.syncRootUuid().equals(file.getUuid())) {
			return true;
		}

		final AbstractFile supplier = StorageProviderFactory.getStorageConfigurationSupplier(file);

		return supplier != null && target.syncRootUuid().equals(supplier.getUuid());
	}

	private NodeInterface getSyncRoot() throws FrameworkException {

		final NodeInterface syncRoot = app.getNodeById(StructrTraits.ABSTRACT_FILE, target.syncRootUuid());
		if (syncRoot == null) {

			logger.warn("Sync root {} of sync target {} does not exist (anymore), skipping", target.syncRootUuid(), target.syncRootPath());
		}

		return syncRoot;
	}

	private PropertyKey<Long> lastSeenMountedKey() {
		return Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.LAST_SEEN_MOUNTED_PROPERTY);
	}

	/**
	 * Counters for scan summaries.
	 */
	public static class Stats {

		private long created   = 0;
		private long updated   = 0;
		private long unchanged = 0;
		private long deleted   = 0;
		private long ignored   = 0;

		public long getCreated()   { return created; }
		public long getUpdated()   { return updated; }
		public long getUnchanged() { return unchanged; }
		public long getDeleted()   { return deleted; }
		public long getIgnored()   { return ignored; }

		@Override
		public String toString() {
			return "created " + created + ", updated " + updated + ", unchanged " + unchanged + ", deleted " + deleted + ", ignored " + ignored;
		}
	}
}
