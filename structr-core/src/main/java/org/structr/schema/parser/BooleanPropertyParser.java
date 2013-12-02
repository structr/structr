package org.structr.schema.parser;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.BooleanProperty;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class BooleanPropertyParser extends PropertyParser {

	public BooleanPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String rawSource) {
		super(errorBuffer, className, propertyName, rawSource);
	}
	
	@Override
	public String getPropertyType() {
		return BooleanProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Boolean.class.getSimpleName();
	}

	@Override
	public String getEnumType() {
		return "";
	}
	
	@Override
	public Type getKey() {
		return Type.Boolean;
	}

	@Override
	public void extractTypeValidation(String expression) throws FrameworkException {
	}
}
