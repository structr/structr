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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * An export rooted at a {@link Path}, whatever filesystem that Path belongs to.
 *
 * Written against Path rather than File on purpose. Today the only providers available are the
 * host filesystem and the JDK zip provider; when a NIO provider for Structr's VFS lands, an
 * export stored as a File node in the database is reachable through this same class, with no
 * change here. That is the whole reason the parser was never given a directory.
 *
 * <p>The path map is built once and holds the NFC-normalised name against the real Path. Two
 * machines disagree about how to spell a name containing an umlaut, one decomposing it and the
 * other not, and comparing exports taken on both would otherwise report a file present on one
 * side and absent on the other. Normalising here settles it before the parser can be misled.
 */
public class PathExportSource implements ExportSource {

	private final Map<String, Path> pathsByName = new LinkedHashMap<>();
	private final String name;
	private final Path root;

	public PathExportSource(final Path root, final String name) throws IOException {

		this.root = root;
		this.name = name;

		final List<Path> found = new ArrayList<>();

		try (final Stream<Path> walk = Files.walk(root)) {

			walk.filter(Files::isRegularFile).forEach(found::add);
		}

		final List<String> relative = new ArrayList<>();
		final Map<String, Path> byRelative = new LinkedHashMap<>();

		for (final Path p : found) {

			final String rel = normalise(root.relativize(p).toString().replace('\\', '/'));

			// a stray .DS_Store is the host filesystem talking, not part of the export
			if (rel.endsWith(".DS_Store")) {

				continue;
			}

			relative.add(rel);
			byRelative.put(rel, p);
		}

		Collections.sort(relative);

		for (final String rel : relative) {

			pathsByName.put(rel, byRelative.get(rel));
		}
	}

	@Override
	public List<String> paths() {

		return new ArrayList<>(pathsByName.keySet());
	}

	@Override
	public InputStream open(final String path) throws IOException {

		final Path resolved = pathsByName.get(normalise(path));

		if (resolved == null) {

			throw new IOException("No such entry in " + name + ": " + path);
		}

		return Files.newInputStream(resolved);
	}

	@Override
	public boolean exists(final String path) {

		return pathsByName.containsKey(normalise(path));
	}

	@Override
	public String getName() {

		return name;
	}

	protected Path getRoot() {

		return root;
	}

	private static String normalise(final String s) {

		return Normalizer.normalize(s, Normalizer.Form.NFC);
	}
}
