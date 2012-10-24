package org.structr.web.property;

import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.property.Property;
import org.structr.core.converter.PropertyConverter;
import org.structr.web.converter.PathsConverter;

/**
 *
 * @author Christian Morgner
 */
public class PathsProperty extends Property<Set<String>> {

	public PathsProperty(String name) {
		super(name);
	}
	
	@Override
	public PropertyConverter<?, Set<String>> databaseConverter(SecurityContext securityContext) {
		return new PathsConverter(securityContext);
	}

	@Override
	public PropertyConverter<?, Set<String>> inputConverter(SecurityContext securityContext) {
		return new PathsConverter(securityContext);
	}
}
