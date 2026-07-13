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

/**
 * Optional capability interface for StorageProvider implementations whose
 * backend can be synchronized INTO Structr, i.e. external changes are
 * reflected as creation/update/move/deletion of the corresponding File and
 * Folder nodes.
 *
 * Discovery: the sync service instantiates the provider for a candidate
 * sync root via StorageProviderFactory and checks instanceof - matching the
 * existing per-call provider instantiation pattern.
 *
 * The returned synchronizer is long-lived and independent of the (per-call,
 * per-file) provider instance that created it: it must only use the
 * {@link SyncTarget} snapshot, never the provider's AbstractFile.
 */
public interface SynchronizableStorageProvider {

	/**
	 * Create a synchronizer for the given target, or return null if this
	 * configuration has no external side to synchronize (e.g. a local
	 * filesystem configuration without a "mountTarget" entry - the
	 * UUID-sharded default tree is Structr-owned).
	 */
	StorageSynchronizer createSynchronizer(final SyncTarget target) throws IOException;
}
