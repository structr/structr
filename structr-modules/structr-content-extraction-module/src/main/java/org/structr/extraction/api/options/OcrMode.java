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
package org.structr.extraction.api.options;

import java.util.Locale;

/**
 * When Tesseract OCR is used during extraction. OCR only ever runs if a tesseract binary is installed.
 */
public enum OcrMode {

	/** Images are OCR'd, PDF pages only when they contain (almost) no extractable text, i.e. scans. */
	AUTO,

	/** Every image and every PDF page is OCR'd, in addition to the extractable text. */
	ALWAYS,

	/** No OCR. */
	OFF;

	public String getValue() {

		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Parses the setting and option value ("auto", "always", "off"), case-insensitively.
	 */
	public static OcrMode fromValue(final String value) {

		if (value != null) {

			for (final OcrMode mode : values()) {

				if (mode.name().equalsIgnoreCase(value.trim())) {

					return mode;
				}
			}
		}

		throw new IllegalArgumentException("Unknown OCR mode '" + value + "', expected one of auto, always, off.");
	}
}
