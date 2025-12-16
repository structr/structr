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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A concept in the Structr Documentation Ontology. This class
 * will store everything you add into the ontology.
 */
public final class Concept {

	protected final Map<String, List<Concept>> children = new LinkedHashMap<>();
	protected final Map<String, List<Concept>> parents  = new LinkedHashMap<>();
	protected final Map<String, String> metadata        =  new LinkedHashMap<>();
	protected final String sourceFile;
	protected final int lineNumber;
	protected final String name;
	protected String type;

	protected Concept(final String sourceFile, final int lineNumber, final String type, final String name) {

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

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void linkChild(final String verb, final Concept concept) {

		if (!hasChild(verb, concept)) {

			children.computeIfAbsent(verb, key -> new LinkedList<>()).add(concept);
		}
	}

	public void linkParent(final String verb, final Concept concept) {
		parents.computeIfAbsent(verb, key -> new LinkedList<>()).add(concept);
	}

	public Map<String, List<Concept>> getChildren() {
		return children;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public List<Concept> getChildrenOfType(final String linkType, final String conceptType) {

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
		return "topic".equals(type);
	}

	public int getTotalChildCount() {
		return getTotalChildCount(this);
	}

	private int getTotalChildCount(final Concept concept) {

		int sum = 0;

		for (final List<Concept> child : concept.getChildren().values()) {

			sum++;

			for (final Concept childConcept : child) {

				if (childConcept == null) {
					throw new RuntimeException("Empty child concept in children of " + getName() + ".");
				}

				sum += childConcept.getTotalChildCount();
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

	public void setOccurrences(final int occurrences) {

		metadata.put("occurrences", String.valueOf(occurrences));
	}
}


























