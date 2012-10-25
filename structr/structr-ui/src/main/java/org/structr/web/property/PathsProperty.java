package org.structr.web.property;

import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.property.Property;
import org.structr.core.GraphObject;
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
	public PropertyConverter<Set<String>, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new PathsConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, Set<String>> inputConverter(SecurityContext securityContext) {
		return null;
	}
}
