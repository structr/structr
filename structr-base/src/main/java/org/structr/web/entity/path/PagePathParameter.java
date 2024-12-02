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

import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.schema.parser.DatePropertyParser;
import org.structr.web.entity.dom.relationship.PagePathHAS_PARAMETERPagePathParameter;

/**
 *
 */
public class PagePathParameter extends AbstractNode {

	public static Property<PagePath> pathProperty             = new StartNode<>("path", PagePathHAS_PARAMETERPagePathParameter.class).partOfBuiltInSchema();
	public static final Property<Integer> positionProperty    = new IntProperty("position").indexed().partOfBuiltInSchema();
	public static final Property<String> valueTypeProperty    = new StringProperty("valueType").partOfBuiltInSchema();
	public static final Property<String> defaultValueProperty = new StringProperty("defaultValue").partOfBuiltInSchema();
	public static final Property<Boolean> isOptionalProperty  = new BooleanProperty("isOptional").partOfBuiltInSchema();

	public static final View defaultView = new View(PagePathParameter.class, PropertyView.Public,
		positionProperty, valueTypeProperty, defaultValueProperty, isOptionalProperty
	);

	public static final View uiView = new View(PagePathParameter.class, PropertyView.Ui,
		positionProperty, valueTypeProperty, defaultValueProperty, isOptionalProperty
	);

	public Integer getPosition() {
		return getProperty(positionProperty);
	}

	public void setPosition(final Integer position) throws FrameworkException {
		setProperty(positionProperty, position);
	}

	public String getValueType() {
		return getProperty(valueTypeProperty);
	}

	public String getDefaultValue() {
		return getProperty(defaultValueProperty);
	}

	public boolean getIsOptional() {
		return getProperty(isOptionalProperty);
	}

	public Object convert(final String src) {

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
