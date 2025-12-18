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

	public enum Type {

		Topic("topic"), Concept("concept"), Component("component"), Feature("feature"), Mechanism("mechanism"),
		Provider("provider"), Service("service"), Capability("capability"), UseCase("use-case"), Type("type"),

		// external sources
		MarkdownFolder("markdown-folder"), MarkdownFile("markdown-file"), CodeSource("code-source"),
		EnumSource("enum-source"), JavascriptFile("javascript-file"),

		// concepts for user interface elements
		Screen("screen"), Form("form"), Area("area"), Tab("tab"), Flyout("flyout"), Menu("menu"), Dialog("dialog"),
		Button("button"),

		// technical concepts
		Logfile("logfile"), Value("value"), LifecycleMethod("lifecycle-method"), HttpVerb("http-verb"),
		Function("function"), Setting("setting"), Helper("helper"),

		// metadata
		Hint("hint"), Note("note"), Description("description"), Info("info"), Configuration("configuration"),
		Synonym("synonym"), Text("text"),

		// java types
		Keyword("keyword"), Method("method"), Property("property"), UserDefinedFunction("user-defined-function"),
		MaintenanceCommand("maintenance-command"), SystemType("system-type"), RequestParameter("request-parameter"),
		RequestHeader("request-header"), Class("class"), RestEndpoint("rest-endpoint"),

		Verb("verb"), Blacklist("blacklist"), Unknown("unknown");

		private final String identifier;

		Type(final String identifier) {
			this.identifier = identifier;
		}

		public String getIdentifier() {
			return identifier;
		}
	}

	protected final Map<String, List<Concept>> children = new LinkedHashMap<>();
	protected final Map<String, List<Concept>> parents  = new LinkedHashMap<>();
	protected final Map<String, String> metadata        =  new LinkedHashMap<>();
	protected final String sourceFile;
	protected final int lineNumber;
	protected final String name;
	protected Type type;

	protected Concept(final String sourceFile, final int lineNumber, final Type type, final String name) {

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

	public Type getType() {
		return type;
	}

	public void setType(final Type type) {
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

	public List<Concept> getChildrenOfType(final String linkType, final Type conceptType) {

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
		return Type.Topic.equals(type);
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

		for (final Type type : Type.values()) {

			if (type.getIdentifier().equals(name)) {
				return true;
			}
		}

		return false;
	}

	public static Type forName(String name) {

		for (final Type type : Type.values()) {

			if (type.getIdentifier().equals(name)) {
				return type;
			}
		}

		return null;
	}
}