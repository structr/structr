package org.structr.schema.parser;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.SchemaNode.Type;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class StringPropertyParser extends PropertyParser {

	public StringPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String rawSource) {
		super(errorBuffer, className, propertyName, rawSource);
	}
	
	@Override
	public String getPropertyType() {
		return StringProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return String.class.getSimpleName();
	}

	@Override
	public String getEnumType() {
		return "";
	}
	
	@Override
	public Type getKey() {
		return Type.String;
	}

	@Override
	public void extractTypeValidation(String expression) throws FrameworkException {
		localValidator = ", new SimpleRegexValidator(\""  + expression + "\")";
	}
}
