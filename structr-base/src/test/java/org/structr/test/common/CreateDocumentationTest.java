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
package org.structr.test.common;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.*;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.fail;

public class CreateDocumentationTest extends StructrUiTest {

	@Test
	public void createDocumentationTest() {

		final List<Documentable> items = Documentable.createMarkdownDocumentation();
		final List<String> errors      = new LinkedList<>();

		for (final Documentable item : items) {

			errors.addAll(checkFunctionMetadata(item));
		}

		if (!errors.isEmpty()) {

			fail(StringUtils.join(errors, "\n"));
		}
	}

	// ----- private methods -----
	private List<String> checkFunctionMetadata(final Documentable item) {

		final Set<DocumentableType> blacklist = Set.of(
			DocumentableType.Service,
			DocumentableType.SystemType,
			DocumentableType.Setting
		);

		final DocumentableType documentableType = item.getDocumentableType();

		// skip documentables that are hidden
		if (item.isHidden()) {
			return Collections.emptyList();
		}

		final List<String> errors = new LinkedList<>();
		final String type         = documentableType.toString();

		// verify that the short description ends with a period
		final String desc = item.getShortDescription();
		if (desc != null) {

			if (!blacklist.contains(documentableType)) {

				if (!desc.strip().endsWith(".")) {

					errors.add("Short description of " + type + " " + item.getName() + " does not end with a period character.");
				}

				if (desc.toLowerCase().startsWith("this method")) {

					errors.add("Short description of " + type + " " + item.getName() + " starts with 'this method' instead of 'this function'.");
				}

				if (containsLowercaseStructr(desc)) {

					errors.add("Short description of " + type + " " + item.getName() + " uses structr, please change to Structr with a capital S.");
				}
			}
		}

		final String longDesc = item.getLongDescription();
		if (StringUtils.isNotBlank(longDesc)) {

			if (longDesc.toLowerCase().startsWith("this method")) {

				errors.add("Long description of " + type + " " + item.getName() + " starts with 'this method' instead of 'this function'.");
			}

			if (longDesc.toLowerCase().contains("/article")) {

				errors.add("Long description of " + type + " " + item.getName() + " still contains Markdown reference link to old documentation page (/article).");
			}

			if (containsLowercaseStructr(longDesc)) {

				errors.add("Long description of " + type + " " + item.getName() + " uses structr, please change to Structr with a capital S.");
			}
		}

		// verify that an item has at least one language
		if (documentableType.supportsLanguages()) {

			final List<Language> languages = item.getLanguages();
			if (languages == null || languages.isEmpty()) {

				errors.add(type + " " + item.getName() + " has no languages.");
			}
		}

		// verify that a documentation item has no empty examples
		if (documentableType.supportsExamples()) {

			final List<Example> examples = item.getExamples();
			if (examples != null) {

				for (final Example example : item.getExamples()) {

					if (StringUtils.isBlank(example.getText())) {

						errors.add(type + " " + item.getName() + " has empty example.");
					}
				}
			}
		}

		// verify signatures, usages and parameters only for built-in functions
		if (DocumentableType.BuiltInFunction.equals(documentableType)) {

			// verify that a function has at least one signature
			final List<Signature> signatures = item.getSignatures();
			if (signatures == null || signatures.isEmpty()) {

				errors.add("Function " + item.getName() + " has no signatures.");
			}

			// verify that a function has at least one usage
			final List<Usage> usages = item.getUsages();
			if (usages == null || usages.isEmpty()) {

				errors.add("Function " + item.getName() + " has no usages.");
			}

			// verify the parameters
			final List<Parameter> parameters = item.getParameters();
			if (parameters != null && !parameters.isEmpty()) {

				for (final Parameter parameter : parameters) {

					if (StringUtils.isEmpty(parameter.getName())) {
						errors.add(type + " " + item.getName() + " has empty parameter name.");
					}

					if (Character.isUpperCase(parameter.getName().charAt(0))) {
						errors.add("Parameter " + parameter.getName() + " of function " + item.getName() + " should not start with an uppercase letter.");
					}

					if (parameter.getDescription() != null) {

						final String parameterDescription = parameter.getDescription();

						if (parameterDescription.endsWith(".") && !parameterDescription.endsWith("etc.")) {
							errors.add("Parameter description for " + parameter.getName() + " of function " + item.getName() + " should not end with a period character.");
						}

						// check some things in the description text
						final String d = parameterDescription.toLowerCase();

						if (d.startsWith("the ") || d.startsWith("a ") || d.startsWith("an ") || d.startsWith("optional ")) {
							errors.add("Parameter description for " + parameter.getName() + " of function " + item.getName() + " should not start with the words 'the', 'a', 'an', or 'optional'.");
						}
					}
				}
			}

			// verify that a function has a usage and a signature for all languages
			for (final Language language : item.getLanguages()) {

				boolean hasUsage     = false;
				boolean hasSignature = false;

				if (usages != null) {

					for (final Usage usage : usages) {

						if (usage.getLanguages().contains(language)) {

							hasUsage = true;
						}
					}
				}

				if (signatures != null) {

					for (final Signature signature : signatures) {

						if (signature.getLanguages().contains(language)) {

							hasSignature = true;
						}
					}
				}

				if (!hasUsage) {

					errors.add(type + " " + item.getName() + " has no usage for language " + language.name());
				}

				if (!hasSignature) {

					errors.add(type + " " + item.getName() + " has no signature for language " + language.name());
				}
			}
		}

		return errors;
	}

	private boolean containsLowercaseStructr(final String string) {

		final int structrLength = "structr".length();
		final int length        = string.length();
		int fromIndex           = 0;

		while (true) {

			final int pos = string.indexOf("structr", fromIndex);
			int counter   = 0;

			if (pos == -1) {
				return false;
			}

			// uncomment this to debug false positives
			//System.out.println(string.substring(Math.max(0, pos - 5), pos + structrLength + 5));

			final int pos2 = string.indexOf("structr.conf", fromIndex);
			if (pos2 == pos) {

				fromIndex = pos + 1;
				continue;
			}

			// check char before
			if (pos > 0) {

				final char before = string.charAt(pos - 1);
				if (isIgnorable(before)) {
					counter++;
				}

			} else {

				// no char before structr => valid
				counter++;
			}

			if (pos + structrLength < length) {

				final char after = string.charAt(pos + structrLength);
				if (isIgnorable(after)) {
					counter++;
				}

			} else {

				// no character after structr => valid
				counter++;
			}

			// we found a match
			if (counter == 2) {

				return true;

			} else {

				// next occurence
				fromIndex = pos + 1;
			}
		}
	}

	private boolean isIgnorable(final char c) {

		if (Character.isWhitespace(c)) {
			return true;
		}

		if (c ==  '"') { return true; }
		if (c ==  '\'') { return true; }
		if (c ==  '`') { return true; }
		if (c ==  ',') { return true; }
		if (c ==  '.') { return true; }
		if (c ==  ';') { return true; }
		if (c ==  '?') { return true; }
		if (c ==  '!') { return true; }
		if (c ==  '(') { return true; }
		if (c ==  ')') { return true; }
		if (c ==  '[') { return true; }
		if (c ==  ']') { return true; }
		if (c ==  '{') { return true; }
		if (c ==  '}') { return true; }

		return false;
	}
}