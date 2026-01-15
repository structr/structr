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
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.formatter.markdown.MarkdownMarkdownFileFormatter;
import org.structr.docs.ontology.*;

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
		final String folderName      = identifierToken.resolve(ontology);
		final String cleanedName     = MarkdownMarkdownFileFormatter.getNameFromFileName(folderName);
		final Concept markdownFolder = ontology.getOrCreateConcept(this, type, cleanedName, false);
		final Path folderPath        = Path.of("structr/docs/" + folderName);
		final Path indexFile         = folderPath.resolve("index.txt");

		if (markdownFolder != null) {

			final AnnotatedConcept annotatedConcept = new AnnotatedConcept(markdownFolder);

			// resolve markdown folder contents and add them as topics
			if (Files.exists(indexFile)) {

				try {
					final List<String> files = Files.readAllLines(indexFile);
					for (final String file : files) {

						final String cleanedFileName = MarkdownMarkdownFileFormatter.getNameFromFileName(file);
						final Concept markdownFile = ontology.getOrCreateConcept(this, ConceptType.MarkdownFile, cleanedFileName, false);

						if (markdownFile != null) {

							final String filePath = folderName + "/" + file;

							markdownFile.getMetadata().put("path", filePath);

							ontology.createSymmetricLink(markdownFolder, Verb.Has, markdownFile);

							// handle children
							final List<String> lines = Files.readAllLines(folderPath.resolve(file));
							final MutableDataSet options = new MutableDataSet();

							options.setAll(PegdownOptionsAdapter.flexmarkOptions(false, Extensions.ALL));

							final Parser parser         = Parser.builder(options).build();
							final Document doc          = parser.parse(StringUtils.join(lines, "\n"));

							for (final Node child : doc.getChildren()) {

								if (child instanceof Heading heading) {

									final int level   = heading.getLevel();
									final String text = heading.getText().unescape();

									if (level == 2) {

										final Concept headingConcept = ontology.getOrCreateConcept(this, ConceptType.MarkdownHeading, text, false);
										if (headingConcept != null) {

											ontology.createSymmetricLink(markdownFile, Verb.Has, headingConcept);
										}
									}
								}
							}

							// store markdown content in concept
							markdownFile.getMetadata().put("content", StringUtils.join(lines, "\n"));

						} else {

							System.out.println("Markdown file " + cleanedFileName + " not created, probably blacklisted..");
						}
					}

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}

			return annotatedConcept;

		} else {

			System.out.println("Folder " + cleanedName + " not created, probably blacklisted..");
		}

		return null;
	}
}
