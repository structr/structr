/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.files.external;

import java.nio.file.Files;
import java.nio.file.Path;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.Folder;

/**
 * Implementation of the watch event listener interface that syncs
 * the discovered files with the database.
 */
public class FileSyncWatchEventListener implements WatchEventListener {

	@Override
	public void onDiscover(final Path path) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void onCreate(final Path path) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void onModify(final Path path) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void onDelete(final Path path) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// ----- private methods -----
	private <T extends NodeInterface> T getOrCreate(final Class<T> type, final Path path) throws FrameworkException {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		final Path parentFolder               = getFolder(path);
		final Folder folder                   = FileHelper.createFolderPath(securityContext, parentFolder.toString());

		if (folder != null && Files.isRegularFile(path)) {

			final T newFile = StructrApp.getInstance(securityContext).create(type, path.getFileName().toString());

			

			return newFile;
		}

		return (T)parentFolder;
	}

	final Path getFolder(final Path path) {

		if (Files.isDirectory(path)) {

			return path;

		} else {

			return path.getParent();
		}
	}

}
