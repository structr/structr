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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.Predicate;
import org.structr.core.function.Functions;
import org.structr.docs.Formatter;
import org.structr.docs.OutputSettings;
import org.structr.docs.ontology.parser.rule.*;
import org.structr.docs.ontology.parser.token.FactToken;
import org.structr.docs.ontology.parser.token.Token;
import org.structr.docs.ontology.parser.token.UnresolvedToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * The Structr Documentation Ontology.
 */
public final class Ontology {

	private final List<Concept> concepts = new LinkedList<>();
	private Concept currentSubject = null;

	public Set<String> getKnownConcepts() {
		return Set.of(

			// structural concepts
			"topic", "concept", "component", "feature", "mechanism", "provider", "service", "capability",
			"use-case", "type",

			// external sources
			"markdown-folder", "markdown-file", "code-source", "javascript-file",

			// concepts for user interface elements
			"screen", "form", "area", "tab", "flyout", "menu", "dialog", "link", "input", "textarea",
			"button", "checkbox", "dropdown", "selector", "list", "table", "row", "notification", "element",
			"icon",

			// concepts for backend elements
			"logfile", "value",

			// metadata
			"hint", "note", "description", "info", "setting", "configuration", "synonym"
		);
	}

	public Map<String, String> getKnownVerbs() {

		final Map<String, String> verbs = new LinkedHashMap<>();

		// verbs must be defined in lower case!
		verbs.put("is",         "is");
		verbs.put("has",        "ispartof");
		verbs.put("uses",       "isusedby");
		verbs.put("provides",   "isprovidedby");
		verbs.put("opens",      "isopenedby");
		verbs.put("closes",     "isclosedby");
		verbs.put("contains",   "iscontainedby");
		verbs.put("creates",    "iscreatedby");
		verbs.put("removes",    "isremovedby");
		verbs.put("deletes",    "isdeletedby");
		verbs.put("configures", "isconfiguredby");
		verbs.put("displays",   "isdisplayedby");
		verbs.put("writesto",   "iswrittenfrom");

		return verbs;
	}

	public Set<String> getBlacklist() {
		return Set.of("!", ";", ".", "the", "a", "an", "with", "named");
	}

	public Set<String> getConjunctions() {
		return Set.of(",", "and");
	}

	public Ontology(final Path pathToFactsFolder) {
		initialize(pathToFactsFolder);
	}

	public Ontology(final String sourceFile, final List<String> facts) {

		int lineNumber = 1;

		for (String line : facts) {

			storeFact(sourceFile, line, lineNumber++);
		}
	}

	/**
	 * Constructs an ontology from a single fact. This method exists
	 * to facilitate testing.
	 * @param line
	 */
	public Ontology(final String sourceFile, final String line) {
		storeFact(sourceFile, line, 1);
	}

	public List<String> createDocumentation(final List<Concept> concepts, final OutputSettings outputSettings) {

		final List<String> lines = new LinkedList<>();

		for (final Concept concept : concepts) {

			Formatter.walkOntology(lines, concept, outputSettings, null, 0, new LinkedHashSet<>());
		}

		return lines;
	}

	public List<Concept> getAllConcepts() {
		return concepts;
	}

	public List<Concept> getConcepts(final Predicate<Concept> predicate) {

		final List<Concept> result = new LinkedList<>();

		for (final Concept concept : concepts) {

			if (predicate.accept(concept)) {

				result.add(concept);
			}
		}

		return result;
	}

	public List<Concept> getRootConcepts() {

		final List<Concept> rootConcepts = new LinkedList<>();

		for (final Concept concept : concepts) {

			if (concept.getParents().isEmpty()) {

				rootConcepts.add(concept);
			}
		}

		return rootConcepts;
	}

	public Set<String> getOutgoingLinkTypes() {

		final Set<String> linkTypes = new LinkedHashSet<>();

		for (final Concept concept : concepts) {

			linkTypes.addAll(concept.getChildren().keySet());
		}

		return linkTypes;
	}

	public void storeFacts(final String sourceFile, final String fact) {

		int lineNumber = 1;

		for (final String line : fact.split("[\\n\\r]+")) {

			storeFact(sourceFile, line, lineNumber++);
		}
	}

	public void storeFact(final String sourceFile, final String line, final int lineNumber) {

		// skip empty lines
		if (StringUtils.isBlank(line)) {
			return;
		}

		// skip lines that start with # (comment character, can be changed if it interferes with Markdown...)
		if (line.trim().startsWith("#")) {
			return;
		}

		final List<String> input  = Functions.tokenize(line, true);
		final Deque<Token> tokens = new LinkedList<>();

		for (final String token : input) {

			tokens.add(new UnresolvedToken(token));
		}

		try {

			reduce(sourceFile, lineNumber, line, tokens);

		} catch (Throwable t) {

			System.out.println("Exception while parsing " + sourceFile + ":" + lineNumber + ": " + t.getMessage());
			t.printStackTrace();
		}

		for (final Token token : tokens) {

			token.resolve(this, sourceFile, lineNumber);
		}
	}

	public void reduce(final String fileName, final int line, final String source, final Deque<Token> tokens) {

		final List<Rule> rules = new LinkedList<>();
		int count = 0;

		rules.add(new RemoveUnwantedTokensRule(this));
		rules.add(new IdentifyAnaphoricPronounRule(this));
		rules.add(new IdentifyConjunctionsRule(this));
		rules.add(new IdentifyConceptsRule(this));
		rules.add(new ResolveConceptPairsRule(this));
		rules.add(new IdentifyVerbsRule(this));
		rules.add(new IdentifyNamesRule(this));
		rules.add(new IdentifyListsRule(this));
		rules.add(new CombineConceptAndIdentifierRule(this));
		rules.add(new FindUnknownIdentifiersRule(this));
		rules.add(new IdentifyFactsRule(this));
		rules.add(new ResolveIsARelationsRule(this));

		while (count++ < 100) {

			for (final Rule rule : rules) {

				rule.apply(tokens);
			}

			// all reduced
			if (tokens.size() == 1) {
				return;
			}

			// more than one token left => error
			if (tokens.stream().noneMatch(token -> token.isUnresolved())) {
				break;
			}
		}

		final List<Token> remainingTokens = new LinkedList<>();
		for (final Token token : tokens) {

			if (token instanceof FactToken) {

				// ignore

			} else {

				remainingTokens.add(token);
			}
		}

		throw new RuntimeException("Syntax error in " + fileName + ":" + line + ": unable to parse " + source + " into facts: remaining tokens are " + remainingTokens + ". This can happen if an identifier matches a known concept, e.g. the topic \"Settings\" and the concept \"settings\". It might help to prefix the identifier with the concept, e.g. write \"topic Settings has code-source settings\" instead of \"Settings has code-source settings\".");
	}

	public Concept getConcept(final String type, final String name) {

		for  (final Concept concept : concepts) {

			if (concept.name.equals(name)) {

				if (concept.type.equals(type) || type.equals("unknown") || concept.type.equals("unknown")) {

					return concept;
				}
			}
		}

		return null;
	}

	public List<Concept> getConceptsByName(final String name) {

		final List<Concept> result = new LinkedList<>();

		for  (final Concept concept : concepts) {

			if (concept.name.equals(name)) {

				result.add(concept);
			}
		}

		return result;
	}

	public Concept getOrCreateConcept(final String sourceFile, final int line, final String type, final String name) {

		for  (final Concept concept : concepts) {

			if (concept.name.equals(name)) {

				if (concept.type.equals(type) || type.equals("unknown") || concept.type.equals("unknown")) {

					// set correct type
					if ("unknown".equals(concept.type) && !"unknown".equals(type)) {
						concept.type = type;
					}

					return concept;
				}
			}
		}

		final Concept concept = new Concept(sourceFile, line, type, name);

		concepts.add(concept);

		return concept;
	}

	public Concept getCurrentSubject() {
		return currentSubject;
	}

	// ----- private methods -----
	private void initialize(final Path path) {

		try (final Stream<Path> files = Files.walk(path).filter(Files::isRegularFile)) {

			files.forEach(file -> {

				try {
					final String fileName = file.getFileName().toString();
					int lineNumber = 1;

					for (final String line : Files.readAllLines(file)) {

						storeFact(fileName, line, lineNumber++);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		} catch (IOException ioex) {

			ioex.printStackTrace();
		}
	}

	public void setCurrentSubject(final Concept subject) {
		this.currentSubject = subject;
	}
}