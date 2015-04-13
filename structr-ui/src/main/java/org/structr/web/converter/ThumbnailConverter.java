/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.converter;

import org.structr.common.ThumbnailParameters;
import org.structr.web.entity.Image;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

//~--- classes ----------------------------------------------------------------

/**
 * Creates a thumbnail for an {@link Image}.
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

		if (((Image) this.currentObject).getProperty(Image.isThumbnail)) {
			return null;
		}
		
		return ((Image) currentObject).getScaledImage(parameters.getMaxWidth(), parameters.getMaxHeight(), parameters.getCropToFit());
	}
}
