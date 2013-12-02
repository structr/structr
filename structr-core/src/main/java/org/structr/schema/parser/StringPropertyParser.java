package org.structr.schema.parser;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaHelper.Type;

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
	public void extractTypeValidation(final String expression) throws FrameworkException {
		
		if ("[]".equals(expression)) {
			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_validation_expression", "Empty validation expression."));
			return;
		}
		
		localValidator = ", new SimpleRegexValidator(\""  + expression + "\")";
	}
}
