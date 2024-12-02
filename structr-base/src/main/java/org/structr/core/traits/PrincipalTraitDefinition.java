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
package org.structr.core.traits;

import org.structr.api.Predicate;
import org.structr.common.EMailValidator;
import org.structr.common.LowercaseTransformator;
import org.structr.common.TrimTransformator;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SecurityRelationship;
import org.structr.core.entity.relationship.GroupCONTAINSPrincipal;
import org.structr.core.entity.relationship.PrincipalFAVORITEFavoritable;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.propertycontainer.GetProperty;
import org.structr.core.traits.operations.propertycontainer.SetProperty;

import java.util.Date;
import java.util.Map;
import java.util.Set;

public class PrincipalTraitDefinition extends AbstractTraitDefinition {

	Property<Iterable<Favoritable>> favoritesProperty = new EndNodes<>("favorites", PrincipalFAVORITEFavoritable.class);
	Property<Iterable<Group>> groupsProperty          = new StartNodes<>("groups", GroupCONTAINSPrincipal.class);

	Property<Iterable<NodeInterface>> ownedNodes   = new EndNodes<>("ownedNodes", PrincipalOwnsNode.class).partOfBuiltInSchema();
	Property<Iterable<NodeInterface>> grantedNodes = new EndNodes<>("grantedNodes", SecurityRelationship.class).partOfBuiltInSchema();

	Property<Boolean> isAdminProperty                           = new BooleanProperty("isAdmin").indexed().readOnly();
	Property<Boolean> blockedProperty                           = new BooleanProperty("blocked");
	Property<String> sessionIdsProperty                         = new ArrayProperty("sessionIds", String.class).indexed();
	Property<String> refreshTokensProperty                      = new ArrayProperty("refreshTokens", String.class).indexed();
	Property<String> sessionDataProperty                        = new StringProperty("sessionData");
	Property<String> eMailProperty                              = new StringProperty("eMail").indexed().unique().transformators(LowercaseTransformator.class.getName(), TrimTransformator.class.getName());
	Property<String> passwordProperty                           = new PasswordProperty("password");
	Property<Date> passwordChangeDateProperty                   = new DateProperty("passwordChangeDate");
	Property<Integer> passwordAttemptsProperty                  = new IntProperty("passwordAttempts");
	Property<Date> lastLoginDateProperty                        = new DateProperty("lastLoginDate");
	Property<String> twoFactorSecretProperty                    = new StringProperty("twoFactorSecret");
	Property<String> twoFactorTokenProperty                     = new StringProperty("twoFactorToken").indexed();
	Property<Boolean> isTwoFactorUserProperty                   = new BooleanProperty("isTwoFactorUser");
	Property<Boolean> twoFactorConfirmedProperty                = new BooleanProperty("twoFactorConfirmed");
	Property<String> saltProperty                               = new StringProperty("salt");
	Property<String> localeProperty                             = new StringProperty("locale");
	Property<String> publicKeyProperty                          = new StringProperty("publicKey");
	Property<String> proxyUrlProperty                           = new StringProperty("proxyUrl");
	Property<String> proxyUsernameProperty                      = new StringProperty("proxyUsername");
	Property<String> proxyPasswordProperty                      = new StringProperty("proxyPassword");
	Property<String> publicKeysProperty                         = new ArrayProperty("publicKeys", String.class);

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

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

				@Override
				public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> key, final Predicate<GraphObject> predicate) {

					if (key.equals(passwordProperty) || key.equals(saltProperty) || key.equals(twoFactorSecretProperty)) {

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
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(

			isAdminProperty,
			blockedProperty,
			sessionIdsProperty,
			refreshTokensProperty,
			sessionDataProperty,
			eMailProperty,
			passwordProperty,
			passwordChangeDateProperty,
			passwordAttemptsProperty,
			lastLoginDateProperty,
			twoFactorSecretProperty,
			twoFactorTokenProperty,
			isTwoFactorUserProperty,
			twoFactorConfirmedProperty,
			saltProperty,
			localeProperty,
			publicKeyProperty,
			proxyUrlProperty,
			proxyUsernameProperty,
			proxyPasswordProperty,
			publicKeysProperty
		);
	}
}
