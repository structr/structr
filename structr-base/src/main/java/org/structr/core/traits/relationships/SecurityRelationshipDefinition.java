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
package org.structr.core.traits.relationships;

import org.structr.api.graph.Relationship;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Security;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.*;
import org.structr.core.traits.RelationshipTraitDefinition;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.propertycontainer.GetPropertyKeys;
import org.structr.core.traits.wrappers.SecurityTraitWrapper;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.structr.core.entity.Relation.Multiplicity.Many;

public class SecurityRelationshipDefinition extends RelationshipTraitDefinition {

	private static final SourceId principalId          = new SourceId("principalId");
	private static final TargetId accessControllableId = new TargetId("accessControllableId");
	private static final Property<String[]> allowed    = new ArrayProperty("allowed", String.class);

	public SecurityRelationshipDefinition() {
		super("SecurityRelationship");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
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

					keys.add(principalId);
					keys.add(accessControllableId);

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
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of(
			principalId,
			accessControllableId,
			allowed
		);
	}

	@Override
	protected String getSourceType() {
		return "Principal";
	}

	@Override
	protected String getTargetType() {
		return "NodeInterface";
	}

	@Override
	protected String getRelationshipType() {
		return "SECURITY";
	}

	@Override
	protected Relation.Multiplicity getSourceMultiplicity() {
		return Many;
	}

	@Override
	protected Relation.Multiplicity getTargetMultiplicity() {
		return Many;
	}
}
