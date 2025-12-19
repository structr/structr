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

import java.util.*;

/**
 * A concept in the Structr Documentation Ontology. This class
 * will store everything you add into the ontology.
 */
public final class Concept {

	protected final Map<String, List<Concept>> children = new LinkedHashMap<>();
	protected final Map<String, List<Concept>> parents  = new LinkedHashMap<>();
	protected final Map<String, String> metadata        =  new LinkedHashMap<>();
	protected String shortDescription                   = null;

	protected final String sourceFile;
	protected final int lineNumber;
	protected final String name;
	protected ConceptType type;

	protected Concept(final String sourceFile, final int lineNumber, final ConceptType type, final String name) {

		this.sourceFile = sourceFile;
		this.lineNumber = lineNumber;
		this.type       = type;
		this.name       = name;
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

	public void linkChild(final String verb, final Concept concept) {

		if (!this.equals(concept) && !hasChild(verb, concept)) {

			children.computeIfAbsent(verb, key -> new LinkedList<>()).add(concept);
		}
	}

	public void linkParent(final String verb, final Concept concept) {

		if (!this.equals(concept) && !hasParent(verb, concept)) {

			parents.computeIfAbsent(verb, key -> new LinkedList<>()).add(concept);
		}
	}

	public Map<String, List<Concept>> getChildren() {
		return children;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(final String shortDescription) {
		this.shortDescription = shortDescription;
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

	public Map<String, List<Concept>> getParents() {
		return parents;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getSourceFile() {
		return sourceFile;
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

	public boolean hasChild(final String has, final Concept additionalConcept) {

		final List<Concept> list = children.get(has);
		if (list != null) {

			for (final Concept child : list) {

				if (child.equals(additionalConcept)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean hasParent(final String has, final Concept additionalConcept) {

		final List<Concept> list = parents.get(has);
		if (list != null) {

			for (final Concept parent : list) {

				if (parent.equals(additionalConcept)) {
					return true;
				}
			}
		}

		return false;
	}

	public void setOccurrences(final int occurrences) {

		metadata.put("occurrences", String.valueOf(occurrences));
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
}