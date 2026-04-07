/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.parser.DatePropertyGenerator;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.servlet.HtmlServlet;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class PagePathParameterTraitWrapper extends AbstractNodeTraitWrapper implements PagePathParameter {

	public PagePathParameterTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public Integer getPosition() {
		return wrappedObject.getProperty(traits.key(PagePathParameterTraitDefinition.POSITION_PROPERTY));
	}

	@Override
	public void setPosition(final Integer position) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PagePathParameterTraitDefinition.POSITION_PROPERTY), position);
	}

	@Override
	public String getValueType() {
		return wrappedObject.getProperty(traits.key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY));
	}

	@Override
	public String getFormat() {
		return wrappedObject.getProperty(traits.key(PagePathParameterTraitDefinition.FORMAT_PROPERTY));
	}

	@Override
	public String getDefaultValue() {
		return wrappedObject.getProperty(traits.key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY));
	}

	@Override
	public boolean getIsOptional() {
		return wrappedObject.getProperty(traits.key(PagePathParameterTraitDefinition.IS_OPTIONAL_PROPERTY));
	}

	@Override
	public Object convert(final SecurityContext securityContext, final String src) {

		try {

			if (src != null) {

				final String valueType = getValueType();

				switch (valueType) {

					case "String":
						return src;

					case "Double":
						return Double.valueOf(src);

					case "Float":
						return Float.parseFloat(src);

					case "Integer":
						return Integer.parseInt(src);

					case "Long":
						return Long.parseLong(src);

					case "Boolean":
						return Boolean.valueOf(src);

					case "Date":

						final String dateFormat = getFormat();

						if (dateFormat != null) {

							try {

								return new SimpleDateFormat(dateFormat).parse(src);

							} catch (ParseException ex) {

								LoggerFactory.getLogger(PagePathParameter.class).warn("Could not parse string '{}' with date pattern '{}' for PagePathParameter with path {}", src, dateFormat, getName());
								return null;
							}

						} else {

							return DatePropertyGenerator.parseISO8601DateString(src);
						}

					case "Node":

						final String typeString = getFormat();
						Traits typeTraits       = null;

						if (typeString != null && !typeString.isBlank()) {

							if (Traits.exists(typeString)) {

								typeTraits = Traits.of(typeString);

							} else {

								LoggerFactory.getLogger(PagePathParameter.class).warn("Unknown node type '{}', NOT applying hierarchy check for PagePathParameter with path {}", typeString, getName());
							}
						}

						if (Settings.isValidUuid(src)) {

							final QueryGroup<NodeInterface> query = StructrApp.getInstance(securityContext).nodeQuery().and().uuid(src);
							if (typeTraits != null) {

								query.types(typeTraits);
							}

							return query.getFirst();

						} else {

							// check for other resolve properties

							final QueryGroup<NodeInterface> query = StructrApp.getInstance(securityContext).nodeQuery().and();
							if (typeTraits != null) {

								query.types(typeTraits);
							}

							HtmlServlet.processConfiguredPropertyNamesForObjectResolution(query, src);

							return query.getFirst();
						}

					default:
						LoggerFactory.getLogger(PagePathParameter.class).warn("Unknown valueType '{}', NOT converting input for PagePathParameter with path {}.", valueType, getName());
						return src;
				}
			}

		} catch (Throwable t) {

			// log error (or report it to somewhere), but don't fail here  because we are resolving a URL in the frontend and we don't want to send a 422 to the client..
			LoggerFactory.getLogger(PagePathParameter.class).warn("Exception while converting input for PagePathParameter with path {}: {}", getName(), t.toString());

			if (Boolean.TRUE.equals(Settings.LogFunctionsStackTrace.getValue())) {
				t.printStackTrace();
			}
		}

		return null;
	}
}
