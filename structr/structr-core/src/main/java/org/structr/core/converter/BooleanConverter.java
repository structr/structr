
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.core.converter;


import org.structr.core.PropertyConverter;
import org.structr.core.Value;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.BooleanUtils;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class BooleanConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(BooleanConverter.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object convertForSetter(Object source, Value value) {

		if (source != null) {

			try {

				if (source instanceof Boolean) {

					return ((Boolean) source);

				} else if (source instanceof String) {

					return BooleanUtils.toBoolean((String) source, "true", "false");

				}

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Exception while parsing boolean", t);

				return null;
			}

		}

		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		return source == null ? false : source;
	}
}
