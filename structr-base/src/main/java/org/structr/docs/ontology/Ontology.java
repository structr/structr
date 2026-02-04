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
package org.structr.docs.ontology;

import groovyjarjarantlr4.runtime.BaseRecognizer;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.resource.Resource;
import org.structr.api.Predicate;
import org.structr.core.app.StructrApp;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.docs.*;
import org.structr.docs.Formatter;
import org.structr.docs.analyzer.ExistingDocs;
import org.structr.docs.ontology.parser.rule.*;
import org.structr.docs.ontology.parser.token.DocumentationAnnotationToken;
import org.structr.docs.ontology.parser.token.AbstractToken;
import org.structr.docs.ontology.parser.token.UnresolvedToken;

import javax.sql.rowset.BaseRowSet;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * The Structr Documentation Ontology.
 */
public final class Ontology {

	private final Set<ConceptType> SearchBlacklist    = EnumSet.of(ConceptType.Text, ConceptType.Heading, ConceptType.MarkdownHeading);
	private final List<FactsContainer> factContainers = new LinkedList<>();
	private final AtomicLong idGenerator              = new AtomicLong();
	private final List<Concept> concepts              = new LinkedList<>();
	private final Set<Link> links                     = new LinkedHashSet<>();
	private final Set<String> blacklist               = new LinkedHashSet<>();
	private Concept currentSubject                    = null;
	private final Resource baseResource;

	public Set<String> getBlacklist() {
		return blacklist;
	}

	public Set<String> getConjunctions() {
		return Set.of(",", "and");
	}

	public Ontology(final Resource baseResource, final Path pathToFactsFolder) {

		this(baseResource);

		initialize(pathToFactsFolder);
		initializeFromDocumentationAnnotations();
	}

	/**
	 * Constructor for testing only..
	 *
	 * @param facts
	 */
	public Ontology(FactsContainer facts) {

		this((Resource)null);

		storeFacts(facts);
	}

	private Ontology(final Resource baseResource) {

		this.baseResource = baseResource;

		blacklist.addAll(Set.of("!", ";", ".", "the", "a", "an", "named"));
	}

	public List<String> createDocumentation(final List<Link> links, final OutputSettings outputSettings) {

		final List<String> lines = new LinkedList<>();

		for (final Link link : links) {

			Formatter.walkOntology(lines, link, outputSettings, 0, new LinkedHashSet<>());
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

			if (concept.getParentLinks().isEmpty()) {

				rootConcepts.add(concept);
			}
		}

		return rootConcepts;
	}

	public void storeFacts(final FactsContainer facts) {

		final Deque<AbstractToken> tokens = reduce(facts);

		for (final AbstractToken token : tokens) {

			token.resolve(this);
		}
	}

	public List<Concept> getConcept(final ConceptType type, final String name) {

		final List<Concept> result = new LinkedList<>();

		for  (final Concept concept : concepts) {

			if (concept.getName().equals(name)) {

				if (concept.type.equals(type) || ConceptType.Unknown.equals(type) || ConceptType.Unknown.equals(concept.type)) {

					result.add(concept);
				}
			}
		}

		return result;
	}

	public void searchConcepts(final Map<Concept, Double> result, final String searchString, final double scoreMultiplier) {

		// check exact match first
		for  (final Concept concept : concepts) {

			// do not show "text" results as they are extracted from Javascript files
			if (SearchBlacklist.contains(concept.type)) {
				continue;
			}

			// exact match yields a score of 100
			if (concept.getName() != null && concept.getName().equals(searchString)) {

				result.compute(concept, (k, v) -> add(v, scoreMultiplier * Concept.EXACT_MATCH_SCORE));
			}

			final Documentable documentable = concept.getDocumentable();
			if (documentable != null) {

				final double score = documentable.matches(searchString);

				if (score > 0) {

					result.compute(concept, (k, v) -> add(v, scoreMultiplier * score));
				}

			} else {

				final double score = concept.matches(searchString);
				if (score > 0) {

					result.compute(concept, (k, v) -> add(v, scoreMultiplier * score));
				}
			}
		}
	}

	public List<Concept> getConceptsByName(final String name) {

		final List<Concept> result = new LinkedList<>();

		for  (final Concept concept : concepts) {

			if (concept.getName().equals(name)) {

				result.add(concept);
			}
		}

		return result;
	}

	public Concept getOrCreateConcept(final AbstractToken token, final ConceptType type, final String name, final boolean useExisting) {

		if (blacklist.contains(name)) {
			return null;
		}

		for (final Concept concept : concepts.reversed()) {

			if (concept.isSame(name, type) && (ConceptType.Unknown.equals(type) || useExisting)) {

				// set correct type
				if (ConceptType.Unknown.equals(concept.type) && !ConceptType.Unknown.equals(type)) {
					concept.type = type;
				}

				concept.getTokens().add(token);

				return concept;
			}
		}

		final Concept concept = new Concept(this, token, type, name);

		concepts.add(concept);

		return concept;
	}

	public Link createSymmetricLink(final Concept subject, final Verb verb, final Concept object) {

		if (subject.equals(object)) {
			return null;
		}

		if (subject.hasChild(verb, object)) {
			return subject.getLinkTo(verb, object);
		}

		final Link link = new Link(subject, verb, object);

		links.add(link);

		return link;
	}

	public Map<Verb, List<Link>> getOutgoingLinks(final Concept source) {

		final Map<Verb, List<Link>> result = new LinkedHashMap<>();

		for (final Link link : links) {

			if (link.getSource().equals(source)) {

				result.computeIfAbsent(link.getVerb(), k -> new LinkedList<>()).add(link);
			}
		}

		return result;
	}

	public List<Link> getOutgoingLinks(final Concept source, final Verb verb) {

		final List<Link> result = new LinkedList<>();

		for (final Link link : links) {

			if (link.getSource().equals(source) && link.getVerb().equals(verb)) {
				result.add(link);
			}
		}

		return result;
	}

	public Map<Verb, List<Link>> getIncomingLinks(final Concept target) {

		final Map<Verb, List<Link>> result = new LinkedHashMap<>();

		for (final Link link : links) {

			if (link.getTarget().equals(target)) {

				result.computeIfAbsent(link.getVerb(), k -> new LinkedList<>()).add(link);
			}
		}

		return result;
	}

	public List<Link> getIncomingLinks(final Concept target, final Verb verb) {

		final List<Link> result = new LinkedList<>();

		for (final Link link : links) {

			if (link.getTarget().equals(target) && link.getVerb().equals(verb)) {
				result.add(link);
			}
		}

		return result;
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

			for (final Concept synonym : concept.getChildrenOfType(Verb.Has, ConceptType.Synonym)) {

				mentions.addAll(existingDocs.getMentions(synonym.getName()));
			}

			concept.setMentions(mentions);
		}
	}

	public Concept getConceptById(final String id) {

		final List<Concept> filtered = getConcepts(c -> c.getId().equals(id));
		if (!filtered.isEmpty()) {

			return filtered.get(0);
		}

		return null;
	}

	public void removeConcept(final Concept concept) {

		final List<Link> linksToRemove = new LinkedList<>();

		concepts.remove(concept);

		for (final Link link : links) {

			if (link.getTarget().equals(concept)) {
				linksToRemove.add(link);
			}

			if  (link.getSource().equals(concept)) {
				linksToRemove.add(link);
			}
		}

		links.removeAll(linksToRemove);
	}

	// ----- private methods -----
	private void initialize(final Path path) {

		try (final Stream<Path> files = Files.walk(path).filter(Files::isRegularFile).sorted()) {

			for (final Path file : files.toList()) {

				final FactsFile factsFile = new FactsFile(file);

				storeFacts(factsFile);

				factContainers.add(factsFile);
			};

		} catch (IOException ioex) {

			ioex.printStackTrace();
		}
	}

	public void initializeFromDocumentationAnnotations() {

		for (final Map.Entry<Class, List<Documentation>> entry : StructrApp.getConfiguration().getDocumentationAnnotations().entrySet()) {

			final Class clazz                       = entry.getKey();
			final List<Documentation> documentation = entry.getValue();

			for (final Documentation doc : documentation) {

				initializeFromDocumentationAnnotations(clazz, doc, clazz.getSimpleName(), null, null);
			}
		}
	}

	public long getNextId() {
		return idGenerator.getAndIncrement();
	}

	public void updateFactsContainers() {

		for (final FactsContainer factsContainer : factContainers) {

			factsContainer.writeToDisc();
		}
	}

	public Resource getBaseResource() {
		return baseResource;
	}

	// ----- private methods -----
	private Deque<AbstractToken> reduce(final FactsContainer factsContainer) {

		final List<Rule> rules = new LinkedList<>();
		int count = 0;

		rules.add(new RemoveUnwantedTokensRule(this));
		rules.add(new IdentifyNewlinesRule(this));
		rules.add(new IdentifyKeywordsRule(this));
		rules.add(new IdentifyAnaphoricPronounRule(this));
		rules.add(new IdentifyConjunctionsRule(this));
		rules.add(new IdentifyConceptsRule(this));
		rules.add(new ResolveKeywordsRule(this));
		rules.add(new ResolveConceptPairsRule(this));
		rules.add(new IdentifyVerbsRule(this));
		rules.add(new IdentifyNamesRule(this));
		rules.add(new ResolveAsRule(this));
		rules.add(new IdentifyListsRule(this));
		rules.add(new IdentifyNamedConceptsRule(this));
		rules.add(new FindUnknownIdentifiersRule(this));
		rules.add(new ResolveWithRule(this));
		rules.add(new IdentifyFactsRule(this));
		rules.add(new ResolveIsARelationsRule(this));

		final Deque<AbstractToken> tokens = factsContainer.getFilteredTokens();

		while (count++ < 100) {

			for (final Rule rule : rules) {

				rule.apply(tokens);
			}

			// all terminal
			if (tokens.stream().allMatch(token -> token.isTerminal())) {
				return tokens;
			}

			// more than one token left => error
			if (tokens.stream().noneMatch(token -> token instanceof UnresolvedToken)) {
				break;
			}
		}

		final List<AbstractToken> remainingTokens = new LinkedList<>();
		for (final AbstractToken token : tokens) {

			if (!token.isTerminal()) {

				remainingTokens.add(token);
			}
		}

		throw new RuntimeException("Parse error in " + factsContainer.getName() + ": remaining tokens are " + remainingTokens + ".");
	}

	private void initializeFromDocumentationAnnotations(final Class clazz, final Documentation documentation, final String typeName, final String itemName, final Concept parentConcept) {

		final AbstractToken token = new DocumentationAnnotationToken(typeName);

		// collect info from annotation and import it into the ontology
		final ConceptType type  = documentation.type();
		final String name       = coalesce(documentation.name(), itemName);
		final String desc       = documentation.shortDescription();
		final String parent     = documentation.parent();
		final String[] synonyms = documentation.synonyms();
		final String[] children = documentation.children();

		final Concept concept = getOrCreateConcept(token, type, name, true);
		if (concept != null) {

			concept.setShortDescription(desc);

			if (children != null && children.length > 0) {

				for (final String child : children) {

					if (StringUtils.isNotBlank(child)) {

						final Concept childConcept = getOrCreateConcept(token, ConceptType.Topic, child, false);
						if (childConcept != null && !concept.hasChild(Verb.Has, childConcept)) {

							createSymmetricLink(concept, Verb.Has, childConcept);
						}
					}
				}
			}

			if (synonyms != null && synonyms.length > 0) {

				for (final String synonym : synonyms) {

					if (StringUtils.isNotBlank(synonym)) {

						final Concept synonymConcept = getOrCreateConcept(token, ConceptType.Synonym, synonym, false);
						if (synonymConcept != null) {

							createSymmetricLink(concept, Verb.Has, synonymConcept);
						}
					}
				}
			}

			if (StringUtils.isNotBlank(parent)) {

				final Concept p = getOrCreateConcept(token, ConceptType.Topic, parent, true);
				if (p != null && !p.hasChild(Verb.Has, concept)) {

					createSymmetricLink(p, Verb.Has, concept);
				}

			} else if (parentConcept != null && !parentConcept.hasChild(Verb.Has, concept)) {

				createSymmetricLink(parentConcept, Verb.Has, concept);
			}

			if (clazz != null) {

				// import enum constants
				if (clazz.isEnum()) {

					for (final Object enumConstant : clazz.getEnumConstants()) {

						if (enumConstant instanceof Documentable documentable) {

							final String childName = documentable.getDisplayName();
							if (childName != null) {

								final DocumentableType documentableType = documentable.getDocumentableType();
								final Concept childConcept = getOrCreateConcept(token, documentableType.getConcept(), childName, false);

								if (childConcept != null) {

									childConcept.setDocumentable(documentable);

									if (documentable.getShortDescription() != null) {
										childConcept.setShortDescription(documentable.getShortDescription());
									}

									// store desired table format in parent
									if (documentable.getTableHeaders() != null) {

										// Enum constants are instances of the enum class, so they all have the
										// same implementation of getTableHeaders(), and the table format must
										// be stored in the parent, not in the children, hence the below code.
										concept.getMetadata().put("table-headers", documentable.getTableHeaders());
									}

									createSymmetricLink(concept, Verb.Has, childConcept);
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

	private Double add(final Double v1, final double v2) {

		if (v1 == null) {
			return v2;
		}

		return v1 + v2;
	}
}