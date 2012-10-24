package org.structr.common.property;

import org.structr.common.SecurityContext;
import org.structr.common.property.Property;
import org.structr.core.converter.PasswordConverter;
import org.structr.core.converter.PropertyConverter;

/**
 *
 * @author Christian Morgner
 */
public class PasswordProperty extends Property<String> {
	
	public PasswordProperty(String name) {
		super(name);
	}
	
	@Override
	public PropertyConverter<?, String> databaseConverter(SecurityContext securityContext) {
		return new PasswordConverter(securityContext);
	}

	@Override
	public PropertyConverter<?, String> inputConverter(SecurityContext securityContext) {
		return new PasswordConverter(securityContext);
	}
}
