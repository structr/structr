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
package org.structr.web.traits.wrappers;

import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.parser.DatePropertyGenerator;
import org.structr.web.entity.path.PagePathParameter;

/**
 *
 */
public class PagePathParameterTraitWrapper extends AbstractNodeTraitWrapper implements PagePathParameter {

	public PagePathParameterTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Integer getPosition() {
		return wrappedObject.getProperty(traits.key("position"));
	}

	@Override
	public void setPosition(final Integer position) throws FrameworkException {
		wrappedObject.setProperty(traits.key("position"), position);
	}

	@Override
	public String getValueType() {
		return wrappedObject.getProperty(traits.key("valueType"));
	}

	@Override
	public String getDefaultValue() {
		return wrappedObject.getProperty(traits.key("defaultValue"));
	}

	@Override
	public boolean getIsOptional() {
		return wrappedObject.getProperty(traits.key("isOptional"));
	}

	@Override
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
						return DatePropertyGenerator.parseISO8601DateString(src);

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
