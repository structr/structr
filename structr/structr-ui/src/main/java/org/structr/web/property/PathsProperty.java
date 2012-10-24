package org.structr.web.property;

import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.property.SourceProperty;
import org.structr.core.converter.PropertyConverter;
import org.structr.web.converter.PathsConverter;

/**
 *
 * @author Christian Morgner
 */
public class PathsProperty extends SourceProperty<Set<String>> {

	public PathsProperty(String name) {
		super(name);
	}
	
	@Override
	public PropertyConverter<?, Set<String>> getSource(SecurityContext securityContext) {
		return new PathsConverter(securityContext);
	}
}
