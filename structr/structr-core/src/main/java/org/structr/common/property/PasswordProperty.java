package org.structr.common.property;

import org.structr.common.SecurityContext;
import org.structr.core.converter.PasswordConverter;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.converter.ValidationInfo;

/**
 *
 * @author Christian Morgner
 */
public class PasswordProperty extends Property<String> {

	private ValidationInfo info = null;
	
	public PasswordProperty(String name) {
		this(name, null);
	}
	
	public PasswordProperty(String name, ValidationInfo info) {
		super(name);
		
		this.info = info;
	}
	
	@Override
	public PropertyConverter<?, String> databaseConverter(SecurityContext securityContext) {
		return new PasswordConverter(securityContext, info);
	}

	@Override
	public PropertyConverter<?, String> inputConverter(SecurityContext securityContext) {
		return new PasswordConverter(securityContext, info);
	}
}
