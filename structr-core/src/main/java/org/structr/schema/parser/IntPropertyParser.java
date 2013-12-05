package org.structr.schema.parser;

import org.apache.commons.lang.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.IntProperty;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class IntPropertyParser extends NumericalPropertyParser {
	
	public IntPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String rawSource) {
		super(errorBuffer, className, propertyName, rawSource);
	}

	@Override
	public String getPropertyType() {
		return IntProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Integer.class.getSimpleName();
	}
	
	@Override
	public Type getKey() {
		return Type.Integer;
	}

	@Override
	public Number parseNumber(final ErrorBuffer errorBuffer, final String source, final String which) {

		try {
			
			return Integer.parseInt(source);
			
		} catch (Throwable t) {
			
			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(source, "invalid_" + which +"_bound", StringUtils.capitalize(which) + " bound must be of type Integer."));
		}
		
		return null;
	}
}
