/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.files.ssh;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.structr.common.SecurityContext;

/**
 *
 *
 */
public class StructrSSHFileSystemView implements FileSystemView {

	private StructrSSHFile rootFolder = null;
	private Session session           = null;

	public StructrSSHFileSystemView(final Session session) {

		this.rootFolder = new StructrSSHFile(SecurityContext.getSuperUserInstance());
		this.session    = session;
	}

	@Override
	public SshFile getFile(final String path) {

		if ("/".equals(path)) {
			return rootFolder;
		}

		return rootFolder.findFile(path);
	}

	@Override
	public SshFile getFile(final SshFile baseDir, final String file) {

		if (baseDir instanceof StructrSSHFile) {

			return ((StructrSSHFile)baseDir).findFile(file);
		}

		return rootFolder.findFile(baseDir.getAbsolutePath() + file);
	}

	@Override
	public FileSystemView getNormalizedView() {
		return this;
	}
}
