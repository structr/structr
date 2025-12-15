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
public class JavascriptFileToken extends NamedConceptToken {

	private final int minLength = 4;

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

							text = text.replace("\\$", "$");
							text = parseAndRemoveStyleAttribute(text, tokens, ontology, fileName, sourceLineNumber);
							text = parseAndRemoveDataComment(text, tokens, ontology, fileName, sourceLineNumber);
							text = text.replaceAll("\\$\\{.*\\}", "");
							text = text.replaceAll("<[^>]*>", "").trim().replaceAll("\\s+", " ");

							handleConcept("unknown", text, tokens, ontology, fileName, sourceLineNumber);
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

	private boolean isCode(final String text) {

		if (text.contains("=>") && text.contains("{") && text.contains(" ")) {
			return true;
		}

		if (text.contains("'") && (text.contains("{") || text.contains("}"))) {
			return true;
		}

		if (text.contains("('')") || text.contains(");") || text.contains(")}")) {
			return true;
		}

		if (text.contains("&#")) {
			return true;
		}

		if (text.contains("===") || text.contains("==")) {
			return true;
		}

		return false;
	}

	private void handleConcept(final String type, final String text, final List<String> tokens, final Ontology ontology, String fileName, final int sourceLineNumber) {

		final String trimmed = text.trim();

		if (StringUtils.isNotBlank(trimmed) && trimmed.length() >= minLength && !isCode(trimmed)) {

			tokens.add(fileName + ":" + sourceLineNumber + ": " + type + ": " + trimmed);

			final Concept concept = ontology.getOrCreateConcept(fileName, sourceLineNumber, type, trimmed);

			for (final NamedConceptToken additionalNamedConcept : getAdditionalNamedConcepts()) {

				for (final Concept additionalConcept : additionalNamedConcept.resolve(ontology, fileName, sourceLineNumber)) {

					if (!concept.equals(additionalConcept) && !concept.hasChild("has", additionalConcept)) {

						concept.linkChild("has", additionalConcept);
					}
				}
			}
		}
	}

	private String parseAndRemoveDataComment(final String line, final List<String> tokens, final Ontology ontology, final String fileName, final int sourceLineNumber) {

		// parse data-comment
		if (line.contains("data-comment=")) {

			final StringBuilder lineBuffer = new StringBuilder();

			int pos = line.indexOf("data-comment=") + 14;
			if (pos > 0 && pos < line.length()) {

				lineBuffer.append(line.substring(0, pos));

				StringBuilder comment = new StringBuilder();
				char quote = line.charAt(pos - 1);
				char c = line.charAt(pos);

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

				lineBuffer.append(line.substring(pos));

				handleConcept("comment", comment.toString(), tokens,  ontology, fileName, sourceLineNumber);
			}

			return lineBuffer.toString();
		}

		return line;
	}

	private String parseAndRemoveStyleAttribute(final String line, final List<String> tokens, final Ontology ontology, final String fileName, final int sourceLineNumber) {

		// parse data-comment
		if (line.contains("style=")) {

			final StringBuilder lineBuffer = new StringBuilder();

			int pos = line.indexOf("style=") + 7;
			if (pos > 0 && pos < line.length()) {

				lineBuffer.append(line.substring(0, pos));

				StringBuilder comment = new StringBuilder();
				char quote = line.charAt(pos - 1);
				char c = line.charAt(pos);

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

				lineBuffer.append(line.substring(pos));
			}

			return lineBuffer.toString();
		}

		return line;
	}
}
