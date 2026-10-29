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

/**
 * Options for finding the figures of a document. Start from {@link #fromSettings()} and override what
 * the call needs.
 */
public final class FigureOptions {

	private int dpi               = 150;
	private int maxPages          = 300;
	private int maxFigures        = 100;
	private int minFigureSize     = 72;
	private boolean includeTables = true;

	/**
	 * Options initialized from the module settings.
	 */
	public static FigureOptions fromSettings() {

		final FigureOptions options = new FigureOptions();

		options.dpi           = ContentExtractionSettings.FigureDpi.getValue();
		options.maxPages      = ContentExtractionSettings.FigureMaxPages.getValue();
		options.maxFigures    = ContentExtractionSettings.FigureMaxFigures.getValue();
		options.minFigureSize = ContentExtractionSettings.FigureMinSize.getValue();

		return options;
	}

	/**
	 * Resolution figures of PDF pages are rendered at.
	 */
	public int getDpi() {

		return dpi;
	}

	public FigureOptions setDpi(final int dpi) {

		this.dpi = dpi;

		return this;
	}

	/**
	 * Number of PDF pages searched for figures; further pages are left out.
	 */
	public int getMaxPages() {

		return maxPages;
	}

	public FigureOptions setMaxPages(final int maxPages) {

		this.maxPages = maxPages;

		return this;
	}

	public int getMaxFigures() {

		return maxFigures;
	}

	public FigureOptions setMaxFigures(final int maxFigures) {

		this.maxFigures = maxFigures;

		return this;
	}

	/**
	 * Minimum width and height of a figure without a caption: points on a PDF page, pixels in an image.
	 * Figures with a caption are kept at any size.
	 */
	public int getMinFigureSize() {

		return minFigureSize;
	}

	public FigureOptions setMinFigureSize(final int minFigureSize) {

		this.minFigureSize = minFigureSize;

		return this;
	}

	/**
	 * Whether tables with a caption ("Table 2: ...") count as figures.
	 */
	public boolean isIncludeTables() {

		return includeTables;
	}

	public FigureOptions setIncludeTables(final boolean includeTables) {

		this.includeTables = includeTables;

		return this;
	}
}
