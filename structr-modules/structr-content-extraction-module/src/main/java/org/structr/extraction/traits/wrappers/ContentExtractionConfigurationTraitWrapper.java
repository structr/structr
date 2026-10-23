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
package org.structr.extraction.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.extraction.entity.ContentExtractionConfiguration;
import org.structr.extraction.traits.definitions.ContentExtractionConfigurationTraitDefinition;
import org.structr.web.entity.AbstractFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContentExtractionConfigurationTraitWrapper extends AbstractNodeTraitWrapper implements ContentExtractionConfiguration {

	public ContentExtractionConfigurationTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	@Override
	public AbstractFile getTarget() {

		final NodeInterface target = wrappedObject.getProperty(traits.key(ContentExtractionConfigurationTraitDefinition.TARGET_PROPERTY));

		return target != null ? target.as(AbstractFile.class) : null;
	}

	@Override
	public boolean isEnabled() {

		return !Boolean.FALSE.equals(wrappedObject.getProperty(traits.key(ContentExtractionConfigurationTraitDefinition.ENABLED_PROPERTY)));
	}

	@Override
	public Map<String, Object> getOptionValues() {

		final Map<String, Object> values = new LinkedHashMap<>();

		for (final String option : ContentExtractionConfigurationTraitDefinition.OPTION_PROPERTIES) {

			final Object value = wrappedObject.getProperty(traits.key(option));
			if (value != null) {

				// the resolver expects lists, like the values of script calls
				values.put(option, value instanceof String[] array ? List.of(array) : value);
			}
		}

		return values;
	}
}
