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
package org.structr.web.maintenance.deploy;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The single spelling of a path inside a deployment export.
 *
 * A deployment constantly compares a path taken from the filesystem with one taken from a JSON
 * manifest or from the database, and those three sources do not agree on how to write the same
 * name. Separators differ between platforms, and a character with a diacritic has two equally
 * valid Unicode spellings: composed (NFC, "ü" as one codepoint) and decomposed (NFD, "u" followed
 * by a combining diaeresis). They render identically and Unicode calls them canonically
 * equivalent, but they are different strings, so a plain comparison fails.
 *
 * That difference is not hypothetical: a browser uploading a file from macOS sends the decomposed
 * name and Structr stores it verbatim, while git rewrites the same name to the composed form when
 * it records the file. An export written on one machine then stops matching itself on another, and
 * the importer reports the same file as both missing and unexpected while the export cleanup pass
 * deletes it as unknown.
 *
 * Rather than normalize at each comparison - which only works for as long as everybody remembers -
 * every path is normalized where it ENTERS the deployment subsystem: manifests when they are read,
 * filesystem paths when they are converted, node names when they are exported. Everything
 * downstream can then compare paths as ordinary strings.
 *
 * NFC is the target because it is what git records and what Linux filesystems conventionally use,
 * so a normalized export survives a commit and a checkout unchanged.
 */
public class DeploymentPaths {

	/**
	 * Joins the given parts into a deployment path: separators harmonized to "/", result in NFC.
	 */
	public static String normalize(final String... parts) {

		final StringBuilder buf = new StringBuilder();

		for (final String part : parts) {

			if (part != null) {

				buf.append(part);
			}
		}

		int pos = buf.indexOf("\\");

		while (pos >= 0) {

			buf.replace(pos, pos + 1, "/");
			pos = buf.indexOf("\\");
		}

		return Normalizer.normalize(buf.toString(), Normalizer.Form.NFC);
	}

	/**
	 * The given map with every key normalized, for a manifest that is keyed by path.
	 */
	public static Map<String, Object> normalizeKeys(final Map<String, Object> source) {

		if (source == null) {

			return null;
		}

		final Map<String, Object> normalized = new LinkedHashMap<>();

		for (final Map.Entry<String, Object> entry : source.entrySet()) {

			normalized.put(normalize(entry.getKey()), entry.getValue());
		}

		return normalized;
	}

	/**
	 * The given list with every entry normalized, for a manifest that is a plain list of paths.
	 */
	public static List<String> normalizeAll(final List<String> source) {

		if (source == null) {

			return null;
		}

		return source.stream().map(DeploymentPaths::normalize).collect(Collectors.toList());
	}
}
