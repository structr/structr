/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.parser;

import java.util.Set;
import java.util.LinkedHashSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 * @author Christian Morgner
 */
public abstract class PropertyParser {

	protected Set<Validator> globalValidators = new LinkedHashSet<>();
	protected Set<String> enumDefinitions     = new LinkedHashSet<>();
	protected PropertyKey realInstance        = null;
	protected ErrorBuffer errorBuffer         = null;
	protected String propertyName             = "";
	protected String dbName                   = "";
	protected String localValidator           = "";
	protected String className                = "";
	protected String rawSource                = "";
	protected String source                   = "";
	protected String format                   = "";
	protected String defaultValue             = "";
	protected boolean notNull                 = false;
	protected PropertyParameters params;

	public abstract Type getKey();
	public abstract String getPropertyType();
	public abstract String getValueType();
	public abstract String getUnqualifiedValueType();
	public abstract String getPropertyParameters();
	public abstract void parseFormatString(final Schema entity, final String expression) throws FrameworkException;

	public PropertyParser(final ErrorBuffer errorBuffer, final String className, final String propertyName, final PropertyParameters params) {

		this.errorBuffer  = errorBuffer;
		this.className    = className;
		this.propertyName = propertyName;
		this.rawSource    = params.rawSource;
		this.source       = params.source;
		this.format       = params.format;
		this.notNull      = Boolean.TRUE.equals(params.notNull);
		this.dbName       = params.dbName;
		this.defaultValue = params.defaultValue;

		if (this.propertyName.startsWith("_")) {
			this.propertyName = this.propertyName.substring(1);
		}
	}

	public String getPropertySource(final Schema entity, final ErrorBuffer errorBuffer) throws FrameworkException {

		final String keyName = getKey().name();
		source               = source.substring(keyName.length());

		setNotNull(notNull);

		// second: uniqueness and/or non-null, check until the two methods to not change the length of the string any more
		extractUniqueness();

		extractComplexValidation(entity);

		return getPropertySource();
	}

	public Set<Validator> getGlobalValidators() {
		return globalValidators;
	}

	public Set<String> getEnumDefinitions() {
		return enumDefinitions;
	}

	public PropertyKey getRealInstance() {
		return realInstance;
	}

	public String getPropertyName() {
		return SchemaHelper.cleanPropertyName(propertyName) + "Property";
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

	private void extractUniqueness() {

		if (source.startsWith("!")) {

			globalValidators.add(new Validator("checkPropertyUniquenessError", className, propertyName));

			source = source.substring(1);
		}

	}

	private void setNotNull(final boolean notNull) {

		if (notNull) {

			globalValidators.add(new Validator("checkPropertyNotNull", className, propertyName));
		}
	}

	private void extractComplexValidation(final Schema entity) throws FrameworkException {

//		if (format != null) {

			parseFormatString(entity, format);

//		} else {
//
//			errorBuffer.add(SchemaNode.class.getSimpleName(), new InvalidPropertySchemaToken(source, "invalid_validation_expression", "Validation expression must be enclosed in (), e.g. (" + source + ")"));
//		}
	}

	public String getDefaultValueSource() {
		return defaultValue;
	}

	public static PropertyParameters detectDbNameNotNullAndDefaultValue(final String rawSource) {

		final PropertyParameters params = new PropertyParameters(rawSource);

		// detect optional db name
		if (rawSource.contains("|")) {

			params.dbName = rawSource.substring(0, rawSource.indexOf("|"));
			params.source = rawSource.substring(rawSource.indexOf("|")+1);

		}

		// detect and remove not-null constraint
		if (params.source.startsWith("+")) {
			params.source = params.source.substring(1);
			params.notNull = true;
		}

		// detect and remove format: <type>(...)
		if (StringUtils.isNotBlank(params.source)) {

			params.format = substringBetween(params.source, "(", ")");
//			params.format = StringUtils.substringBetween(params.source, "(", ")");
			params.source = params.source.replaceFirst("\\(.*\\)", "");

		}

		// detect and remove default value
		if (params.source.contains(":")) {

			// default value is everything after the first :
			// this is possible because we stripped off the format (...) above
			int firstIndex      = params.source.indexOf(":");
			params.defaultValue = params.source.substring(firstIndex + 1);
			params.source       = params.source.substring(0, firstIndex);

		}

		return params;

	}

	public static String substringBetween(final String source, final String prefix, final String suffix) {

		final int pos1 = source.indexOf(prefix);
		final int pos2 = source.lastIndexOf(suffix);

		if (pos1 < pos2 && pos2 > 0) {

			return source.substring(pos1 + 1, pos2);
		}

		return null;
	}
}
