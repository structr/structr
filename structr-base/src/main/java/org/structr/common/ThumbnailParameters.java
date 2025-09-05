/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.common;

/**
 * Simple class to hold parameters for thumbnail generation.
 */
public class ThumbnailParameters {

	private final int maxWidth;
	private final int maxHeight;
	private final boolean cropToFit;

	public ThumbnailParameters(final int maxWidth, final int maxHeight, final boolean cropToFit) {

		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.cropToFit = cropToFit;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public boolean getCropToFit() {
		return cropToFit;
	}
}
