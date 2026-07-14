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
import java.util.concurrent.ConcurrentHashMap;
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
	public static final String DIRECTION_KEY    = SyncDirection.DIRECTION_KEY;

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
	private final List<PendingOutbound> outboundQueue  = new LinkedList<>();
	private final Set<Thread> scanThreads              = ConcurrentHashMap.newKeySet();
	private volatile boolean running                   = false;

	// per-transaction capture state for outbound (Structr->physical) propagation:
	// pre-move locations stashed before a parent relationship changes (the old
	// parent is not recoverable afterwards), and the events captured during the
	// transaction, coalesced per node. Flushed or discarded exactly once per
	// toplevel transaction by handleTransactionFinished().
	private static final ThreadLocal<Map<String, PreMoveLocation>> preMoveStash   = new ThreadLocal<>();
	private static final ThreadLocal<Map<String, PendingOutbound>> capturedEvents = new ThreadLocal<>();

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
	 * @return the outbound-enabled sync target governing the given node
	 * (nearest-config rule), or null if the service is not running, the node
	 * is not governed by a sync target, or the governing target's direction
	 * does not include OUT. Must be called within a transaction.
	 */
	public static OutboundTarget getOutboundTarget(final AbstractFile file) {

		final StorageSyncService service = runningService();
		if (service == null) {
			return null;
		}

		synchronized (service.targets) {

			if (service.targets.isEmpty()) {
				return null;
			}
		}

		final AbstractFile supplier = StorageProviderFactory.getStorageConfigurationSupplier(file);
		if (supplier == null) {
			return null;
		}

		final ActiveSync sync = service.getActiveSync(supplier.getUuid());
		if (sync == null || !sync.direction.isOutbound()) {
			return null;
		}

		// read the sync root's path fresh from the node - ancestors may have
		// been renamed since the target snapshot was taken
		return new OutboundTarget(supplier.getUuid(), supplier.getPath());
	}

	/**
	 * @return true if the given node is governed by an outbound-enabled sync
	 * target (its structural changes are propagated to the external storage)
	 */
	public static boolean isOutboundGoverned(final AbstractFile file) {
		return getOutboundTarget(file) != null;
	}

	/**
	 * Called before a parent/parentId change is applied to the given node.
	 * Stashes the node's current absolute path and its current governing
	 * outbound target - the old parent is not recoverable after the
	 * relationship changed. The first call per node and transaction wins,
	 * preserving the original location.
	 */
	public static void stashPreMoveLocation(final AbstractFile file) {

		final OutboundTarget target = getOutboundTarget(file);
		if (target == null) {
			return;
		}

		Map<String, PreMoveLocation> stash = preMoveStash.get();
		if (stash == null) {

			stash = new LinkedHashMap<>();
			preMoveStash.set(stash);
		}

		stash.putIfAbsent(file.getUuid(), new PreMoveLocation(target, file.getPath()));
	}

	/**
	 * Called from the AbstractFile modification callback for changes that do
	 * not originate from the sync layer. Captures a MOVED, DELETED and/or
	 * CREATED event depending on the node's old and new governance.
	 *
	 * @param previousName the node's previous name from the modification
	 * queue's before map, or null if the name did not change
	 * @return true if the location change is covered by outbound
	 * synchronization (callers must not reject the rename), false otherwise
	 */
	public static boolean recordOutboundLocationChange(final AbstractFile file, final String previousName) {

		final Map<String, PreMoveLocation> stash = preMoveStash.get();
		final String uuid                        = file.getUuid();
		final PreMoveLocation stashed            = stash != null ? stash.get(uuid) : null;

		if (stashed == null && previousName == null) {

			// no location change: the return value only matters for renames
			return false;
		}

		final OutboundTarget newTarget = getOutboundTarget(file);
		final OutboundTarget oldTarget = stashed != null ? stashed.target() : newTarget;

		if (oldTarget == null && newTarget == null) {
			return false;
		}

		// the sync root's physical location is defined by its configuration,
		// not by its virtual path - no event, but the change is covered
		if ((oldTarget != null && uuid.equals(oldTarget.syncRootUuid())) || (newTarget != null && uuid.equals(newTarget.syncRootUuid()))) {
			return true;
		}

		final String currentPath    = file.getPath();
		final String oldParentPath  = parentOf(stashed != null ? stashed.absolutePath() : currentPath);
		final String oldName        = previousName != null ? previousName : file.getName();
		final String oldAbsolute    = oldParentPath + "/" + oldName;
		final boolean directory     = file.is(StructrTraits.FOLDER);
		final String oldRelative    = oldTarget != null ? relativize(oldTarget, oldAbsolute) : null;
		final String newRelative    = newTarget != null ? relativize(newTarget, currentPath) : null;

		if (oldTarget != null && newTarget != null && oldTarget.syncRootUuid().equals(newTarget.syncRootUuid())) {

			// moved/renamed within the same target
			if (oldRelative != null && newRelative != null && !oldRelative.equals(newRelative)) {

				capture(oldTarget.syncRootUuid(), VirtualChangeEvent.moved(uuid, directory, oldRelative, newRelative));
			}

			return true;
		}

		if (oldTarget != null && oldRelative != null) {

			// moved out of the old target
			capture(oldTarget.syncRootUuid(), VirtualChangeEvent.deleted(uuid, directory, oldRelative));
		}

		if (newTarget != null && newRelative != null) {

			// moved into the new target
			capture(newTarget.syncRootUuid(), VirtualChangeEvent.created(uuid, directory, newRelative));
		}

		return true;
	}

	/**
	 * Called from the pre-delete callback while the node and its parent
	 * chain are still intact. Captures a DELETED event.
	 *
	 * @return true if the deletion is covered by outbound synchronization
	 * (the direct physical delete must then be skipped)
	 */
	public static boolean recordOutboundDeletion(final AbstractFile file) {

		final OutboundTarget target = getOutboundTarget(file);
		if (target == null) {
			return false;
		}

		final String uuid = file.getUuid();

		if (uuid.equals(target.syncRootUuid())) {

			// deleting the sync root node does not delete the external storage
			// behind it - the physical root is defined by the configuration
			return true;
		}

		final String relativePath = relativize(target, file.getPath());
		if (relativePath == null) {
			return false;
		}

		capture(target.syncRootUuid(), VirtualChangeEvent.deleted(uuid, file.is(StructrTraits.FOLDER), relativePath));

		return true;
	}

	/**
	 * Called from the folder creation callback: captures a CREATED event for
	 * new folders below an outbound-governed subtree, so path-based backends
	 * can materialize the physical directory.
	 */
	public static void recordOutboundCreation(final AbstractFile file) {

		final OutboundTarget target = getOutboundTarget(file);
		if (target == null) {
			return;
		}

		final String uuid = file.getUuid();

		if (uuid.equals(target.syncRootUuid())) {
			return;
		}

		final String relativePath = relativize(target, file.getPath());
		if (relativePath == null) {
			return;
		}

		capture(target.syncRootUuid(), VirtualChangeEvent.created(uuid, file.is(StructrTraits.FOLDER), relativePath));
	}

	/**
	 * Called by TransactionCommand.finishTx for every finished toplevel
	 * transaction: discards the capture buffer on rollback, enqueues the
	 * captured events for asynchronous dispatch on commit. Cheap no-op when
	 * nothing was captured.
	 */
	public static void handleTransactionFinished(final boolean successful) {

		final Map<String, PendingOutbound> captured = capturedEvents.get();

		preMoveStash.remove();
		capturedEvents.remove();

		if (!successful || captured == null || captured.isEmpty()) {
			return;
		}

		final StorageSyncService service = runningService();
		if (service == null) {
			return;
		}

		// dispatch order: CREATED and MOVED in capture order, DELETED
		// deepest-path-first so children empty their parent directories
		// before the directory's own deletion arrives
		final List<PendingOutbound> ordered = new LinkedList<>();
		final List<PendingOutbound> deleted = new LinkedList<>();

		for (final PendingOutbound pending : captured.values()) {

			if (VirtualChangeEvent.Type.DELETED.equals(pending.event().type())) {

				deleted.add(pending);

			} else {

				ordered.add(pending);
			}
		}

		deleted.sort(Comparator.comparingInt((final PendingOutbound p) -> p.event().previousRelativePath().split("/").length).reversed());

		ordered.addAll(deleted);

		service.enqueueOutbound(ordered);
	}

	/**
	 * Notifies a running sync service (if any) that the given file/folder
	 * was created or modified, so its synchronizer can be created, updated
	 * or removed. Safe to call from node lifecycle callbacks.
	 */
	public static void handleNodeChanged(final AbstractFile abstractFile) {

		final StorageSyncService service = runningService();
		if (service != null) {

			service.attach(abstractFile);
		}
	}

	/**
	 * Notifies a running sync service (if any) that the node with the given
	 * UUID was deleted, so a synchronizer watching it can be closed.
	 */
	public static void handleNodeDeleted(final String uuid) {

		final StorageSyncService service = runningService();
		if (service != null) {

			service.detach(uuid);
		}
	}

	/**
	 * Notifies a running sync service (if any) that the StorageConfiguration
	 * with the given UUID (or one of its entries) changed or was deleted.
	 */
	public static void handleConfigurationChanged(final String storageConfigurationUuid) {

		final StorageSyncService service = runningService();
		if (service != null) {

			service.configurationChanged(storageConfigurationUuid);
		}
	}

	/**
	 * @return the running service instance, looked up without ever
	 * re-initializing or starting the service layer (Services.getInstance()
	 * would re-initialize after shutdown, which under the embedded database
	 * driver calls System.exit), or null when the service layer is shut down,
	 * not initialized, or the service is not running
	 */
	private static StorageSyncService runningService() {

		final Services services = Services.peekInstance();
		if (services == null || !services.isInitialized()) {
			return null;
		}

		final StorageSyncService service = services.getServiceImplementation(StorageSyncService.class);

		return (service != null && service.isRunning()) ? service : null;
	}

	/**
	 * @return true if the service is running and the service layer is still
	 * initialized - background work (scans) must stop touching the graph once
	 * this returns false, to avoid re-initializing a shut-down service layer
	 */
	boolean isReady() {

		final Services services = Services.peekInstance();

		return running && services != null && services.isInitialized();
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
		final SyncDirection direction = SyncDirection.fromConfiguration(syncTarget.configuration());

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

			final ActiveSync sync = new ActiveSync(syncTarget, synchronizer, scanInterval, watchContents, deleteStale, direction);

			targets.put(uuid, sync);

			// watching and scanning are inbound-only features
			if (!direction.isInbound()) {

				logger.info("Outbound-only synchronization of {}, skipping scans", path);
				return;
			}

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

				startScan(sync);

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

					if (sync.direction.isInbound() && (sync.fullScanRequested || sync.shouldScan())) {

						sync.fullScanRequested = false;
						sync.lastScanned       = System.currentTimeMillis();

						startScan(sync);
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

			if (!dueEvents.isEmpty() && isReady()) {

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

			// propagate committed Structr-side changes to the external storages
			dispatchOutbound();
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

		// stop first so in-flight scans bail at their next isReady() check and
		// no new scans are started
		running = false;

		// wait (bounded) for in-flight scans to finish - a scan thread that
		// outlived shutdown would call StructrApp.getInstance(), re-initializing
		// the shut-down service layer (System.exit under the embedded driver)
		final long deadline = System.currentTimeMillis() + 10_000;

		for (final Thread scan : scanThreads) {
			scan.interrupt();
		}

		for (final Thread scan : scanThreads) {

			final long remaining = deadline - System.currentTimeMillis();
			if (remaining <= 0) {
				break;
			}

			try {
				scan.join(remaining);
			} catch (InterruptedException iex) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		synchronized (targets) {

			for (final ActiveSync sync : targets.values()) {
				closeQuietly(sync.synchronizer);
			}

			targets.clear();
		}
	}

	/**
	 * Starts a tracked scan thread for the given target, unless the service is
	 * already stopping. Tracked threads are interrupted and joined by
	 * stopService() so no scan touches the graph after shutdown.
	 */
	private void startScan(final ActiveSync sync) {

		if (!running) {
			return;
		}

		final Thread scan = new Thread(() -> {

			try {

				new ScanJob(this, sync).run();

			} finally {

				scanThreads.remove(Thread.currentThread());
			}

		}, "StorageSyncScan-" + sync.target.syncRootUuid());

		scanThreads.add(scan);

		// re-check after registering: if stopService ran in between, do not start
		if (!running) {

			scanThreads.remove(scan);
			return;
		}

		scan.start();
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

	/**
	 * Captures an outbound event in the per-transaction buffer, coalescing
	 * with an already-captured event of the same node so that intermediate
	 * states that never existed externally are not propagated.
	 */
	private static void capture(final String syncRootUuid, final VirtualChangeEvent event) {

		Map<String, PendingOutbound> captured = capturedEvents.get();
		if (captured == null) {

			captured = new LinkedHashMap<>();
			capturedEvents.set(captured);
		}

		final String key               = syncRootUuid + "#" + event.nodeUuid();
		final PendingOutbound existing = captured.get(key);

		if (existing == null) {

			captured.put(key, new PendingOutbound(syncRootUuid, event));
			return;
		}

		final VirtualChangeEvent first = existing.event();

		switch (first.type()) {

			case MOVED -> {

				switch (event.type()) {

					// keep the original location, take the latest one
					case MOVED   -> captured.put(key, new PendingOutbound(syncRootUuid, VirtualChangeEvent.moved(event.nodeUuid(), event.directory(), first.previousRelativePath(), event.relativePath())));

					// the entry effectively vanished from its original location
					case DELETED -> captured.put(key, new PendingOutbound(syncRootUuid, VirtualChangeEvent.deleted(event.nodeUuid(), event.directory(), first.previousRelativePath())));

					case CREATED -> captured.put(key, new PendingOutbound(syncRootUuid, event));
				}
			}

			case CREATED -> {

				switch (event.type()) {

					// created and moved within one transaction: only the final location exists
					case MOVED   -> captured.put(key, new PendingOutbound(syncRootUuid, VirtualChangeEvent.created(event.nodeUuid(), event.directory(), event.relativePath())));

					// created and deleted within one transaction: nothing ever existed externally
					case DELETED -> captured.remove(key);

					case CREATED -> captured.put(key, new PendingOutbound(syncRootUuid, event));
				}
			}

			case DELETED -> captured.put(key, new PendingOutbound(syncRootUuid, event));
		}
	}

	/**
	 * @return the parent portion of the given absolute virtual path
	 */
	private static String parentOf(final String absolutePath) {

		final int index = absolutePath.lastIndexOf('/');

		return index > 0 ? absolutePath.substring(0, index) : "";
	}

	/**
	 * @return the given absolute virtual path relative to the target's sync
	 * root, "" for the root itself, or null if the path is not below the root
	 */
	private static String relativize(final OutboundTarget target, final String absolutePath) {

		if (absolutePath == null) {
			return null;
		}

		if (absolutePath.equals(target.syncRootPath())) {
			return "";
		}

		final String prefix = target.syncRootPath() + "/";

		return absolutePath.startsWith(prefix) ? absolutePath.substring(prefix.length()) : null;
	}

	private void enqueueOutbound(final List<PendingOutbound> events) {

		synchronized (outboundQueue) {
			outboundQueue.addAll(events);
		}
	}

	/**
	 * Drains the outbound queue and notifies the synchronizers of committed
	 * virtual filesystem changes. Runs on the service thread, outside of any
	 * transaction.
	 */
	private void dispatchOutbound() {

		final List<PendingOutbound> due;

		synchronized (outboundQueue) {

			if (outboundQueue.isEmpty()) {
				return;
			}

			due = new LinkedList<>(outboundQueue);
			outboundQueue.clear();
		}

		for (final PendingOutbound pending : due) {

			final ActiveSync sync = getActiveSync(pending.syncRootUuid());

			if (sync == null || !sync.direction.isOutbound()) {

				// target was detached or reconfigured while the event was queued
				continue;
			}

			try {

				sync.synchronizer.onVirtualChange(pending.event());

			} catch (Throwable t) {
				logger.warn("Error while propagating virtual change {} to {}: {}", pending.event(), sync.target.syncRootPath(), t.getMessage());
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
		final SyncDirection direction;

		volatile boolean fullScanRequested = false;
		private long scanInterval          = 0L;
		long lastScanned                   = 0L;

		ActiveSync(final SyncTarget target, final StorageSynchronizer synchronizer, final Integer scanInterval, final boolean watchContents, final boolean deleteStale, final SyncDirection direction) {

			this.target        = target;
			this.synchronizer  = synchronizer;
			this.watchContents = watchContents;
			this.deleteStale   = deleteStale;
			this.direction     = direction;
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
	 * The outbound-enabled sync target governing a node: the sync root's
	 * UUID and its current (live) absolute virtual path.
	 */
	public record OutboundTarget(String syncRootUuid, String syncRootPath) {}

	/**
	 * A node's location before a parent change, stashed pre-set because the
	 * previous parent is not recoverable from the modification queue.
	 */
	private record PreMoveLocation(OutboundTarget target, String absolutePath) {}

	/**
	 * A captured, committed outbound event awaiting dispatch.
	 */
	private record PendingOutbound(String syncRootUuid, VirtualChangeEvent event) {}

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
