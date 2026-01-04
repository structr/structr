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
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.docs.*;
import org.structr.docs.Formatter;
import org.structr.docs.analyzer.ExistingDocs;
import org.structr.docs.ontology.parser.rule.*;
import org.structr.docs.ontology.parser.token.FactToken;
import org.structr.docs.ontology.parser.token.Token;
import org.structr.docs.ontology.parser.token.UnresolvedToken;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * The Structr Documentation Ontology.
 */
public final class Ontology {

	private final List<Concept> concepts = new LinkedList<>();
	private final Set<String> blacklist  = new LinkedHashSet<>();
	private Concept currentSubject = null;

	public Map<String, String> getKnownVerbs() {

		final Map<String, String> verbs = new LinkedHashMap<>();

		// verbs must be defined in lower case!
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
		verbs.put("executes",   "isexecutedby");

		// symmetrical links
		verbs.put("matches",    "matches");
		verbs.put("is",         "is");

		return verbs;
	}

	public Set<String> getBlacklist() {
		return blacklist;
	}

	public Set<String> getConjunctions() {
		return Set.of(",", "and");
	}

	public Ontology(final Path pathToFactsFolder) {

		this();

		initialize(pathToFactsFolder);
		initializeFromDocumentationAnnotations();

		// link concepts with the same text
		/*
		for (final Concept concept1 : concepts) {

			for (final Concept concept2 : concepts) {

				if (concept1.getName().toLowerCase().equals(concept2.getName().toLowerCase())) {

					if (concept1 != concept2 && !concept1.hasChild("matches", concept2) && !concept2.hasChild("matches", concept1)) {

						concept1.linkChild("matches", concept2);
					}
				}

			}
		}
		*/
	}

	/**
	 * Constructor for testing only..
	 *
	 * @param sourceFile
	 * @param facts
	 */
	public Ontology(final String sourceFile, final List<String> facts) {

		this();

		int lineNumber = 1;

		for (String line : facts) {

			storeFact(sourceFile, line, lineNumber++);
		}
	}

	public Ontology() {

		blacklist.addAll(Set.of("!", ";", ".", "the", "a", "an", "named"));
	}

	/**
	 * Constructor for testing only..
	 *
	 * @param line
	 */
	public Ontology(final String sourceFile, final String line) {

		this();

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

		reduce(sourceFile, lineNumber, line, tokens);

		for (final Token token : tokens) {

			token.resolve(this, sourceFile, lineNumber);
		}
	}

	public void reduce(final String fileName, final int line, final String source, final Deque<Token> tokens) {

		final List<Rule> rules = new LinkedList<>();
		int count = 0;

		rules.add(new RemoveUnwantedTokensRule(this));
		rules.add(new IdentifyPrepositionsRule(this));
		rules.add(new IdentifyAnaphoricPronounRule(this));
		rules.add(new IdentifyConjunctionsRule(this));
		rules.add(new IdentifyConceptsRule(this));
		rules.add(new ResolveConceptPairsRule(this));
		rules.add(new IdentifyVerbsRule(this));
		rules.add(new IdentifyNamesRule(this));
		rules.add(new IdentifyListsRule(this));
		rules.add(new CombineConceptAndIdentifierRule(this));
		rules.add(new FindUnknownIdentifiersRule(this));
		rules.add(new CombineNamedConceptsAndPrepositionsRule(this));
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

	public Concept getConcept(final ConceptType type, final String name) {

		for  (final Concept concept : concepts) {

			if (concept.name.equals(name)) {

				if (concept.type.equals(type) || ConceptType.Unknown.equals(type) || ConceptType.Unknown.equals(concept.type)) {

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

	public Concept getOrCreateConcept(final String sourceFile, final int line, final ConceptType type, final String name, final boolean useExisting) {

		if (blacklist.contains(name)) {
			return null;
		}

		for (final Concept concept : concepts) {

			if (concept.isSame(name, type, sourceFile, line) && useExisting) {

				// set correct type
				if (ConceptType.Unknown.equals(concept.type) && !ConceptType.Unknown.equals(type)) {
					concept.type = type;
				}

				concept.getOccurrences().add(new Occurrence(sourceFile, line));

				return concept;
			}
		}

		final Concept concept = new Concept(sourceFile, line, type, name);

		concepts.add(concept);

		return concept;
	}

	public Concept getCurrentSubject() {
		return currentSubject;
	}

	public void setCurrentSubject(final Concept subject) {
		this.currentSubject = subject;
	}

	public void countConcepts(final ExistingDocs existingDocs) {

		for (final Concept concept : concepts) {

			final List<Occurrence> mentions = existingDocs.getMentions(concept.getName());

			for (final Concept synonym : concept.getChildrenOfType("has", ConceptType.Synonym)) {

				mentions.addAll(existingDocs.getMentions(synonym.getName()));
			}

			concept.setMentions(mentions);
		}
	}

	// ----- private methods -----
	private void initialize(final Path path) {

		try (final Stream<Path> files = Files.walk(path).filter(Files::isRegularFile).sorted()) {

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

	private void initializeFromDocumentationAnnotations() {

		for (final Map.Entry<Class, List<Documentation>> entry : StructrApp.getConfiguration().getDocumentationAnnotations().entrySet()) {

			final Class clazz                       = entry.getKey();
			final List<Documentation> documentation = entry.getValue();

			for (final Documentation doc : documentation) {

				initializeFromDocumentationAnnotations(clazz, doc, clazz.getSimpleName(), null, null);
			}
		}
	}

	private void initializeFromDocumentationAnnotations(final Class clazz, final Documentation documentation, final String typeName, final String itemName, final Concept parentConcept) {

		final String sourceFile = "@Documentation on " + typeName;

		// collect info from annotation and import it into the ontology
		final ConceptType type  = documentation.type();
		final String name       = coalesce(documentation.name(), itemName);
		final String desc       = documentation.shortDescription();
		final String parent     = documentation.parent();
		final String[] synonyms = documentation.synonyms();
		final String[] children = documentation.children();

		// allow this getOrCreate call to find existing concepts so we can combine concepts from different @Documentation annotations
		final Concept concept = getOrCreateConcept(sourceFile, 0, type, name, true);
		if (concept != null) {

			concept.setShortDescription(desc);

			if (children != null && children.length > 0) {

				for (final String child : children) {

					if (StringUtils.isNotBlank(child)) {

						final Concept childConcept = getOrCreateConcept(sourceFile, 0, ConceptType.Topic, child, false);
						if (childConcept != null) {

							concept.linkChild("has", childConcept);
						}
					}
				}
			}

			if (synonyms != null && synonyms.length > 0) {

				for (final String synonym : synonyms) {

					if (StringUtils.isNotBlank(synonym)) {

						final Concept synonymConcept = getOrCreateConcept(sourceFile, 0, ConceptType.Synonym, synonym, false);
						if (synonymConcept != null) {

							concept.linkChild("has", synonymConcept);
						}
					}
				}
			}

			if (StringUtils.isNotBlank(parent)) {

				final Concept p = getOrCreateConcept(sourceFile, 0, ConceptType.Unknown, parent, true);
				if (p != null) {

					p.linkChild("has", concept);
				}

			} else if (parentConcept != null) {

				parentConcept.linkChild("has", concept);
			}

			if (clazz != null) {

				// import properties and important methods from system types, maybe longDescription as well...?
				if (AbstractNodeTraitDefinition.class.isAssignableFrom(clazz)) {

					try {

						final Constructor constructor = clazz.getConstructor();
						if (constructor != null) {

							final AbstractNodeTraitDefinition def = (AbstractNodeTraitDefinition) constructor.newInstance();
							if (def != null) {

								final String dynamicTypeName = def.getName();
								if (Traits.exists(dynamicTypeName)) {

									final Traits traits = Traits.of(dynamicTypeName);

									// collect properties here
									for (final DocumentedProperty property : traits.getDocumentedProperties()) {

										final Concept propertyConcept = getOrCreateConcept(sourceFile, 0, ConceptType.Property, property.getName(), false);
										if (propertyConcept != null) {

											concept.linkChild("has", propertyConcept);
										}
									}
								}
							}

						} else {

							System.out.println("Cannot instantiate " + clazz.getSimpleName() + " because it has no no-args constructor.");
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}

				}

				// import enum constants as well
				if (clazz.isEnum()) {

					for (final Object enumConstant : clazz.getEnumConstants()) {

						if (enumConstant instanceof Documentable documentable) {

							final String childName = documentable.getDisplayName();
							if (childName != null) {

								final DocumentableType documentableType = documentable.getDocumentableType();
								final Concept childConcept = getOrCreateConcept(sourceFile, 0, documentableType.getConcept(), childName, false);

								if (childConcept != null) {

									if (documentable.getShortDescription() != null) {
										childConcept.setShortDescription(documentable.getShortDescription());
									}

									concept.linkChild("has", childConcept);
								}
							}
						}
					}
				}

				// examine static fields etc.
				for (final Field field : clazz.getDeclaredFields()) {

					final Documentations fieldAnnotation = field.getAnnotation(Documentations.class);
					if (fieldAnnotation != null) {

						for (final Documentation doc : fieldAnnotation.value()) {

							// recurse
							initializeFromDocumentationAnnotations(null, doc, typeName + "." + field.getName(), field.getName(), concept);
						}
					}

					final Documentation doc = field.getAnnotation(Documentation.class);
					if (doc != null) {

						// recurse
						initializeFromDocumentationAnnotations(null, doc, typeName + "." + field.getName(), field.getName(), concept);
					}
				}
			}
		}
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