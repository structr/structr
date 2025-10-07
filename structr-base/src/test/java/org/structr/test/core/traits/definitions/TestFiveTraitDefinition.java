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
package org.structr.test.core.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AfterCreation;
import org.structr.core.traits.operations.graphobject.AfterModification;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;

import java.util.Map;
import java.util.Set;

public class TestFiveTraitDefinition extends AbstractNodeTraitDefinition {

	public TestFiveTraitDefinition() {
		super("TestFive");
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final PropertyKey<Integer> modifiedInBeforeCreation = graphObject.getTraits().key("modifiedInBeforeCreation");

					int value = getIncreasedValue(graphObject, modifiedInBeforeCreation);
					graphObject.setProperty(modifiedInBeforeCreation, value);
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final PropertyKey<Integer> modifiedInBeforeModification = graphObject.getTraits().key("modifiedInBeforeModification");

					int value = getIncreasedValue(graphObject, modifiedInBeforeModification);
					graphObject.setProperty(modifiedInBeforeModification, value);
				}
			},

			AfterCreation.class,
			new AfterCreation() {

				@Override
				public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

					final PropertyKey<Integer> modifiedInAfterCreation = graphObject.getTraits().key("modifiedInAfterCreation");
					final App app                                      = StructrApp.getInstance(securityContext);

					try (final Tx tx = app.tx()) {

						final int value = getIncreasedValue(graphObject, modifiedInAfterCreation);
						graphObject.setProperty(modifiedInAfterCreation, value);

						tx.success();

					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			},

			AfterModification.class,
			new AfterModification() {

				@Override
				public void afterModification(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

					final PropertyKey<Integer> modifiedInAfterModification = graphObject.getTraits().key("modifiedInAfterModification");
					final App app                                      = StructrApp.getInstance(securityContext);

					try (final Tx tx = app.tx()) {

						final int value = getIncreasedValue(graphObject, modifiedInAfterModification);

						graphObject.setProperty(modifiedInAfterModification, value);
						tx.success();

					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Integer> intProperty                  = new IntProperty("integerProperty").indexed();
		final Property<Integer> modifiedInBeforeCreation     = new IntProperty("modifiedInBeforeCreation").defaultValue(0).indexed().unvalidated();
		final Property<Integer> modifiedInBeforeModification = new IntProperty("modifiedInBeforeModification").defaultValue(0).indexed().unvalidated();
		final Property<Integer> modifiedInAfterCreation      = new IntProperty("modifiedInAfterCreation").defaultValue(0).indexed().unvalidated();
		final Property<Integer> modifiedInAfterModification  = new IntProperty("modifiedInAfterModification").defaultValue(0).indexed().unvalidated();

		return newSet(
			intProperty,
			modifiedInBeforeCreation,
			modifiedInBeforeModification,
			modifiedInAfterCreation,
			modifiedInAfterModification
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
				"integerProperty", "modifiedInBeforeCreation", "modifiedInBeforeModification", "modifiedInAfterCreation", "modifiedInAfterModification"
			)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- private methods -----
	private int getIncreasedValue(final GraphObject graphObject, final PropertyKey<Integer> key) {

		Integer value = graphObject.getProperty(key);

		if (value != null) {

			return value.intValue() + 1;
		}

		return 1;
	}
}
