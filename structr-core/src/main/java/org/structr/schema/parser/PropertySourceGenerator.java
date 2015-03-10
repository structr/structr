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
import org.apache.commons.lang3.StringEscapeUtils;
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
public abstract class PropertySourceGenerator {

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
	protected String contentType              = "";
	protected boolean notNull                 = false;
	protected boolean unique                  = false;

	public abstract Type getKey();
	public abstract String getPropertyType();
	public abstract String getValueType();
	public abstract String getUnqualifiedValueType();
	public abstract String getPropertyParameters();
	public abstract void parseFormatString(final Schema entity, final String expression) throws FrameworkException;

	public PropertySourceGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {

		this.errorBuffer  = errorBuffer;
		this.className    = className;
		this.rawSource    = params.getRawSource();
		this.source       = params.getSource();
		this.format       = params.getFormat();
		this.notNull      = params.isNotNull();
		this.dbName       = params.getDbName();
		this.defaultValue = params.getDefaultValue();
		this.contentType  = params.getContentType();

		if (this.propertyName.startsWith("_")) {
			this.propertyName = this.propertyName.substring(1);
		}
	}

	public String getPropertySource(final Schema entity) throws FrameworkException {

		final String keyName = getKey().name();
		source               = source.substring(keyName.length());

		if (notNull) {

			globalValidators.add(new Validator("checkPropertyNotNull", className, propertyName));
		}


		if (source.startsWith("!")) {

			globalValidators.add(new Validator("checkPropertyUniquenessError", className, propertyName));

			source = source.substring(1);
			unique = true;
		}

		parseFormatString(entity, format);

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

		final String valueType     = getValueType();

		buf.append("\tpublic static final Property<").append(valueType).append("> ").append(SchemaHelper.cleanPropertyName(propertyName)).append("Property");
		buf.append(" = new ").append(getPropertyType()).append("(\"").append(propertyName).append("\"");

		if (StringUtils.isNotBlank(dbName)) {
			buf.append(", \"").append(dbName).append("\"");
		}

		buf.append(getPropertyParameters());

		buf.append(localValidator);

		buf.append(")");

		if (StringUtils.isNotBlank(contentType)) {
			buf.append(".contentType(\"").append(contentType).append("\")");
		}

		if (defaultValue != null) {
			buf.append(".defaultValue(").append(getDefaultValueSource()).append(")");
		}

		if (StringUtils.isNotBlank(format)) {
			buf.append(".format(\"").append(StringEscapeUtils.escapeJava(format)).append("\")");
		}

		if (unique) {
			buf.append(".unique()");
		}

		if (notNull) {
			buf.append(".notNull()");
		}

		if (defaultValue != null) {
			buf.append(".indexedWhenEmpty()");
		} else {
			buf.append(".indexed()");
		}

		buf.append(";\n");


		System.out.println("##############################################");
		System.out.println("propertyName:      " + propertyName);
		System.out.println("dbName:            " + dbName);
		System.out.println("localValidator:    " + localValidator);
		System.out.println("className:         " + className);
		System.out.println("rawSource:         " + rawSource);
		System.out.println("source:            " + source);
		System.out.println("format:            " + format);
		System.out.println("defaultValue:      " + defaultValue);
		System.out.println("contentType:       " + contentType);
		System.out.println("notNull:           " + notNull);
		System.out.println("unique:            " + unique);


		return buf.toString();
	}


	public String getDefaultValueSource() {
		return defaultValue;
	}
}
