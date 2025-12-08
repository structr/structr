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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Structr Documentation Ontology.
 */
public final class Ontology {

	private static Ontology ontology;
	private final Map<String, Concept> index = new LinkedHashMap<>();
	private final Root root             = new Root();

	private Ontology() {
	}

	public static Ontology getInstance() {

		if (ontology == null) {

			ontology = new Ontology();
		}

		return ontology;
	}

	public List<String> createMarkdownDocumentation(final Set<Details> details) {
		//return rootConcept.getFilteredDocumentationLines(details, 0);
		return null;
	}

	private void initialize() {

		final Topic coreSystem    = root.topic("Core System");
		final Topic operations    = root.topic("Operations");
		final Topic userInterface = root.topic("User Interface");

		initializeUserInterface(userInterface);
	}

	private void initializeUserInterface(final Topic userInterface) {

		initializeFrontend(userInterface.hasTopic("Frontend"));
		initializeBackend(userInterface.hasTopic("Backend"));
	}

	private void initializeFrontend(final Topic fontend) {
	}

	private void initializeBackend(final Topic backend) {

		initializePagesArea(backend.hasScreenArea("Pages Area"));


	}

	private void initializePagesArea(final ScreenArea pagesArea) {

		final DropdownMenu createPageMenu = pagesArea.hasDropdownMenu("Create Page Menu");

		final Button createPageButton = createPageMenu.hasButton("Create Page");
		final Button importPageButton = createPageMenu.hasButton("Import Page");

		final Dialog createPageDialog = createPageButton.opensDialog(initializeCreatePageDialog());
		final Dialog importPageDialog = createPageButton.opensDialog(initializeImportPageDialog());
	}

	private Dialog initializeCreatePageDialog() {

		final Dialog createPageDialog = root.dialog("Create Page Dialog");



		return createPageDialog;
	}

	private Dialog initializeImportPageDialog() {

		final Dialog importPageDialog = root.dialog("Import Page Dialog");

		final UseCase createPageFromHTML = root.useCase("Create Page From HTML source");
		final UseCase fetchPageFromURL   = root.useCase("Fetch Page from URL");

		final SystemType pageType = root.systemType("Page");


		importPageDialog.implementsUseCase(createPageFromHTML);
		importPageDialog.implementsUseCase(fetchPageFromURL);

		return importPageDialog;
	}
}






































