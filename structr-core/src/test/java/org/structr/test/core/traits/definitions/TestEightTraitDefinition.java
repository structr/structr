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
package org.structr.test.core.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.IntProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.*;
import org.structr.test.core.entity.TestEight;
import org.structr.test.core.traits.wrappers.TestEightTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class TestEightTraitDefinition extends AbstractNodeTraitDefinition {

	public TestEightTraitDefinition() {
		super("TestEight");
	}

	/*
	public static final View defaultView = new View(TestEightTraitDefinition.class, PropertyView.Public, testProperty);
	*/

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final NodeInterface node = (NodeInterface) graphObject;

					node.getTemporaryStorage().put("onCreationTimestamp", System.currentTimeMillis());
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final NodeInterface node = (NodeInterface) graphObject;

					node.getTemporaryStorage().put("onModificationTimestamp", System.currentTimeMillis());
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

					final NodeInterface node = (NodeInterface) graphObject;

					node.getTemporaryStorage().put("onDeletionTimestamp", System.currentTimeMillis());
				}
			},

			AfterCreation.class,
			new AfterCreation() {

				@Override
				public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

					final NodeInterface node = (NodeInterface) graphObject;

					node.getTemporaryStorage().put("afterCreationTimestamp", System.currentTimeMillis());
				}
			},

			AfterModification.class,
			new AfterModification() {

				@Override
				public void afterModification(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

					final NodeInterface node = (NodeInterface) graphObject;

					node.getTemporaryStorage().put("afterModificationTimestamp", System.currentTimeMillis());
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return newSet(
			new IntProperty("testProperty")
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet("testProperty")
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			TestEight.class, (traits, node) -> new TestEightTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
