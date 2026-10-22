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

import java.util.Locale;
import java.util.Map;

/**
 * What happens to a File or Folder node when its counterpart disappears from
 * the external storage, configured per StorageConfiguration via the
 * "sync.onVanished" entry.
 *
 * IGNORE   - the node is left untouched. This is the default, so an unconfigured
 *            mount keeps everything and a backend that enumerates unreliably
 *            cannot destroy data on its own.
 * DELETE   - the node is deleted. Only the node: deletion of external nodes
 *            never reaches the physical side.
 * READONLY - the node is kept and marked isExternallyDeleted, which makes it
 *            reject modification and refuse to hand out binary content. The
 *            mark is cleared again if the entry reappears.
 *
 * The policy governs both the live watch path and rescan reconciliation, which
 * is why it is read once per sync target rather than at each use.
 */
public enum VanishPolicy {

	DELETE("delete"), IGNORE("ignore"), READONLY("readonly");

	public static final String POLICY_KEY = "sync.onVanished";

	private static final Logger logger = LoggerFactory.getLogger(VanishPolicy.class);

	private final String value;

	VanishPolicy(final String value) {

		this.value = value;
	}

	/**
	 * @return the string this policy is stored as. Spelled out rather than derived from the
	 * constant name, which would be locale-dependent: "IGNORE" lowercases to "ignore" in most
	 * locales and to "ıgnore" in a Turkish one.
	 */
	public String value() {

		return value;
	}

	/**
	 * Parses the "sync.onVanished" entry of the given configuration map.
	 * Absent or unrecognized values default to IGNORE.
	 */
	public static VanishPolicy fromConfiguration(final Map<String, String> configuration) {

		final String value = configuration != null ? configuration.get(POLICY_KEY) : null;
		if (value == null) {

			return IGNORE;
		}

		switch (value.trim().toLowerCase(Locale.ROOT)) {

			case "delete":   { return DELETE; }
			case "ignore":   { return IGNORE; }
			case "readonly": { return READONLY; }

			default: {

				logger.warn("Unrecognized value '{}' for configuration entry {}, falling back to '{}'", value, POLICY_KEY, IGNORE.value());

				return IGNORE;
			}
		}
	}

	/**
	 * @return true if a vanished entry requires any action at all
	 */
	public boolean reconciles() {

		return this != IGNORE;
	}
}
