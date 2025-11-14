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
package org.structr.test.common;

import org.apache.commons.lang3.StringUtils;
import org.structr.autocomplete.AbstractHintProvider;
import org.structr.common.error.FrameworkException;
import org.structr.core.function.Functions;
import org.structr.core.graph.Tx;
import org.structr.docs.*;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.fail;

public class CreateDocumentationTest extends StructrUiTest {

	@Test
	public void createDocumentationTest() {

		try (final Tx tx = app.tx()) {

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<Documentable> functions = new LinkedList<>(Functions.getFunctions());
		final List<String> lines           = new LinkedList<>();
		final List<String> errors          = new LinkedList<>();

		// add expressions and hints that are not registered as module functions
		Functions.addExpressions(functions);
		AbstractHintProvider.addKeywordHints(functions);

		// sort by name
		Collections.sort(functions, Comparator.comparing(Documentable::getName));

		// header
		lines.add("# Built-In functions");

		for (final Documentable function : functions) {

			// check metadata for style errors etc.
			errors.addAll(checkFunctionMetadata(function));

			lines.addAll(function.createMarkdownDocumentation());
		}

		try {

			Files.writeString(Path.of("Functions.md"), StringUtils.join(lines, "\n"));

		} catch (IOException e) {

			e.printStackTrace();
		}

		if (!errors.isEmpty()) {

			fail(StringUtils.join(errors, "\n"));
		}
	}

	// ----- private methods -----
	private List<String> checkFunctionMetadata(final Documentable func) {

		final List<String> errors = new LinkedList<>();

		// verify that the short description ends with a period
		final String desc = func.getShortDescription();
		if (!desc.endsWith(".")) {

			errors.add("Short description of " + func.getName() + " does not end with a period character.");
		}

		// verify that a function has at least one language
		final List<Language> languages = func.getLanguages();
		if (languages.isEmpty()) {

			errors.add("Function " + func.getName() + " has no languages.");
		}

		// verify signatures, usages and parameters only for built-in functions
		if (DocumentableType.UserDefinedFunction.equals(func.getType())) {

			// verify that a function has at least one signature
			final List<Signature> signatures = func.getSignatures();
			if (signatures == null || signatures.isEmpty()) {

				errors.add("Function " + func.getName() + " has no signatures.");
			}

			// verify that a function has at least one usage
			final List<Usage> usages = func.getUsages();
			if (usages == null || usages.isEmpty()) {

				errors.add("Function " + func.getName() + " has no usages.");
			}

			// verify the parameters
			final List<Parameter> parameters = func.getParameters();
			if (parameters != null && !parameters.isEmpty()) {

				for (final Parameter parameter : parameters) {

					if (StringUtils.isEmpty(parameter.getName())) {
						errors.add("Function " + func.getName() + " has empty parameter name.");
					}

					if (Character.isUpperCase(parameter.getName().charAt(0))) {
						errors.add("Parameter " + parameter.getName() + " of function " + func.getName() + " should not start with an uppercase letter.");
					}

					if (parameter.getDescription() != null) {

						if (parameter.getDescription().endsWith(".")) {
							errors.add("Parameter description for " + parameter.getName() + " of function " + func.getName() + " should not end with a period character.");
						}

						// check some things in the description text
						final String d = parameter.getDescription().strip().toLowerCase();

						if (d.startsWith("the ") || d.startsWith("a ") || d.startsWith("an ") || d.startsWith("optional ")) {
							errors.add("Parameter description for " + parameter.getName() + " of function " + func.getName() + " should not start with the words 'the', 'a', 'an', or 'optional'.");
						}
					}
				}
			}

			// verify that a function has a usage and a signature for all languages
			for (final Language language : func.getLanguages()) {

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

					errors.add("Function " + func.getName() + " has no usage for language " + language.name());
				}

				if (!hasSignature) {

					errors.add("Function " + func.getName() + " has no signature for language " + language.name());
				}
			}
		}

		return errors;
	}
}

























