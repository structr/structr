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
package org.structr.core.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;

public class UserTraitWrapper extends PrincipalTraitWrapper implements User {

	public UserTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public Folder getHomeDirectory() {
		return wrappedObject.getProperty(traits.key(UserTraitDefinition.HOME_DIRECTORY_PROPERTY));
	}

	@Override
	public void setWorkingDirectory(final Folder workDir) throws FrameworkException {
		wrappedObject.setProperty(traits.key(UserTraitDefinition.WORKING_DIRECTORY_PROPERTY), workDir);
	}

	@Override
	public Folder getWorkingDirectory() {
		return wrappedObject.getProperty(traits.key(UserTraitDefinition.WORKING_DIRECTORY_PROPERTY));
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
