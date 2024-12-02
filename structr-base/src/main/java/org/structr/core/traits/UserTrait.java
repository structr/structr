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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;

public class UserTrait extends PrincipalTrait implements User {

	public UserTrait(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public Folder getHomeDirectory() {
		return nodeInterface.getProperty(traits.key("homeDirectory"));
	}

	@Override
	public void setWorkingDirectory(final Folder workDir) throws FrameworkException {
		nodeInterface.setProperty(traits.key("workingDirectory"), workDir);
	}

	@Override
	public Folder getWorkingDirectory() {
		return nodeInterface.getProperty(traits.key("workingDirectory"));
	}

	@Override
	public void setLocalStorage(final String localStorage) throws FrameworkException {
		nodeInterface.setProperty(traits.key("localStorage"), localStorage);
	}

	@Override
	public String getLocalStorage() {
		return nodeInterface.getProperty(traits.key("localStorage"));
	}
}
