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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.parser.DatePropertyGenerator;
import org.structr.web.entity.path.PagePath;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.error.ParseFailureException;
import org.structr.web.servlet.HtmlServlet;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition.PathParameterValueType;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class PagePathParameterTraitWrapper extends AbstractNodeTraitWrapper implements PagePathParameter {

	public PagePathParameterTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public PagePath getPagePath() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(PagePathParameterTraitDefinition.PATH_PROPERTY));
		if (node != null) {
			return node.as(PagePath.class);
		}

		return null;
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
	public boolean getIsRequired() {
		return wrappedObject.getProperty(traits.key(PagePathParameterTraitDefinition.IS_REQUIRED_PROPERTY));
	}

	@Override
	public boolean getUseDefaultIfInvalid() {
		return wrappedObject.getProperty(traits.key(PagePathParameterTraitDefinition.USE_DEFAULT_IF_INVALID_PROPERTY));
	}

	@Override
	public Object convert(final SecurityContext securityContext, final String src) throws ParseFailureException {

		try {

			if (src != null) {

				final String valueType = getValueType();
				final PathParameterValueType type = PathParameterValueType.fromString(valueType);

				if (type == null) {

					LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to use PagePathParameter '{}' with unknown type '{}'", getName(), valueType);
					return src;
				}

				switch (type) {

					case PathParameterValueType.String:
						return src;

					case PathParameterValueType.Base64UrlString: {

						final String configuredCharset = getFormat();

						try {

							final Base64.Decoder decoder = Base64.getUrlDecoder();
							final Charset charset = (configuredCharset != null) ? Charset.forName(configuredCharset) : Charset.defaultCharset();

							return new String(decoder.decode(src), charset);

						} catch (final UnsupportedCharsetException uce) {

							LoggerFactory.getLogger(PagePathParameter.class).warn("Unsupported charset '{}' for PagePathParameter '{}'", configuredCharset, getName());
							throw new ParseFailureException();

						} catch (final IllegalArgumentException iae) {

							LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to decode base64_url encoded string '{}' with charset '{}' for PagePathParameter '{}'", src, configuredCharset, getName());
							throw new ParseFailureException();
						}
					}

					case PathParameterValueType.Double: {

						try {

							return Double.valueOf(src);

						} catch (final NumberFormatException nfe) {

							LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to parse Double from '{}' for PagePathParameter '{}'", src, getName());
							throw new ParseFailureException();

						}
					}

					case PathParameterValueType.Float: {

						try {

							return Float.parseFloat(src);

						} catch (final NumberFormatException nfe) {

							LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to parse Float from '{}' for PagePathParameter '{}'", src, getName());
							throw new ParseFailureException();

						}
					}

					case PathParameterValueType.Integer: {

						try {

							return Integer.parseInt(src);

						} catch (final NumberFormatException nfe) {

							LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to parse Integer from '{}' for PagePathParameter '{}'", src, getName());
							throw new ParseFailureException();

						}
					}

					case PathParameterValueType.Long: {

						try {

							return Long.parseLong(src);

						} catch (final NumberFormatException nfe) {

							LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to parse Long from '{}' for PagePathParameter '{}'", src, getName());
							throw new ParseFailureException();

						}
					}

					case PathParameterValueType.Boolean: {

						if (src.equalsIgnoreCase("true")) {

							return true;

						} else if (src.equalsIgnoreCase("false")) {

							return false;
						}

						LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to parse boolean from string '{}' for PagePathParameter '{}'", src, getName());
						throw new ParseFailureException();
					}

					case PathParameterValueType.Date: {

						final String dateFormat = getFormat();

						if (dateFormat != null) {

							try {

								return new SimpleDateFormat(dateFormat).parse(src);

							} catch (ParseException ex) {

								LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to parse date from string '{}' using pattern '{}' for PagePathParameter '{}'", src, dateFormat, getName());
								throw new ParseFailureException();

							}

						} else {

							final Date parsedDate = DatePropertyGenerator.parseISO8601DateString(src);
							if (parsedDate == null) {

								LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to parse date from string '{}' for PagePathParameter '{}'", src, getName());
								throw new ParseFailureException();
							}

							return parsedDate;
						}
					}

					case PathParameterValueType.Node: {

						final String typeString = getFormat();
						Traits typeTraits = null;

						if (typeString != null && !typeString.isBlank()) {

							if (Traits.exists(typeString)) {

								typeTraits = Traits.of(typeString);

							} else {

								LoggerFactory.getLogger(PagePathParameter.class).warn("Unknown node type '{}', NOT applying hierarchy check for PagePathParameter '{}'", typeString, getName());
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
					}

					default:

						LoggerFactory.getLogger(PagePathParameter.class).warn("Unable to use PagePathParameter '{}'. Conversion for type '{}' is not implemented yet.", getName(), valueType);
						return src;
				}
			}

		} catch (ParseFailureException pfe) {

			// we catch this in the upper layer
			throw pfe;

		} catch (Throwable t) {

			// log error (or report it to somewhere), but don't fail here because we are resolving a URL in the frontend, and we don't want to send a 422 to the client...
			LoggerFactory.getLogger(PagePathParameter.class).warn("Exception while converting input '{}' for PagePathParameter '{}': {}", src, getName(), t.toString());

			if (Boolean.TRUE.equals(Settings.LogFunctionsStackTrace.getValue())) {
				LoggerFactory.getLogger(PagePathParameter.class).warn("", t);
			}
		}

		return null;
	}
}
