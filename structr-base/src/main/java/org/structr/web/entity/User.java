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
	*/
}
