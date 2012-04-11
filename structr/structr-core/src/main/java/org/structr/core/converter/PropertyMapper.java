/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.converter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */
public class PropertyMapper extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(PropertyMapper.class.getName());

	@Override
	public Object convertForSetter(Object source, Value value) {
		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		if(value != null) {

			Object valueObject = value.get();
			if(valueObject instanceof String) {

				String mappedKey = (String)valueObject;
				return currentObject.getProperty(mappedKey);

			} else {

				logger.log(Level.WARNING, "Value parameter is not a String!");
			}

		} else {

			logger.log(Level.WARNING, "Required value parameter is missing!");
		}

		return source;
	}
}
