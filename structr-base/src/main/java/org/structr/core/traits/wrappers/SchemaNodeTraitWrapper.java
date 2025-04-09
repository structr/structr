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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Relationship;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.MigrationService;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Trait;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaNodeTraitDefinition;
import org.structr.schema.DynamicNodeTraitDefinition;

import java.util.*;

public class SchemaNodeTraitWrapper extends AbstractSchemaNodeTraitWrapper implements SchemaNode {

	public SchemaNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public boolean defaultVisibleToPublic() {
		return wrappedObject.getProperty(traits.key(SchemaNodeTraitDefinition.DEFAULT_VISIBLE_TO_PUBLIC_PROPERTY));
	}

	@Override
	public boolean defaultVisibleToAuth() {
		return wrappedObject.getProperty(traits.key(SchemaNodeTraitDefinition.DEFAULT_VISIBLE_TO_AUTH_PROPERTY));
	}

	@Override
	public Iterable<SchemaRelationshipNode> getRelatedTo() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(SchemaNodeTraitDefinition.RELATED_TO_PROPERTY);

		return Iterables.map(n -> n.as(SchemaRelationshipNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<SchemaRelationshipNode> getRelatedFrom() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(SchemaNodeTraitDefinition.RELATED_FROM_PROPERTY);

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

		final Set<String> allTraits = new LinkedHashSet<>();

		allTraits.add(StructrTraits.PROPERTY_CONTAINER);
		allTraits.add(StructrTraits.GRAPH_OBJECT);
		allTraits.add(StructrTraits.NODE_INTERFACE);
		allTraits.add(StructrTraits.ACCESS_CONTROLLABLE);
		allTraits.addAll(getInheritedTraits());

		// check inheritance
		for (final String name : allTraits) {

			final Trait trait = Traits.getTrait(name);
			if (trait != null) {

				final PropertyKey key = trait.getPropertyKeys().get(propertyNameToCheck);
				if (key != null && key instanceof RelationProperty rel) {

					final Relation relation = rel.getRelation();
					if (relation != null) {

						final PropertyKey sourceProperty = relation.getSourceProperty();
						if (sourceProperty.jsonName().equals(propertyNameToCheck)) {

							return Relation.Multiplicity.One.equals(relation.getSourceMultiplicity()) ? "1" : "*";
						}

						final PropertyKey targetProperty = relation.getTargetProperty();
						if (targetProperty.jsonName().equals(propertyNameToCheck)) {

							return Relation.Multiplicity.One.equals(relation.getTargetMultiplicity()) ? "1" : "*";
						}
					}
				}
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
		final PropertyKey<String[]> key   = traits.key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY);
		final String[] value              = wrappedObject.getProperty(key);

		if (value != null) {

			for (final String trait : value) {

				inheritedTraits.add(trait);
			}
		}

		return inheritedTraits;
	}

	@Override
	public void setInheritedTraits(final Set<String> setOfTraits) throws FrameworkException {
		wrappedObject.setProperty(traits.key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), setOfTraits.toArray(new String[0]));
	}

	@Override
	public TraitDefinition[] getTraitDefinitions() {

		final ArrayList<TraitDefinition> definitions = new ArrayList<>(recursivelyResolveTraitInheritance(this));

		return definitions.toArray(new TraitDefinition[0]);
	}

	@Override
	public void handleMigration() throws FrameworkException {

		final List<SchemaProperty> properties = Iterables.toList(getSchemaProperties());
		final List<SchemaMethod> methods      = Iterables.toList(getSchemaMethods());
		final App app                         = StructrApp.getInstance();

		// remove properties from static schema
		for (final SchemaProperty property : properties) {

			if (MigrationService.propertyShouldBeRemoved(property)) {

				app.delete(property);
			}
		}

		// remove methods from static schema
		for (final SchemaMethod method : methods) {

			if (MigrationService.methodShouldBeRemoved(method)) {

				app.delete(method);
			}
		}

		// move extendsClass values to inheriting traits
		final String extendsClassInternal = (String) getNode().getProperty("extendsClassInternal");
		final List<Relationship> rels     = Iterables.toList(getNode().getRelationships());
		final Set<String> traits          = new LinkedHashSet<>(getInheritedTraits());
		final String name                 = getName();

		if (extendsClassInternal != null) {

			traits.add(StringUtils.substringAfterLast(extendsClassInternal, "."));

			getNode().removeProperty("extendsClassInternal");
		}

		if (!rels.isEmpty()) {

			for (final Relationship rel : rels) {

				final String relType = rel.getType().name();

				if ("EXTENDS".equals(relType)) {

					final String startType = (String) rel.getStartNode().getProperty("name");
					final String endType   = (String) rel.getEndNode().getProperty("name");

					// only process (and delete) relationship if we're actually looking at the start node of the relationship
					if (startType.equals(name)) {

						traits.add(endType);

						// delete
						rel.delete(false);
					}
				}
			}
		}

		setInheritedTraits(traits);
	}

	// ----- private methods -----
	private Set<TraitDefinition> recursivelyResolveTraitInheritance(final SchemaNode schemaNode) {

		final Set<TraitDefinition> definitions = new LinkedHashSet<>();

		for (final String inheritedTrait : schemaNode.getInheritedTraits()) {

			try {

				final NodeInterface inheritedSchemaNode = StructrApp.getInstance().nodeQuery(StructrTraits.SCHEMA_NODE).name(inheritedTrait).getFirst();
				if (inheritedSchemaNode != null && !inheritedTrait.equals(schemaNode.getName())) {

					// recurse
					definitions.addAll(recursivelyResolveTraitInheritance(inheritedSchemaNode.as(SchemaNode.class)));
				}

				if (Traits.exists(inheritedTrait)) {

					final Traits traits = Traits.of(inheritedTrait);

					definitions.addAll(traits.getTraitDefinitions());
				}

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		definitions.add(new DynamicNodeTraitDefinition(schemaNode));

		return definitions;
	}
}
