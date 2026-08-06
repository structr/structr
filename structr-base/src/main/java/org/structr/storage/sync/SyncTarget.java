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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable snapshot describing one synchronization target: the sync root
 * (the AbstractFile - usually a Folder, possibly a single File - that
 * directly carries the StorageConfiguration) whose subtree mirrors the
 * external storage, and the effective configuration entries at the time
 * the synchronizer was created.
 *
 * Synchronizers run on threads without a transaction context and therefore
 * MUST NOT access graph nodes; everything they need is snapshotted here.
 * When the underlying StorageConfiguration changes or the sync root moves,
 * the sync service closes the synchronizer and creates a new one with a
 * fresh snapshot.
 */
public record SyncTarget(String syncRootUuid, String syncRootPath, boolean syncRootIsFolder, String storageConfigurationUuid, Map<String, String> configuration) {

	public SyncTarget {

		if (syncRootUuid == null || syncRootPath == null) {

			throw new IllegalArgumentException("SyncTarget needs a sync root UUID and path");
		}

		// immutable, order-preserving snapshot; a configuration entry may have
		// a null value (or name) when it has been created but not yet set, so
		// skip those instead of failing like Map.copyOf would - consumers treat
		// an absent key exactly like an unset one
		if (configuration == null) {

			configuration = Map.of();

		} else {

			final Map<String, String> snapshot = new LinkedHashMap<>();

			for (final Map.Entry<String, String> entry : configuration.entrySet()) {

				if (entry.getKey() != null && entry.getValue() != null) {

					snapshot.put(entry.getKey(), entry.getValue());
				}
			}

			configuration = Collections.unmodifiableMap(snapshot);
		}
	}
}
