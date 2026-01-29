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
package org.structr.core.traits.wrappers;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FolderTraitDefinition;

public class UserTraitWrapper extends PrincipalTraitWrapper implements User {

	public UserTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public Folder getHomeDirectory() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(UserTraitDefinition.HOME_DIRECTORY_PROPERTY));
		if (node != null) {

			return node.as(Folder.class);
		}

		return null;
	}

	@Override
	public Folder getOrCreateHomeDirectory() {

		final User user = wrappedObject.as(User.class);
		Folder homeDir  = user.getHomeDirectory();

		if (homeDir != null) {
			return homeDir;
		}

		// create home directory
		final Traits folderTraits                      = Traits.of(StructrTraits.FOLDER);
		final PropertyKey<NodeInterface> homeFolderKey = folderTraits.key(FolderTraitDefinition.HOME_FOLDER_OF_USER_PROPERTY);
		final PropertyKey<NodeInterface> parentKey     = folderTraits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final App app            = StructrApp.getInstance();
			NodeInterface homeFolder = app.nodeQuery(StructrTraits.FOLDER).key(folderTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "home").key(parentKey, null).getFirst();

			if (homeFolder == null) {

				homeFolder = app.create(StructrTraits.FOLDER,
						new NodeAttribute(folderTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "home"),
						new NodeAttribute(folderTraits.key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), null),
						new NodeAttribute(folderTraits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true)
				);
			}

			NodeInterface userHomeDir = app.nodeQuery(StructrTraits.FOLDER).key(folderTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), user.getUuid()).key(parentKey, homeFolder).getFirst();

			if (userHomeDir == null) {

				userHomeDir = app.create(StructrTraits.FOLDER,
						new NodeAttribute(folderTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), user.getUuid()),
						new NodeAttribute(folderTraits.key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), user),
						new NodeAttribute(parentKey, homeFolder),
						new NodeAttribute(homeFolderKey, user)
				);

			} else {

				userHomeDir.setProperty(homeFolderKey, user);
			}

			tx.success();

			return userHomeDir.as(Folder.class);

		} catch (Throwable t) {

			LoggerFactory.getLogger(User.class).error("Unable to create user home directory: {}", ExceptionUtils.getStackTrace(t));
		}

		return null;
	}

	@Override
	public void setWorkingDirectory(final Folder workDir) throws FrameworkException {
		wrappedObject.setProperty(traits.key(UserTraitDefinition.WORKING_DIRECTORY_PROPERTY), workDir);
	}

	@Override
	public Folder getWorkingDirectory() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(UserTraitDefinition.WORKING_DIRECTORY_PROPERTY));
		if (node != null) {

			return node.as(Folder.class);
		}

		return null;
	}

	@Override
	public void setLocalStorage(final String localStorage) throws FrameworkException {
		wrappedObject.setProperty(traits.key(UserTraitDefinition.LOCAL_STORAGE_PROPERTY), localStorage);
	}

	@Override
	public String getLocalStorage() {
		return wrappedObject.getProperty(traits.key(UserTraitDefinition.LOCAL_STORAGE_PROPERTY));
	}

	@Override
	public String getConfirmationKey() {
		return wrappedObject.getProperty(traits.key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY));
	}
}
