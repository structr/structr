/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.validator;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 *
 * @author Christian Morgner
 */
public class BooleanValidator extends PropertyValidator {

	@Override
	public boolean isValid(GraphObject object, String key, Object value, ErrorBuffer errorBuffer) {

		String stringValue = value != null ? value.toString() : "";
		
		boolean valid = ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue));
		
		if(!valid) {
			errorBuffer.add(object.getType(), new ValueToken(key, new Object[] { true, false } ));
		}
		
		return valid;
	}
}
