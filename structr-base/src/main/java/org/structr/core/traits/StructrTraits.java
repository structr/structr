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

	/*
	private static final PropertyContainerTraitDefinition propertyContainerTraitImplementation;
	private static final GraphObjectTraitDefinition graphObjectTraitImplementation;
	private static final NodeInterfaceTraitDefinition nodeInterfaceTraitImplementation;
	private static final AccessControllableTraitDefinition accessControllableTraitImplementation;

	static {

		propertyContainerTraitImplementation  = new PropertyContainerTraitDefinition();
		graphObjectTraitImplementation        = new GraphObjectTraitDefinition();
		nodeInterfaceTraitImplementation      = new NodeInterfaceTraitDefinition();
		accessControllableTraitImplementation = new AccessControllableTraitDefinition();


		final Traits graphObjectTraits   = new Traits("GraphObject",   false, false);
		final Traits nodeInterfaceTraits = new Traits("NodeInterface", true, false);

		graphObjectTraits.registerImplementation(graphObjectTraitImplementation);

		// NodeInterface extends GraphObject
		nodeInterfaceTraits.registerImplementation(graphObjectTraitImplementation);
		nodeInterfaceTraits.registerImplementation(nodeInterfaceTraitImplementation);
	}
	*/

	public static void registerNodeType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new Traits(typeName, true, false);

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

		final Traits traits = new Traits(typeName, false, true);

		// Relationship types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition());
		traits.registerImplementation(new GraphObjectTraitDefinition());

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition);
		}
	}
}
