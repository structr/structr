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
import org.structr.core.entity.Image;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

//~--- classes ----------------------------------------------------------------

/**
 * Converts an image into a thumbnail
 *
 * @author Axel Morgner
 */
public class ThumbnailConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(ThumbnailConverter.class.getName());

	private ThumbnailParameters parameters = null;
	
	public ThumbnailConverter(SecurityContext securityContext, GraphObject entity, ThumbnailParameters parameters) {
		super(securityContext, entity);
		
		this.parameters = parameters;
	}
	
	//~--- methods --------------------------------------------------------

	@Override
	public Object convert(Object source) {

		return source;

	}

	@Override
	public Object revert(Object source) {

		Image thumbnail = ((Image) currentObject).getScaledImage(parameters.getMaxWidth(), parameters.getMaxHeight(), parameters.getCropToFit());

		if (thumbnail == null) {
			logger.log(Level.WARNING, "Could not create thumbnail for {0}", source);
			return null;
		}

		return thumbnail;//.getUuid();
	}
}
