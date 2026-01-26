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
package org.structr.docs.ontology.parser.token;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profile.pegdown.Extensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.resource.Resource;
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.formatter.markdown.MarkdownMarkdownFileFormatter;
import org.structr.docs.ontology.*;
import org.structr.web.traits.definitions.html.I;

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

	public boolean isUnknown() {
		return "unknown".equals(conceptToken.getToken());
	}

	@Override
	public AnnotatedConcept resolve(final Ontology ontology) {

		final ConceptType type       = conceptToken.resolve(ontology);
		final Resource baseResource  = ontology.getBaseResource();
		final String folderName      = identifierToken.resolve(ontology);
		final String cleanedName     = MarkdownMarkdownFileFormatter.getNameFromFileName(folderName);
		final Concept markdownFolder = ontology.getOrCreateConcept(this, type, cleanedName, false);

		if (markdownFolder != null) {

			final AnnotatedConcept annotatedConcept = new AnnotatedConcept(markdownFolder);

			// baseResource can be null in the tests
			if (baseResource != null) {

				final Resource docsResource = baseResource.resolve("docs");
				final Path folderPath       = docsResource.resolve(folderName).getPath();
				final Path indexFile        = folderPath.resolve("index.txt");

				// resolve markdown folder contents and add them as topics
				if (Files.exists(indexFile)) {

					try {
						final List<String> files = Files.readAllLines(indexFile);
						for (final String file : files) {

							final Token token                  = identifierToken.getToken().copy(folderName + "/" + file);
							final MarkdownFileToken fileToken  = new MarkdownFileToken(new ConceptToken(ConceptType.MarkdownFile, null), new IdentifierToken(token));
							final AnnotatedConcept fileConcept = fileToken.resolve(ontology);

							if (fileConcept != null) {

								ontology.createSymmetricLink(markdownFolder, Verb.Has, fileConcept.getConcept());
							}
						}

					} catch (IOException ioex) {
						ioex.printStackTrace();
					}
				}
			}

			return annotatedConcept;

		} else {

			System.out.println("Folder " + cleanedName + " not created, probably blacklisted..");
		}

		return null;
	}
}
