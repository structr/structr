/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.core.property;

import org.apache.commons.codec.digest.DigestUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TooShortToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.ValidationInfo;

/**
 * A {@see StringProperty} that converts its value to a hexadecimal SHA512 hash upon storage.
 * The return value of this property will always be the password hash, the clear-text password
 * will be lost.
 *
 * @author Christian Morgner
 */
public class PasswordProperty extends StringProperty {

	private ValidationInfo validationInfo = null;
	
	public PasswordProperty(String name) {
		this(name, null);
	}
	
	public PasswordProperty(String name, ValidationInfo info) {
		super(name);
		
		this.validationInfo = info;
	}
	
	@Override
	public void registrationCallback(Class entityType) {

		if (validationInfo != null && validationInfo.getErrorKey() == null) {
			validationInfo.setErrorKey(this);
		}
	}
	
	@Override
	public String typeName() {
		return "String";
	}
	
	@Override
	public void setProperty(SecurityContext securityContext, GraphObject obj, String clearTextPassword) throws FrameworkException {
		
		if (clearTextPassword != null) {
			
			if (validationInfo != null) {

				String errorType     = validationInfo.getErrorType();
				PropertyKey errorKey = validationInfo.getErrorKey();
				int minLength        = validationInfo.getMinLength();

				if (minLength > 0 && clearTextPassword.length() < minLength) {

					throw new FrameworkException(errorType, new TooShortToken(errorKey, minLength));
				}
			}
		
			super.setProperty(securityContext, obj, DigestUtils.sha512Hex(clearTextPassword));
			
		} else {
			
			super.setProperty(securityContext, obj, null);
		}
	}
}
