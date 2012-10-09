/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.converter;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

import org.apache.commons.lang.StringUtils;

import org.structr.common.ImageHelper;
import org.structr.core.Value;
import org.structr.core.entity.Image;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyKey;
import org.structr.core.Services;
import org.structr.core.node.IndexNodeCommand;

//~--- classes ----------------------------------------------------------------

/**
 * Converts image data into an image node sets its id as the property
 * with the key from the given value
 *
 * @author Axel Morgner
 */
public class ImageConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(ImageConverter.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object convertForSetter(Object source, Value value) {

		if (source == null) {

			return false;
		}

		try {

			Image img = null;
			
			if (source instanceof byte[]) {

				byte[] data      = (byte[]) source;
				MagicMatch match = Magic.getMagicMatch(data);
				String mimeType  = match.getMimeType();

				img = ImageHelper.createImage(securityContext, data, mimeType, Image.class);

			} else if (source instanceof String) {

				if (StringUtils.isNotBlank((String) source)) {

					img = ImageHelper.createImageBase64(securityContext, (String) source, Image.class);
				}

			}

			// manual indexing needed here
			Services.command(securityContext, IndexNodeCommand.class).execute(img);
			
			currentObject.setProperty((PropertyKey) value.get(securityContext), img.getUuid());
			
			return null;

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Cannot create image node from given data", t);

			return null;

		}

	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		return source;

//              
//              if (source == null) {
//
//                      return null;
//              }
//
//              try {
//
//                      return (AbstractNode) Services.command(securityContext, FindNodeCommand.class).execute(source);
//
//              } catch (Throwable t) {
//
//                      logger.log(Level.WARNING, "Cannot retrieve image node from source data", t);
//
//                      return null;
//
//              }
//

	}

}
