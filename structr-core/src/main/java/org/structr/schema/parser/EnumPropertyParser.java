package org.structr.schema.parser;

import org.apache.commons.lang.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.EnumProperty;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class EnumPropertyParser extends PropertyParser {
	
	private String enumTypeName = "";
	private String enumType     = "";
	
	public EnumPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String rawSource) {
		super(errorBuffer, className, propertyName, dbName, rawSource);
	}

	@Override
	public String getPropertyType() {
		return EnumProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return enumTypeName;
	}

	@Override
	public String getAuxiliaryType() {
		return enumType;
	}
	
	@Override
	public Type getKey() {
		return Type.Enum;
	}

	@Override
	public void extractTypeValidation(String expression) throws FrameworkException {

		final String[] enumTypes = expression.split("[, ]+");
		if (StringUtils.isNotBlank(expression) && enumTypes.length > 0) {

			enumTypeName = StringUtils.capitalize(propertyName);

			// create enum type
			enumType = ", " + enumTypeName + ".class";

			// build enum type definition

			final StringBuilder buf = new StringBuilder();

			buf.append("\n\tpublic enum ").append(enumTypeName).append(" {\n\t\t");
			for (int i=0; i<enumTypes.length; i++) {

				final String element = enumTypes[i];

				if (element.matches("[a-zA-Z_]+")) {

					buf.append(element);

					// comma separation
					if (i < enumTypes.length-1) {
						buf.append(", ");
					}

				} else {

					errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_property_definition", "Invalid enum type name, must match [a-zA-Z_]+."));

				}
			}
			buf.append("\n\t};");

			enumDefinitions.add(buf.toString());

		} else {

			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(expression, "invalid_property_definition", "No enum types found, please specify a list of types, e.g. (red, green, blue)"));
		}
	}
}
