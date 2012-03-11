/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.converter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.structr.core.PropertyConverter;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */
public class PasswordEncrypter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(PasswordEncrypter.class.getName());

	@Override
	public Object convertForSetter(Object source, Value value) {


		if(source != null) {

			if(source instanceof String) {

				if(!((String)source).isEmpty()) {

					return PasswordEncrypter.encryptPassword((String)source);

				} else {

					logger.log(Level.WARNING, "Received empty string, returning null.");
				}

			} else {

				logger.log(Level.WARNING, "Received object of invalid type {0}, returning null.", source.getClass().getName());
			}
		} else {

			logger.log(Level.WARNING, "Received null object, returning null.");
		}

		return null;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
//		Thread.dumpStack();
		return source;
	}

	public static String encryptPassword(String password) {
//		Thread.dumpStack();
		return DigestUtils.sha512Hex(password);
	}
}
