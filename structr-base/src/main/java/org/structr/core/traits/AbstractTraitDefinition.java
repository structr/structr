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
package org.structr.core.traits;

import org.structr.api.util.FixedSizeCache;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyKey;

import java.util.*;

public abstract class AbstractTraitDefinition implements TraitDefinition {

	private static final FixedSizeCache<String, Object> relationshipTemplateInstanceCache = new FixedSizeCache<>("Relationship template cache", 1000);

	protected final Map<String, PropertyKey> properties = new HashMap<>();

	@Override
	public Set<PropertyKey> getPropertyKeys(final String view) {
		return new LinkedHashSet<>(properties.values());
	}

	@Override
	public boolean hasKey(final String name) {
		return properties.containsKey(name);
	}

	@Override
	public PropertyKey key(final String name) {
		return properties.get(name);
	}

	protected Relation getRelationForType(final String type) {

		Relation instance = (Relation)relationshipTemplateInstanceCache.get(type);
		if (instance == null) {

			try {

				instance = type.getDeclaredConstructor().newInstance();
				relationshipTemplateInstanceCache.put(type, instance);

			} catch (Throwable t) {

				// TODO: throw meaningful exception here,
				// should be a RuntimeException that indicates
				// wrong use of Relationships etc.
				logger.warn("", t);
			}
		}

		return instance;
	}

	public static void clearRelationshipTemplateInstanceCache() {
		relationshipTemplateInstanceCache.clear();
	}
}
