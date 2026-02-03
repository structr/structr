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
package org.structr.docs;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Category;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Details;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Base interface for all things that are documentable. Implement this
 * interface to enable automatic documentation generation for your class,
 * but keep in mind that the object still needs to be registered in the
 * appropriate places.
 */
public interface Documentable {

	/**
	 * Creates the full Markdown documentation to be used directly.
	 * This method exists so the individual implementations can
	 * control the content that is created from their data.
	 *
	 * The resulting list of strings will be joined together using
	 * newline characters, so each string represents a single line
	 * in the output.
	 *
	 * @return the Markdown documentation for this documentable
	 */
	default List<String> createMarkdownDocumentation(final Set<Details> details, final int startLevel) {

		final List<String> lines = new LinkedList<>();

		// build Markdown
		final List<Signature> signatures          = getSignatures();
		final List<Setting> settings              = getSettings();
		final List<DocumentedProperty> properties = getDocumentedProperties();
		final List<Parameter> parameters          = getParameters();
		final List<Example> examples              = getExamples();
		final List<String> notes                  = getNotes();
		final String longDescription              = getLongDescription();
		final String name                         = getName();
		final String startHeading                 = StringUtils.repeat("#", startLevel);

		if (details.contains(Details.name) || details.contains(Details.all)) {
			lines.add(startHeading + " " + getDisplayName(false));
		}

		if (details.contains(Details.shortDescription) || details.contains(Details.all)) {
			lines.add(getShortDescription());
		}

		// should we use longDescription or details here?
		if (details.contains(Details.all)) {

			if (isJavaScriptOnly()) {

				lines.add("");
				lines.add("**JavaScript only**");
				lines.add("");

			} else if (isStructrScriptOnly()) {

				lines.add("");
				lines.add("**StructrScript only**");
				lines.add("");
			}

			if (properties != null && !properties.isEmpty()) {

				lines.add(startHeading + "# Properties");

				lines.add("");
				lines.add("|Name|Description|");
				lines.add("|---|---|");

				for (final DocumentedProperty property : properties) {
					lines.add("|" + property.getName() + "|" + property.getDescription() + "|");
				}

				lines.add("");
			}

			if (settings != null && !settings.isEmpty()) {

				lines.add(startHeading + "# Settings");

				lines.add("");
				lines.add("|Name|Description|");
				lines.add("|---|---|");

				for (final Setting setting : settings) {
					lines.add("|" + setting.getName() + "|" + setting.getDescription() + "|");
				}

				lines.add("");
			}

			if (parameters != null && !parameters.isEmpty()) {

				lines.add(startHeading + "# Parameters");

				lines.add("");
				lines.add("|Name|Description|Optional|");
				lines.add("|---|---|---|");

				for (final Parameter parameter : parameters) {
					lines.add("|" + parameter.getName() + "|" + parameter.getDescription() + "|" + (parameter.isOptional() ? "yes" : "no") + "|");
				}

				lines.add("");
			}

			// longDescription can be empty
			if (StringUtils.isNotEmpty(longDescription)) {

				lines.add("");
				lines.add(longDescription);
			}

			if (notes != null && !notes.isEmpty()) {

				lines.add(startHeading + "# Notes");

				for (final String note : notes) {
					lines.add("- " + note);
				}

				lines.add("");
			}

			if (signatures != null && !signatures.isEmpty()) {

				lines.add(startHeading + "# Signatures");
				lines.add("");
				lines.add("```");

				for (final Signature signature : signatures) {
					lines.add(name + "(" + signature.getSignature() + ")");
				}

				lines.add("```");
				lines.add("");
			}

			if (examples != null && !examples.isEmpty()) {

				int index = 1;

				lines.add(startHeading + "# Examples");

				for (final Example example : examples) {

					if (StringUtils.isNotBlank(example.getTitle())) {

						lines.add(startHeading + "## " + index + ". (" + example.getLanguage() + ") " + example.getTitle());

					} else {

						lines.add(startHeading + "## Example " + index + " (" + example.getLanguage() + ")");
					}
					lines.add("```");
					lines.add(example.getText());
					lines.add("```");

					index++;
				}
			}
		}

		lines.add("");

		return lines;
	}

	/**
	 * Returns the type of this Documentable.
	 *
	 * @return
	 */
	DocumentableType getDocumentableType();

	/**
	 * Returns the name of this Documentable.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the short description of this Documentable. This method
	 * must return a non-null value, otherwise a NullPointerException
	 * will be thrown, because every Documentable needs at least a
	 * short description.
	 *
	 * @return the short description
	 */
	String getShortDescription();

	/**
	 * Returns the long description of this Documentable. This method
	 * may return null or the empty string to indicate that the object
	 * has no long description.
	 *
	 * @return the long description or null
	 */
	default String getLongDescription() {
		return null;
	}

	/**
	 * Returns the parameters of this Documentable, or null if no
	 * parameters are defined.
	 *
	 * @return the parameters or null
	 */
	default List<Parameter> getParameters() {
		return null;
	}

	/**
	 * Returns examples for this Documentable, or null if no examples
	 * exist.
	 *
	 * @return the examples or null
	 */
	default List<Example> getExamples() {
		return null;
	}

	/**
	 * Returns notes for this Documentable, or null if no notes
	 * exist.
	 *
	 * @return the notes or null
	 */
	default List<String> getNotes() {
		return null;
	}

	/**
	 * Returns the signatures of this Documentable, or null if no
	 * signatures are defined.
	 *
	 * @return the signatures or null
	 */
	default List<Signature> getSignatures() {
		return null;
	}

	/**
	 * Returns the languages for which this Documentable is valid. This
	 * method must return a non-null value, otherwise a NullPointerException
	 * is thrown, because every Documentable must specify the languages
	 * for which it is valid.
	 *
	 * @return the languages
	 */
	default List<Language> getLanguages() {
		return null;
	}

	/**
	 * Returns the usages of this Documentable, or null if no usages exist.
	 *
	 * @return the usages or null
	 */
	default List<Usage> getUsages() {
		return null;
	}

	/**
	 * Returns the properties of this Documentable, or null if no
	 * properties are defined.
	 *
	 * @return the properties or null
	 */
	default List<DocumentedProperty> getDocumentedProperties() {
		return null;
	}

	/**
	 * Returns the settings of this Documentable, or null if no
	 * settings are defined.
	 *
	 * @return the settings or null
	 */
	default List<Setting> getSettings() {
		return null;
	}

	/**
	 * Override this method to return concepts that this Documentable
	 * is a direct "has" child of. Please don't use this method to indicate
	 * relations other than parent-child, use getLinkedConcepts() for
	 * that.
	 * @return
	 */
	default List<ConceptReference> getParentConcepts() {
		return new LinkedList<>();
	}

	default List<Link> getLinkedConcepts() {
		return new LinkedList<>();
	}

	default List<String> getSynonyms() {
		return new LinkedList<>();
	}

	default Category getCategory() {
		return null;
	}

	default Map<String, String> getTableHeaders() {
		return null;
	}

	// ----- "private" methods, don't override
	default boolean isDynamic() {
		return false;
	}

	default boolean isHidden() {
		return DocumentableType.Hidden.equals(getDocumentableType());
	}

	default String getDisplayName() {
		return getDisplayName(true);
	}

	default String getDisplayName(boolean includeParameters) {

		switch (getDocumentableType()) {

			case BuiltInFunction:
			case Method:
			case UserDefinedFunction:

				return getName() + "()";
		}

		return getName();
	}

	default boolean isJavaScriptOnly() {

		final List<Language> languages = getLanguages();
		if (languages != null && languages.size() == 1) {

			return languages.get(0).equals(Language.JavaScript);
		}

		return false;
	}

	default boolean isStructrScriptOnly() {

		final List<Language> languages = getLanguages();
		if (languages != null && languages.size() == 1) {

			return languages.get(0).equals(Language.StructrScript);
		}

		return false;
	}

	default boolean hasExamples() {
		return getExamples() != null;
	}

	default double matches(final String searchString) {

		double score = 0.0;

		if (getName() != null) {

			if (getName() != null && getName().toLowerCase().equals(searchString)) {

				score += Concept.EXACT_MATCH_SCORE;

			} else if (getDisplayName() != null && getDisplayName().toLowerCase().equals(searchString)) {

				score += Concept.EXACT_MATCH_SCORE;

			} else if (getName() != null && getName().toLowerCase().contains(searchString)) {

				score += Concept.NAME_MATCH_SCORE;

			} else if (getDisplayName() != null && getDisplayName().toLowerCase().contains(searchString)) {

				score += Concept.NAME_MATCH_SCORE;
			}
		}

		if (getShortDescription() != null && getShortDescription().toLowerCase().contains(searchString)) {
			score += Concept.SHORT_DESC_MATCH_SCORE;
		}

		if (getLongDescription() != null && getLongDescription().toLowerCase().contains(searchString)) {
			score += Concept.LONG_DESC_MATCH_SCORE;
		}

		if (getNotes() != null) {

			for (final String note : getNotes()) {

				if (note.toLowerCase().contains(searchString)) {
					score += Concept.NOTES_MATCH_SCORE;
				}
			}
		}

		return score;
	}

	class ConceptReference {

		public ConceptType type;
		public String name;

		public ConceptReference(final ConceptType type, final String name) {
			this.type = type;
			this.name = name;
		}

		public static ConceptReference of(final ConceptType type, final String name) {
			return new ConceptReference(type, name);
		}
	}

	class Link {

		public String verb;
		public ConceptReference target;

		public Link(final String verb, final ConceptReference target) {

			this.verb   = verb;
			this.target = target;
		}

		public static Link to(final String verb, final ConceptReference target) {
			return new Link(verb, target);
		}
	}

	static List<Documentable> createMarkdownDocumentation() {

		final List<Documentable> documentables = new LinkedList<>();

		// this map controls which reference lists are generated, and
		// into which file they are written
		final Map<DocumentableType, DocumentationEntry> files = Map.of(

			DocumentableType.Keyword,            new DocumentationEntry("1-Keywords.md",             "Keywords"),
			DocumentableType.BuiltInFunction,    new GroupedDocumentationEntry("2-Functions.md",     "Built-in Functions", "Functions"),
			DocumentableType.LifecycleMethod,    new DocumentationEntry("3-Lifecycle Methods.md",    "Lifecycle Methods"),
			DocumentableType.SystemType,         new DocumentationEntry("4-System Types.md",         "System Types"),
			DocumentableType.Service,            new DocumentationEntry("5-Services.md",             "Services"),
			DocumentableType.MaintenanceCommand, new DocumentationEntry("6-Maintenance Commands.md", "Maintenance Commands"),
			DocumentableType.Setting,            new GroupedDocumentationEntry("7-Settings.md",      "Settings", "Settings")
		);

		DocumentableType.collectAllDocumentables(documentables);

		// sort by name
		Collections.sort(documentables, Comparator.comparing(documentable -> documentable.getDisplayName()));

		// check style and content and generate Markdown docs
		for (final Documentable item : documentables) {

			// check metadata for style errors etc.
			//errors.addAll(checkFunctionMetadata(item));

			final DocumentableType itemType = item.getDocumentableType();
			if (itemType != null) {

				final DocumentationEntry entry = files.get(itemType);
				if (entry != null) {

					final Category itemCategory = item.getCategory();
					if (itemCategory != null && itemCategory.getDisplayName() != null) {

						final List<String> lines = item.createMarkdownDocumentation(EnumSet.allOf(Details.class), 3);
						final String displayName = itemCategory.getDisplayName();

						entry.addLines(lines, displayName);

					} else {

						final List<String> lines = item.createMarkdownDocumentation(EnumSet.allOf(Details.class), 2);

						entry.addLines(lines);
					}
				}
			}
		}

		/* dont write to disk
		for (final DocumentationEntry entry : files.values()) {

			try {

				Files.writeString(Path.of(entry.getFileName()), StringUtils.join(entry.getLines(), "\n"));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/

		return documentables;
	}
}
