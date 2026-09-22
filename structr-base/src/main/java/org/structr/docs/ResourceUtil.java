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
package org.structr.docs;

import org.eclipse.jetty.util.resource.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reads documentation sources through the Jetty Resource API so that the
 * same code works for a directory on disk, a resource inside the
 * application jar and a CombinedResource spanning several of them.
 *
 * Resource.getPath() must not be used for that: it is null for a
 * CombinedResource and for any URL-backed resource, and only happens to
 * work for a single jar because Jetty mounts it as a ZipFileSystem.
 */
public final class ResourceUtil {

	private ResourceUtil() {
	}

	public static String readString(final Resource resource) throws IOException {

		if (resource == null || !resource.exists()) {

			throw new FileNotFoundException(resource == null ? "null" : resource.getURI().toString());
		}

		try (final InputStream in = resource.newInputStream()) {

			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public static List<String> readAllLines(final Resource resource) throws IOException {

		return readString(resource).lines().toList();
	}
}
