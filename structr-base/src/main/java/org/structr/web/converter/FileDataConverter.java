/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import net.sf.jmimemagic.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.KeyAndClass;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.CreationContainer;
import org.structr.core.traits.StructrTraits;
import org.structr.web.common.FileHelper;
import org.structr.web.common.FileHelper.Base64URIData;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.File;

import java.io.IOException;

//~--- classes ----------------------------------------------------------------

/**
 * Converts image data into an image node.
 *
 * If a {@link KeyAndClass} object is given, the image will be created with
 * the corresponding type and with setProperty to the given property key.
 *
 * If no {@link KeyAndClass} object is given, the image data will be set on
 * the image node itself.
 *
 *
 */
public class FileDataConverter extends PropertyConverter {

	private static final Logger logger = LoggerFactory.getLogger(FileDataConverter.class.getName());

	public FileDataConverter(final SecurityContext securityContext, final GraphObject entity) {
		super(securityContext, entity);
	}

	@Override
	public Object convert(final Object source) throws FrameworkException {

		if (source == null) {
			return false;
		}

		final File currentFile = getCurrentObject().as(File.class);

		if (source instanceof byte[]) {

			try {
				byte[] data      = (byte[]) source;
				MagicMatch match = Magic.getMagicMatch(data);
				String mimeType  = match.getMimeType();

				try {
					FileHelper.setFileData(currentFile, data, mimeType);

				} catch (IOException ioex) {

					logger.warn("Unable to store file", ioex);
				}

			} catch (MagicException | MagicParseException | MagicMatchNotFoundException mex) {

				logger.warn("Unable to parse file data", mex);
			}

		} else if (source instanceof String sourceString) {

			if (StringUtils.isNotBlank(sourceString)) {

				final Base64URIData uriData = new Base64URIData(sourceString);

				try {

					FileHelper.setFileData(currentFile, uriData.getBinaryData(), uriData.getContentType());

				} catch (IOException ioex) {

					logger.warn("Unable to store file", ioex);
				}
			}
		}

		return null;
	}

	@Override
	public Object revert(final Object source) {

		if (currentObject != null && currentObject.is(StructrTraits.FILE)) {

			final File currentFile = currentObject.as(File.class);
			return ImageHelper.getBase64String(currentFile);

		} else {

			return source;
		}
	}

	// ----- private methods -----
	private File getCurrentObject() {

		if (currentObject instanceof CreationContainer) {

			return ((CreationContainer)currentObject).getWrappedObject().as(File.class);
		}

		if (currentObject != null && currentObject.is(StructrTraits.FILE)) {
			return currentObject.as(File.class);
		}

		return null;
	}
}
