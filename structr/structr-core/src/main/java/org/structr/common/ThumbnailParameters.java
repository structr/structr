/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;

/**
 * Simple class to hold parameters for thumbnail generation,
 * see {@link ThumbnailConverter}
 *
 * @author Axel Morgner
 */
public class ThumbnailParameters {

	private int maxWidth;
	private int maxHeight;
	private boolean cropToFit;

	//~--- constructors ---------------------------------------------------

	public ThumbnailParameters(final int maxWidth, final int maxHeight, final boolean cropToFit) {

		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.cropToFit = cropToFit;
	}

	//~--- get methods ----------------------------------------------------

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
