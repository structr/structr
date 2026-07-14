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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.SyncTarget;
import org.structr.web.entity.AbstractFile;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FolderTraitDefinition;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Pull-side reconciliation for one sync target: consumes the synchronizer's
 * enumeration in batched transactions, then (opt-in via the configuration
 * entry "sync.deleteStale") prunes external nodes that were not seen during
 * the scan - i.e. entries that vanished from the external storage while
 * Structr was not watching. The prune traversal stops at nested sync roots,
 * which are governed by their own synchronizer.
 */
class ScanJob implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ScanJob.class);

	private static final int BATCH_SIZE = 1000;

	private final StorageSyncService.ActiveSync sync;
	private final SyncTarget target;

	ScanJob(final StorageSyncService.ActiveSync sync) {

		this.sync   = sync;
		this.target = sync.target;
	}

	@Override
	public void run() {

		// wait for the creating or modifying transaction to finish before we start,
		// otherwise the sync root will not be available and no files will be created
		if (!waitForSyncRoot()) {

			logger.warn("Unable to scan {}, sync root node was not created", target.syncRootPath());
			return;
		}

		final ExternalFileSyncHandler handler = new ExternalFileSyncHandler(target);
		final long scanStart                  = System.currentTimeMillis();
		long pruned                           = 0;

		if (!discover(handler, scanStart)) {

			// never prune after a partial scan
			return;
		}

		if (sync.deleteStale) {

			pruned = prune(scanStart);
		}

		updateLastScanned(scanStart);

		if (Boolean.FALSE.equals(Settings.LogDirectoryWatchServiceQuiet.getValue())) {

			logger.info("Scanned {}: {}, pruned {}", target.syncRootPath(), handler.getStats(), pruned);
		}
	}

	// ----- private methods -----
	private boolean waitForSyncRoot() {

		for (int i = 0; i < 3; i++) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				if (StructrApp.getInstance().getNodeById(StructrTraits.ABSTRACT_FILE, target.syncRootUuid()) != null) {

					return true;
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.error(ExceptionUtils.getStackTrace(fex));
			}

			// wait for the transaction in a different thread to finish
			try { Thread.sleep(1000); } catch (InterruptedException ex) {}
		}

		return false;
	}

	/**
	 * Consumes the synchronizer's enumeration in batched transactions.
	 *
	 * @return true if the enumeration completed without errors
	 */
	private boolean discover(final ExternalFileSyncHandler handler, final long scanStart) {

		final SecurityContext securityContext = StorageSyncService.createSyncContext();

		// configure security context for maximum performance
		securityContext.disablePreventDuplicateRelationships();
		securityContext.disableModificationOfAccessTime();

		try {

			final Iterator<ExternalEntry> it = sync.synchronizer.enumerate(null);

			while (it.hasNext()) {

				int batchCount = 0;

				try (final Tx tx = StructrApp.getInstance(securityContext).tx(true, false, false)) {

					while (it.hasNext() && batchCount++ < BATCH_SIZE) {

						handler.handleDiscovered(it.next(), scanStart);
					}

					tx.success();
				}
			}

			return true;

		} catch (IOException | FrameworkException ex) {

			logger.warn("Unable to scan {}: {}", target.syncRootPath(), ex.getMessage());

		} catch (UncheckedIOException uioe) {

			// synchronizers surface expected operational failures (endpoint
			// unreachable, bucket missing, ...) as an already-concise cause;
			// log that, not the SDK's verbose async stack trace
			final Throwable cause = uioe.getCause() != null ? uioe.getCause() : uioe;

			logger.warn("Unable to scan {}: {}", target.syncRootPath(), cause.getMessage());

		} catch (Throwable t) {

			logger.error("Unable to scan {}: {}", target.syncRootPath(), ExceptionUtils.getStackTrace(t));
		}

		return false;
	}

	/**
	 * Deletes external nodes below the sync root that were not seen by the
	 * scan that started at the given timestamp. Children are deleted before
	 * their parents; nodes with non-external or foreign-governed content
	 * below them are kept.
	 *
	 * @return the number of deleted nodes
	 */
	private long prune(final long scanStart) {

		final List<String> staleUuids = new LinkedList<>();

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final NodeInterface syncRoot = StructrApp.getInstance().getNodeById(StructrTraits.ABSTRACT_FILE, target.syncRootUuid());
			if (syncRoot != null && target.syncRootIsFolder()) {

				collectStale(syncRoot, scanStart, staleUuids);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("Unable to determine stale nodes of {}: {}", target.syncRootPath(), fex.getMessage());
			return 0;
		}

		final Iterator<String> it = staleUuids.iterator();
		long deleted              = 0;

		while (it.hasNext()) {

			int batchCount = 0;

			try (final Tx tx = StructrApp.getInstance(StorageSyncService.createSyncContext()).tx(true, false, false)) {

				while (it.hasNext() && batchCount++ < BATCH_SIZE) {

					final NodeInterface node = StructrApp.getInstance().getNodeById(StructrTraits.ABSTRACT_FILE, it.next());
					if (node != null) {

						StructrApp.getInstance().delete(node);
						deleted++;
					}
				}

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("Unable to prune stale nodes of {}: {}", target.syncRootPath(), fex.getMessage());
			}
		}

		return deleted;
	}

	/**
	 * Post-order traversal collecting deletable nodes (children before
	 * parents).
	 *
	 * @return true if the subtree of the given node contains anything that
	 * must be kept
	 */
	private boolean collectStale(final NodeInterface node, final long scanStart, final List<String> out) throws FrameworkException {

		final Traits traits                        = Traits.of(StructrTraits.ABSTRACT_FILE);
		final PropertyKey<NodeInterface> parentKey = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);
		final PropertyKey<Long> lastSeenKey        = traits.key(AbstractFileTraitDefinition.LAST_SEEN_MOUNTED_PROPERTY);
		final boolean isSyncRoot                   = target.syncRootUuid().equals(node.getUuid());

		if (!isSyncRoot && node.as(AbstractFile.class).getStorageConfiguration() != null) {

			// nested sync root: governed by its own synchronizer, keep the whole subtree
			return true;
		}

		boolean kept = false;

		if (node.is(StructrTraits.FOLDER)) {

			for (final NodeInterface child : StructrApp.getInstance().nodeQuery(StructrTraits.ABSTRACT_FILE).key(parentKey, node).getAsList()) {

				kept |= collectStale(child, scanStart, out);
			}
		}

		if (isSyncRoot) {
			return true;
		}

		final Long lastSeen  = node.getProperty(lastSeenKey);
		final boolean stale  = node.as(AbstractFile.class).isExternal() && (lastSeen == null || lastSeen < scanStart);

		if (stale && !kept) {

			out.add(node.getUuid());
			return false;
		}

		return true;
	}

	private void updateLastScanned(final long scanStart) {

		if (!target.syncRootIsFolder()) {
			return;
		}

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final NodeInterface syncRoot = StructrApp.getInstance().getNodeById(StructrTraits.FOLDER, target.syncRootUuid());
			if (syncRoot != null) {

				syncRoot.setProperty(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_LAST_SCANNED_PROPERTY), scanStart);
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.error(ExceptionUtils.getStackTrace(fex));
		}
	}
}
