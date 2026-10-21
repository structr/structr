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
import org.structr.storage.sync.VanishPolicy;
import org.structr.web.entity.AbstractFile;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.StorageConfigurationTraitDefinition;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Pull-side reconciliation for one sync target: consumes the synchronizer's
 * enumeration in batched transactions, then applies the target's VanishPolicy
 * (configuration entry "sync.onVanished") to external nodes that were not seen
 * during the scan - i.e. entries that vanished from the external storage while
 * Structr was not watching. The traversal stops at nested sync roots, which are
 * governed by their own synchronizer.
 */
class ScanJob implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ScanJob.class);

	private static final int BATCH_SIZE = 1000;

	private final StorageSyncService service;
	private final StorageSyncService.ActiveSync sync;
	private final SyncTarget target;

	ScanJob(final StorageSyncService service, final StorageSyncService.ActiveSync sync) {

		this.service = service;
		this.sync    = sync;
		this.target  = sync.target;
	}

	@Override
	public void run() {

		// bail if the service layer is shutting down - touching the graph now
		// would re-initialize it (System.exit under the embedded driver)
		if (!service.isReady()) {

			return;
		}

		// wait for the creating or modifying transaction to finish before we start,
		// otherwise the sync root will not be available and no files will be created
		if (!waitForSyncRoot()) {

			logger.warn("Unable to scan {}, sync root node was not created", target.syncRootPath());

			return;
		}

		final ExternalFileSyncHandler handler = new ExternalFileSyncHandler(target, sync.vanishPolicy);
		final long scanStart                  = System.currentTimeMillis();
		long reconciled                       = 0;

		if (!discover(handler, scanStart)) {

			// a partial enumeration cannot distinguish a vanished entry from an
			// unread one, so it never reconciles

			return;
		}

		if (sync.vanishPolicy.reconciles()) {

			reconciled = reconcile(scanStart);

			if (reconciled < 0) {

				// the next complete scan applies the policy again from the start

				return;
			}
		}

		updateLastScanned(scanStart);

		final ExternalFileSyncHandler.Stats stats = handler.getStats();
		final boolean changed                     = stats.getCreated() + stats.getUpdated() + stats.getDeleted() + stats.getMarked() + reconciled > 0;

		// a scan that found nothing to do is not news, which matters at short intervals
		if (changed && Boolean.FALSE.equals(Settings.LogDirectoryWatchServiceQuiet.getValue())) {

			logger.info("Scanned {}: {}, vanish policy {} applied to {}", target.syncRootPath(), stats, sync.vanishPolicy.value(), reconciled);

		} else {

			logger.debug("Scanned {}: {}, vanish policy {} applied to {}", target.syncRootPath(), stats, sync.vanishPolicy.value(), reconciled);
		}
	}

	// ----- private methods -----
	private boolean waitForSyncRoot() {

		for (int i = 0; i < 3; i++) {

			if (!service.isReady()) {

				return false;
			}

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

				if (!service.isReady()) {

					return false;
				}

				if (!syncRootExists()) {

					logger.info("Stopping the scan of {}: its sync root no longer exists", target.syncRootPath());

					return false;
				}

				// read the batch before opening the transaction: enumerating may mean I/O against
				// the backend, which does not belong inside a write transaction
				final List<ExternalEntry> batch = new ArrayList<>(BATCH_SIZE);

				while (it.hasNext() && batch.size() < BATCH_SIZE) {

					batch.add(it.next());
				}

				if (handleInOneTransaction(securityContext, handler, batch, scanStart) != null) {

					// validation runs when the batch commits, so one invalid entry fails all of
					// them: handle each on its own so that only the offending one is missing
					for (final ExternalEntry entry : batch) {

						final Throwable failure = handleInOneTransaction(securityContext, handler, List.of(entry), scanStart);
						if (failure != null) {

							logger.warn("Unable to import {} of {}: {}", entry.hasPath() ? entry.relativePath() : "#" + entry.nodeUuid(), target.syncRootPath(), failure.getMessage());
						}
					}
				}
			}

			return true;

		} catch (IOException ex) {

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

	private boolean syncRootExists() {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final boolean exists = StructrApp.getInstance().getNodeById(StructrTraits.ABSTRACT_FILE, target.syncRootUuid()) != null;

			tx.success();

			return exists;

		} catch (FrameworkException fex) {

			return false;
		}
	}

	/**
	 * @return null if the given entries were handled and committed, the failure otherwise
	 */
	private Throwable handleInOneTransaction(final SecurityContext securityContext, final ExternalFileSyncHandler handler, final List<ExternalEntry> entries, final long scanStart) {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx(true, false, false)) {

			for (final ExternalEntry entry : entries) {

				handler.handleDiscovered(entry, scanStart);
			}

			tx.success();

			return null;

		} catch (Throwable t) {

			return t;
		}
	}

	/**
	 * Applies the target's VanishPolicy to external nodes below the sync root
	 * that were not seen by the scan that started at the given timestamp.
	 * Children are handled before their parents; nodes with non-external or
	 * foreign-governed content below them are kept.
	 *
	 * @return the number of nodes deleted or newly marked, or -1 if the policy could not be applied
	 * completely: it stops at the first batch that fails, since a parent folder later in the list
	 * would otherwise be deleted above children that were kept
	 */
	private long reconcile(final long scanStart) {

		if (!service.isReady()) {

			return -1;
		}

		final List<String> staleUuids = new LinkedList<>();

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final NodeInterface syncRoot = StructrApp.getInstance().getNodeById(StructrTraits.ABSTRACT_FILE, target.syncRootUuid());
			if (syncRoot != null) {

				collectStale(syncRoot, scanStart, staleUuids);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("Unable to determine stale nodes of {}: {}", target.syncRootPath(), fex.getMessage());

			return -1;
		}

		final boolean markOnly    = VanishPolicy.READONLY.equals(sync.vanishPolicy);
		final Iterator<String> it = staleUuids.iterator();
		long applied              = 0;

		while (it.hasNext()) {

			if (!service.isReady()) {

				return -1;
			}

			int batchCount   = 0;
			long batchApplied = 0;

			// the sync-origin context is what tells the lifecycle guards that the
			// physical side is already gone, so neither the delete nor the mark is
			// propagated back to the external storage
			final App app = StructrApp.getInstance(StorageSyncService.createSyncContext());

			try (final Tx tx = app.tx(true, false, false)) {

				while (it.hasNext() && batchCount++ < BATCH_SIZE) {

					final NodeInterface node = app.getNodeById(StructrTraits.ABSTRACT_FILE, it.next());
					if (node == null) {

						continue;
					}

					if (markOnly) {

						final AbstractFile file = node.as(AbstractFile.class);

						// every later scan sees an already marked node as stale again,
						// so re-marking it would be a write per scan interval
						if (!file.isExternallyDeleted()) {

							file.setExternallyDeleted(true);
							batchApplied++;
						}

					} else {

						app.delete(node);
						batchApplied++;
					}
				}

				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("Unable to reconcile stale nodes of {}: {}", target.syncRootPath(), fex.getMessage());

				return -1;
			}

			applied += batchApplied;
		}

		return applied;
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

		if (!service.isReady()) {

			return;
		}

		// written as the sync layer, so that recording the scan does not re-attach the mount
		final App app = StructrApp.getInstance(StorageSyncService.createSyncContext());

		try (final Tx tx = app.tx()) {

			final NodeInterface configuration = app.getNodeById(StructrTraits.STORAGE_CONFIGURATION, target.storageConfigurationUuid());
			if (configuration != null) {

				configuration.setProperty(Traits.of(StructrTraits.STORAGE_CONFIGURATION).key(StorageConfigurationTraitDefinition.LAST_SCANNED_PROPERTY), scanStart);
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.error(ExceptionUtils.getStackTrace(fex));
		}
	}
}
