/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.structr.common.property.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TooShortToken;
import org.structr.core.Value;

/**
 * @author Axel Morgner
 */
public class PasswordConverter extends PropertyConverter<String, String, ValidationInfo> {

	@Override
	public String convertForSetter(String clearTextPassword, Value<ValidationInfo> value) throws FrameworkException {
		
		if (StringUtils.isBlank(clearTextPassword)) return null;

		if (value != null) {
			
			ValidationInfo validationInfo = (ValidationInfo)value.get(securityContext);
			if (validationInfo != null) {

				String errorType     = validationInfo.getErrorType();
				PropertyKey errorKey = validationInfo.getErrorKey();
				int minLength        = validationInfo.getMinLength();

				if (minLength > 0 && clearTextPassword.length() < minLength) {

					throw new FrameworkException(errorType, new TooShortToken(errorKey, minLength));
				}
			}
		}
		
		return DigestUtils.sha512Hex(clearTextPassword);
	}

	@Override
	public String convertForGetter(String passwordHash, Value<ValidationInfo> value) {
		return passwordHash;
	}
}
