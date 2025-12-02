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
package org.structr.docs;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;

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
	default List<String> createMarkdownDocumentation() {

		final List<String> lines = new LinkedList<>();

		// build Markdown
		final List<Signature> signatures = getSignatures();
		final List<Setting> settings     = getSettings();
		final List<Property> properties  = getProperties();
		final List<Parameter> parameters = getParameters();
		final List<Example> examples     = getExamples();
		final List<String> notes         = getNotes();
		final String longDescription     = getLongDescription();
		final String name                = getName();

		lines.add("## " + getDisplayName());
		lines.add(getShortDescription());

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

			lines.add("### Properties");

			lines.add("");
			lines.add("|Name|Description|");
			lines.add("|---|---|");

			for (final Property property : properties) {
				lines.add("|" + property.getName() + "|" + property.getDescription() + "|");
			}

			lines.add("");
		}

		if (settings != null && !settings.isEmpty()) {

			lines.add("### Settings");

			lines.add("");
			lines.add("|Name|Description|");
			lines.add("|---|---|");

			for (final Setting setting : settings) {
				lines.add("|" + setting.getName() + "|" + setting.getDescription() + "|");
			}

			lines.add("");
		}

		if (parameters != null && !parameters.isEmpty()) {

			lines.add("### Parameters");

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

			lines.add("### Notes");

			for (final String note : notes) {
				lines.add("- " + note);
			}

			lines.add("");
		}

		if (signatures != null && !signatures.isEmpty()) {

			lines.add("### Signatures");
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

			lines.add("### Examples");

			for (final Example example : examples) {

				if (StringUtils.isNotBlank(example.getTitle())) {

					lines.add("##### " + index + ". (" + example.getLanguage() + ") " + example.getTitle());

				} else {

					lines.add("##### Example " + index + " (" + example.getLanguage() + ")");
				}
				lines.add("```");
				lines.add(example.getText());
				lines.add("```");

				index++;
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
	String getLongDescription();

	/**
	 * Returns the parameters of this Documentable, or null if no
	 * parameters are defined.
	 *
	 * @return the parameters or null
	 */
	List<Parameter> getParameters();

	/**
	 * Returns examples for this Documentable, or null if no examples
	 * exist.
	 *
	 * @return the examples or null
	 */
	List<Example> getExamples();

	/**
	 * Returns notes for this Documentable, or null if no notes
	 * exist.
	 *
	 * @return the notes or null
	 */
	List<String> getNotes();

	/**
	 * Returns the signatures of this Documentable, or null if no
	 * signatures are defined.
	 *
	 * @return the signatures or null
	 */
	List<Signature> getSignatures();

	/**
	 * Returns the languages for which this Documentable is valid. This
	 * method must return a non-null value, otherwise a NullPointerException
	 * is thrown, because every Documentable must specify the languages
	 * for which it is valid.
	 *
	 * @return the languages
	 */
	List<Language> getLanguages();

	/**
	 * Returns the usages of this Documentable, or null if no usages exist.
	 *
	 * @return the usages or null
	 */
	List<Usage> getUsages();

	/**
	 * Returns the properties of this Documentable, or null if no
	 * properties are defined.
	 *
	 * @return the properties or null
	 */
	default List<Property> getProperties() {
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

	default boolean isDynamic() {
		return false;
	}

	default boolean isHidden() {
		return DocumentableType.Hidden.equals(getDocumentableType());
	}

	default String getDisplayName() {

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
}
