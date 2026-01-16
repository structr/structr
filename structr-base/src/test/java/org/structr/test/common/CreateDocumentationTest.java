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
import org.structr.api.config.Settings;
import org.structr.api.config.SettingsGroup;
import org.structr.autocomplete.AbstractHintProvider;
import org.structr.core.Services;
import org.structr.core.function.Functions;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.docs.*;
import org.structr.docs.impl.lifecycle.*;
import org.structr.docs.impl.settings.SettingDocumentable;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.testng.AssertJUnit.fail;

public class CreateDocumentationTest extends StructrUiTest {

	@Test
	public void createDocumentationTest() {

		final List<Documentable> documentables = new LinkedList<>();
		final List<String> errors              = new LinkedList<>();

		final Map<DocumentableType, DocumentationEntry> files = Map.of(

			DocumentableType.Keyword,            new DocumentationEntry("1-Keywords.md",             "Keywords"),
			DocumentableType.BuiltInFunction,    new DocumentationEntry("2-Functions.md",            "Built-in Functions"),
			DocumentableType.LifecycleMethod,    new DocumentationEntry("3-Lifecycle Methods.md",    "Lifecycle Methods"),
			DocumentableType.SystemType,         new DocumentationEntry("4-System Types.md",         "System Types"),
			DocumentableType.Service,            new DocumentationEntry("5-Services.md",             "Services"),
			DocumentableType.MaintenanceCommand, new DocumentationEntry("6-Maintenance Commands.md", "Maintenance Commands"),
			DocumentableType.Setting,            new DocumentationEntry("7-Settings.md",             "Settings")
		);

		collectDocumentables(documentables);

		// sort by name
		Collections.sort(documentables, Comparator.comparing(Documentable::getName));

		// check style and content and generate Markdown docs
		for (final Documentable item : documentables) {

			// check metadata for style errors etc.
			errors.addAll(checkFunctionMetadata(item));

			final DocumentableType itemType = item.getDocumentableType();
			if (itemType != null) {

				final DocumentationEntry entry = files.get(itemType);
				if (entry != null) {

					entry.lines.addAll(item.createMarkdownDocumentation());
				}
			}
		}

		for (final DocumentationEntry entry : files.values()) {

			try {

				Files.writeString(Path.of(entry.fileName), StringUtils.join(entry.lines, "\n"));

			} catch (IOException e) {

				fail("Unable to create documentation for " + entry.fileName + ": " + e.getMessage());
			}
		}

		if (!errors.isEmpty()) {

			fail(StringUtils.join(errors, "\n"));
		}
	}

	// ----- private methods -----
	private void collectDocumentables(final List<Documentable> documentables) {

		// built-in functions
		documentables.addAll(Functions.getFunctions());

		// expressions and hints that are not registered as module functions
		Functions.addExpressions(documentables);

		// keywords
		AbstractHintProvider.addKeywordHints(documentables);

		// maintenance commands
		documentables.addAll(MaintenanceResource.getMaintenanceCommands());

		// system types
		final TraitsInstance rootInstance = TraitsManager.getRootInstance();
		for (final String traitName : rootInstance.getAllTypes(t -> t.isNodeType())) {

			final Traits traits = rootInstance.getTraits(traitName);
			if (!traits.isHidden()) {

				documentables.add(traits);
			}
		}

		// lifecycle methods
		documentables.add(new OnCreate());
		documentables.add(new OnSave());
		documentables.add(new OnDelete());
		documentables.add(new AfterCreate());
		documentables.add(new AfterSave());
		documentables.add(new AfterDelete());

		// services
		Services.collectDocumentation(documentables);

		// settings
		for (final SettingsGroup group : Settings.getGroups()) {

			for (final org.structr.api.config.Setting setting : group.getSettings()) {

				if (setting.getComment() != null) {

					documentables.add(new SettingDocumentable(setting));
				}
			}
		}
	}

	private List<String> checkFunctionMetadata(final Documentable item) {

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

			if (DocumentableType.Service.equals(documentableType) || DocumentableType.SystemType.equals(documentableType) || DocumentableType.Setting.equals(documentableType)) {

				// ignore
			} else {

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

	private class DocumentationEntry {

		final List<String> lines = new LinkedList<>();
		String fileName;
		String header;

		public DocumentationEntry(final String fileName, final String header) {

			this.fileName = fileName;
			this.header = header;

			lines.add("# " + header);
		}
	}
}