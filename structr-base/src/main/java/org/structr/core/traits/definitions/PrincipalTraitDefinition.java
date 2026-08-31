/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.EMailValidator;
import org.structr.common.LowercaseTransformator;
import org.structr.common.PropertyView;
import org.structr.common.TrimTransformator;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.auth.HashHelper;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.*;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.principal.IsValidPassword;
import org.structr.core.traits.operations.propertycontainer.GetProperty;
import org.structr.core.traits.operations.propertycontainer.SetProperty;
import org.structr.core.traits.wrappers.PrincipalTraitWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Map;
import java.util.Set;

public class PrincipalTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String GROUPS_PROPERTY                = "groups";
	public static final String OWNED_NODES_PROPERTY           = "ownedNodes";
	public static final String GRANTED_NODES_PROPERTY         = "grantedNodes";
	public static final String IS_ADMIN_PROPERTY              = "isAdmin";
	public static final String BLOCKED_PROPERTY               = "blocked";
	public static final String SESSION_IDS_PROPERTY           = "sessionIds";
	public static final String REFRESH_TOKENS_PROPERTY        = "refreshTokens";
	public static final String SESSION_DATA_PROPERTY          = "sessionData";
	public static final String EMAIL_PROPERTY                 = "eMail";
	public static final String PASSWORD_PROPERTY              = "password";
	public static final String PASSWORD_CHANGE_DATE_PROPERTY  = "passwordChangeDate";
	public static final String PASSWORD_ATTEMPTS_PROPERTY     = "passwordAttempts";
	public static final String LAST_LOGIN_DATE_PROPERTY       = "lastLoginDate";
	public static final String TWO_FACTOR_SECRET_PROPERTY     = "twoFactorSecret";
	public static final String TWO_FACTOR_TOKEN_PROPERTY      = "twoFactorToken";
	public static final String IS_TWO_FACTOR_USER_PROPERTY    = "isTwoFactorUser";
	public static final String TWO_FACTOR_CONFIRMED_PROPERTY  = "twoFactorConfirmed";
	public static final String SALT_PROPERTY                  = "salt";
	public static final String LOCALE_PROPERTY                = "locale";
	public static final String PUBLIC_KEY_PROPERTY            = "publicKey";
	public static final String PUBLIC_KEYS_PROPERTY           = "publicKeys";
	public static final String PROXY_URL_PROPERTY             = "proxyUrl";
	public static final String PROXY_USERNAME_PROPERTY        = "proxyUsername";
	public static final String PROXY_PASSWORD_PROPERTY        = "proxyPassword";
	public static final String DEVICE_TRUST_SECRET_PROPERTY   = "deviceTrustSecret";
	public static final String DEVICE_TRUST_POSSIBLE_PROPERTY = "deviceTrustPossible";


	public PrincipalTraitDefinition() {

		super(StructrTraits.PRINCIPAL);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class, new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final PropertyKey<String> eMailProperty = obj.getTraits().key(EMAIL_PROPERTY);
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

			GetProperty.class, new GetProperty() {

				final Set<String> hiddenProperties = newSet(PASSWORD_PROPERTY, SALT_PROPERTY, TWO_FACTOR_SECRET_PROPERTY, DEVICE_TRUST_SECRET_PROPERTY);

				@Override
				public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> key, final Predicate<GraphObject> predicate) {

					if (hiddenProperties.contains(key.jsonName())) {

						return (V) Principal.HIDDEN;

					} else {

						return this.getSuper().getProperty(graphObject, key, predicate);
					}
				}
			},

			SetProperty.class, new SetProperty() {

				@Override
				public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {

					graphObject.clearCaches();

					return getSuper().setProperty(graphObject, key, value, isCreation);
				}
			},

			IsValidPassword.class, new IsValidPassword() {

				@Override
				public boolean isValidPassword(Principal principal, String password) {

					final String encryptedPasswordFromDatabase = principal.getEncryptedPassword();
					if (encryptedPasswordFromDatabase != null) {

						final boolean valid = HashHelper.verifyPassword(password, encryptedPasswordFromDatabase, principal.getSalt());

						/* A login attempt must cost one password verification whatever its outcome, or
						   the difference identifies the account. A legacy SHA-512 hash is compared in
						   microseconds while Argon2id takes far longer, so a failed attempt against a
						   legacy account has to make up that difference. The valid case needs nothing:
						   the migration below re-hashes, which costs at least as much. */
						if (!valid && !HashHelper.isArgon2Hash(encryptedPasswordFromDatabase)) {

							HashHelper.spendVerificationTime(password);
						}

						// Transparent migration: re-hash legacy SHA-512 to Argon2id on successful login
						if (valid && !HashHelper.isArgon2Hash(encryptedPasswordFromDatabase)) {

							try {

								final PropertyKey<String> passwordKey = principal.getTraits().key(PASSWORD_PROPERTY);
								principal.setProperty(passwordKey, password);

							} catch (FrameworkException e) {

								// Re-hashing failed; not critical, login still succeeds.
								// The password will be migrated on the next login attempt.
								LoggerFactory.getLogger(PrincipalTraitDefinition.class).warn("Unable to re-hash password for user {}: {}", principal.getName(), e.getMessage());
							}
						}

						return valid;
					}

					/* An account with no stored password at all, an external one for example, would
					   otherwise be refused instantly and stand out from every other failed attempt. */
					HashHelper.spendVerificationTime(password);

					return false;
				}
			}
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return Set.of(

				new JavaMethod("rotateDeviceTrustSecret", false, false) {

					@Override
					public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

						entity.as(Principal.class).rotateDeviceTrustSecret();

						return null;
					}

					@Override
					public String getDescription() {

						return "Generates a new device trust secret for the principal and stores it. This invalidates all previously issued device trust cookies.";
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

		return Map.of(Principal.class, (traits, node) -> new PrincipalTraitWrapper(traits, node));
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		return newSet(
			new StartNodes(traitsInstance, GROUPS_PROPERTY, StructrTraits.GROUP_CONTAINS_PRINCIPAL),
			new EndNodes(traitsInstance, OWNED_NODES_PROPERTY, StructrTraits.PRINCIPAL_OWNS_NODE),
			new EndNodes(traitsInstance, GRANTED_NODES_PROPERTY, StructrTraits.SECURITY).readOnly(),
			new BooleanProperty(PrincipalTraitDefinition.IS_ADMIN_PROPERTY).indexed().readOnly(),
			new BooleanProperty(BLOCKED_PROPERTY),
			new ArrayProperty(SESSION_IDS_PROPERTY, String.class).indexed(),
			new ArrayProperty(REFRESH_TOKENS_PROPERTY, String.class).indexed(),
			new StringProperty(SESSION_DATA_PROPERTY),
			new StringProperty(EMAIL_PROPERTY).indexed().unique().transformators(LowercaseTransformator.class.getName(), TrimTransformator.class.getName()),
			new PasswordProperty(PASSWORD_PROPERTY),
			new DateProperty(PASSWORD_CHANGE_DATE_PROPERTY),
			new IntProperty(PASSWORD_ATTEMPTS_PROPERTY),
			new DateProperty(LAST_LOGIN_DATE_PROPERTY),
			new StringProperty(TWO_FACTOR_SECRET_PROPERTY),
			new StringProperty(TWO_FACTOR_TOKEN_PROPERTY).indexed(),
			new BooleanProperty(IS_TWO_FACTOR_USER_PROPERTY),
			new BooleanProperty(TWO_FACTOR_CONFIRMED_PROPERTY),
			new StringProperty(SALT_PROPERTY),
			new StringProperty(LOCALE_PROPERTY),
			new StringProperty(PUBLIC_KEY_PROPERTY),
			new ArrayProperty(PUBLIC_KEYS_PROPERTY, String.class),
			new StringProperty(PROXY_URL_PROPERTY),
			new StringProperty(PROXY_USERNAME_PROPERTY),
			new StringProperty(PROXY_PASSWORD_PROPERTY),
			new StringProperty(DEVICE_TRUST_SECRET_PROPERTY),
			new BooleanProperty(DEVICE_TRUST_POSSIBLE_PROPERTY).defaultValue(true).description("When true and the system configuration allows it, gives user the option trust the browser they are logging in from to skip authentication with a second factor for a certain duration or until the browser fingerprint changes.")
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(PropertyView.Ui, newSet(BLOCKED_PROPERTY));
	}

	@Override
	public Relation getRelation() {

		return null;
	}
}
