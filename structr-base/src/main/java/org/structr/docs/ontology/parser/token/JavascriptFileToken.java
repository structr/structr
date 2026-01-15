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

import org.apache.commons.lang3.StringUtils;
import org.structr.core.Services;
import org.structr.docs.ontology.*;
import org.structr.module.StructrModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An identifier that is augmented with a type so we know what it is.
 */
public class JavascriptFileToken extends NamedConceptToken {

	private final Pattern HTMLLine      = Pattern.compile("<[a-zA-Z0-9_\\p{Punct} ]+>.*");
	private final Pattern JSONToken     = Pattern.compile("\\s*([a-zA-Z0-9_]+)\\s*:\\s*[\"']{1}([a-zA-Z0-9_ !$%&/\\(\\)\\?]+)\\s*[\\p{Punct}]*");
	private final Set<String> stopWords = new LinkedHashSet<>();
	private final int minLength         = 4;
	private final int maxLength         = 40;

	public JavascriptFileToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {

		super(conceptToken, identifierToken);

		// fetch stop words from text search module..
		final StructrModule textSearchModule = Services.getInstance().getConfigurationProvider().getModules().get("text-search-module");
		if (textSearchModule != null) {

			try {

				final Set<String> stopWordsFromModule = textSearchModule.getModuleSpecificFeature("stopwords", Set.class, "en");
				if (stopWordsFromModule != null) {

					stopWords.addAll(stopWordsFromModule);
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public boolean isUnknown() {
		return "unknown".equals(conceptToken.getToken());
	}

	@Override
	public AnnotatedConcept resolve(final Ontology ontology) {

		final String fileName     = identifierToken.resolve(ontology);
		final Concept mainConcept = ontology.getOrCreateConcept(this, ConceptType.JavascriptFile, fileName, false);
		final Path path           = Path.of(fileName);

		// resolve markdown folder contents and add them as topics
		if (Files.exists(path)) {

			try {
				final List<String> lines  = Files.readAllLines(path);
				int sourceLineNumber      = 1;
				boolean inHtml            = false;

				for (final String rawLine : lines) {

					final String line    = rawLine.trim();
					final boolean isHtml = isHtmlLine(line);
					final boolean isJson = isJSONLine(line);

					if (isHtml || isJson || inHtml) {

						inHtml = isHtml;

						String text = line;

						if (isJson) {

							final Matcher matcher = JSONToken.matcher(line);
							if (matcher.matches()) {

								text = matcher.group(2);

								if (StringUtils.isNumeric(text) || text.length() < minLength || text.matches("'[0-9]+[a-z]+'")) {
									continue;
								}
							}
						}

						text = text.replace("\\$", "$");
						text = parseAndRemoveStyleAttribute(text);
						text = parseAndRemoveDataComment(text, ontology, fileName, sourceLineNumber);
						text = text.replaceAll("\\$\\{.*\\}", "");
						text = text.replaceAll("<[^>]*>", "").trim().replaceAll("\\s+", " ");

						final Concept concept = handleConcept(ConceptType.Text, text, ontology);
						if (concept != null) {

							ontology.createSymmetricLink(mainConcept, Verb.Has, concept);
						}
					}

					sourceLineNumber++;
				}

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}

		return new AnnotatedConcept(mainConcept);
	}

	// ----- private methods -----
	private boolean isHtmlLine(final String line) {

		// any sort of HTML tag
		final Matcher htmlMatcher = HTMLLine.matcher(line);

		return htmlMatcher.matches();
	}

	private boolean isJSONLine(final String line) {

		// key: value (and nothing else)
		final Matcher jsonMatcher = JSONToken.matcher(line);

		return jsonMatcher.matches();
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

		if (text.contains("&#") || text.contains("&&") || text.contains("||")) {
			return true;
		}

		if (text.contains("===") || text.contains("==") || text.contains("---")) {
			return true;
		}

		if (text.contains("&mdash")) {
			return true;
		}

		return false;
	}

	private Concept handleConcept(final ConceptType type, final String text, final Ontology ontology) {

		final String trimmed = text.trim();
		if (StringUtils.isNotBlank(trimmed)) {

			final String cleaned = clean(trimmed);
			if (StringUtils.isNotBlank(cleaned)) {

				final int len = cleaned.length();

				if (StringUtils.isNotBlank(cleaned) && len >= minLength && len <= maxLength && !isCode(cleaned)) {

					final Concept concept = ontology.getOrCreateConcept(this, type, cleaned, false);
					if (concept != null) {

						for (final NamedConceptToken additionalNamedConcept : getAdditionalNamedConcepts()) {

							final AnnotatedConcept additionalConcept = additionalNamedConcept.resolve(ontology);
							if (additionalConcept != null) {

								if (!concept.equals(additionalConcept)) {

									// FIXME: annotate link here!
									ontology.createSymmetricLink(concept, Verb.Has, additionalConcept.getConcept());
								}
							}
						}

						return concept;
					}
				}
			}
		}

		return null;
	}

	private String parseAndRemoveDataComment(final String line, final Ontology ontology, final String fileName, final int sourceLineNumber) {

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

				// ignore comments for now
				//handleConcept("comment", comment.toString(), ontology, fileName, sourceLineNumber);
			}

			return lineBuffer.toString();
		}

		return line;
	}

	private String parseAndRemoveStyleAttribute(final String line) {

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

	private String clean(final String name) {

		if (StringUtils.isNumeric(name)) {
			return null;
		}

		String cleaned     = StringUtils.stripToEmpty(name);
		boolean hasChanged = true;

		// clean prefix
		while (cleaned.length() > 0 && hasChanged) {

			hasChanged = false;

			final char first = cleaned.charAt(0);

			if (!Character.isAlphabetic(first) && !Character.isDigit(first) && first != '(') {

				cleaned = cleaned.substring(1);
				hasChanged = true;
			}
		}

		// start again
		hasChanged = true;

		// clean suffix
		while (cleaned.length() > 0 && hasChanged) {

			int len = cleaned.length();

			hasChanged = false;

			final char last = cleaned.charAt(len - 1);

			if (!Character.isAlphabetic(last) && !Character.isDigit(last) && last != ')') {

				cleaned = cleaned.substring(0, len - 1);
				hasChanged = true;
			}
		}

		return cleaned;
	}
}
