/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * A property that applies a given converter to its value. This is needed for backwards compatibility only
 * and will be removed in future releases.
 *
 * @deprecated This property is needed for backwards compatibility only and will be removed in future releases
 *
 *
 */
@Deprecated
public class ConverterProperty<T> extends AbstractPrimitiveProperty<T> {

	private static final Logger logger = LoggerFactory.getLogger(ConverterProperty.class.getName());
	private Constructor constructor    = null;

	public ConverterProperty(final String name, final Class<? extends PropertyConverter<?, T>> converterClass) {

		super(name);

		try {
			this.constructor = converterClass.getConstructor(SecurityContext.class, GraphObject.class);

		} catch(NoSuchMethodException nsmex) {

			logger.error("Unable to instantiate converter of type {} for key {}", new Object[] {
				converterClass.getName(),
				name
			});
		}

		// make us known to the entity context
		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public String typeName() {
		return ""; // read-only
	}

	@Override
	public String valueType() {
		return null;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		return createConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(final SecurityContext securityContext) {
		return null;
	}

	private PropertyConverter createConverter(final SecurityContext securityContext, final GraphObject entity) {

		try {

			return (PropertyConverter<?, T>)constructor.newInstance(securityContext, entity);

		} catch(Throwable t) {

			logger.error("Unable to instantiate converter of type {} for key {}", new Object[] {
				constructor.getClass().getName(),
				dbName
			});
		}

		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
