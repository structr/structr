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

import org.structr.core.traits.definitions.*;

public class StructrTraits {

	public static void registerBaseType(final TraitDefinition definition) {

		final Traits traits = new Traits(definition.getName(), true, false, false);

		traits.registerImplementation(definition);
	}

	public static void registerNodeInterface() {

		final Traits traits = new Traits("NodeInterface", true, true, false);

		traits.registerImplementation(new PropertyContainerTraitDefinition());
		traits.registerImplementation(new GraphObjectTraitDefinition());
		traits.registerImplementation(new NodeInterfaceTraitDefinition());
	}

	public static void registerRelationshipInterface() {

		final Traits traits = new Traits("RelationshipInterface", true, false, true);

		traits.registerImplementation(new PropertyContainerTraitDefinition());
		traits.registerImplementation(new GraphObjectTraitDefinition());
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition());
	}

	public static void registerDynamicNodeType(final String typeName, final TraitDefinition... definitions) {

		// do not overwrite types
		if (Traits.getAllTypes(null).contains(typeName)) {
			return;
		}

		final Traits traits = new Traits(typeName, false, true, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition());
		traits.registerImplementation(new GraphObjectTraitDefinition());
		traits.registerImplementation(new NodeInterfaceTraitDefinition());
		traits.registerImplementation(new AccessControllableTraitDefinition());

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition);
		}
	}

	public static void registerDynamicRelationshipType(final String typeName, final TraitDefinition... definitions) {

		// do not overwrite types
		if (Traits.getAllTypes(null).contains(typeName)) {
			return;
		}

		final Traits traits = new Traits(typeName, false, false, true);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition());
		traits.registerImplementation(new GraphObjectTraitDefinition());
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition());

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition);
		}
	}

	public static void registerNodeType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new Traits(typeName, true, true, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition());
		traits.registerImplementation(new GraphObjectTraitDefinition());
		traits.registerImplementation(new NodeInterfaceTraitDefinition());
		traits.registerImplementation(new AccessControllableTraitDefinition());

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition);
		}
	}

	public static void registerRelationshipType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new Traits(typeName, true, false, true);

		// Relationship types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition());
		traits.registerImplementation(new GraphObjectTraitDefinition());
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition());

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition);
		}
	}
}
