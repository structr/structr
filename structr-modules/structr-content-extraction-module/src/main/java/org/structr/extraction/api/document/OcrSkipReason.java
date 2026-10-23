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
package org.structr.extraction.api.document;

/**
 * Why OCR did not run during an extraction.
 */
public enum OcrSkipReason {

	/** The OCR mode of the call, folder or setting is "off". */
	MODE_OFF("mode-off"),

	/** No working tesseract binary was found. */
	TESSERACT_NOT_INSTALLED("tesseract-not-installed"),

	/** None of the requested OCR languages has an installed language pack. */
	LANGUAGES_NOT_INSTALLED("ocr-languages-not-installed"),

	/** The PDF has more pages than the OCR page limit. */
	MAX_PAGES_EXCEEDED("maxpages-exceeded");

	private final String value;

	OcrSkipReason(final String value) {

		this.value = value;
	}

	public String getValue() {

		return value;
	}
}
