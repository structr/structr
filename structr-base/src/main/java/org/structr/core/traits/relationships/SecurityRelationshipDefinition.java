/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.traits.relationships;

import org.structr.api.graph.Relationship;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Security;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.SourceId;
import org.structr.core.property.TargetId;
import org.structr.core.traits.*;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.propertycontainer.GetPropertyKeys;
import org.structr.core.traits.wrappers.SecurityTraitWrapper;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.structr.core.entity.Relation.Multiplicity.Many;

public class SecurityRelationshipDefinition extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public static final String PRINCIPAL_ID_PROPERTY           = "principalId";
	public static final String ACCESS_CONTROLLABLE_ID_PROPERTY = "accessControllableId";
	public static final String ALLOWED_PROPERTY                = "allowed";

	public SecurityRelationshipDefinition() {
		super(StructrTraits.SECURITY);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetPropertyKeys.class,
			new GetPropertyKeys() {

				@Override
				public Set<PropertyKey> getPropertyKeys(final GraphObject graphObject, final String propertyView) {

					final Set<PropertyKey> keys = new LinkedHashSet<>();
					final Traits traits         = graphObject.getTraits();

					keys.addAll(getSuper().getPropertyKeys(graphObject, propertyView));

					keys.add(traits.key(PRINCIPAL_ID_PROPERTY));
					keys.add(traits.key(ACCESS_CONTROLLABLE_ID_PROPERTY));

					final Relationship dbRelationship = ((RelationshipInterface) graphObject).getRelationship();
					if (dbRelationship != null) {

						for (String key : dbRelationship.getPropertyKeys()) {

							final PropertyKey propertyKey = traits.key(key);
							if (propertyKey != null) {

								keys.add(propertyKey);
							}
						}
					}

					return keys;
				}
			}
		);
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of(
			Security.class, (traits, rel) -> new SecurityTraitWrapper(traits, rel)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		return newSet(
			new SourceId(PRINCIPAL_ID_PROPERTY),
			new TargetId(ACCESS_CONTROLLABLE_ID_PROPERTY),
			new ArrayProperty(ALLOWED_PROPERTY, String.class)
		);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.PRINCIPAL;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.NODE_INTERFACE;
	}

	@Override
	public String getRelationshipType() {
		return "SECURITY";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {
		return Many;
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.NONE;
	}

	@Override
	public boolean isInternal() {
		return false;
	}
}
