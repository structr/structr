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
package org.structr.extraction.api.search;

import java.util.Locale;

/**
 * How a search query is read.
 */
public enum ContentSearchMode {

	/** Lucene query syntax, passed to the fulltext index as it is. */
	LUCENE,

	/** What a user types into a search box: every word must occur, as a prefix or with typos, ignoring case. */
	SIMPLE;

	public String value() {

		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * The mode with the given name, ignoring case.
	 *
	 * @throws IllegalArgumentException for an unknown name
	 */
	public static ContentSearchMode of(final String name) {

		for (final ContentSearchMode mode : values()) {

			if (mode.value().equalsIgnoreCase(name)) {

				return mode;
			}
		}

		throw new IllegalArgumentException("unknown search mode '" + name + "', expected lucene or simple");
	}
}
