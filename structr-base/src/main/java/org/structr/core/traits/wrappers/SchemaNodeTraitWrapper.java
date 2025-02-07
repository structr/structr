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
package org.structr.core.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.schema.DynamicNodeTraitDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class SchemaNodeTraitWrapper extends AbstractSchemaNodeTraitWrapper implements SchemaNode {

	public SchemaNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public boolean defaultVisibleToPublic() {
		return wrappedObject.getProperty(traits.key("defaultVisibleToPublic"));
	}

	@Override
	public boolean defaultVisibleToAuth() {
		return wrappedObject.getProperty(traits.key("defaultVisibleToAuth"));
	}

	@Override
	public Iterable<SchemaRelationshipNode> getRelatedTo() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("relatedTo");

		return Iterables.map(n -> n.as(SchemaRelationshipNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<SchemaRelationshipNode> getRelatedFrom() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("relatedFrom");

		return Iterables.map(n -> n.as(SchemaRelationshipNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public String getMultiplicity(final String propertyNameToCheck) {

		final Set<String> existingPropertyNames = new LinkedHashSet<>();

		for (final SchemaRelationshipNode outRel : getRelatedTo()) {

			if (propertyNameToCheck.equals(SchemaRelationshipNode.getPropertyName(outRel, existingPropertyNames, true))) {
				return outRel.getTargetMultiplicity();
			}
		}

		// output related node definitions, collect property views
		for (final SchemaRelationshipNode inRel : getRelatedFrom()) {

			if (propertyNameToCheck.equals(SchemaRelationshipNode.getPropertyName(inRel, existingPropertyNames, false))) {
				return inRel.getSourceMultiplicity();
			}
		}

		return null;
	}

	@Override
	public String getRelatedType(final String propertyNameToCheck) {

		final Set<String> existingPropertyNames = new LinkedHashSet<>();

		for (final SchemaRelationshipNode outRel : getRelatedTo()) {

			if (propertyNameToCheck.equals(SchemaRelationshipNode.getPropertyName(outRel, existingPropertyNames, true))) {
				return outRel.getSchemaNodeTargetType();
			}
		}

		// output related node definitions, collect property views
		for (final SchemaRelationshipNode inRel : getRelatedFrom()) {

			if (propertyNameToCheck.equals(SchemaRelationshipNode.getPropertyName(inRel, existingPropertyNames, false))) {
				return inRel.getSchemaNodeSourceType();
			}
		}

		return null;
	}

	@Override
	public Set<String> getInheritedTraits() {

		final Set<String> inheritedTraits = new TreeSet<>();
		final PropertyKey<String[]> key   = traits.key("inheritedTraits");
		final String[] value              = wrappedObject.getProperty(key);

		if (value != null) {

			for (final String trait : value) {

				inheritedTraits.add(trait);
			}
		}

		return inheritedTraits;
	}

	@Override
	public TraitDefinition[] getTraitDefinitions() {

		final ArrayList<TraitDefinition> definitions = new ArrayList<>(recursivelyResolveTraitInheritance(this));

		return definitions.toArray(new TraitDefinition[0]);
	}

	// ----- private methods -----
	private Set<TraitDefinition> recursivelyResolveTraitInheritance(final SchemaNode schemaNode) {

		final Set<TraitDefinition> definitions = new LinkedHashSet<>();

		for (final String inheritedTrait : schemaNode.getInheritedTraits()) {

			try {

				final NodeInterface inheritedSchemaNode = StructrApp.getInstance().nodeQuery("SchemaNode").andName(inheritedTrait).getFirst();
				if (inheritedSchemaNode != null) {

					// recurse
					definitions.addAll(recursivelyResolveTraitInheritance(inheritedSchemaNode.as(SchemaNode.class)));

				} else {

					// try to find internal trait
					if (Traits.exists(inheritedTrait)) {

						final Traits traits = Traits.of(inheritedTrait);

						definitions.addAll(traits.getTraitDefinitions());

					}
				}

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		definitions.add(new DynamicNodeTraitDefinition(schemaNode));

		return definitions;
	}
}
