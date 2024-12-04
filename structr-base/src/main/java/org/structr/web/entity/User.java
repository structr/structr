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
package org.structr.web.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;

public interface User extends Principal {

        Folder getHomeDirectory();
        void setWorkingDirectory(final Folder workDir) throws FrameworkException;
        Folder getWorkingDirectory();
        void setLocalStorage(final String localStorage) throws FrameworkException;
        String getLocalStorage();
        String getConfirmationKey();

	/*

	public static final View defaultView = new View(User.class, PropertyView.Public,
		isUserProperty, name
	);

	public static final View uiView = new View(User.class, PropertyView.Ui,
		isUserProperty, confirmationKeyProperty, eMailProperty, favoritesProperty, groupsProperty, homeDirectoryProperty, isAdminProperty, localeProperty, passwordProperty, proxyPasswordProperty,
		proxyUrlProperty, proxyUsernameProperty, publicKeyProperty, sessionIdsProperty, refreshTokensProperty, workingDirectoryProperty, twoFactorTokenProperty, isTwoFactorUserProperty,
		twoFactorConfirmedProperty, passwordAttemptsProperty, passwordChangeDateProperty, lastLoginDateProperty, skipSecurityRelationshipsProperty, imgProperty
	);

	@Override
	public boolean shouldSkipSecurityRelationships() {
		return isAdmin() && getProperty(skipSecurityRelationshipsProperty);
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);
		onCreateAndModify(securityContext, null);
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);
		onCreateAndModify(securityContext, modificationQueue);
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

		super.onDeletion(securityContext, errorBuffer, properties);
		checkAndRemoveHomeDirectory(securityContext);
	}

	public String getLocalStorage() {
		return getProperty(localStorageProperty);
	}

	public void setLocalStorage(final String localStorage) throws FrameworkException {
		setProperty(localStorageProperty, localStorage);
	}

	public void setHomeDirectory(final Folder homeDir) throws FrameworkException {
		setProperty(homeDirectoryProperty, homeDir);
	}

	public Folder getHomeDirectory() {
		return getProperty(homeDirectoryProperty);
	}

	public void setWorkingDirectory(final Folder workDir) throws FrameworkException {
		setProperty(workingDirectoryProperty, workDir);
	}

	public Folder getWorkingDirectory() {
		return getProperty(workingDirectoryProperty);
	}

	// ----- public static methods -----
	public void onCreateAndModify(final SecurityContext securityContext, final ModificationQueue modificationQueue) throws FrameworkException {

		final SecurityContext previousSecurityContext = getSecurityContext();

		try {

			// check per-user licensing count
			final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
			if (licenseManager != null) {

				final int userCount         = Iterables.count(StructrApp.getInstance().nodeQuery(User.class).getResultStream());
				final int licensedUserCount = licenseManager.getNumberOfUsers();

				// -1 means no limit
				if (licensedUserCount >= 0 && userCount > licensedUserCount) {

					throw new FrameworkException(422, "The number of users on this instance may not exceed " + licensedUserCount);
				}
			}

			setSecurityContext(SecurityContext.getSuperUserInstance());

			final PropertyKey<Boolean> skipSecurityRels = StructrApp.key(User.class, "skipSecurityRelationships");
			if (getProperty(skipSecurityRels).equals(Boolean.TRUE) && !isAdmin()) {

				TransactionCommand.simpleBroadcastWarning("Info", "This user has the skipSecurityRels flag set to true. This flag only works for admin accounts!", Predicate.only(securityContext.getSessionId()));
			}

			if (getTwoFactorSecret() == null) {

				setProperty(User.isTwoFactorUserProperty,    false);
				setProperty(User.twoFactorConfirmedProperty, false);
				setProperty(User.twoFactorSecretProperty,    TimeBasedOneTimePasswordHelper.generateBase32Secret());
			}

			if (Settings.FilesystemEnabled.getValue()) {

				final PropertyKey<Folder> homeFolderKey = StructrApp.key(Folder.class, "homeFolderOfUser");
				final PropertyKey<Folder> parentKey     = StructrApp.key(AbstractFile.class, "parent");

				try {

					Folder homeDir = getHomeDirectory();
					if (homeDir == null) {

						// create home directory
						final App app     = StructrApp.getInstance();
						Folder homeFolder = app.nodeQuery(Folder.class).and(Folder.name, "home").and(parentKey, null).getFirst();

						if (homeFolder == null) {

							homeFolder = app.create(Folder.class,
								new NodeAttribute(Folder.name, "home"),
								new NodeAttribute(Folder.owner, null),
								new NodeAttribute(Folder.visibleToAuthenticatedUsers, true)
							);
						}

						app.create(Folder.class,
							new NodeAttribute(Folder.name, getUuid()),
							new NodeAttribute(Folder.owner, this),
							new NodeAttribute(Folder.visibleToAuthenticatedUsers, true),
							new NodeAttribute(parentKey, homeFolder),
							new NodeAttribute(homeFolderKey, this)
						);
					}

				} catch (Throwable t) {

					LoggerFactory.getLogger(User.class).error("{}", ExceptionUtils.getStackTrace(t));
				}
			}

		} finally {

			// restore previous context
			setSecurityContext(previousSecurityContext);
		}
	}

	public void checkAndRemoveHomeDirectory(final SecurityContext securityContext) throws FrameworkException {

		if (Settings.FilesystemEnabled.getValue()) {

			// use superuser context here
			final SecurityContext storedContext = getSecurityContext();

			try {

				setSecurityContext(SecurityContext.getSuperUserInstance());

				final Folder homeDir = getHomeDirectory();
				if (homeDir != null) {

					StructrApp.getInstance().delete(homeDir);
				}

			} catch (Throwable ignore) {
			} finally {

				// restore previous context
				setSecurityContext(storedContext);
			}

		}
	}
	*/
}
