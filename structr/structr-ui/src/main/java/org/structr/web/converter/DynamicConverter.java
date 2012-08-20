/*
 *  Copyright (C) 2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.converter;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.converter.PropertyConverter;
import org.structr.web.entity.Content;
import org.structr.web.entity.TypeDefinition;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

//~--- classes ----------------------------------------------------------------

/**
 * Wrapper for an arbitrary converter.
 *
 * Instantiates an existing converter by reflection, based on the 'converter'
 * property. Converts the result to string (has to, kind of).
 *
 * @author Axel Morgner
 */
public class DynamicConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(DynamicConverter.class.getName());

	//~--- fields ---------------------------------------------------------

	private PropertyConverter converter = null;

	//~--- methods --------------------------------------------------------

	@Override
	public Object convertForSetter(Object source, Value value) {

		instantiateConverter(currentObject);

		try {

			return converter != null
			       ? converter.convertForSetter(source, value)
			       : source;

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, null, ex);

		}

		return null;

	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		instantiateConverter(currentObject);

		Object result = (converter != null
		       ? converter.convertForGetter(source, value)
		       : source);
		
		return result != null ? result.toString() : null;

	}

	private void instantiateConverter(final GraphObject currentObject) {

		if (currentObject != null && currentObject instanceof Content) {

			Content content               = (Content) currentObject;
			TypeDefinition typeDefinition = content.getTypeDefinition();

			if (typeDefinition != null) {

				String converterProp = typeDefinition.getStringProperty(TypeDefinition.Key.converter);

				if (StringUtils.isNotBlank(converterProp)) {

					Class converterClass = null;

					try {

						converterClass = Class.forName(converterProp);

					} catch (ClassNotFoundException ex) {

						Logger.getLogger(DynamicConverter.class.getName()).log(Level.SEVERE, null, ex);

					}

					if (converterClass != null) {

						try {

							converter = (PropertyConverter) converterClass.newInstance();

						} catch (InstantiationException ex) {

							logger.log(Level.SEVERE, null, ex);

						} catch (IllegalAccessException ex) {

							logger.log(Level.SEVERE, null, ex);

						}

					}

				}

			}

		}

	}

}
