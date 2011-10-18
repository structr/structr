/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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



package org.structr.chart.renderer;

import java.awt.image.BufferedImage;

import java.io.OutputStream;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
class ChartRenderer {

	static final Logger logger = Logger.getLogger(ChartRenderer.class.getName());

	//~--- methods --------------------------------------------------------

	protected void writeImage(OutputStream out, BufferedImage image) {

		try {

			ImageIO.write(image,
				      "PNG",
				      out);

		} catch (Throwable t) {

			logger.log(Level.WARNING,
				   "Error writing image to stream: {0}",
				   t.getMessage());
		}
	}
}
