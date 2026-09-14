/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.files.ssh.filesystem;

import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeInterface;

import java.nio.file.AccessDeniedException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;

/**
 * Ticket 1586: the SSH paths resolved a node by READABILITY and then went straight to the storage
 * provider. Structr's permission check sits in setProperty, on node properties, and never sees the
 * byte I/O that follows - so being allowed to SEE a file was enough to replace its contents, and a JS
 * asset of a public site is exactly such a file.
 */
public class StructrFileWriteAccess {

	/**
	 * Whether the given open options are asking to write. A read-only channel on a readable file stays
	 * allowed, which is why this is not simply "check write on every open".
	 */
	public static boolean isWriteAccess(final Collection<? extends OpenOption> options) {

		return options.contains(StandardOpenOption.WRITE)
			|| options.contains(StandardOpenOption.APPEND)
			|| options.contains(StandardOpenOption.CREATE)
			|| options.contains(StandardOpenOption.CREATE_NEW)
			|| options.contains(StandardOpenOption.TRUNCATE_EXISTING);
	}

	public static boolean isWriteAccess(final OpenOption... options) {

		return isWriteAccess(Arrays.asList(options));
	}

	/**
	 * Refuses opening a write channel on a file the caller may not write.
	 */
	public static void assertWritable(final SecurityContext securityContext, final NodeInterface file) throws AccessDeniedException {

		if (file != null && !file.isGranted(Permission.write, securityContext)) {

			throw new AccessDeniedException(file.getName(), null, "write permission required");
		}
	}
}
