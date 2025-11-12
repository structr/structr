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
package org.structr.core.traits.definitions;

import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.service.LicenseManager;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.wrappers.UserTraitWrapper;
import org.structr.rest.auth.TimeBasedOneTimePasswordHelper;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;

import java.util.Map;
import java.util.Set;

public final class UserTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String HOME_DIRECTORY_PROPERTY              = "homeDirectory";
	public static final String WORKING_DIRECTORY_PROPERTY           = "workingDirectory";
	public static final String IMG_PROPERTY                         = "img";
	public static final String CONFIRMATION_KEY_PROPERTY            = "confirmationKey";
	public static final String LOCAL_STORAGE_PROPERTY               = "localStorage";
	public static final String SKIP_SECURITY_RELATIONSHIPS_PROPERTY = "skipSecurityRelationships";
	public static final String IS_USER_PROPERTY                     = "isUser";

	public UserTraitDefinition() {
		super(StructrTraits.USER);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					onCreateAndModify(graphObject.as(User.class), securityContext);
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
					onCreateAndModify(graphObject.as(User.class), securityContext);
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
					checkAndRemoveHomeDirectory(graphObject.as(User.class));
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> homeDirectoryProperty       = new EndNode(traitsInstance, HOME_DIRECTORY_PROPERTY, StructrTraits.USER_HOME_DIR_FOLDER);
		final Property<NodeInterface> workingDirectoryProperty    = new EndNode(traitsInstance, WORKING_DIRECTORY_PROPERTY, StructrTraits.USER_WORKING_DIR_FOLDER);
		final Property<NodeInterface> imgProperty                 = new StartNode(traitsInstance, IMG_PROPERTY, StructrTraits.IMAGE_PICTURE_OF_USER);
		final Property<String> confirmationKeyProperty            = new StringProperty(CONFIRMATION_KEY_PROPERTY).indexed();
		final Property<String> localStorageProperty               = new StringProperty(LOCAL_STORAGE_PROPERTY);
		final Property<Boolean> skipSecurityRelationshipsProperty = new BooleanProperty(SKIP_SECURITY_RELATIONSHIPS_PROPERTY).defaultValue(false).indexed();
		final Property<Boolean> isUserProperty                    = new ConstantBooleanProperty(IS_USER_PROPERTY, true);

		return Set.of(
			homeDirectoryProperty,
			workingDirectoryProperty,
			imgProperty,
			confirmationKeyProperty,
			localStorageProperty,
			skipSecurityRelationshipsProperty,
			isUserProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			Set.of(
					IS_USER_PROPERTY
			),

			PropertyView.Ui,
			Set.of(
					IS_USER_PROPERTY,
					CONFIRMATION_KEY_PROPERTY, HOME_DIRECTORY_PROPERTY, WORKING_DIRECTORY_PROPERTY, SKIP_SECURITY_RELATIONSHIPS_PROPERTY, IMG_PROPERTY,
					PrincipalTraitDefinition.EMAIL_PROPERTY, PrincipalTraitDefinition.GROUPS_PROPERTY,
					PrincipalTraitDefinition.IS_ADMIN_PROPERTY, PrincipalTraitDefinition.LOCALE_PROPERTY,
					PrincipalTraitDefinition.PASSWORD_PROPERTY, PrincipalTraitDefinition.PROXY_PASSWORD_PROPERTY,
					PrincipalTraitDefinition.PROXY_URL_PROPERTY, PrincipalTraitDefinition.PROXY_USERNAME_PROPERTY,
					PrincipalTraitDefinition.PUBLIC_KEY_PROPERTY, PrincipalTraitDefinition.SESSION_IDS_PROPERTY,
					PrincipalTraitDefinition.REFRESH_TOKENS_PROPERTY, PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY,
					PrincipalTraitDefinition.IS_TWO_FACTOR_USER_PROPERTY, PrincipalTraitDefinition.TWO_FACTOR_CONFIRMED_PROPERTY,
					PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY, PrincipalTraitDefinition.PASSWORD_CHANGE_DATE_PROPERTY,
					PrincipalTraitDefinition.LAST_LOGIN_DATE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			User.class, (traits, node) -> new UserTraitWrapper(traits, node)
		);
	}

	public void onCreateAndModify(final User user, final SecurityContext securityContext) throws FrameworkException {

		final SecurityContext previousSecurityContext = user.getSecurityContext();
		final Traits userTraits                       = Traits.of(StructrTraits.USER);

		try {

			// make sure that username OR email is set
			final ErrorBuffer errorBuffer      = new ErrorBuffer();
			final PropertyKey<String> nameKey  = userTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final PropertyKey<String> eMailKey = userTraits.key(PrincipalTraitDefinition.EMAIL_PROPERTY);

			if (!ValidationHelper.isValidStringNotBlank(user, nameKey, errorBuffer) && !ValidationHelper.isValidStringNotBlank(user, eMailKey, errorBuffer)) {

				throw new FrameworkException(422, "Cannot create a user who has neither a name nor an email address", errorBuffer);
			}

			// check per-user licensing count
			final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
			if (licenseManager != null) {

				final int userCount         = Iterables.count(StructrApp.getInstance().nodeQuery(StructrTraits.USER).getResultStream());
				final int licensedUserCount = licenseManager.getNumberOfUsers();

				// -1 means no limit
				if (licensedUserCount >= 0 && userCount > licensedUserCount) {

					throw new FrameworkException(422, "The number of users on this instance may not exceed " + licensedUserCount);
				}
			}

			user.setSecurityContext(SecurityContext.getSuperUserInstance());

			final PropertyKey<Boolean> skipSecurityRelationships = Traits.of(StructrTraits.USER).key(SKIP_SECURITY_RELATIONSHIPS_PROPERTY);
			if (user.getProperty(skipSecurityRelationships).equals(Boolean.TRUE) && !user.isAdmin()) {

				TransactionCommand.simpleBroadcastWarning("Info", "This user has the '" + SKIP_SECURITY_RELATIONSHIPS_PROPERTY + "' flag set to true. This flag only works for admin accounts!", Predicate.only(securityContext.getSessionId()));
			}

			if (user.getTwoFactorSecret() == null) {

				user.setProperty(userTraits.key(PrincipalTraitDefinition.IS_TWO_FACTOR_USER_PROPERTY),   false);
				user.setProperty(userTraits.key(PrincipalTraitDefinition.TWO_FACTOR_CONFIRMED_PROPERTY), false);
				user.setProperty(userTraits.key(PrincipalTraitDefinition.TWO_FACTOR_SECRET_PROPERTY),    TimeBasedOneTimePasswordHelper.generateBase32Secret());
			}

			if (Settings.FilesystemEnabled.getValue()) {

				final Folder homeDir = user.getOrCreateHomeDirectory();
			}

		} finally {

			// restore previous context
			user.setSecurityContext(previousSecurityContext);
		}
	}

	public void checkAndRemoveHomeDirectory(final User user) throws FrameworkException {

		if (Settings.FilesystemEnabled.getValue()) {

			// use superuser context here
			final SecurityContext storedContext = user.getSecurityContext();

			try {

				user.setSecurityContext(SecurityContext.getSuperUserInstance());

				final Folder homeDir = user.getHomeDirectory();
				if (homeDir != null) {

					StructrApp.getInstance().delete(homeDir);
				}

			} catch (Throwable ignore) {
			} finally {

				// restore previous context
				user.setSecurityContext(storedContext);
			}

		}
	}
}
