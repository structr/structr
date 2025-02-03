/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.converter;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.KeyAndClass;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.Image;

/**
 * Converts image data into an image node.
 *
 * If a {@link KeyAndClass} object is given, the image will be created with
 * the corresponding type and with setProperty to the given property key.
 *
 * If no {@link KeyAndClass} object is given, the image data will be set on
 * the image node itself.
 *
 */
public class ImageConverter extends PropertyConverter {

	private static final Logger logger = LoggerFactory.getLogger(ImageConverter.class.getName());

	private KeyAndClass<Image> keyAndClass = null;

	public ImageConverter(SecurityContext securityContext, GraphObject entity, KeyAndClass<Image> kc) {

		super(securityContext, entity);

		this.keyAndClass = kc;
	}

	@Override
	public Object convert(final Object source) {

		if (source == null) {

			return false;
		}

		try {

			Image img = null;

			try {
				if (source instanceof byte[]) {

					byte[] data      = (byte[]) source;
					MagicMatch match = Magic.getMagicMatch(data);
					String mimeType  = match.getMimeType();

					if (keyAndClass != null) {

						img = ImageHelper.createFile(securityContext, data, mimeType, keyAndClass.getType()).as(Image.class);

					} else {

						ImageHelper.setImageData((Image) currentObject, data, mimeType);

					}

				} else if (source instanceof String) {

					String sourceString = (String) source;

					if (StringUtils.isNotBlank(sourceString)) {

						if (keyAndClass != null) {

							// UUID?
							if (sourceString.length() == 32) {

								img = (Image) ImageHelper.transformFile(securityContext, sourceString, keyAndClass != null ? keyAndClass.getType() : null);
							}

							if (img == null) {

								img = (Image) ImageHelper.createFileBase64(securityContext, sourceString, keyAndClass != null ? keyAndClass.getType() : null);

							}

						} else {

							ImageHelper.decodeAndSetFileData((Image) currentObject, sourceString);
							ImageHelper.updateMetadata((Image)currentObject);

						}
					}

				}

			} catch (Throwable t) {
				logger.warn("Cannot create image node from given data", t);
			}

			if (img != null) {

				// manual indexing of UUID needed here to avoid a 404 in the following setProperty call
				img.getWrappedNode().addToIndex();
				
				currentObject.setProperties(securityContext, new PropertyMap(keyAndClass.getPropertyKey(), img));
			}


		} catch (Throwable t) {

			logger.warn("Cannot create image node from given data", t);
		}

		return null;
	}

	@Override
	public Object revert(Object source) {

		if (currentObject.is("Image")) {
			return ImageHelper.getBase64String((Image) currentObject);
		} else {
			return source;
		}
	}

}
