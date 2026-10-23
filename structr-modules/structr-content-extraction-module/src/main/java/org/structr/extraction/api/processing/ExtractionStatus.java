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

import java.util.Locale;

/**
 * The extraction state of a file (File.contentExtractionStatus); null means never extracted.
 */
public enum ExtractionStatus {

	/** A job is extracting the file right now. */
	RUNNING,

	/** The file was extracted and its chunks are stored. */
	DONE,

	/** Extraction failed; File.contentExtractionError says why. */
	FAILED,

	/** The file was not extracted, e.g. because it is larger than the size limit. */
	SKIPPED;

	public String getValue() {

		return name().toLowerCase(Locale.ROOT);
	}
}
