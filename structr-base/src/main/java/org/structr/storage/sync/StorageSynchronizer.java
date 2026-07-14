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
package org.structr.storage.sync;

import java.io.IOException;
import java.util.Iterator;

/**
 * Long-lived monitor for one {@link SyncTarget}, created by a storage
 * provider that implements {@link SynchronizableStorageProvider}. Combines
 * the push side (live change notification via {@link StorageSyncListener})
 * and the pull side (full or subtree enumeration used for initial sync,
 * periodic rescans and deletion reconciliation).
 *
 * Lifecycle is owned by the sync service: created on attach/startup, closed
 * on detach, configuration change, maintenance mode and shutdown.
 */
public interface StorageSynchronizer extends AutoCloseable {

	/**
	 * @return the target this synchronizer was created for
	 */
	SyncTarget getTarget();

	/**
	 * @return true if this synchronizer can push live events. Providers
	 * without a native notification mechanism return false and are driven
	 * purely by periodic {@link #enumerate} calls.
	 */
	boolean supportsWatching();

	/**
	 * Start pushing live change events to the given listener. Called at
	 * most once, and only when {@link #supportsWatching()} is true.
	 * Implementations spawn their own (daemon) threads or async callbacks.
	 */
	void startWatching(final StorageSyncListener listener) throws IOException;

	/**
	 * Pull side: lazily enumerate all entries below the given relative
	 * virtual path ("" or null enumerates the whole target), parents before
	 * children for path-addressed entries; uuid-only entries may appear in
	 * any order. Must not require a transaction. The iterator may be
	 * consumed slowly (batched transactions); implementations should stream
	 * rather than materialize.
	 */
	Iterator<ExternalEntry> enumerate(final String relativePath) throws IOException;

	/**
	 * Outbound side: called after a Structr-side structural change (create/
	 * move/rename/delete of a file or folder governed by this target) was
	 * committed, if the target's {@link SyncDirection} includes OUT. Called
	 * on the sync service thread, never within a transaction -
	 * implementations must not access graph nodes and should only use the
	 * event payload and their {@link SyncTarget} snapshot.
	 *
	 * The default implementation does nothing: providers whose physical
	 * keys do not depend on the virtual path (UUID-sharded local tree,
	 * object stores) may simply ignore structural changes.
	 */
	default void onVirtualChange(final VirtualChangeEvent event) {
	}

	/**
	 * Stop watching and release all resources. Idempotent.
	 */
	@Override
	void close();
}
