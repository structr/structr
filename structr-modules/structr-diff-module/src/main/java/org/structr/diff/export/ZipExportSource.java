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
package org.structr.diff.export;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * An export stored as a zip, read without unpacking it to disk.
 *
 * This is why an export is stored zipped rather than as a tree of File and Folder nodes: measured
 * on real exports, unpacking one costs about 580 nodes and 9 MB against one node and 4.6 MB, and
 * version management is what turns one snapshot into many.
 *
 * <p><b>Deliberately not the JDK zip filesystem.</b> {@code FileSystems.newFileSystem(path)} would
 * be the elegant way to do this and would give lazy, streaming access. It also needs the underlying
 * provider to support random access, because a zip's central directory lives at the END of the
 * archive and zipfs seeks to it. That is fine on the host filesystem and unproven on Structr's VFS,
 * where a channel over a graph-backed blob has so far only ever been written sequentially. Reading
 * with {@link ZipInputStream} instead needs nothing but an ordinary InputStream, which every
 * storage provider supports, so this class behaves identically wherever the archive lives.
 *
 * <p>The cost is that entries are materialised in memory on construction rather than read lazily.
 * That is cheaper than it sounds here: the parser hashes every payload under {@code files/} anyway,
 * so the whole archive gets read either way and the only real cost is holding it. If an export ever
 * grows large enough for that to hurt, zipfs becomes worth revisiting, but the decision should then
 * be made on a measurement rather than on elegance.
 *
 * <p>{@code zipPath} is a Path rather than a File so that an archive living in the VFS is reachable
 * through {@code structr:///} once a NIO provider for it exists, with no change here.
 */
public class ZipExportSource implements ExportSource {

	private final Map<String, byte[]> entries = new LinkedHashMap<>();
	private final String name;

	private ZipExportSource(final String name) {

		this.name = name;
	}

	public static ZipExportSource open(final Path zipPath, final String name) throws IOException {

		try (final InputStream in = Files.newInputStream(zipPath)) {

			return read(in, name);
		}
	}

	/**
	 * Reads an archive from any stream, which is what makes the VFS case work without a filesystem
	 * provider at all: a File node hands out an InputStream and that is the entire requirement.
	 */
	public static ZipExportSource read(final InputStream in, final String name) throws IOException {

		final ZipExportSource source = new ZipExportSource(name);
		final Map<String, byte[]> raw = new LinkedHashMap<>();

		try (final ZipInputStream zip = new ZipInputStream(in)) {

			ZipEntry entry = zip.getNextEntry();

			while (entry != null) {

				if (!entry.isDirectory()) {

					raw.put(normalise(entry.getName()), zip.readAllBytes());
				}

				entry = zip.getNextEntry();
			}
		}

		final String prefix = commonPrefix(raw.keySet());
		final List<String> paths = new ArrayList<>(raw.keySet());

		paths.sort(null);

		for (final String path : paths) {

			final String relative = path.substring(prefix.length());

			// a stray .DS_Store is the host filesystem talking, not part of the export
			if (!relative.isEmpty() && !relative.endsWith(".DS_Store")) {

				source.entries.put(relative, raw.get(path));
			}
		}

		return source;
	}

	/**
	 * The directory the export actually starts at, as a prefix to strip.
	 *
	 * A zip made from an export FOLDER wraps everything in one top-level directory named after it; a
	 * zip made from the folder's CONTENTS does not. Rather than make the caller know which they
	 * have, look for the wrapper. deployment.conf is the marker because every export has exactly one
	 * at its root.
	 */
	private static String commonPrefix(final Iterable<String> paths) {

		String candidate = null;

		for (final String path : paths) {

			if (path.equals("deployment.conf")) {

				return "";
			}

			if (path.endsWith("/deployment.conf")) {

				candidate = path.substring(0, path.length() - "deployment.conf".length());
			}
		}

		return candidate != null ? candidate : "";
	}

	@Override
	public List<String> paths() {

		return new ArrayList<>(entries.keySet());
	}

	@Override
	public InputStream open(final String path) throws IOException {

		final byte[] bytes = entries.get(normalise(path));

		if (bytes == null) {

			throw new IOException("No such entry in " + name + ": " + path);
		}

		return new ByteArrayInputStream(bytes);
	}

	@Override
	public boolean exists(final String path) {

		return entries.containsKey(normalise(path));
	}

	@Override
	public String getName() {

		return name;
	}

	private static String normalise(final String s) {

		return Normalizer.normalize(s, Normalizer.Form.NFC);
	}
}
