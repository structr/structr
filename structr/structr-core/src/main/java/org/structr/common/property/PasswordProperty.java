package org.structr.common.property;

import org.apache.commons.codec.digest.DigestUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TooShortToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.ValidationInfo;
import org.structr.core.property.PropertyKey;

/**
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
