/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.extraction.traits.properties;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.StringProperty;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.File;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Writes the content of a stored figure: the image as {@code { data: <base64>, mediaType }}, as
 * documents.extractFigures() returns it, or as a base64 string or data URI. So a figure from
 * extractFigures() can be stored with $.create('DocumentFigure', figure) as it is. Nothing is stored in
 * the property itself: reading it returns null, the image is the content of the file.
 */
public class FigureImageProperty extends StringProperty {

	public FigureImageProperty(final String name) {

		super(name);

		this.unvalidated = true;
	}

	@Override
	public String typeName() {

		return "Object";
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {

		return new PropertyConverter(securityContext, entity) {

			@Override
			public Object convert(final Object source) throws FrameworkException {

				if (source != null) {

					write(currentObject.as(File.class), source);
				}

				return null;
			}

			@Override
			public Object revert(final Object source) {

				return null;
			}
		};
	}

	@Override
	public PropertyConverter<?, String> inputConverter(final SecurityContext securityContext, final boolean fromString) {

		return null;
	}

	@Override
	public Object fixDatabaseProperty(final Object value) {

		return null;
	}

	// ----- private methods -----
	private void write(final File file, final Object value) throws FrameworkException {

		// the image of a figure map from documents.extractFigures() comes back as a GraphObjectMap
		final Object source = value instanceof GraphObjectMap map ? map.toMap() : value;

		try {

			if (source instanceof Map<?, ?> map && map.get("data") != null) {

				final Object mediaType = map.get("mediaType");

				FileHelper.setFileData(file, Base64.getDecoder().decode(map.get("data").toString()), mediaType != null ? mediaType.toString() : "image/png", true);

			} else if (source instanceof String text && !text.isBlank()) {

				FileHelper.decodeAndSetFileData(file, text);

			} else {

				throw new FrameworkException(422, "Property '" + jsonName() + "' expects an image as { data, mediaType } with base64 data, or a base64 string.");
			}

			ImageHelper.updateMetadata(file);

		} catch (final IllegalArgumentException ex) {

			throw new FrameworkException(422, "Property '" + jsonName() + "': the image data is not valid base64.");

		} catch (final IOException ex) {

			throw new FrameworkException(500, "Unable to write the image of " + file.getName() + ": " + ex.getMessage());
		}
	}
}
