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
import org.structr.common.SecurityContext;
import org.structr.core.property.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TooShortToken;

/**
 * @author Axel Morgner
 */
public class PasswordConverter extends PropertyConverter<String, String> {

	private ValidationInfo validationInfo = null;
	
	public PasswordConverter(SecurityContext securityContext) {
		this(securityContext, null);
	}
	
	public PasswordConverter(SecurityContext securityContext, ValidationInfo info) {
		
		super(securityContext, null);
		
		this.validationInfo = info;
	}
	
	@Override
	public String convert(String clearTextPassword) throws FrameworkException {
		
		if (StringUtils.isBlank(clearTextPassword)) {
			return null;
		}

		if (validationInfo != null) {
			
			String errorType     = validationInfo.getErrorType();
			PropertyKey errorKey = validationInfo.getErrorKey();
			int minLength        = validationInfo.getMinLength();

			if (minLength > 0 && clearTextPassword.length() < minLength) {

				throw new FrameworkException(errorType, new TooShortToken(errorKey, minLength));
			}
		}
		
		return DigestUtils.sha512Hex(clearTextPassword);
	}

	@Override
	public String revert(String passwordHash) {
		return passwordHash;
	}
}
