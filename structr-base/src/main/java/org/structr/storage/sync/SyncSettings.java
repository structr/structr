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
 * The per-mount sync settings, read from the entries of a mount's storage configuration. An absent
 * entry means the default, and a value that cannot be read falls back to it with a warning.
 *
 * @param watchContents whether the storage is watched for changes (sync.watchContents, "true" to enable)
 * @param scanInterval  the seconds between full scans, or null for no rescans (sync.scanInterval)
 * @param fileType      the node type created for discovered files, or null for the default (sync.fileType)
 * @param folderType    the node type created for discovered folders, or null for the default (sync.folderType)
 */
public record SyncSettings(boolean watchContents, Integer scanInterval, String fileType, String folderType) {

	public static final String WATCH_CONTENTS_KEY = "sync.watchContents";
	public static final String SCAN_INTERVAL_KEY  = "sync.scanInterval";
	public static final String FILE_TYPE_KEY      = "sync.fileType";
	public static final String FOLDER_TYPE_KEY    = "sync.folderType";

	private static final Logger logger = LoggerFactory.getLogger(SyncSettings.class);

	public static SyncSettings fromConfiguration(final Map<String, String> configuration) {

		final Map<String, String> entries = configuration != null ? configuration : Map.of();

		return new SyncSettings(
			parseWatchContents(entries.get(WATCH_CONTENTS_KEY)),
			parseScanInterval(entries.get(SCAN_INTERVAL_KEY)),
			blankToNull(entries.get(FILE_TYPE_KEY)),
			blankToNull(entries.get(FOLDER_TYPE_KEY))
		);
	}

	private static boolean parseWatchContents(final String value) {

		if (value == null || value.isBlank()) {

			return false;
		}

		switch (value.trim().toLowerCase(Locale.ROOT)) {

			case "true":  { return true; }
			case "false": { return false; }

			default: {

				logger.warn("Unrecognized value '{}' for configuration entry {}, falling back to '{}'", value, WATCH_CONTENTS_KEY, "false");

				return false;
			}
		}
	}

	private static Integer parseScanInterval(final String value) {

		if (value == null || value.isBlank()) {

			return null;
		}

		try {

			final int seconds = Integer.parseInt(value.trim());
			if (seconds > 0) {

				return seconds;
			}

			if (seconds == 0) {

				return null;
			}

		} catch (NumberFormatException ignore) {}

		logger.warn("Unrecognized value '{}' for configuration entry {}, falling back to no rescans", value, SCAN_INTERVAL_KEY);

		return null;
	}

	private static String blankToNull(final String value) {

		return value == null || value.isBlank() ? null : value.trim();
	}
}
