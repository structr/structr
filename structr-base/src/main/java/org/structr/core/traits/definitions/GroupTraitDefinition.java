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
package org.structr.core.traits.definitions;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Group;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
import org.structr.core.traits.wrappers.GroupTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 */
public final class GroupTraitDefinition extends AbstractNodeTraitDefinition {

	/*
	public static final View defaultView = new View(GroupTraitDefinition.class, PropertyView.Public,
		nameProperty, isGroupProperty, membersProperty, blockedProperty
	);

	public static final View uiView = new View(GroupTraitDefinition.class, PropertyView.Ui,
		isGroupProperty, jwksReferenceIdProperty, membersProperty
	);
	*/

	public GroupTraitDefinition() {
		super("Group");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					boolean valid = true;

					final Traits traits                       = obj.getTraits();
					final PropertyKey nameProperty            = traits.key("name");
					final PropertyKey jwksReferenceIdProperty = traits.key("jwksReferenceId");

					valid &= ValidationHelper.isValidPropertyNotNull(obj, nameProperty, errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj,  nameProperty, errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj,  jwksReferenceIdProperty, errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			SetProperty.class,
			new SetProperty() {

				@Override
				public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {

					graphObject.clearCaches();

					return getSuper().setProperty(graphObject, key, value, isCreation);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Group.class, (traits, node) -> new GroupTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> membersProperty = new EndNodes("members", "GroupCONTAINSPrincipal");
		final Property<String> jwksReferenceIdProperty          = new StringProperty("jwksReferenceId").indexed().unique();
		final Property<String> nameProperty                     = new StringProperty("name").indexed().notNull().unique();
		final Property<Boolean> isGroupProperty                 = new ConstantBooleanProperty("isGroup", true);

		return newSet(
			membersProperty,
			jwksReferenceIdProperty,
			nameProperty,
			isGroupProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	@Override
	public boolean shouldSkipSecurityRelationships() {
		return isAdmin();
	}
	*/
}
