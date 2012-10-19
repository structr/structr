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

import org.structr.common.ThumbnailParameters;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.Image;
import org.structr.core.node.IndexNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Converts an image into a thumbnail
 *
 * @author Axel Morgner
 */
public class ThumbnailConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(ThumbnailConverter.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object convertForSetter(Object source, Value value) {

		return source;

	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		Object val = value.get(securityContext);

		if (val instanceof ThumbnailParameters) {

			ThumbnailParameters params = (ThumbnailParameters) val;
			Image thumbnail            = ((Image) currentObject).getScaledImage(params.getMaxWidth(), params.getMaxHeight(), params.getCropToFit());
			
			if (thumbnail == null) {
				logger.log(Level.WARNING, "Could not create thumbnail for {0}", source);
				return null;
			}
			
			return thumbnail;//.getUuid();

		}

		return source;

	}

}
