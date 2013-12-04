package org.structr.schema.parser;

import java.util.Date;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.DateProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class DatePropertyParser extends PropertyParser {

	private String auxType = ", \"" + ISO8601DateProperty.PATTERN + "\"";
	
	public DatePropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String rawSource) {
		super(errorBuffer, className, propertyName, rawSource);
	}
	
	@Override
	public String getPropertyType() {
		return DateProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Date.class.getSimpleName();
	}

	@Override
	public String getAuxiliaryType() {
		return auxType;
	}
	
	@Override
	public Type getKey() {
		return Type.Date;
	}

	@Override
	public void extractTypeValidation(String expression) throws FrameworkException {
		
		if (expression.length() == 0) {
			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_date_pattern", "Empty date pattern."));
			return;
		}
		
		auxType = ", \"" + expression +"\"";
	}
}
