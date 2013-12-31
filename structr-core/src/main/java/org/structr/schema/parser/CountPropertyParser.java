package org.structr.schema.parser;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.ElementCounter;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class CountPropertyParser extends PropertyParser {

	private String auxType = "";
	
	public CountPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String rawSource, final String defaultValue) {
		super(errorBuffer, className, propertyName, dbName, rawSource, defaultValue);
	}
	
	@Override
	public String getPropertyType() {
		return ElementCounter.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Integer.class.getSimpleName();
	}

	@Override
	public String getAuxiliaryType() {
		return auxType;
	}
	
	@Override
	public Type getKey() {
		return Type.Count;
	}

	@Override
	public void extractTypeValidation(final String expression) throws FrameworkException {
		
		if (expression.isEmpty()) {
			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_property_reference", "Empty property reference."));
			return;
		}

		auxType = ", " + expression + "Property";
	}
}
