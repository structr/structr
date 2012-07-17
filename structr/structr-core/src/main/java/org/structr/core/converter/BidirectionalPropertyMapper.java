/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.converter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.Value;
import org.structr.core.converter.PropertyMapper;

/**
 *
 * @author Christian Morgner
 */
public class BidirectionalPropertyMapper extends PropertyMapper {

	private static final Logger logger = Logger.getLogger(BidirectionalPropertyMapper.class.getName());
	
	@Override
	public Object convertForSetter(Object source, Value value) {

		if(value != null) {

			Object valueObject = value.get(securityContext);
			if(valueObject instanceof String) {

				String mappedKey = (String)valueObject;
				
				try {
					currentObject.setProperty(mappedKey, source);
					
				} catch(FrameworkException fex) {
					
					logger.log(Level.WARNING, "Unable to set mapped property {0} on {1}: {2}", new Object[] { mappedKey, currentObject, fex.getMessage() } );
				}

			} else {

				logger.log(Level.WARNING, "Value parameter is not a String!");
			}

		} else {

			logger.log(Level.WARNING, "Required value parameter is missing!");
		}

		return null;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		if(value != null) {

			Object valueObject = value.get(securityContext);
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
