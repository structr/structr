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

import org.structr.extraction.api.ContentExtraction;

/**
 * Which cleanup steps {@link ContentExtraction#normalize(String, NormalizeOptions)} applies. All steps
 * are on by default.
 */
public final class NormalizeOptions {

	private boolean unicode           = true;
	private boolean controlCharacters = true;
	private boolean hyphenation       = true;
	private boolean whitespace        = true;

	public boolean isUnicode() {

		return unicode;
	}

	/**
	 * Unicode NFKC normalization: composes characters, and replaces compatibility forms such as the
	 * "ﬁ" ligature or full-width letters, which PDFs often contain, with their plain equivalents.
	 */
	public NormalizeOptions setUnicode(final boolean unicode) {

		this.unicode = unicode;

		return this;
	}

	public boolean isControlCharacters() {

		return controlCharacters;
	}

	/**
	 * Removes control characters except line breaks and tabs, and soft hyphens.
	 */
	public NormalizeOptions setControlCharacters(final boolean controlCharacters) {

		this.controlCharacters = controlCharacters;

		return this;
	}

	public boolean isHyphenation() {

		return hyphenation;
	}

	/**
	 * Joins words hyphenated at a line break ("extrac-\ntion" becomes "extraction"), when the line
	 * continues in lower case; "Nord-\nSüd" keeps its hyphen.
	 */
	public NormalizeOptions setHyphenation(final boolean hyphenation) {

		this.hyphenation = hyphenation;

		return this;
	}

	public boolean isWhitespace() {

		return whitespace;
	}

	/**
	 * Collapses runs of spaces and tabs, trims lines, and reduces more than one blank line to one.
	 * Paragraph breaks are kept.
	 */
	public NormalizeOptions setWhitespace(final boolean whitespace) {

		this.whitespace = whitespace;

		return this;
	}
}
