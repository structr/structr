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

import org.structr.extraction.ContentExtractionSettings;
import java.util.List;

/**
 * Options for splitting content into chunks. Start from {@link #fromSettings()} and override what the
 * call needs; {@link #validate()} checks the combination.
 */
public final class SplitOptions {

	private SplitterType splitter       = SplitterType.STRUCTURE;
	private int chunkSize               = 512;
	private int chunkOverlap            = 64;
	private SizeUnit sizeUnit           = SizeUnit.TOKENS;
	private List<String> separators     = null;
	private boolean includeHeadingPath  = false;
	private String tokenEncoding        = "cl100k_base";

	/**
	 * Options initialized from the module settings.
	 */
	public static SplitOptions fromSettings() {

		final SplitOptions options = new SplitOptions();

		options.splitter      = SplitterType.fromValue(ContentExtractionSettings.Splitter.getValue());
		options.chunkSize     = ContentExtractionSettings.ChunkSize.getValue();
		options.chunkOverlap  = ContentExtractionSettings.ChunkOverlap.getValue();
		options.sizeUnit      = SizeUnit.fromValue(ContentExtractionSettings.SizeUnit.getValue());
		options.tokenEncoding = ContentExtractionSettings.TokenEncoding.getValue();

		return options;
	}

	/**
	 * Checks that size and overlap fit together.
	 *
	 * @throws IllegalArgumentException with a message that can be shown to the user
	 */
	public SplitOptions validate() {

		if (chunkSize < 1) {

			throw new IllegalArgumentException("chunkSize must be at least 1, got " + chunkSize + ".");
		}

		if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {

			throw new IllegalArgumentException("chunkOverlap must be at least 0 and smaller than chunkSize (" + chunkSize + "), got " + chunkOverlap + ".");
		}

		if (separators != null && separators.isEmpty()) {

			throw new IllegalArgumentException("separators must not be empty.");
		}

		return this;
	}

	public SplitterType getSplitter() {

		return splitter;
	}

	public SplitOptions setSplitter(final SplitterType splitter) {

		this.splitter = splitter;

		return this;
	}

	public int getChunkSize() {

		return chunkSize;
	}

	public SplitOptions setChunkSize(final int chunkSize) {

		this.chunkSize = chunkSize;

		return this;
	}

	/**
	 * How much consecutive chunks overlap, in the size unit. Except for the token splitter, the overlap is
	 * made of whole pieces (sentences, words) as in LangChain: a piece larger than the overlap is not
	 * repeated, so chunks made of long sentences may not overlap at all.
	 */
	public int getChunkOverlap() {

		return chunkOverlap;
	}

	public SplitOptions setChunkOverlap(final int chunkOverlap) {

		this.chunkOverlap = chunkOverlap;

		return this;
	}

	public SizeUnit getSizeUnit() {

		return sizeUnit;
	}

	public SplitOptions setSizeUnit(final SizeUnit sizeUnit) {

		this.sizeUnit = sizeUnit;

		return this;
	}

	/**
	 * Separators of the recursive splitter, tried in order; null means the default (paragraphs, lines,
	 * sentences, words). An empty string splits anywhere. Text that none of them can divide is cut hard.
	 */
	public List<String> getSeparators() {

		return separators;
	}

	public SplitOptions setSeparators(final List<String> separators) {

		this.separators = separators != null ? List.copyOf(separators) : null;

		return this;
	}

	public boolean isIncludeHeadingPath() {

		return includeHeadingPath;
	}

	/**
	 * Whether the text of each chunk starts with the path of the section it belongs to, e.g.
	 * "Installation > Requirements". This helps retrieval, because a chunk deep in a section rarely
	 * repeats what the section is about.
	 */
	public SplitOptions setIncludeHeadingPath(final boolean includeHeadingPath) {

		this.includeHeadingPath = includeHeadingPath;

		return this;
	}

	public String getTokenEncoding() {

		return tokenEncoding;
	}

	public SplitOptions setTokenEncoding(final String tokenEncoding) {

		this.tokenEncoding = tokenEncoding;

		return this;
	}
}
