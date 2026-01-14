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
package org.structr.web.common;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.property.CustomHtmlAttributeProperty;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;

import java.util.*;

/**
 */
public class DOMNodeContent {

	private static final Set<String> whitelist   = new TreeSet<>(Set.of("structr", "root", "slot", "layout-container", "component-container", "list-container", "label", "leaf"));
	private static final Set<String> attributes  = new TreeSet<>(Set.of("functionQuery, dataKey"));

	private final Map<String, SlotData> slotData = new LinkedHashMap<>();

	public void loadFrom(final DOMNode source) throws FrameworkException {

		final String slotName = getSlotName(source.getCssClass());
		final String uuid     = source.getUuid();
		boolean recurse       = true;

		if (slotName != null) {

			final SlotData data              = getSlotData(uuid, slotName, true);
			final PropertyMap dataProperties = new PropertyMap();

			// data-widget-elements indicates that the child elements should be preserved
			data.putSlotElements(Iterables.toList(source.getChildren()));

			// store data properties for slots
			for (final PropertyKey key : source.getDataPropertyKeys()) {
				dataProperties.put(key, source.getProperty(key));
			}

			if (!dataProperties.isEmpty()) {
				data.putDataAttributes(dataProperties);
			}

			// don't recurse to children here
			recurse = false;
		}

		final SlotData data   = getAttributeSlotData(uuid, "attributes", true);
		final PropertyMap map = new PropertyMap();

		for (final String attribute : attributes) {

			final PropertyKey<String> key1 = new CustomHtmlAttributeProperty<>(CustomHtmlAttributeProperty.CUSTOM_HTML_ATTRIBUTE_PREFIX + attribute);
			final PropertyKey<String> key2 = new CustomHtmlAttributeProperty<>(PropertyView.Html + attribute);
			final PropertyKey<String> key3 = new CustomHtmlAttributeProperty<>(attribute);
			final String value1            = source.getProperty(key1);
			final String value2            = source.getProperty(key2);
			final String value3            = source.getProperty(key3);

			if (value1 != null) {
				map.put(key1, value1);
			}

			if (value2 != null) {
				map.put(key2, value2);
			}

			if (value3 != null) {
				map.put(key3, value3);
			}
		}

		if (!map.isEmpty()) {
			data.putSlotAttributes(map);
		}

		if (recurse) {

			// only recurse if it is not a slot
			for (final DOMNode child : source.getChildren()) {

				loadFrom(child);
			}
		}
	}

	public void moveTo(final DOMNode target) throws FrameworkException {

		// slot name must be present
		final String slotName = getSlotName(target.getCssClass());
		final String uuid     = target.getUuid();
		boolean recurse       = true;

		if (slotName != null) {

			final SlotData data = getSlotData(uuid, slotName, false);
			if (data != null) {

				// remove existing?
				target.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.CHILDREN_PROPERTY), Collections.EMPTY_LIST);

				// add
				final List<DOMNode> children = data.getNextSlotElements();
				if (children != null) {

					for (final DOMNode child : children) {

						target.appendChild(child);
					}
				}

				// data properties?
				final PropertyMap dataProperties = data.getNextDataAttributes();
				if (dataProperties != null) {

					target.setProperties(target.getSecurityContext(), dataProperties);
				}

				// don't recurse to children here
				recurse = false;
			}

		}


		final SlotData data   = getAttributeSlotData(uuid, "attributes", false);
		final PropertyMap map = data.getNextSlotAttributes();

		if (map != null) {

			for (final String attribute : attributes) {

				final PropertyKey<String> key1 = new CustomHtmlAttributeProperty<>(CustomHtmlAttributeProperty.CUSTOM_HTML_ATTRIBUTE_PREFIX + attribute);
				if (key1 != null) {

					final String value1 = map.get(key1);
					if (value1 != null) {

						target.setProperty(key1, value1);
					}
				}

				final PropertyKey<String> key2 = new CustomHtmlAttributeProperty<>(PropertyView.Html + attribute);
				if (key2 != null) {

					final String value2 = map.get(key2);
					if (value2 != null) {

						target.setProperty(key2, value2);
					}
				}

				final PropertyKey<String> key3 = new CustomHtmlAttributeProperty<>(attribute);
				if (key3 != null) {

					final String value3 = map.get(key3);
					if (value3 != null) {

						target.setProperty(key3, value3);
					}
				}
			}
		}

		if (recurse) {

			// only recurse if it is not a slot
			for (final DOMNode child : target.getChildren()) {

				moveTo(child);
			}
		}
	}

	// ----- private methods -----
	private SlotData getSlotData(final String id, final String slotName, final boolean create) {

		SlotData data = slotData.get(slotName);
		if (data != null) {

			return data;

		} else if (create) {

			data = new SlotData(id);
			slotData.put(slotName, data);

			return data;
		}

		return null;
	}

	private SlotData getAttributeSlotData(final String id, final String uniqueKey, final boolean create) {

		SlotData data = slotData.get(uniqueKey);
		if (data != null) {

			return data;

		} else if (create) {

			data = new SlotData(id);
			slotData.put(uniqueKey, data);

			return data;
		}

		return null;
	}

	private String getSlotName(final String source) {

		if (source != null) {

			final Set<String> names = new TreeSet<>();
			final String[] parts    = source.split(" ");
			boolean isSlot          = false;

			for (final String part : parts) {

				String trimmed = part.trim();

				if (StringUtils.isNotBlank(trimmed)) {

					if ("slot".equals(trimmed)) {
						isSlot = true;
					}

					if (whitelist.contains(trimmed)) {
						names.add(trimmed);
					}
				}
			}

			if (isSlot && !names.isEmpty()) {

				return StringUtils.join(names, " ");
			}

			// node is not a slot => no data to copy
			return null;
		}

		return null;
	}
}
