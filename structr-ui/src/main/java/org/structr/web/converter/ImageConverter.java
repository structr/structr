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


package org.structr.web.converter;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

import org.apache.commons.lang.StringUtils;

import org.structr.web.common.ImageHelper;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.KeyAndClass;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NewIndexNodeCommand;
import org.structr.web.entity.Image;

//~--- classes ----------------------------------------------------------------

/**
 * Converts image data into an image node sets its id as the property
 * with the key from the given value
 *
 * @author Axel Morgner
 */
public class ImageConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(ImageConverter.class.getName());

	private KeyAndClass<Image> keyAndClass = null;
	
	public ImageConverter(SecurityContext securityContext, GraphObject entity, KeyAndClass<Image> kc) {
		
		super(securityContext, entity);
		
		this.keyAndClass = kc;
	}
	
	//~--- methods --------------------------------------------------------

	@Override
	public Object convert(Object source) {

		if (source == null) {

			return false;
		}

		try {

			Image img = null;
			
			if (keyAndClass == null) {
				return false;
			}
			
			if (source instanceof byte[]) {

				byte[] data      = (byte[]) source;
				MagicMatch match = Magic.getMagicMatch(data);
				String mimeType  = match.getMimeType();

				img = ImageHelper.createImage(securityContext, data, mimeType, keyAndClass.getCls());

			} else if (source instanceof String) {

				if (StringUtils.isNotBlank((String) source)) {

					img = ImageHelper.createImageBase64(securityContext, (String) source, keyAndClass.getCls());
				}

			}

			
			if (img != null) {
				
				// manual indexing of UUID needed here to avoid a 404 in the following setProperty call
				Services.command(securityContext, NewIndexNodeCommand.class).updateNode(img);
				
				currentObject.setProperty(keyAndClass.getPropertyKey(), img);
			}
			
			return null;

		} catch (Throwable t) {

			logger.log(Level.WARNING, "Cannot create image node from given data", t);

			return null;

		}

	}

	@Override
	public Object revert(Object source) {

		return source;
	}

}
