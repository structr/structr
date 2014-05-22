/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.parser;

import java.util.Set;
import java.util.LinkedHashSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public abstract class PropertyParser {

	protected Set<String> globalValidators = new LinkedHashSet<>();
	protected Set<String> enumDefinitions  = new LinkedHashSet<>();
	protected ErrorBuffer errorBuffer      = null;
	protected String propertyName          = "";
	protected String dbName                = "";
	protected String localValidator        = "";
	protected String className             = "";
	protected String rawSource             = "";
	protected String defaultValue          = "";

	public abstract Type getKey();
	public abstract String getPropertyType();
	public abstract String getValueType();
	public abstract String getPropertyParameters();
	public abstract void extractTypeValidation(final Schema entity, final String expression) throws FrameworkException;

	public PropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final String dbName, final String rawSource, final String defaultValue) {

		this.errorBuffer  = errorBuffer;
		this.className    = className;
		this.propertyName = propertyName;
		this.dbName       = dbName;
		this.rawSource    = rawSource;
		this.defaultValue = defaultValue;

		if (this.propertyName.startsWith("_")) {
			this.propertyName = this.propertyName.substring(1);
		}
	}

	public String getPropertySource(final Schema entity, final ErrorBuffer errorBuffer) throws FrameworkException {

		final String keyName   = getKey().name();
		String parserSource    = rawSource.substring(keyName.length());

		// second: uniqueness and/or non-null, check until the two methods to not change the length of the string any more
		parserSource = extractUniqueness(parserSource);

		extractComplexValidation(entity, parserSource);

		return getPropertySource();
	}

	public Set<String> getGlobalValidators() {
		return globalValidators;
	}

	public Set<String> getEnumDefinitions() {
		return enumDefinitions;
	}

	// ----- protected methods -----
	protected String getPropertySource() {

		final StringBuilder buf = new StringBuilder();

		String valueType = getValueType();

		buf.append("\tpublic static final Property<").append(valueType).append("> ").append(SchemaHelper.cleanPropertyName(propertyName)).append("Property");
		buf.append(" = new ").append(getPropertyType()).append("(\"").append(propertyName).append("\"");
		if (dbName != null) {
			buf.append(", \"").append(dbName).append("\"");
		} else if ("String".equals(valueType)) {
			// StringProperty has three leading String parameters
			buf.append(", \"").append(propertyName).append("\"");
		}
		buf.append(getPropertyParameters());
		if (defaultValue != null) {
			buf.append(", ").append(getDefaultValueSource());
		}
		buf.append(localValidator);
		buf.append(").indexed();\n");

		return buf.toString();
	}

	private String extractUniqueness(final String source) {

		if (source.startsWith("!")) {

			StringBuilder buf = new StringBuilder();
			buf.append("ValidationHelper.checkPropertyUniquenessError(this, ");
			buf.append(className).append(".").append(SchemaHelper.cleanPropertyName(propertyName)).append("Property");
			buf.append(", errorBuffer)");

			globalValidators.add(buf.toString());

			return source.substring(1);
		}

		return source;
	}

	public void setNotNull(final boolean notNull) {

		if (notNull) {

			StringBuilder buf = new StringBuilder();
			buf.append("ValidationHelper.checkPropertyNotNull(this, ");
			buf.append(className).append(".").append(SchemaHelper.cleanPropertyName(propertyName)).append("Property");
			buf.append(", errorBuffer)");

			globalValidators.add(buf.toString());
		}
	}

	private void extractComplexValidation(final Schema entity, final String source) throws FrameworkException {

		if (StringUtils.isNotBlank(source)) {

			if (source.startsWith("(") && source.endsWith(")")) {

				extractTypeValidation(entity, source.substring(1, source.length() - 1));

			} else {

				errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(source, "invalid_validation_expression", "Valdation expression must be enclosed in (), e.g. (" + source + ")"));
			}
		}
	}

	public String getDefaultValueSource() {
		return defaultValue;
	}

}
