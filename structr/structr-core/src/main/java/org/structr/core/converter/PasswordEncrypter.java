/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
