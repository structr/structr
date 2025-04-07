/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.ldap.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.auth.exception.DeleteInvalidUserException;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.principal.IsValidPassword;
import org.structr.ldap.LDAPService;
import org.structr.ldap.LDAPUser;
import org.structr.ldap.traits.wrappers.LDAPUserTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LDAPUserTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String ORIGIN_ID_PROPERTY          = "originId";
	public static final String DISTINGUISHED_NAME_PROPERTY = "distinguishedName";
	public static final String LAST_LDAP_SYNC_PROPERTY     = "lastLDAPSync";

	private static final Logger logger = LoggerFactory.getLogger(LDAPUserTraitDefinition.class);

	public LDAPUserTraitDefinition() {
		super(StructrTraits.LDAP_USER);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Traits traits = Traits.of(StructrTraits.LDAP_USER);
					boolean valid = true;

					valid &= ValidationHelper.isValidUniqueProperty(obj, traits.key(DISTINGUISHED_NAME_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj, traits.key(ORIGIN_ID_PROPERTY), errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			IsValidPassword.class,
			new IsValidPassword() {

				@Override
				public boolean isValidPassword(final Principal principal, final String password) {

					final LDAPService ldapService = Services.getInstance().getService(LDAPService.class, "default");
					final LDAPUser user           = principal.as(LDAPUser.class);
					boolean hasLDAPGroups         = false;

					if (ldapService != null) {

						// update user..
						user.update();

						for (final Group group : user.getGroups()) {

							if (group.is(StructrTraits.LDAP_GROUP)) {

								hasLDAPGroups = true;
								break;
							}
						}

						if (!hasLDAPGroups) {

							final String uuid = user.getUuid();

							logger.warn("LDAPUser {} with UUID {} is not associated with an LDAPGroup, removing.", user.getName(), uuid);

							// this user must be deleted immediately
							throw new DeleteInvalidUserException(uuid);
						}

						return ldapService.canSuccessfullyBind(user.getDistinguishedName(), password);

					} else {

						logger.warn("Unable to reach LDAP server for authentication of {}", user.getDistinguishedName());
					}

					return false;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of(
			LDAPUser.class, (traits, node) -> new LDAPUserTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<String> originIdProperty          = new StringProperty(ORIGIN_ID_PROPERTY).unique().indexed();
		final PropertyKey<String> distinguishedNameProperty = new StringProperty(DISTINGUISHED_NAME_PROPERTY).indexed();
		final PropertyKey<Long> lastLDAPSyncProperty        = new LongProperty(LAST_LDAP_SYNC_PROPERTY);

		return newSet(
			originIdProperty,
			distinguishedNameProperty,
			lastLDAPSyncProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {
		return Map.of(
			PropertyView.Public,
			newSet(
					ORIGIN_ID_PROPERTY, DISTINGUISHED_NAME_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					ORIGIN_ID_PROPERTY, DISTINGUISHED_NAME_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
