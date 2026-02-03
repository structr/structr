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

import org.apache.commons.lang3.StringUtils;
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.Documentable;
import org.structr.docs.ontology.parser.token.*;
import org.structr.web.common.FileHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * A concept in the Structr Documentation Ontology. This class
 * will store everything you add into the ontology.
 */
public final class Concept implements Comparable<Concept> {

	public static final double EXACT_MATCH_SCORE      = 100.0;
	public static final double NAME_MATCH_SCORE       = 10.0;
	public static final double SHORT_DESC_MATCH_SCORE = 2.0;
	public static final double LONG_DESC_MATCH_SCORE  = 1.0;
	public static final double NOTES_MATCH_SCORE      = 0.1;

	protected final Map<String, Object> metadata = new LinkedHashMap<>();
	protected final Set<AbstractToken> tokens    = new LinkedHashSet<>();
	protected Documentable documentable          = null;
	protected String shortDescription            = null;

	protected final AbstractToken token;
	protected final Ontology ontology;
	protected final String name;
	protected final long id;
	protected ConceptType type;

	protected Concept(final Ontology ontology, final AbstractToken token, final ConceptType type, final String name) {

		this.ontology = ontology;
		this.id       = ontology.getNextId();
		this.type     = type;
		this.name     = name;
		this.token    = token;

		tokens.add(token);
	}

	public String getId() {
		return "concept" + StringUtils.leftPad(String.valueOf(id), 5, '0');
	}

	@Override
	public String toString() {
		return type + "(" + getName() + ")";
	}

	@Override
	public int compareTo(final Concept o) {
		return getName().compareTo(o.getName());
	}

	public String getNameTypeAndLinks() {
		return type + "(" + getName() + ")";
	}

	public ConceptType getType() {
		return type;
	}

	public void setType(final ConceptType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Set<AbstractToken> getTokens() {
		return tokens;
	}

	/*
	public void createSymmetricLink(final Verb verb, final Concept annotatedConcept) {

		final Concept concept = annotatedConcept.getConcept();
		final String ltr      = verb.getLeftToRight();
		final String rtl      = verb.getRightToLeft();

		if (!this.equals(concept)) {

			if (!hasChild(ltr, concept)) {

				children.computeIfAbsent(ltr, key -> new AnnotatedConceptList(null)).add(annotatedConcept);
			}

			if (!concept.hasParent(rtl, this)) {

				//concept.parents.computeIfAbsent(rtl, key -> new LinkedList<>()).add(new Concept(this, annotatedConcept.getAnnotations()));
				// FIXME
				concept.parents.computeIfAbsent(rtl, key -> new AnnotatedConceptList(null)).add(new Concept(this));
			}
		}
	}

	public void createSymmetricLink(final Verb verb, final AnnotatedConceptList annotatedConcepts) {

		final String ltr = verb.getLeftToRight();
		final String rtl = verb.getRightToLeft();

		if (children.containsKey(ltr)) {
			children.get(ltr).addAll(annotatedConcepts);
		} else {
			children.put(ltr, annotatedConcepts);
		}

		for (final Concept annotatedConcept : annotatedConcepts) {

			final Concept concept = annotatedConcept.getConcept();

			if (!concept.hasParent(rtl, this)) {

				concept.parents.computeIfAbsent(rtl, key -> new AnnotatedConceptList(null)).addAll(new Concept(annotatedConcepts));
			}
		}
	}
	*/

	public List<Concept> getChildren(final Verb verb) {

		final List<Concept> result = new LinkedList<>();

		for (final Link childLink : getChildLinks(verb)) {

			result.add(childLink.getTarget());
		}

		return result;
	}

	public Map<Verb, List<Link>> getChildLinks() {
		return ontology.getOutgoingLinks(this);
	}

	public List<Link> getChildLinks(final Verb verb) {
		return ontology.getOutgoingLinks(this, verb);
	}

	public List<Concept> getChildrenOfType(final Verb verb, final ConceptType conceptType) {

		final List<Link> list = getChildLinks(verb);
		if (list != null) {

			final List<Concept> result = new LinkedList<>();

			for (final Link link : list) {

				final Concept child = link.getTarget();

				if (child.getType().equals(conceptType)) {
					result.add(child);
				}
			}

			return result;
		}

		return List.of();
	}

	public List<Concept> getParents(final Verb verb) {

		final List<Concept> result = new LinkedList<>();

		for (final Link parentLink : getParentLinks(verb)) {

			result.add(parentLink.getSource());
		}

		return result;
	}

	public Map<Verb, List<Link>> getParentLinks() {
		return ontology.getIncomingLinks(this);
	}

	public List<Link> getParentLinks(final Verb verb) {
		return ontology.getIncomingLinks(this, verb);
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(final String shortDescription) {
		this.shortDescription = shortDescription;
	}

	public void setDocumentable(final Documentable documentable) {
		this.documentable = documentable;
	}

	public Documentable getDocumentable() {
		return documentable;
	}

	public String getParentConceptName() {

		final List<Link> parentLinks = getParentLinks(Verb.Has);
		if (parentLinks != null && !parentLinks.isEmpty()) {

			final Link link      = parentLinks.get(0);
			final Concept source = link.getSource();

			if (source != null) {

				return source.getName();
			}
		}

		return null;

	}

	public boolean isTopic() {
		return ConceptType.Topic.equals(type);
	}

	public int getTotalChildCount() {
		return getTotalChildCount(this, new LinkedHashSet<>(), 0);
	}

	private int getTotalChildCount(final Concept concept, final Set<String> visited, final int level) {

		if (!visited.add(concept.getName())) {
			return 0;
		}

		int sum = 0;

		for (final List<Link> child : concept.getChildLinks().values()) {

			sum++;

			for (final Link childLink : child) {

				if (childLink == null) {
					throw new RuntimeException("Empty child link in children of " + getName() + ".");
				}

				sum += getTotalChildCount(childLink.getTarget(), new LinkedHashSet<>(visited), level + 1);
			}
		}

		return sum;
	}

	public boolean hasChild(final Verb verb, final Concept concept) {

		final List<Link> list = getChildLinks(verb);
		if (list != null) {

			for (final Link link : list) {

				if (link.getTarget().equals(concept)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean hasChild(final Verb verb, final ConceptType type) {

		final List<Link> list = getChildLinks(verb);
		if (list != null) {

			for (final Link link : list) {

				if (type.equals(link.getTarget().getType())) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean hasParent(final Verb verb, final Concept concept) {

		final List<Link> list = getParentLinks(verb);
		if (list != null) {

			for (final Link link : list) {

				if (link.getSource().equals(concept)) {
					return true;
				}
			}
		}

		return false;
	}

	public void setMentions(final List<Occurrence> mentions) {

		metadata.put("mentions", mentions);
	}

	public double matches(final String searchString) {

		final String name = getName();
		double score      = 0.0;

		if (name != null && name.toLowerCase().equals(searchString)) {

			score += EXACT_MATCH_SCORE;

		} else {

			if (name != null && name.toLowerCase().contains(searchString)) {

				score += NAME_MATCH_SCORE;
			}
		}

		if (shortDescription != null && shortDescription.toLowerCase().contains(searchString)) {

			score += SHORT_DESC_MATCH_SCORE;
		}

		if (metadata.containsKey("description")) {

			final String desc = (String) metadata.get("description");

			if (desc != null && desc.toLowerCase().contains(searchString)) {

				score += SHORT_DESC_MATCH_SCORE;
			}
		}

		// content of markdown files
		if (metadata.containsKey("content")) {

			final String content = (String) metadata.get("content");

			if (content != null && content.toLowerCase().contains(searchString)) {

				score += SHORT_DESC_MATCH_SCORE;
			}
		}

		return score;
	}

	public boolean isSame(final String otherName, final ConceptType type) {

		final String name = getName();
		if (name.equals(otherName)) {

			if (this.type.equals(type) || ConceptType.Unknown.equals(type)) {

				return true;
			}
		}

		return false;
	}

	/**
	 * Indicates whether this concept is a toplevel concept, meaning it
	 * has no parent, or the generic "Structr" concept as a parent.
	 * @return whether this concept is a toplevel concept
	 */
	public boolean isToplevelConcept() {

		final Map<Verb, List<Link>> parents = getParentLinks();
		if (parents.isEmpty()) {
			return true;
		}

		for (final Map.Entry<Verb, List<Link>> entry : parents.entrySet()) {

			for (final Link parentLink : entry.getValue()) {

				if ("Structr".equals(parentLink.getSource().getName())) {
					return true;
				}
			}
		}

		return false;
	}

	public Map<ConceptType, Set<Concept>> getGroupedChildren() {

		final Map<ConceptType, Set<Concept>> groupedChildren = new HashMap<>();

		for (final Map.Entry<Verb, List<Link>> entry : getChildLinks().entrySet()) {

			for (final Link childLink : entry.getValue()) {

				groupedChildren.computeIfAbsent(childLink.getType(), k -> new TreeSet<>()).add(childLink.getTarget());
			}
		}

		return groupedChildren;
	}

	public Concept getChildWithName(final String name) {

		final List<Link> list = getChildLinks(Verb.Has);
		for (final Link link : list) {

			if (link.getTarget().getName().equals(name)) {
				return link.getTarget();
			}
		}

		return null;
	}

	public List<Map<String, Object>> getReferences() {

		final List<Map<String, Object>> references = new LinkedList<>();

		for (final AbstractToken<?> abstractToken : getTokens()) {

			if (abstractToken != null) {

				final Token token = abstractToken.getToken();
				if (token != null) {

					references.add(Map.of(
						"sourceFile", token.getSource(),
						"row",        token.getRow(),
						"column",     token.getColumn()
					));
				}
			}
		}

		return references;
	}

	public void renameTo(final String test) {

		for (final AbstractToken token : getTokens()) {

			token.renameTo(test);
		}
	}

	public List<String> getSynonyms() {

		final List<String> synonyms = new LinkedList<>();

		getChildrenOfType(Verb.Has, ConceptType.Synonym).forEach(synonym -> synonyms.add(synonym.getName()));

		return synonyms;
	}

	public void updateContent(final String key, final String value) {

		if ("insert-paragraph".equals(key)) {

			final IntConsumer intConsumer = new IntConsumer();
			final AbstractToken rootToken = getRootToken(intConsumer);

			if (rootToken instanceof TokenCollection tokenCollection) {

				final String folder         = "structr/docs";
				final List<Token> allTokens = tokenCollection.getAllSourceTokens();
				final Token lastToken        = allTokens.getLast();
				final String fileName        = getNextSnippetName(folder);

				lastToken.insertAfter("\n" + StringUtils.repeat(" ", 4 * (intConsumer.getValue() - 1)) + "\"" + getName() + "\" has markdown-file \"" + fileName + "\"");

				try {

					Files.writeString(Path.of(folder, fileName), value);

				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}
		}

		if ("content".equals(key) || "name".equals(key)) {

			// update all tokens
			for (final AbstractToken token : getTokens()) {

				token.updateContent(key, value);
			}

			return;
		}

		if ("shortDescription".equals(key)) {

			for (final FactToken factToken : getFactTokens()) {

				final VerbToken verb = factToken.getVerbToken();
				if (verb != null) {

					final NamedConceptToken objectToken = factToken.getObjectToken();
					if (objectToken != null) {

						final ConceptToken conceptToken        = objectToken.getConceptToken();
						final IdentifierToken descriptionToken = objectToken.getIdentifierToken();

						if (conceptToken != null && descriptionToken != null) {

							if (ConceptType.Description.equals(conceptToken.getType())) {

								if (value.contains("\n")) {

									try {

										final String cleanedName = FileHelper.cleanFileName(getName());
										final String fileName    = "snippets/" + cleanedName + ".md";

										Files.writeString(Path.of("structr/docs/", fileName), value);

										conceptToken.updateContent("content", "markdown-file");
										descriptionToken.updateContent("content", fileName);

									} catch (IOException ioex) {
										ioex.printStackTrace();
									}

								} else {

									conceptToken.updateContent("content", "description");
									descriptionToken.updateContent("content", value);
								}
							}
						}
					}
				}

			}
		}
	}

	// ----- private methods -----
	private AbstractToken getRootToken(final Consumer<Integer> levelConsumer) {

		AbstractToken current = this.token;
		int level = 0;

		while (current != null) {

			if (current.getParent() == null) {

				if (levelConsumer != null) {
					levelConsumer.accept(level);
				}

				return current;
			}

			current = current.getParent();

			level++;
		}

		return null;
	}

	private List<FactToken> getFactTokens() {

		final List<FactToken> factTokens = new LinkedList<>();

		for (final AbstractToken token : getTokens()) {

			collectFactParents(factTokens, token);
		}

		return factTokens;
	}

	private void collectFactParents(final List<FactToken> parents, final AbstractToken token) {

		if (token != null) {

			if (token instanceof FactToken factToken) {

				parents.add(factToken);
			}

			final AbstractToken parent = token.getParent();
			if (parent != null) {

				collectFactParents(parents, parent);

			}
		}
	}

	public String getNextSnippetName(final String folder) {

		String name = getSnippetName(0);
		Path path   = Path.of(folder, name);
		int index   = 0;

		while (Files.exists(path)) {

			name = getSnippetName(index++);
			path = Path.of(folder, name);
		}

		return name;
	}

	public String getSnippetName(final int index) {
		return "snippets/" + this.getName() + " - " + StringUtils.leftPad(Integer.toString(index), 3, "0") + ".md";
	}

	public Link getLinkTo(final Verb verb, final Concept concept) {

		for (final Link link : getChildLinks(verb)) {

			if (link.getTarget().equals(concept)) {
				return link;
			}
		}

		return null;
	}

	// ----- public static methods -----
	public static boolean exists(final String name) {

		for (final ConceptType type : ConceptType.values()) {

			if (type.getIdentifier().equals(name)) {
				return true;
			}
		}

		return false;
	}

	public static ConceptType forName(String name) {

		for (final ConceptType type : ConceptType.values()) {

			if (type.getIdentifier().equals(name)) {
				return type;
			}
		}

		return null;
	}

	public static Concept create(final Ontology ontology, final AbstractToken token, final ConceptType type, final String name) {
		return new Concept(ontology, token, type, name);
	}

	// ----- nested classes -----
	private class IntConsumer implements Consumer<Integer> {

		private Integer value = null;

		@Override
		public void accept(final Integer value) {
			this.value = value;
		}

		public Integer getValue() {
			return value;
		}
	}
}