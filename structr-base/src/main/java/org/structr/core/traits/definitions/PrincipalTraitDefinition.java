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

import org.structr.api.Predicate;
import org.structr.common.EMailValidator;
import org.structr.common.LowercaseTransformator;
import org.structr.common.PropertyView;
import org.structr.common.TrimTransformator;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.auth.HashHelper;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.principal.IsValidPassword;
import org.structr.core.traits.operations.propertycontainer.GetProperty;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
import org.structr.core.traits.wrappers.PrincipalTraitWrapper;

import java.util.Map;
import java.util.Set;

public class PrincipalTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String GROUPS_PROPERTY      = "groups";
	public static final String OWNED_NODES_PROPERTY = "ownedNodes";
	public static final String GRANTED_NODES_PROPERTY = "grantedNodes";
	public static final String IS_ADMIN_PROPERTY = "isAdmin";


	public PrincipalTraitDefinition() {
		super(StructrTraits.PRINCIPAL);
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

				final Set<String> hiddenProperties = newSet("password", "salt", "twoFactorSecret");

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
			},

			IsValidPassword.class,
			new IsValidPassword() {

				@Override
				public boolean isValidPassword(Principal principal, String password) {

					final String encryptedPasswordFromDatabase = principal.getEncryptedPassword();
					if (encryptedPasswordFromDatabase != null) {

						final String encryptedPasswordToCheck = HashHelper.getHash(password, principal.getSalt());

						if (encryptedPasswordFromDatabase.equals(encryptedPasswordToCheck)) {
							return true;
						}
					}

					return false;
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

		return newSet(
			new StartNodes(GROUPS_PROPERTY, StructrTraits.GROUP_CONTAINS_PRINCIPAL),
			new EndNodes(OWNED_NODES_PROPERTY, StructrTraits.PRINCIPAL_OWNS_NODE),
			new EndNodes(GRANTED_NODES_PROPERTY, StructrTraits.SECURITY),
			new BooleanProperty(PrincipalTraitDefinition.IS_ADMIN_PROPERTY).indexed().readOnly(),
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
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet("blocked")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
