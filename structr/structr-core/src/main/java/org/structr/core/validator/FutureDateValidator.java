/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.validator;

import java.util.logging.Logger;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FutureDateToken;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Value;

/**
 * A validator that ensures a given date lies in the future.
 *
 * @author Christian Morgner
 */
public class FutureDateValidator extends PropertyValidator {

	private static final Logger logger = Logger.getLogger(FutureDateValidator.class.getName());

	@Override
	public boolean isValid(GraphObject object, String key, Object value, ErrorBuffer errorBuffer) {

		if(value != null) {

			if(value instanceof Long) {
				
				if(((Long)value).longValue() < System.currentTimeMillis()) {

					errorBuffer.add(object.getType(), new FutureDateToken(key));
					return false;
				}

				return true;

			} else {

				errorBuffer.add(object.getType(), new TypeToken(key, "long"));
			}

		} else {

			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));
		}

		return false;
	}
}
