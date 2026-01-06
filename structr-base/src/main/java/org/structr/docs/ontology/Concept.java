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
import org.structr.docs.Documentable;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A concept in the Structr Documentation Ontology. This class
 * will store everything you add into the ontology.
 */
public final class Concept {

	public static final double EXACT_MATCH_SCORE      = 100.0;
	public static final double NAME_MATCH_SCORE       = 10.0;
	public static final double SHORT_DESC_MATCH_SCORE = 2.0;
	public static final double LONG_DESC_MATCH_SCORE  = 1.0;
	public static final double NOTES_MATCH_SCORE      = 0.1;

	private static final AtomicLong idGenerator = new AtomicLong();
	private final long id = idGenerator.getAndIncrement();

	protected final Map<String, List<Concept>> children = new LinkedHashMap<>();
	protected final Map<String, List<Concept>> parents  = new LinkedHashMap<>();
	protected final Map<String, Object> metadata        = new LinkedHashMap<>();
	protected final Set<Occurrence> occurrences         = new LinkedHashSet<>();
	protected Documentable documentable                 = null;
	protected String shortDescription                   = null;
	protected ConceptType format                        = null;

	protected final String sourceFile;
	protected final int lineNumber;
	protected final String name;
	protected ConceptType type;

	protected Concept(final String sourceFile, final int lineNumber, final ConceptType type, final String name) {

		this.type       = type;
		this.name       = name;
		this.lineNumber = lineNumber;
		this.sourceFile = sourceFile;

		occurrences.add(new Occurrence(sourceFile, lineNumber));
	}

	public String getId() {
		return "concept" + StringUtils.leftPad(String.valueOf(id), 5, '0');
	}

	@Override
	public String toString() {
		return type + "(" + name + ")";
	}

	public String getNameTypeAndLinks() {
		return type + "(" + name + ") -> " + children + " <- " + parents;
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

	public void createSymmetricLink(final Verb verb, final Concept concept) {

		final String ltr = verb.getLeftToRight();
		final String rtl = verb.getRightToLeft();

		if (!this.equals(concept)) {

			if (!hasChild(ltr, concept)) {

				children.computeIfAbsent(ltr, key -> new LinkedList<>()).add(concept);
			}

			if (!concept.hasParent(rtl, this)) {

				concept.parents.computeIfAbsent(rtl, key -> new LinkedList<>()).add(this);
			}
		}
	}

	public Map<String, List<Concept>> getChildren() {
		return children;
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

	public List<Concept> getChildren(final String linkType) {
		return children.get(linkType);
	}

	public List<Concept> getChildrenOfType(final String linkType, final ConceptType conceptType) {

		final List<Concept> list = children.get(linkType);
		if (list != null) {

			final List<Concept> result = new LinkedList<>();

			for (final Concept child : list) {

				if (child.getType().equals(conceptType)) {
					result.add(child);
				}
			}

			return result;
		}

		return List.of();
	}

	public String getParentConceptName() {

		final List<Concept> p = parents.get("ispartof");
		if (p != null) {

			return p.get(0).getName();
		}

		return null;

	}

	public Map<String, List<Concept>> getParents() {
		return parents;
	}

	public Set<Occurrence> getOccurrences() {
		return occurrences;
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

		for (final List<Concept> child : concept.getChildren().values()) {

			sum++;

			for (final Concept childConcept : child) {

				if (childConcept == null) {
					throw new RuntimeException("Empty child concept in children of " + getName() + ".");
				}

				sum += getTotalChildCount(childConcept, new LinkedHashSet<>(visited), level + 1);
			}
		}

		return sum;
	}

	public boolean hasChild(final String has, final Concept concept) {

		final List<Concept> list = children.get(has);
		if (list != null) {

			for (final Concept child : list) {

				if (child.equals(concept)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean hasParent(final String has, final Concept concept) {

		final List<Concept> list = parents.get(has);
		if (list != null) {

			for (final Concept parent : list) {

				if (parent.equals(concept)) {
					return true;
				}
			}
		}

		return false;
	}

	public void setMentions(final List<Occurrence> mentions) {

		metadata.put("mentions", mentions);
	}

	public void setFormat(final ConceptType format) {
		this.format = format;
	}

	public ConceptType getFormat() {
		return format;
	}

	public double matches(final String searchString) {

		double score = 0.0;

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

	public boolean isSame(final String name, final ConceptType type, final String sourceFile, final int lineNumber) {

		if (this.name.equals(name)) {

			if (this.type.equals(type) || ConceptType.Unknown.equals(type)) {

				return true;

				/*
				if (this.sourceFile.equals(sourceFile)) {

					if (this.lineNumber == lineNumber) {

						return true;
					}
				}
				*/
			}
		}

		return false;
	}

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

	/**
	 * Indicates whether this concept is a toplevel concept, meaning it
	 * has no parent, or the generic "Structr" concept as a parent.
	 * @return whether this concept is a toplevel concept
	 */
	public boolean isToplevelConcept() {

		final Map<String, List<Concept>> parents = getParents();
		if (parents.isEmpty()) {
			return true;
		}

		for (final Map.Entry<String, List<Concept>> entry : parents.entrySet()) {

			for (final Concept parent : entry.getValue()) {

				if ("Structr".equals(parent.getName())) {
					return true;
				}
			}
		}

		return false;
	}
}