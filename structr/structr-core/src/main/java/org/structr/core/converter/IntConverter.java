
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.core.converter;

import org.apache.commons.lang.time.DateUtils;

import org.structr.core.PropertyConverter;
import org.structr.core.Value;

//~--- JDK imports ------------------------------------------------------------

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class IntConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(IntConverter.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object convertForSetter(Object source, Value value) {

		if (source != null) {

                    if (source instanceof Integer) {
                        return source;
                    } else if (source instanceof Double) {
                        return Math.round((Double) source);
                    } else if (source instanceof Float) {
                        return Math.round((Float) source);
                    } else {
                        return Math.round(Float.parseFloat((String) source));
                    }
		}

		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		if (source != null) {

			return source;
		}

		return source;
	}
}
