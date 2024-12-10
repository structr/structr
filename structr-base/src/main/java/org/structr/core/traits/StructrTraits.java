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
import org.structr.core.traits.nodes.PrincipalTraitDefinition;
import org.structr.core.traits.relationships.*;
import org.structr.rest.traits.relationships.ObjectEventRelationship;
import org.structr.rest.traits.definitions.LogEventTraitDefinition;
import org.structr.rest.traits.definitions.LogObjectTraitDefinition;
import org.structr.rest.traits.definitions.LogSubjectTraitDefinition;
import org.structr.rest.traits.relationships.SubjectEventRelationship;

public class StructrTraits {

	private static final PropertyContainerTraitDefinition propertyContainerTraitImplementation   = new PropertyContainerTraitDefinition();
	private static final GraphObjectTraitDefinition graphObjectTraitImplementation               = new GraphObjectTraitDefinition();
	private static final NodeInterfaceTraitDefinition nodeInterfaceTraitImplementation           = new NodeInterfaceTraitDefinition();
	private static final AccessControllableTraitDefinition accessControllableTraitImplementation = new AccessControllableTraitDefinition();
	//private static final RelationshipInterfaceTraitImplementation nodeInterfaceTraitImplementation = new NodeInterfaceTraitImplementation();

	static {

		StructrTraits.registerBaseTypes();
	}

	private static void registerBaseTypes() {

		final Traits graphObjectTraits   = new Traits("GraphObject",   false, false);
		final Traits nodeInterfaceTraits = new Traits("NodeInterface", true, false);

		graphObjectTraits.registerImplementation(graphObjectTraitImplementation);

		// NodeInterface extends GraphObject
		nodeInterfaceTraits.registerImplementation(graphObjectTraitImplementation);
		nodeInterfaceTraits.registerImplementation(nodeInterfaceTraitImplementation);
	}

	private static void registerPrincipal() {

		registerNodeType("Principal");
	}

	public static void registerNodeType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new Traits(typeName, true, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(propertyContainerTraitImplementation);
		traits.registerImplementation(graphObjectTraitImplementation);
		traits.registerImplementation(nodeInterfaceTraitImplementation);
		traits.registerImplementation(accessControllableTraitImplementation);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition);
		}
	}

	public static void registerRelationshipType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new Traits(typeName, false, true);

		// Relationship types consist of at least the following traits
		traits.registerImplementation(propertyContainerTraitImplementation);
		traits.registerImplementation(graphObjectTraitImplementation);
		//traits.registerImplementation(relationshipInterfaceTraitImplementation);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition);
		}
	}
}
