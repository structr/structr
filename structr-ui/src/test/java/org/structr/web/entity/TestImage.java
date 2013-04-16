/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity;

import org.structr.web.entity.Image;
import org.structr.common.ThumbnailParameters;
import org.structr.web.property.ThumbnailProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Image with a 200x100 thumbnail
 * 
 * @author Axel Morgner
 */
public class TestImage extends Image {

	public static final ThumbnailProperty thumbnail = new ThumbnailProperty("thumbnail", new ThumbnailParameters(200, 100, false));
	
}
