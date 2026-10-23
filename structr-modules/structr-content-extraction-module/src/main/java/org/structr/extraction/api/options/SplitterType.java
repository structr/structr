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
 * How content is divided into chunks.
 */
public enum SplitterType {

	/** LangChain's RecursiveCharacterTextSplitter: paragraphs, then lines, sentences, words, characters. */
	RECURSIVE,

	/** Follows the document structure: blocks stay whole where possible, sections are not merged. */
	STRUCTURE,

	/** Packs whole sentences up to the chunk size. */
	SENTENCE,

	/** Fixed windows of tokens, regardless of text structure. */
	TOKEN,

	/** One chunk per page; pages larger than the chunk size are split recursively. */
	PAGE;

	public String getValue() {

		return name().toLowerCase(Locale.ROOT);
	}

	public static SplitterType fromValue(final String value) {

		if (value != null) {

			for (final SplitterType type : values()) {

				if (type.name().equalsIgnoreCase(value.trim())) {

					return type;
				}
			}
		}

		throw new IllegalArgumentException("Unknown splitter '" + value + "', expected one of recursive, structure, sentence, token, page.");
	}
}
