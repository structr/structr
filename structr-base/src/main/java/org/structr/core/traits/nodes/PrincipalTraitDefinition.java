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
package org.structr.core.traits.nodes;

import org.structr.api.Predicate;
import org.structr.common.EMailValidator;
import org.structr.common.LowercaseTransformator;
import org.structr.common.TrimTransformator;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.propertycontainer.GetProperty;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
import org.structr.core.traits.wrappers.PrincipalTraitWrapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class PrincipalTraitDefinition extends AbstractTraitDefinition {

	public PrincipalTraitDefinition() {
		super("Principal");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final PropertyKey<String> eMailProperty = obj.getTraits().key("eMail");
					boolean valid = true;

					valid &= ValidationHelper.isValidUniqueProperty(obj, eMailProperty, errorBuffer);
					valid &= new EMailValidator().isValid(obj, errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetProperty.class,
			new GetProperty() {

				final Set<String> hiddenProperties = Set.of("password", "salt", "twoFactorSecret");

				@Override
				public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> key, final Predicate<GraphObject> predicate) {

					if (hiddenProperties.contains(key.jsonName())) {

						return (V) Principal.HIDDEN;

					} else {

						return this.getSuper().getProperty(graphObject, key, predicate);
					}
				}
			},

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
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Principal.class, (traits, node) -> new PrincipalTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			new StartNodes("groups", "GroupCONTAINSPrincipal"),
			new EndNodes("ownedNodes", "PrincipalOwnsNode").partOfBuiltInSchema(),
			new EndNodes("grantedNodes", "SecurityRelationship").partOfBuiltInSchema(),
			new BooleanProperty("isAdmin").indexed().readOnly(),
			new BooleanProperty("blocked"),
			new ArrayProperty("sessionIds", String.class).indexed(),
			new ArrayProperty("refreshTokens", String.class).indexed(),
			new StringProperty("sessionData"),
			new StringProperty("eMail").indexed().unique().transformators(LowercaseTransformator.class.getName(), TrimTransformator.class.getName()),
			new PasswordProperty("password"),
			new DateProperty("passwordChangeDate"),
			new IntProperty("passwordAttempts"),
			new DateProperty("lastLoginDate"),
			new StringProperty("twoFactorSecret"),
			new StringProperty("twoFactorToken").indexed(),
			new BooleanProperty("isTwoFactorUser"),
			new BooleanProperty("twoFactorConfirmed"),
			new StringProperty("salt"),
			new StringProperty("locale"),
			new StringProperty("publicKey"),
			new StringProperty("proxyUrl"),
			new StringProperty("proxyUsername"),
			new StringProperty("proxyPassword"),
			new ArrayProperty("publicKeys", String.class)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
