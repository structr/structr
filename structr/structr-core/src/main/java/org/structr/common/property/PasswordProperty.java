package org.structr.common.property;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
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
	public String typeName() {
		return "String";
	}
	
	@Override
	public PropertyConverter<String, String> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new PasswordConverter(securityContext, info);
	}

	@Override
	public PropertyConverter<String, String> inputConverter(SecurityContext securityContext) {
		return null;
	}
}
