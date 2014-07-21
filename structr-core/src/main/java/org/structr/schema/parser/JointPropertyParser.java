package org.structr.schema.parser;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.JointProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public class JointPropertyParser extends PropertyParser {

	private String parameters   = "";

	public JointPropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String rawSource, final String defaultValue) {
		super(errorBuffer, className, propertyName, dbName, rawSource, defaultValue);
	}

	@Override
	public SchemaHelper.Type getKey() {
		return Type.Joint;
	}

	@Override
	public String getPropertyType() {
		return JointProperty.class.getName();
	}

	@Override
	public String getValueType() {
		return String.class.getSimpleName();
	}

	@Override
	public String getPropertyParameters() {
		return parameters;
	}

	@Override
	public void parseFormatString(Schema entity, String expression) throws FrameworkException {

		final StringBuilder buf = new StringBuilder(", ");
		final String[] parts    = expression.split("[, ]+");

		buf.append(parts[0]);
		buf.append(", ");

		for (int i=1; i<parts.length; i++) {

			String propertyName = parts[i];

			if (propertyName.startsWith("_")) {
				propertyName = propertyName.substring(1) + "Property";
			}

			buf.append(propertyName);

			if (i < parts.length-1) {
				buf.append(", ");
			}
		}

		parameters = buf.toString();
	}
}
