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

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;
import org.structr.docs.formatter.MarkdownMarkdownFileFormatter;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An identifier that is augmented with a type so we know what it is.
 */
public class JavascriptFileToken extends NamedConceptToken {

	public JavascriptFileToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {

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
	public List<Concept> resolve(final Ontology ontology, final String sourceFile, final int lineNumber) {

		final List<String> identifiers = identifierToken.resolve(ontology, sourceFile, lineNumber);
		final List<Concept> concepts   = new LinkedList<>();

		for (final String fileName : identifiers) {

			final Path path = Path.of(fileName);

			// resolve markdown folder contents and add them as topics
			if (Files.exists(path)) {

				try {
					final List<String> lines  = Files.readAllLines(path);
					final List<String> tokens = new LinkedList<>();
					int sourceLineNumber      = 1;
					boolean inHtml             = false;

					for (final String rawLine : lines) {

						final String line    = rawLine.trim();
						final boolean isHtml = isHtmlLine(line);

						if (isHtml || inHtml) {

							inHtml = isHtml;

							String text = line;

							text = text.replaceAll("\\$\\{.*\\}", "");
							text = text.replaceAll("<[^>]*>", "").trim().replaceAll("\\s+", " ");

							final String trimmed = text.trim();

							if (StringUtils.isNotBlank(trimmed) && !trimmed.contains("$")) {

								tokens.add(trimmed);
								ontology.getOrCreateConcept(fileName, sourceLineNumber, "unknown", trimmed);
							}

							parseDataComment(line, tokens, ontology, fileName, sourceLineNumber);

							//FIXME: remove data-comment after parsing?!
						}

						sourceLineNumber++;
					}

					for (final String token : tokens) {
						System.out.println(token);
					}

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}
		}

		return concepts;
	}

	// ----- private methods -----
	private boolean isHtmlLine(final String line) {
		return line.matches("<[a-zA-Z0-9_\\p{Punct} ]+>.*");
	}

	private void parseDataComment(final String line, final List<String> tokens, final Ontology ontology, final String fileName, final int sourceLineNumber) {

		// parse data-comment
		if (line.contains("data-comment=")) {

			int start = 0;

			int pos = line.indexOf("data-comment=", start) + 14;
			if (pos > 0) {

				StringBuilder comment = new StringBuilder();
				char quote            = line.charAt(pos - 1);
				char c                = line.charAt(pos);

				while (c != quote) {

					comment.append(c);
					pos++;

					// skip escaped chars
					if (c == '\\') {
						comment.append(line.charAt(pos));
						pos++;
					}

					c = line.charAt(pos);
				}

				tokens.add(comment.toString());

				ontology.getOrCreateConcept(fileName, sourceLineNumber, "data-comment", comment.toString());
			}
		}
	}
}
