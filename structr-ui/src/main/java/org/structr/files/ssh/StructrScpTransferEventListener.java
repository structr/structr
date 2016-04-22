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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.common.scp.ScpTransferEventListener;

/**
 *
 */
public class StructrScpTransferEventListener implements ScpTransferEventListener {

	private static final Logger logger = Logger.getLogger(StructrScpTransferEventListener.class.getName());
	
	@Override
	public void startFileEvent(FileOperation fileOperation, Path path, long l, Set<PosixFilePermission> set) {
		logger.log(Level.INFO, "startFileEvent (" + (fileOperation == FileOperation.SEND ? "SEND" : "RECEIVE") + ") " + path);
	}

	@Override
	public void endFileEvent(FileOperation fileOperation, Path path, long l, Set<PosixFilePermission> set, Throwable throwable) {
		logger.log(Level.INFO, "endFileEvent (" + (fileOperation == FileOperation.SEND ? "SEND" : "RECEIVE") + ") " + path);

		if (throwable != null) {
			throwable.printStackTrace();
			return;
		}

		try {
			byte bytes[] = Files.readAllBytes(path);
			String s = new String(bytes);
			System.out.println(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void startFolderEvent(FileOperation fileOperation, Path path, Set<PosixFilePermission> set) {
		logger.log(Level.INFO, "startFolderEvent (" + (fileOperation == FileOperation.SEND ? "SEND" : "RECEIVE") + ") " + path);
	}

	@Override
	public void endFolderEvent(FileOperation fileOperation, Path path, Set<PosixFilePermission> set, Throwable throwable) {
		logger.log(Level.INFO, "endFolderEvent (" + (fileOperation == FileOperation.SEND ? "SEND" : "RECEIVE") + ") " + path);
		if (throwable != null) {
			throwable.printStackTrace();
		}
	}
}
