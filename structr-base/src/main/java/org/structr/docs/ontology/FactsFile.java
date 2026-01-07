/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.docs.ontology;

import org.structr.core.function.tokenizer.FactsTokenizer;
import org.structr.core.function.tokenizer.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class FactsFile {

	private final List<Entry> entries = new LinkedList<>();
	private final String path;

	public FactsFile(final String path) {

		this.path = path;

		initialize();
	}

	// ----- private methods -----
	private void initialize() {

		final Path p = Paths.get(path);


		if (Files.exists(p)) {

			try (final BufferedReader reader = Files.newBufferedReader(p)) {

				final FactsTokenizer tokenizer = new FactsTokenizer();
				final List<Token> tokens       = tokenizer.tokenize(reader.readAllAsString());

				for (final Token token : tokens) {
					System.out.print(token.toString());
				}

			} catch (IOException e) {

				e.printStackTrace();
			}
		}
	}

	private class Entry {


	}
}
