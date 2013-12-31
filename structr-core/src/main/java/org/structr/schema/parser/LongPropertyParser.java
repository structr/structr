package org.structr.schema.parser;

import org.apache.commons.lang.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.LongProperty;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class LongPropertyParser extends NumericalPropertyParser {

	public LongPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String rawSource, final String defaultValue) {
		super(errorBuffer, className, propertyName, dbName, rawSource, defaultValue);
	}
	
	@Override
	public String getPropertyType() {
		return LongProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Long.class.getSimpleName();
	}
	
	@Override
	public Type getKey() {
		return Type.Long;
	}

	@Override
	public Number parseNumber(final ErrorBuffer errorBuffer, final String source, final String which) {
							
		try {
			return Long.parseLong(source);
			
		} catch (Throwable t) {
			
			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(source, "invalid_" + which +"_bound", StringUtils.capitalize(which) + " bound must be of type Long."));
		}
		
		return null;
	}

	@Override
	public String getDefaultValueSource() {
		return defaultValue.concat("L");
	}
}
