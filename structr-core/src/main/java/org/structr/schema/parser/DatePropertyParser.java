package org.structr.schema.parser;

import java.util.Date;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaNode.Type;
import org.structr.core.property.ISO8601DateProperty;

/**
 *
 * @author Christian Morgner
 */
public class DatePropertyParser extends PropertyParser {

	public DatePropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String rawSource) {
		super(errorBuffer, className, propertyName, rawSource);
	}
	
	@Override
	public String getPropertyType() {
		return ISO8601DateProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Date.class.getSimpleName();
	}

	@Override
	public String getEnumType() {
		return "";
	}
	
	@Override
	public Type getKey() {
		return Type.Date;
	}

	@Override
	public void extractTypeValidation(String expression) throws FrameworkException {
	}
}
