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
package org.structr.odf.traits.wrappers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyMapping;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.odf.entity.ODFExporter;
import org.structr.odf.traits.definitions.ODFExporterTraitDefinition;
import org.structr.web.entity.File;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base class for ODF exporter
 */
public class ODFExporterTraitWrapper extends AbstractNodeTraitWrapper implements ODFExporter {

	public ODFExporterTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	public File getDocumentTemplate() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ODFExporterTraitDefinition.DOCUMENT_TEMPLATE_PROPERTY));
		if (node != null) {

			return node.as(File.class);
		}

		return null;
	}

	public File getResultDocument() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ODFExporterTraitDefinition.RESULT_DOCUMENT_PROPERTY));
		if (node != null) {

			return node.as(File.class);
		}

		return null;
	}

	public void setResultDocument(final File resultDocument) throws FrameworkException {

		wrappedObject.setProperty(traits.key(ODFExporterTraitDefinition.RESULT_DOCUMENT_PROPERTY), resultDocument);
	}

	/**
	 * Parses the JSON mapping of this exporter. Its keys are the targets (cell addresses or
	 * field names), each value is either the name of the source property or an object with
	 * the keys "source" and/or "output", the latter being a script that receives the
	 * exported object as "input".
	 */
	public PropertyMapping getPropertyMapping() throws FrameworkException {

		final String json = wrappedObject.getProperty(traits.key(ODFExporterTraitDefinition.PROPERTY_MAPPING_PROPERTY));
		if (StringUtils.isBlank(json)) {

			return null;
		}

		final List<PropertyMapping.Entry> entries = new LinkedList<>();

		try {

			for (final Map.Entry<String, JsonElement> entry : JsonParser.parseString(json).getAsJsonObject().entrySet()) {

				final String target       = entry.getKey();
				final JsonElement element = entry.getValue();

				if (element.isJsonObject()) {

					final JsonObject object = element.getAsJsonObject();

					entries.add(new PropertyMapping.Entry(getString(object, "source", target), target, null, getString(object, "output", target)));

				} else {

					entries.add(new PropertyMapping.Entry(asString(element, target), target, null, null));
				}
			}

		} catch (JsonParseException | IllegalStateException | UnsupportedOperationException ex) {

			throw new FrameworkException(422, "Invalid property mapping for " + getName() + ": " + ex.getMessage());
		}

		return new PropertyMapping(entries);
	}

	// ----- private methods -----
	private String getString(final JsonObject object, final String key, final String target) {

		final JsonElement element = object.get(key);
		if (element == null || element.isJsonNull()) {

			return null;
		}

		return asString(element, target);
	}

	// only accepts strings, Gson would silently convert numbers and single-element arrays
	private String asString(final JsonElement element, final String target) {

		if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {

			return element.getAsString();
		}

		throw new IllegalStateException("Expected a string for target '" + target + "', got " + element);
	}
}
