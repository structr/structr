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
import org.structr.api.service.*;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.SchemaService;
import org.structr.storage.StorageProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.sync.*;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FolderTraitDefinition;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Central synchronization service that keeps the Structr filesystem (File
 * and Folder nodes) in step with external storage backends. Storage
 * providers that implement {@link SynchronizableStorageProvider} monitor
 * their backend themselves and report changes here; this service owns all
 * graph writes, event debouncing/coalescing and scan scheduling.
 *
 * One synchronizer is maintained per sync target, i.e. per (AbstractFile,
 * StorageConfiguration) pair where the file/folder directly carries the
 * configuration. Nested folders with their own StorageConfiguration form
 * independent targets. Replaces the legacy DirectoryWatchService.
 */
@ServiceDependency(SchemaService.class)
@StopServiceForMaintenanceMode
public class StorageSyncService extends Thread implements RunnableService {

	public static final String DELETE_STALE_KEY = "sync.deleteStale";

	/**
	 * SecurityContext attribute marking graph modifications that originate
	 * from the external storage side. Lifecycle guards that reject
	 * Structr-initiated changes to external files (e.g. renames, which
	 * cannot be propagated to the physical storage) let sync-originated
	 * changes pass, because the physical side has already changed.
	 */
	public static final String SYNC_ORIGIN_ATTRIBUTE = "storageSyncOrigin";

	private static final long EVENT_DEBOUNCE_MILLIS = 2000L;

	private static final Logger logger = LoggerFactory.getLogger(StorageSyncService.class);

	private final Map<String, ActiveSync> targets      = new LinkedHashMap<>();
	private final Map<String, PendingEvent> eventQueue = new LinkedHashMap<>();
	private boolean running                            = false;

	public StorageSyncService() {

		super("StorageSyncService");
		setDaemon(true);
	}

	/**
	 * @return a superuser SecurityContext marked as sync-originated (see
	 * {@link #SYNC_ORIGIN_ATTRIBUTE}); all sync transactions use this.
	 */
	public static SecurityContext createSyncContext() {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		securityContext.setAttribute(SYNC_ORIGIN_ATTRIBUTE, true);

		return securityContext;
	}

	/**
	 * @return true if the given SecurityContext marks a modification that
	 * originates from the external storage side
	 */
	public static boolean isSyncOrigin(final SecurityContext securityContext) {
		return securityContext != null && Boolean.TRUE.equals(securityContext.getAttribute(SYNC_ORIGIN_ATTRIBUTE));
	}

	/**
	 * Notifies a running sync service (if any) that the given file/folder
	 * was created or modified, so its synchronizer can be created, updated
	 * or removed. Safe to call from node lifecycle callbacks.
	 */
	public static void handleNodeChanged(final AbstractFile abstractFile) {

		final StorageSyncService service = StructrApp.getInstance().getService(StorageSyncService.class);
		if (service != null && service.isRunning()) {

			service.attach(abstractFile);
		}
	}

	/**
	 * Notifies a running sync service (if any) that the node with the given
	 * UUID was deleted, so a synchronizer watching it can be closed.
	 */
	public static void handleNodeDeleted(final String uuid) {

		final StorageSyncService service = StructrApp.getInstance().getService(StorageSyncService.class);
		if (service != null && service.isRunning()) {

			service.detach(uuid);
		}
	}

	/**
	 * Notifies a running sync service (if any) that the StorageConfiguration
	 * with the given UUID (or one of its entries) changed or was deleted.
	 */
	public static void handleConfigurationChanged(final String storageConfigurationUuid) {

		final StorageSyncService service = StructrApp.getInstance().getService(StorageSyncService.class);
		if (service != null && service.isRunning()) {

			service.configurationChanged(storageConfigurationUuid);
		}
	}

	/**
	 * @return true if the node with the given UUID is an active sync root
	 */
	public boolean isSynchronized(final String uuid) {

		synchronized (targets) {
			return targets.containsKey(uuid);
		}
	}

	/**
	 * Creates (or re-creates) the synchronizer for the given sync root.
	 * Must be called within a transaction. No-op for nodes that do not
	 * directly carry a StorageConfiguration or whose provider cannot
	 * synchronize; an existing synchronizer is detached in that case.
	 */
	public void attach(final AbstractFile abstractFile) {

		final String uuid                 = abstractFile.getUuid();
		final StorageConfiguration config = abstractFile.getStorageConfiguration();

		if (config == null) {

			detach(uuid);
			return;
		}

		final StorageProvider provider = StorageProviderFactory.getStorageProvider(abstractFile);
		if (!(provider instanceof SynchronizableStorageProvider synchronizable)) {

			detach(uuid);
			return;
		}

		final boolean isFolder      = abstractFile.is(StructrTraits.FOLDER);
		final String path           = abstractFile.getPath();
		final SyncTarget syncTarget = new SyncTarget(uuid, path, isFolder, config.getUuid(), config.getConfiguration());

		// folder-level sync settings, defaults for single-file sync roots
		final Traits folderTraits    = Traits.of(StructrTraits.FOLDER);
		final boolean watchContents  = isFolder && Boolean.TRUE.equals(abstractFile.getProperty(folderTraits.key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY)));
		final Integer scanInterval   = isFolder ? abstractFile.getProperty(folderTraits.key(FolderTraitDefinition.MOUNT_SCAN_INTERVAL_PROPERTY)) : null;
		final Long lastScanned       = isFolder ? abstractFile.getProperty(folderTraits.key(FolderTraitDefinition.MOUNT_LAST_SCANNED_PROPERTY)) : null;
		final boolean deleteStale    = Boolean.parseBoolean(syncTarget.configuration().get(DELETE_STALE_KEY));

		synchronized (targets) {

			final ActiveSync existing = targets.get(uuid);
			if (existing != null) {

				if (existing.target.equals(syncTarget) && existing.watchContents == watchContents) {

					// unchanged target: only update the scan interval
					existing.setScanInterval(scanInterval);
					return;
				}

				// target changed: close the old synchronizer before re-creating
				detach(uuid);
			}

			final StorageSynchronizer synchronizer;

			try {

				synchronizer = synchronizable.createSynchronizer(syncTarget);

			} catch (IOException ioex) {

				logger.warn("Unable to create synchronizer for {}: {}", path, ioex.getMessage());
				return;
			}

			if (synchronizer == null) {

				// this configuration has no external side to synchronize
				return;
			}

			logger.info("Synchronizing {} via {}..", path, synchronizer.getClass().getSimpleName());

			final ActiveSync sync = new ActiveSync(syncTarget, synchronizer, scanInterval, watchContents, deleteStale);

			targets.put(uuid, sync);

			if (watchContents && synchronizer.supportsWatching()) {

				try {

					synchronizer.startWatching(new BoundListener(uuid));

				} catch (IOException ioex) {
					logger.warn("Unable to start watching {}: {}", path, ioex.getMessage());
				}
			}

			// upon creation, set the last scanned date correctly to prevent early scanning
			final boolean wasNeverScanned = (lastScanned == null);

			if (!wasNeverScanned) {
				sync.lastScanned = lastScanned;
			}

			if (wasNeverScanned || sync.shouldScan()) {

				sync.lastScanned = System.currentTimeMillis();

				new Thread(new ScanJob(sync), "StorageSyncScan-" + uuid).start();

			} else {

				logger.info("Not scanning {} - scan interval is not yet elapsed.", path);
			}
		}
	}

	/**
	 * Closes and removes the synchronizer for the given sync root, if one
	 * exists.
	 */
	public void detach(final String syncRootUuid) {

		final ActiveSync sync;

		synchronized (targets) {
			sync = targets.remove(syncRootUuid);
		}

		if (sync != null) {

			logger.info("Stopping synchronization of {}..", sync.target.syncRootPath());
			closeQuietly(sync.synchronizer);
		}
	}

	/**
	 * Re-attaches all sync roots linked to the given StorageConfiguration
	 * after it (or one of its entries) changed, and detaches sync roots
	 * whose configuration was removed. Must be called within a transaction.
	 */
	public void configurationChanged(final String storageConfigurationUuid) {

		final List<String> attachedRoots = new LinkedList<>();

		synchronized (targets) {

			for (final ActiveSync sync : targets.values()) {

				if (storageConfigurationUuid.equals(sync.target.storageConfigurationUuid())) {
					attachedRoots.add(sync.target.syncRootUuid());
				}
			}
		}

		try {

			final App app                  = StructrApp.getInstance();
			final NodeInterface configNode = app.getNodeById(StructrTraits.STORAGE_CONFIGURATION, storageConfigurationUuid);

			if (configNode == null) {

				attachedRoots.forEach(this::detach);
				return;
			}

			final PropertyKey<NodeInterface> configKey = Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY);
			final Set<String> linkedRoots              = new LinkedHashSet<>();

			for (final NodeInterface node : app.nodeQuery(StructrTraits.ABSTRACT_FILE).key(configKey, configNode).getAsList()) {

				linkedRoots.add(node.getUuid());
				attach(node.as(AbstractFile.class));
			}

			for (final String attachedRoot : attachedRoots) {

				if (!linkedRoots.contains(attachedRoot)) {
					detach(attachedRoot);
				}
			}

		} catch (FrameworkException fex) {
			logger.warn("Unable to handle storage configuration change of {}: {}", storageConfigurationUuid, fex.getMessage());
		}
	}

	// ----- interface RunnableService -----
	@Override
	public void run() {

		while (running) {

			final Services services = Services.peekInstance();

			// Services has been shut down: stop instead of calling getInstance(), which would
			// re-initialize the whole service layer on this background thread (fatal for the
			// embedded database -> System.exit, killing e.g. a test fork).
			if (services == null) {
				break;
			}

			if (!services.isInitialized()) {

				try { Thread.sleep(1000); } catch (InterruptedException i) {}

				// loop until we are stopped
				continue;
			}

			synchronized (targets) {

				for (final ActiveSync sync : targets.values()) {

					if (sync.fullScanRequested || sync.shouldScan()) {

						sync.fullScanRequested = false;
						sync.lastScanned       = System.currentTimeMillis();

						new Thread(new ScanJob(sync), "StorageSyncScan-" + sync.target.syncRootUuid()).start();
					}
				}
			}

			try { Thread.sleep(100); } catch (InterruptedException i) {}

			// collect all events that are older than the debounce window
			final List<PendingEvent> dueEvents = new LinkedList<>();

			synchronized (eventQueue) {

				for (final Iterator<PendingEvent> it = eventQueue.values().iterator(); it.hasNext();) {

					final PendingEvent item = it.next();
					if (item.olderThan(EVENT_DEBOUNCE_MILLIS)) {

						dueEvents.add(item);
						it.remove();
					}
				}
			}

			if (!dueEvents.isEmpty()) {

				final SecurityContext securityContext = createSyncContext();

				try (final Tx tx = StructrApp.getInstance(securityContext).tx(true, true, false)) {

					tx.prefetchHint("StorageSyncService main loop");

					for (final PendingEvent item : dueEvents) {

						handleEvent(item);
					}

					tx.success();

				} catch (Throwable t) {

					logger.warn("Unable to process storage sync event queue, waiting for 1 minute before trying again.");

					try { Thread.sleep(TimeUnit.MINUTES.toMillis(1)); } catch (InterruptedException i) {}
				}
			}
		}

		logger.info("StorageSyncService stopped");
	}

	@Override
	public void startService() throws Exception {

		final PropertyKey<String> storageConfigurationKey = Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY);
		final App app                                     = StructrApp.getInstance();

		try (final Tx tx = app.tx(false, false, false)) {

			// find all files/folders that directly carry a storage configuration and try to attach them
			for (final NodeInterface node : app.nodeQuery(StructrTraits.ABSTRACT_FILE).notBlank(storageConfigurationKey).getAsList()) {

				attach(node.as(AbstractFile.class));
			}

			tx.success();
		}

		running = true;

		this.start();
	}

	@Override
	public void stopService() {

		running = false;

		synchronized (targets) {

			for (final ActiveSync sync : targets.values()) {
				closeQuietly(sync.synchronizer);
			}

			targets.clear();
		}
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public ServiceResult initialize(final StructrServices services, String serviceName) throws ReflectiveOperationException {

		return new ServiceResult(true);
	}

	@Override
	public void shutdown() {
		stopService();
	}

	@Override
	public void initialized() {
	}

	@Override
	public boolean isVital() {
		return false;
	}

	@Override
	public boolean waitAndRetry() {
		return false;
	}

	@Override
	public String getModuleName() {
		return "ui";
	}

	// ----- package methods -----
	ActiveSync getActiveSync(final String syncRootUuid) {

		synchronized (targets) {
			return targets.get(syncRootUuid);
		}
	}

	// ----- private methods -----
	private void handleEvent(final PendingEvent item) {

		final ActiveSync sync = getActiveSync(item.syncRootUuid);
		if (sync == null) {

			// target was detached while the event was queued
			return;
		}

		try (final Tx tx = StructrApp.getInstance(createSyncContext()).tx()) {

			final ExternalFileSyncHandler handler = new ExternalFileSyncHandler(sync.target);
			final ExternalChangeEvent event       = item.event;

			switch (event.type()) {

				case CREATED, MODIFIED -> handler.handleCreatedOrModified(event.entry());
				case DELETED           -> handler.handleDeleted(event.entry());
				case MOVED             -> handler.handleMoved(event.previousRelativePath(), event.entry());
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.error(ExceptionUtils.getStackTrace(fex));
		}
	}

	private void addToQueue(final PendingEvent item) {

		final String key = item.getKey();

		synchronized (eventQueue) {

			// queue should contain at most one item for the given key
			final PendingEvent existingItem = eventQueue.get(key);
			if (existingItem != null) {

				final ExternalChangeEvent.Type existingType = existingItem.event.type();
				final ExternalChangeEvent.Type newType      = item.event.type();

				switch (existingType) {

					case CREATED, MOVED -> {

						switch (newType) {

							// delay existing item (duplicate CREATE / additional MODIFY)
							case CREATED, MODIFIED -> existingItem.time = item.time;

							// remove CREATE due to DELETE
							case DELETED           -> eventQueue.remove(key);

							// MOVED replaces the pending item
							case MOVED             -> eventQueue.put(key, item);
						}
					}

					case MODIFIED -> {

						switch (newType) {

							// delay MODIFY
							case MODIFIED                 -> existingItem.time = item.time;

							// CREATE / DELETE / MOVED replace MODIFY
							case CREATED, DELETED, MOVED  -> eventQueue.put(key, item);
						}
					}

					case DELETED -> {

						switch (newType) {

							// CREATE / MODIFY / MOVED replace DELETE
							case CREATED, MODIFIED, MOVED -> eventQueue.put(key, item);

							// earlier DELETE can stay in the queue
							case DELETED                  -> { }
						}
					}
				}

			} else {

				eventQueue.put(key, item);
			}
		}
	}

	private static void closeQuietly(final StorageSynchronizer synchronizer) {

		try {

			synchronizer.close();

		} catch (Throwable t) {
			logger.warn("Error while closing synchronizer for {}: {}", synchronizer.getTarget().syncRootPath(), t.getMessage());
		}
	}

	// ----- nested classes -----
	/**
	 * Listener handed to a synchronizer, pre-bound to its sync root. Only
	 * enqueues - all graph work happens on the service thread.
	 */
	private class BoundListener implements StorageSyncListener {

		private final String syncRootUuid;

		public BoundListener(final String syncRootUuid) {
			this.syncRootUuid = syncRootUuid;
		}

		@Override
		public void onEvent(final ExternalChangeEvent event) {
			addToQueue(new PendingEvent(syncRootUuid, event));
		}

		@Override
		public void requestFullScan() {

			final ActiveSync sync = getActiveSync(syncRootUuid);
			if (sync != null) {

				sync.fullScanRequested = true;
			}
		}
	}

	/**
	 * State of one active sync target.
	 */
	static final class ActiveSync {

		final SyncTarget target;
		final StorageSynchronizer synchronizer;
		final boolean watchContents;
		final boolean deleteStale;

		volatile boolean fullScanRequested = false;
		private long scanInterval          = 0L;
		long lastScanned                   = 0L;

		ActiveSync(final SyncTarget target, final StorageSynchronizer synchronizer, final Integer scanInterval, final boolean watchContents, final boolean deleteStale) {

			this.target        = target;
			this.synchronizer  = synchronizer;
			this.watchContents = watchContents;
			this.deleteStale   = deleteStale;
			this.lastScanned   = System.currentTimeMillis();

			setScanInterval(scanInterval);
		}

		void setScanInterval(final Integer scanIntervalSeconds) {
			this.scanInterval = scanIntervalSeconds != null ? scanIntervalSeconds * 1000L : 0L;
		}

		boolean shouldScan() {
			return scanInterval > 0 && System.currentTimeMillis() > (lastScanned + scanInterval);
		}
	}

	/**
	 * A queued change event, debounced and coalesced per sync root and
	 * addressing key.
	 */
	private static final class PendingEvent {

		private final String syncRootUuid;
		private final ExternalChangeEvent event;
		private long time;

		public PendingEvent(final String syncRootUuid, final ExternalChangeEvent event) {

			this.syncRootUuid = syncRootUuid;
			this.event        = event;
			this.time         = System.nanoTime();
		}

		public String getKey() {

			final ExternalEntry entry = event.entry();

			return syncRootUuid + " " + (entry.hasUuid() ? "#" + entry.nodeUuid() : entry.relativePath());
		}

		public boolean olderThan(final long milliseconds) {

			final double now = System.nanoTime();
			final double dt  = now - time;

			return dt > (milliseconds * 1_000_000);
		}
	}
}
