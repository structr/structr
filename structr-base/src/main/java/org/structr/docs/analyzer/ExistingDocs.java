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
package org.structr.docs.analyzer;

import org.structr.docs.ontology.Occurrence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ExistingDocs {

	private final Map<String, List<String>> filesAndLines = new LinkedHashMap<>();
	private final String docsPath;

	public ExistingDocs(final String path) {

		this.docsPath = path;

		initialize();
	}

	public List<Occurrence> getMentions(final String concept) {

		final List<Occurrence> mentions = new LinkedList<>();
		final String lowerCaseConcept   = concept.toLowerCase();

		for (final Map.Entry<String, List<String>> entry : filesAndLines.entrySet()) {

			final String fileName = entry.getKey();
			int lineNumber        = 0;

			for (final String line : entry.getValue()) {

				if (line.toLowerCase().contains(lowerCaseConcept)) {

					mentions.add(new Occurrence(fileName, lineNumber));
				}

				lineNumber++;
			}
		}

		return mentions;
	}

	// ----- private methods -----
	private void initialize() {

		final Path path = Path.of(docsPath);

		// resolve markdown folder contents and add them as topics
		if (Files.exists(path)) {

			try (final Stream<Path> files = Files.walk(path).filter(Files::isRegularFile).filter(p -> p.toString().toLowerCase().endsWith(".md")).sorted()) {

				files.forEach(file -> {

					try {

						final String fullName = file.toString().substring(docsPath.length() + 1);
						final List<String> lines = Files.readAllLines(file);

						filesAndLines.put(fullName, lines);

					} catch (IOException e) {
						e.printStackTrace();
					}
				});

			} catch (IOException ioex) {

				ioex.printStackTrace();
			}
		}
	}
}
