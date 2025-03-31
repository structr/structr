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
package org.structr.core.traits.definitions;

import org.structr.core.property.PropertyKey;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractNodeTraitDefinition extends AbstractTraitDefinition {

	public AbstractNodeTraitDefinition(final String name) {
		super(name);
	}

	public AbstractNodeTraitDefinition(final String name, final String label) {
		super(name, label);
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	// ----- protected methods -----
	protected <T> Set<T> newSet(final T... entries) {

		final Set<T> set = new LinkedHashSet<>();

		// keep order (Set.of(..) doesn't)
		for (final T entry : entries) {
			set.add(entry);
		}

		return set;
	}

	protected Set<String> newSetFromPropertyKeys(final PropertyKey... entries) {

		final Set<String> set = new LinkedHashSet<>();

		// keep order (Set.of(..) doesn't)
		for (final PropertyKey entry : entries) {
			set.add(entry.jsonName());
		}

		return set;
	}

	protected Set<String> newSetFromPropertyKeySet(final Set<PropertyKey> keySet) {

		final Set<String> set = new LinkedHashSet<>();

		for (final PropertyKey entry : keySet) {
			set.add(entry.jsonName());
		}

		return set;
	}
}
