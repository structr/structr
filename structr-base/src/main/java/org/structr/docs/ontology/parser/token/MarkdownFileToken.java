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

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profile.pegdown.Extensions;
import com.vladsch.flexmark.profile.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.docs.formatter.markdown.MarkdownMarkdownFileFormatter;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Ontology;
import org.structr.docs.ontology.Verb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An identifier that is augmented with a type so we know what it is.
 */
public class MarkdownFileToken extends NamedConceptToken {

	public MarkdownFileToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {

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

		final List<String> identifiers = identifierToken.resolve(ontology, sourceFile, line);
		final List<Concept> concepts   = new LinkedList<>();

		for (final String path : identifiers) {

			final Map<String, String> metadata = getMetadata(ontology, sourceFile, line);
			final String fileName              = StringUtils.substringAfterLast(path, "/");
			final String cleanedName           = coalesce(metadata.get("heading"), MarkdownMarkdownFileFormatter.getNameFromFileName(fileName));
			final Path folderPath              = Path.of("structr/docs/" + path);
			final Concept markdownFile         = ontology.getOrCreateConcept(fileName, line, ConceptType.MarkdownFile, cleanedName, false);

			if (markdownFile != null) {

				markdownFile.getMetadata().put("path", path);

				concepts.add(markdownFile);

				try {

					// handle children
					final List<String> lines = Files.readAllLines(folderPath);
					final MutableDataSet options = new MutableDataSet();

					options.setAll(PegdownOptionsAdapter.flexmarkOptions(false, Extensions.ALL));

					final Parser parser = Parser.builder(options).build();
					final Document doc = parser.parse(StringUtils.join(lines, "\n"));

					for (final Node child : doc.getChildren()) {

						if (child instanceof Heading heading) {

							final int level = heading.getLevel();
							final String text = heading.getText().unescape();

							if (level == 2) {

								final Concept headingConcept = ontology.getOrCreateConcept(sourceFile, line, ConceptType.MarkdownHeading, text, false);
								if (headingConcept != null) {

									markdownFile.createSymmetricLink(Verb.Has, headingConcept);
								}
							}
						}
					}

					// store markdown content in concept
					markdownFile.getMetadata().put("content", StringUtils.join(lines, "\n"));

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}
		}

		return concepts;
	}

	// ----- private methods -----
	private Map<String, String> getMetadata(final Ontology ontology, final String sourceFile, final int line) {

		final Map<String, String> metadata = new LinkedHashMap<>();

		// additional named concepts go into metadata of a concept (for now..)
		for (final NamedConceptToken additionalNamedConcept : additionalNamedConcepts) {

			final List<Concept> additionalConcepts = additionalNamedConcept.resolve(ontology, sourceFile, line);
			for (final Concept additionalConcept : additionalConcepts) {

				metadata.put(additionalConcept.getType().getIdentifier(), additionalConcept.getName());
			}
		}

		return metadata;
	}

	private String coalesce(final String... strings) {

		for (final String string : strings) {

			if (StringUtils.isNotBlank(string)) {

				return string;
			}
		}

		return null;
	}
}
