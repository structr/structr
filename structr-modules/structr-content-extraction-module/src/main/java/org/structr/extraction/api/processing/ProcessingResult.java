/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.extraction.api.processing;

import org.structr.extraction.api.options.EffectiveContentOptions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of processing one file.
 *
 * @param fileUuid  the file
 * @param status    DONE, SKIPPED or FAILED; null if content and options were unchanged and the file was left alone
 * @param nodeUuids the ids of the new chunks, in reading order, if DONE
 * @param options   the effective options, if DONE
 * @param message   why the file was skipped or failed
 */
public record ProcessingResult(String fileUuid, ExtractionStatus status, List<String> nodeUuids, EffectiveContentOptions options, String message) {

	public ProcessingResult {

		nodeUuids = List.copyOf(nodeUuids);
	}

	public static ProcessingResult failed(final String fileUuid, final String message) {

		return new ProcessingResult(fileUuid, ExtractionStatus.FAILED, List.of(), null, message);
	}

	public static ProcessingResult skipped(final String fileUuid, final String message) {

		return new ProcessingResult(fileUuid, ExtractionStatus.SKIPPED, List.of(), null, message);
	}

	public static ProcessingResult unchanged(final String fileUuid) {

		return new ProcessingResult(fileUuid, null, List.of(), null, "Content and options are unchanged.");
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("file",       fileUuid);
		map.put("status",     status != null ? status.getValue() : "unchanged");
		map.put("chunkCount", nodeUuids.size());
		map.put("message",    message);

		return map;
	}
}
