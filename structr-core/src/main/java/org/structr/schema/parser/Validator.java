package org.structr.schema.parser;

import org.structr.schema.SchemaHelper;

/**
 *
 * @author Christian Morgner
 */
public class Validator {

	private String validator    = null;
	private String className    = null;
	private String propertyName = null;

	public Validator(final String validator, final String className, final String propertyName) {

		this.validator    = validator;
		this.className    = className;
		this.propertyName = propertyName;
	}

	public String getSource(final String obj, final boolean includeClassName) {

		StringBuilder buf = new StringBuilder();

		buf.append("ValidationHelper.").append(validator).append("(").append(obj).append(", ");

		if (includeClassName) {
			buf.append(className).append(".");
		}

		buf.append(SchemaHelper.cleanPropertyName(propertyName)).append("Property");
		buf.append(", errorBuffer)");

		return buf.toString();
	}
}
