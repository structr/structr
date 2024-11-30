package org.structr.core.traits;

public class StructrBaseTraits {

	private static final PropertyContainerTraitImplementation propertyContainerTraitImplementation   = new PropertyContainerTraitImplementation();
	private static final GraphObjectTraitImplementation graphObjectTraitImplementation               = new GraphObjectTraitImplementation();
	private static final NodeInterfaceTraitImplementation nodeInterfaceTraitImplementation           = new NodeInterfaceTraitImplementation();
	private static final AccessControllableTraitImplementation accessControllableTraitImplementation = new AccessControllableTraitImplementation();
	//private static final RelationshipInterfaceTraitImplementation nodeInterfaceTraitImplementation = new NodeInterfaceTraitImplementation();

	static {

		StructrBaseTraits.registerPrincipal();
	}

	private static void registerPrincipal() {

		final Traits traits = new Traits("Principal", true);

		// Principal consists of the following traits
		traits.registerImplementation(propertyContainerTraitImplementation);
		traits.registerImplementation(graphObjectTraitImplementation);
		traits.registerImplementation(nodeInterfaceTraitImplementation);
		traits.registerImplementation(accessControllableTraitImplementation);
	}

	private static void registerDynamicNodeType(final String typeName) {

		final Traits traits = new Traits(typeName, true);

		// Node types consist of at least the following traits
		traits.registerImplementation(propertyContainerTraitImplementation);
		traits.registerImplementation(graphObjectTraitImplementation);
		traits.registerImplementation(nodeInterfaceTraitImplementation);
		traits.registerImplementation(accessControllableTraitImplementation);
	}

	private static void registerDynamicRelationshipType(final String typeName) {

		final Traits traits = new Traits(typeName, false);

		// Relationship types consist of at least the following traits
		traits.registerImplementation(propertyContainerTraitImplementation);
		traits.registerImplementation(graphObjectTraitImplementation);
		//traits.registerImplementation(relationshipInterfaceTraitImplementation);
	}
}
