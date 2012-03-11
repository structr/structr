/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.converter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.math.NumberUtils;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;

/**
 *
 * @author Axel Morgner
 */
public class DoubleConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(DoubleConverter.class.getName());

	@Override
	public Object convertForSetter(Object source, Value value) {

		if(source != null) {
			
			try {

				if(source instanceof Double) {
					return ((Double)source);
				} else if(source instanceof String) {
					return NumberUtils.createDouble((String) source);
				}

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Exception while parsing double", t);
				return null;
			}
		}

		return source;

	}

	@Override
	public Object convertForGetter(Object source, Value value) {

//		if(source != null) {
//			return source.toString();
//		}

		return source;
	}
}
