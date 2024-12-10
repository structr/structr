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


import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;

/**
 * Base class for filesystem objects in structr.
 */
public interface AbstractFile extends NodeTrait {

	void setParent(final Folder parent) throws FrameworkException;
	void setHasParent(final boolean hasParent) throws FrameworkException;
	Folder getParent();

	String getPath();
	String getFolderPath();

	StorageConfiguration getStorageConfiguration();

	boolean isMounted();
	boolean isExternal();
	boolean getHasParent();

	void setHasParent() throws FrameworkException;

	boolean isBinaryDataAccessible(final SecurityContext securityContext);
	boolean includeInFrontendExport();

	boolean validateAndRenameFileOnce(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException;
	boolean renameMountedAbstractFile (final Folder thisFolder, final AbstractFile file, final String path, final String previousName);
}
