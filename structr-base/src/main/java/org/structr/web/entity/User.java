/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.service.LicenseManager;
import org.structr.api.util.Iterables;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.rest.auth.TimeBasedOneTimePasswordHelper;
import org.structr.schema.SchemaService;

import java.net.URI;

public interface User extends Principal {

	static class Impl { static {

		final JsonSchema schema     = SchemaService.getDynamicSchema();
		final JsonObjectType user   = schema.addType("User");
		final JsonObjectType folder = schema.addType("Folder");

		user.setExtends(schema.getType("Principal"));
		user.setImplements(URI.create("https://structr.org/v1.1/definitions/User"));
		user.setCategory("ui");

		user.addStringProperty("confirmationKey", PropertyView.Ui).setIndexed(true);
		user.addStringProperty("localStorage");

		user.addBooleanProperty("skipSecurityRelationships", PropertyView.Ui).setDefaultValue("false").setIndexed(true);
		user.addBooleanProperty("isUser",                    PropertyView.Ui, PropertyView.Public).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());

		user.addPropertySetter("localStorage", String.class);
		user.addPropertyGetter("localStorage", String.class);

		user.addPropertyGetter("workingDirectory", Folder.class);
		user.addPropertyGetter("homeDirectory", Folder.class);

		user.overrideMethod("shouldSkipSecurityRelationships", false, "return isAdmin() && getProperty(skipSecurityRelationshipsProperty);");

		user.overrideMethod("onCreation",     true, User.class.getName() + ".onCreateAndModify(this, arg0);");
		user.overrideMethod("onModification", true, User.class.getName() + ".onCreateAndModify(this, arg0, arg2);");
		user.overrideMethod("onDeletion",     true, User.class.getName() + ".checkAndRemoveHomeDirectory(this, arg0);");

		user.addMethod("setHomeDirectory")
			.setSource("setProperty(homeDirectoryProperty, (org.structr.dynamic.Folder)homeDirectory);")
			.addException(FrameworkException.class.getName())
			.addParameter("homeDirectory", "org.structr.web.entity.Folder");

		user.addMethod("setWorkingDirectory")
			.setSource("setProperty(workingDirectoryProperty, (org.structr.dynamic.Folder)workingDirectory);")
			.addException(FrameworkException.class.getName())
			.addParameter("workingDirectory", "org.structr.web.entity.Folder");

		user.relate(folder, "HOME_DIR",    Cardinality.OneToOne,  "homeFolderOfUser",  "homeDirectory");
		user.relate(folder, "WORKING_DIR", Cardinality.ManyToOne, "workFolderOfUsers", "workingDirectory");

		// view configuration
		user.addViewProperty(PropertyView.Public, "name");

		user.addViewProperty(PropertyView.Ui, "confirmationKey");
		user.addViewProperty(PropertyView.Ui, "eMail");
		user.addViewProperty(PropertyView.Ui, "favorites");
		user.addViewProperty(PropertyView.Ui, "groups");
		user.addViewProperty(PropertyView.Ui, "homeDirectory");
		user.addViewProperty(PropertyView.Ui, "img");
		user.addViewProperty(PropertyView.Ui, "isAdmin");
		user.addViewProperty(PropertyView.Ui, "locale");
		user.addViewProperty(PropertyView.Ui, "password");
		user.addViewProperty(PropertyView.Ui, "proxyPassword");
		user.addViewProperty(PropertyView.Ui, "proxyUrl");
		user.addViewProperty(PropertyView.Ui, "proxyUsername");
		user.addViewProperty(PropertyView.Ui, "publicKey");
		user.addViewProperty(PropertyView.Ui, "sessionIds");
		user.addViewProperty(PropertyView.Ui, "refreshTokens");
		user.addViewProperty(PropertyView.Ui, "workingDirectory");

		user.addViewProperty(PropertyView.Ui, "twoFactorToken");
		user.addViewProperty(PropertyView.Ui, "isTwoFactorUser");
		user.addViewProperty(PropertyView.Ui, "twoFactorConfirmed");

		user.addViewProperty(PropertyView.Ui, "passwordAttempts");
		user.addViewProperty(PropertyView.Ui, "passwordChangeDate");

		user.addViewProperty(PropertyView.Ui, "lastLoginDate");
	}}

	String getLocalStorage();
	void setLocalStorage(final String localStorage) throws FrameworkException;

	void setHomeDirectory(final Folder homeDir) throws FrameworkException;
	Folder getHomeDirectory();

	void setWorkingDirectory(final Folder workDir) throws FrameworkException;
	Folder getWorkingDirectory();

	// ----- public static methods -----
	public static void onCreateAndModify(final User user, final SecurityContext securityContext) throws FrameworkException {
		onCreateAndModify(user, securityContext, null);
	}

	public static void onCreateAndModify(final User user, final SecurityContext securityContext, final ModificationQueue modificationQueue) throws FrameworkException {

		final SecurityContext previousSecurityContext = user.getSecurityContext();

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

			user.setSecurityContext(SecurityContext.getSuperUserInstance());

			final PropertyKey<Boolean> skipSecurityRels = StructrApp.key(User.class, "skipSecurityRelationships");
			if (user.getProperty(skipSecurityRels).equals(Boolean.TRUE) && !user.isAdmin()) {

				TransactionCommand.simpleBroadcastWarning("Info", "This user has the skipSecurityRels flag set to true. This flag only works for admin accounts!", Predicate.only(securityContext.getSessionId()));
			}

			if (Principal.getTwoFactorSecret(user) == null) {

				user.setProperty(StructrApp.key(User.class, "isTwoFactorUser"),    false);
				user.setProperty(StructrApp.key(User.class, "twoFactorConfirmed"), false);
				user.setProperty(StructrApp.key(User.class, "twoFactorSecret"),    TimeBasedOneTimePasswordHelper.generateBase32Secret());
			}

			if (Settings.FilesystemEnabled.getValue()) {

				final PropertyKey<Folder> homeFolderKey = StructrApp.key(Folder.class, "homeFolderOfUser");
				final PropertyKey<Folder> parentKey     = StructrApp.key(AbstractFile.class, "parent");

				try {

					Folder homeDir = user.getHomeDirectory();
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
							new NodeAttribute(Folder.name, user.getUuid()),
							new NodeAttribute(Folder.owner, user),
							new NodeAttribute(Folder.visibleToAuthenticatedUsers, true),
							new NodeAttribute(parentKey, homeFolder),
							new NodeAttribute(homeFolderKey, user)
						);
					}

				} catch (Throwable t) {

					LoggerFactory.getLogger(User.class).error("{}", ExceptionUtils.getStackTrace(t));
				}
			}

		} finally {

			// restore previous context
			user.setSecurityContext(previousSecurityContext);
		}
	}

	public static void checkAndRemoveHomeDirectory(final User user, final SecurityContext securityContext) throws FrameworkException {

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
