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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Direction of synchronization between the external storage and the Structr
 * virtual filesystem, configured per StorageConfiguration via the
 * "sync.direction" entry.
 *
 * IN   - external changes are mirrored into Structr (watching/scanning);
 *        this is the default and matches the behavior of configurations
 *        created before this setting existed.
 * OUT  - Structr-side structural changes (create/move/rename/delete) are
 *        propagated to the external storage; no watching or scanning.
 *        Note that inbound-only features like "sync.deleteStale" pruning
 *        never run for outbound-only configurations.
 * BOTH - full two-way synchronization.
 */
public enum SyncDirection {

	IN, OUT, BOTH;

	public static final String DIRECTION_KEY = "sync.direction";

	private static final Logger logger = LoggerFactory.getLogger(SyncDirection.class);

	/**
	 * Parses the "sync.direction" entry of the given configuration map.
	 * Absent or unrecognized values default to IN.
	 */
	public static SyncDirection fromConfiguration(final Map<String, String> configuration) {

		final String value = configuration != null ? configuration.get(DIRECTION_KEY) : null;

		if (value == null) {
			return IN;
		}

		switch (value.trim().toLowerCase()) {

			case "in":   { return IN; }
			case "out":  { return OUT; }
			case "both": { return BOTH; }

			default: {

				logger.warn("Unrecognized value '{}' for configuration entry {}, falling back to '{}'", value, DIRECTION_KEY, "in");
				return IN;
			}
		}
	}

	public boolean isInbound() {
		return this != OUT;
	}

	public boolean isOutbound() {
		return this != IN;
	}
}
