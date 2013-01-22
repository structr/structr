/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.converter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.structr.common.SecurityContext;

/**
 * Returns the hexadecimal SHA512 hash of a given value.
 * 
 * @author Christian Morgner
 */
public class PasswordEncrypter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(PasswordEncrypter.class.getName());

	public PasswordEncrypter(SecurityContext securityContext) {
		super(securityContext, null);
	}
	
	@Override
	public Object convert(Object source) {

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
	public Object revert(Object source) {
		return source;
	}

	public static String encryptPassword(String password) {
		return DigestUtils.sha512Hex(password);
	}
}
