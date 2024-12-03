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

public class StructrBaseTraits {

	private static final PropertyContainerTraitDefinition propertyContainerTraitImplementation   = new PropertyContainerTraitDefinition();
	private static final GraphObjectTraitDefinition graphObjectTraitImplementation               = new GraphObjectTraitDefinition();
	private static final NodeInterfaceTraitDefinition nodeInterfaceTraitImplementation           = new NodeInterfaceTraitDefinition();
	private static final AccessControllableTraitDefinition accessControllableTraitImplementation = new AccessControllableTraitDefinition();
	//private static final RelationshipInterfaceTraitImplementation nodeInterfaceTraitImplementation = new NodeInterfaceTraitImplementation();

	static {

		StructrBaseTraits.registerPrincipal();
	}

	private static void registerGraphObject() {

		final Traits traits = new Traits("GraphObject", false, false);

		traits.registerImplementation(graphObjectTraitImplementation);
	}

	private static void registerPrincipal() {

		final Traits traits = new Traits("Principal", true, false);

		// Principal consists of the following traits
		traits.registerImplementation(propertyContainerTraitImplementation);
		traits.registerImplementation(graphObjectTraitImplementation);
		traits.registerImplementation(nodeInterfaceTraitImplementation);
		traits.registerImplementation(accessControllableTraitImplementation);
	}

	private static void registerDynamicNodeType(final String typeName) {

		final Traits traits = new Traits(typeName, true, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(propertyContainerTraitImplementation);
		traits.registerImplementation(graphObjectTraitImplementation);
		traits.registerImplementation(nodeInterfaceTraitImplementation);
		traits.registerImplementation(accessControllableTraitImplementation);
	}

	private static void registerDynamicRelationshipType(final String typeName) {

		final Traits traits = new Traits(typeName, false, true);

		// Relationship types consist of at least the following traits
		traits.registerImplementation(propertyContainerTraitImplementation);
		traits.registerImplementation(graphObjectTraitImplementation);
		//traits.registerImplementation(relationshipInterfaceTraitImplementation);
	}
}
