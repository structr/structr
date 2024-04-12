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
package org.structr.web.entity.path;

import java.net.URI;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.parser.DatePropertyParser;

/**
 *
 */
public interface PagePathParameter extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("PagePathParameter");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/PagePathParameter"));

		type.addIntegerProperty("position",    PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("valueType",    PropertyView.Public, PropertyView.Ui);
		type.addStringProperty("defaultValue", PropertyView.Public, PropertyView.Ui);
		type.addBooleanProperty("isOptional",  PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("position",      Integer.class);
		type.addPropertyGetter("valueType",     String.class);
		type.addPropertyGetter("defaultValue",  String.class);
		type.addPropertyGetter("isOptional",    Boolean.TYPE);

		type.addPropertySetter("position", Integer.class);
	}}

	Integer getPosition();
	void setPosition(final Integer position) throws FrameworkException;

	String getValueType();
	String getDefaultValue();
	boolean getIsOptional();

	default Object convert(final String src) {

		try {

			if (src != null) {

				final String valueType = getValueType();

				switch (valueType) {

					case "String":
						return src;

					case "Double":
						return Double.valueOf(src);

					case "Float":
						return Double.valueOf(src).floatValue();

					case "Integer":
						return Double.valueOf(src).intValue();

					case "Long":
						return Double.valueOf(src).longValue();

					case "Boolean":
						return Boolean.valueOf(src);

					case "Date":
						return DatePropertyParser.parseISO8601DateString(src);

					default:
						LoggerFactory.getLogger(PagePathParameter.class).warn("Unknown valueType '{}', NOT converting input for PagePathParameter with path {}.", valueType, getName());
						return src;
				}
			}

		} catch (Throwable t) {

			// log error (or report it to somewhere), but don't fail here  because we are resolving a URL in the frontend and we don't want to send a 422 to the client..
			LoggerFactory.getLogger(PagePathParameter.class).warn("Exception while converting input for PagePathParameter with path {}: {}", getName(), t.getMessage());
		}

		return null;
	}
}
