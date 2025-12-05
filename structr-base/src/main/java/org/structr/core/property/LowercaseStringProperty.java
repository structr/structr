/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import java.util.Map;

/**
 * A property that stores and retrieves a String value.
 *
 * The String can have an optional content MIME type, as described in http://en.wikipedia.org/wiki/MIME
 *
 *
 *
 */
public class LowercaseStringProperty extends AbstractPrimitiveProperty<String> {

	private static final Logger logger = LoggerFactory.getLogger(LowercaseStringProperty.class.getName());
	private ContentType contentType;

	public LowercaseStringProperty(final String jsonName) {
		super(jsonName);
	}

	public LowercaseStringProperty(final String jsonName, final String dbName) {
		super(jsonName);
		this.dbName = dbName;
	}

	@Override
	public String getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		final String value = super.getProperty(securityContext, obj, applyConverter, predicate);
		if (value != null) {

			return value.toLowerCase();
		}

		return null;
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final String value) throws FrameworkException {

		if (value != null) {

			return super.setProperty(securityContext, obj, value.toLowerCase());
		}

		return super.setProperty(securityContext, obj, null);
	}

	@Override
	public boolean isArray() {
		return false;
	}

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

			return value.toString().toLowerCase();
		}

		return null;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
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
	public PropertyConverter<?, String> inputConverter(SecurityContext securityContext, boolean fromString) {
		return new PropertyConverter<Object, String>(securityContext) {

			@Override
			public Object revert(String source) throws FrameworkException {
				return source;
			}

			@Override
			public String convert(final Object source) throws FrameworkException {

				if (source != null) {
					return source.toString().toLowerCase();
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

	public LowercaseStringProperty contentType(final String contentType) {
		this.contentType = parse(contentType);
		return this;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return "lowercase string example";
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	// ----- private methods -----
	private static ContentType parse(final String contentTypeString) {

		try {

			return new ContentType(contentTypeString);

		} catch (ParseException pe) {

			logger.warn("Could not parse " + contentTypeString, pe);

		}

		return null;
	}
}
