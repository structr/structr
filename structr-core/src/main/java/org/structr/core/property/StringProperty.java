/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.property;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.converter.PropertyConverter;

/**
 * A property that stores and retrieves a String value.
 *
 * The String can have an optional content MIME type, as described in http://en.wikipedia.org/wiki/MIME
 *
 *
 *
 */
public class StringProperty extends AbstractPrimitiveProperty<String> {

	private static final Logger logger = Logger.getLogger(StringProperty.class.getName());
	private ContentType contentType;

	public StringProperty(final String jsonName) {
		super(jsonName);
	}

	public StringProperty(final String jsonName, final String dbName) {
		super(jsonName);
		this.dbName = dbName;
	}

	public StringProperty(final String jsonName, final PropertyValidator<String>... validators) {
		this(jsonName, jsonName, validators);
	}

	public StringProperty(final String jsonName, final String dbName, final PropertyValidator<String>... validators) {

		super(jsonName);
		this.dbName = dbName;

		for (PropertyValidator<String> validator : validators) {
			addValidator(validator);
		}
	}
//
//
//
//	public StringProperty(final String jsonName, final ContentType contentType, final String defaultValue) {
//		this(jsonName, jsonName, contentType, defaultValue, null, new PropertyValidator[0]);
//	}
//
//	public StringProperty(final String jsonName, final ContentType contentType, final String defaultValue, final String format) {
//		this(jsonName, jsonName, contentType, defaultValue, format, new PropertyValidator[0]);
//	}
//
//
//
//	public StringProperty(final String jsonName, final String dbName, final PropertyValidator<String>... validators) {
//		this(jsonName, dbName, null, null, null, validators);
//	}
//
//	public StringProperty(final String jsonName, final String dbName, final String defaultValue, final PropertyValidator<String>... validators) {
//		this(jsonName, dbName, null, defaultValue, null, validators);
//	}
//
//	public StringProperty(final String jsonName, final String dbName, final String defaultValue, final String format, final PropertyValidator<String>... validators) {
//		this(jsonName, dbName, null, defaultValue, format, validators);
//	}
//
//	public StringProperty(final String jsonName, final String dbName, final ContentType contentType, final String defaultValue) {
//		this(jsonName, dbName, contentType, defaultValue, null, new PropertyValidator[0]);
//	}
//
//	public StringProperty(final String jsonName, final String dbName, final ContentType contentType, final String defaultValue, final String format) {
//		this(jsonName, dbName, contentType, defaultValue, format, new PropertyValidator[0]);
//	}
//
//
//
//	public StringProperty(final String jsonName, final String dbName, final ContentType contentType, final String defaultValue, final String format, final PropertyValidator<String>... validators) {
//
//		super(jsonName, dbName, defaultValue);
//		this.contentType = contentType;
//		this.format = format;
//
//		for (PropertyValidator<String> validator : validators) {
//			addValidator(validator);
//		}
//	}

	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public Class valueType() {
		return String.class;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof String) {
				return value;
			}

			return value.toString();
		}

		return null;
	}

	@Override
	public Integer getSortType() {
		return null;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, String> inputConverter(SecurityContext securityContext) {
		return new PropertyConverter<Object, String>(securityContext) {

			@Override
			public Object revert(String source) throws FrameworkException {
				return source;
			}

			@Override
			public String convert(final Object source) throws FrameworkException {

				if (source != null) {
					return source.toString();
				}

				return null;
			}
		};
	}

	/**
	 * Returns the optional content type for this property.
	 *
	 * @return contentType
	 */
	public String contentType() {
		return contentType != null ? contentType.toString() : null;
	}

	public StringProperty contentType(final String contentType) {
		this.contentType = parse(contentType);
		return this;
	}

	// ----- CMIS support -----
	@Override
	public PropertyType getDataType() {
		return PropertyType.STRING;
	}

	// ----- private methods -----
	private static ContentType parse(final String contentTypeString) {

		try {

			return new ContentType(contentTypeString);

		} catch (ParseException pe) {

			logger.log(Level.WARNING, "Could not parse " + contentTypeString, pe);

		}

		return null;
	}
}
