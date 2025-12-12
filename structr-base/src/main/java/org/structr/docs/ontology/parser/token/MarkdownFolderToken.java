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
package org.structr.docs.ontology.parser.token;

import org.structr.docs.formatter.MarkdownMarkdownFileFormatter;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * An identifier that is augmented with a type so we know what it is.
 */
public class MarkdownFolderToken extends NamedConceptToken {

	public MarkdownFolderToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {

		super(conceptToken, identifierToken);
	}

	@Override
	public boolean isUnresolved() {
		return false;
	}

	public boolean isUnknown() {
		return "unknown".equals(conceptToken.getName());
	}

	@Override
	public List<Concept> resolve(final Ontology ontology, final String sourceFile, final int line) {

		final String type              = conceptToken.resolve(ontology, sourceFile, line);
		final List<String> identifiers = identifierToken.resolve(ontology, sourceFile, line);
		final List<Concept> concepts   = new LinkedList<>();

		for (final String folderName : identifiers) {

			final String cleanedName = MarkdownMarkdownFileFormatter.getNameFromFileName(folderName);
			final Concept folder     = ontology.getOrCreateConcept(sourceFile, line, type, cleanedName);
			final Path path          = Path.of("structr/docs/" + folderName + "/index.txt");

			concepts.add(folder);

			// resolve markdown folder contents and add them as topics
			if (Files.exists(path)) {

				try {
					final List<String> files = Files.readAllLines(path);
					for (final String file : files) {

						final String cleanedFileName = MarkdownMarkdownFileFormatter.getNameFromFileName(file);
						final Concept markdownFile   = ontology.getOrCreateConcept("sourceFile", line, "markdown-file", cleanedFileName);
						final String filePath        = folderName + "/" + file;

						markdownFile.getMetadata().put("path", filePath);

						folder.linkChild("has", markdownFile);
					}

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}
		}

		return concepts;
	}
}
