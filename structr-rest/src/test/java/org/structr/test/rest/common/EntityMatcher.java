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
package org.structr.test.rest.common;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.structr.common.PropertyView;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 *
 */
public class EntityMatcher extends BaseMatcher {

	private Map<String, Object> entityValues = new LinkedHashMap<>();

	public EntityMatcher(final String type) {
		this(type, PropertyView.Public);
	}
	
	public EntityMatcher(final String type, final String view) {
		this(type, view, Collections.EMPTY_MAP);
	}
	
	public EntityMatcher(String type, String view, Map<String, Object> values) {

		final Set<PropertyKey> propertyView = new LinkedHashSet<>(Traits.of(type).getPropertyKeysForView(view));
		for (PropertyKey key : propertyView) {
			
			entityValues.put(key.jsonName(), values.get(key.jsonName()));
		}
	}

	@Override
	public boolean matches(Object item) {

		if (item instanceof Map) {

			Map map = (Map) item;
			
			// check if key sets match
			if (map.keySet().containsAll(entityValues.keySet())) {

				// check for values (if present)
				for (Entry<String, Object> entry : entityValues.entrySet()) {

					String key   = entry.getKey();
					Object value = entry.getValue();

					Object entityValue = map.get(key);

					// mismatch
					if (value != null && !value.equals(entityValue)) {
						return false;
					}
				}
				
				return true;
			}
		}

		return false;
	}

	@Override
	public void describeTo(Description description) {
		description.appendText(entityValues.toString());
	}
}
