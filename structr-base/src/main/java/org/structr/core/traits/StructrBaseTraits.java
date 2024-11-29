package org.structr.core.traits;

public class StructrBaseTraits {

	static {

		StructrBaseTraits.registerGraphObjectTraits();
		StructrBaseTraits.registerPrincipalTraits();
	}

	private static void registerGraphObjectTraits() {

		Traits.registerImplementation(new GraphObjectTraitImplementation());

		final Traits traits = new Traits("GraphObject", false);

	}

	private static void registerPrincipalTraits() {

		final Traits traits = new Traits("Principal", true);

		traits.addTrait(new GraphObjectTraitImplementation(traits));


	}
}
