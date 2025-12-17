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
package org.structr.docs.ontology.parser.token;

import org.structr.api.config.Settings;
import org.structr.api.config.SettingsGroup;
import org.structr.autocomplete.AbstractHintProvider;
import org.structr.core.Services;
import org.structr.core.function.Functions;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.documentables.lifecycle.*;
import org.structr.docs.documentables.settings.SettingDocumentable;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;
import org.structr.rest.api.RESTEndpoints;
import org.structr.rest.resource.MaintenanceResource;

import javax.print.Doc;
import java.util.LinkedList;
import java.util.List;

public class CodeSourceToken extends NamedConceptToken {

	public CodeSourceToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {
		super(conceptToken, identifierToken);
	}

	@Override
	public List<Concept> resolve(final Ontology ontology, final String sourceFile, final int lineNumber) {

		final List<String> identifiers          = identifierToken.resolve(ontology, sourceFile, lineNumber);
		final List<Documentable>  documentables = new LinkedList<>();
		final List<Concept> concepts            = new LinkedList<>();

		for (final String identifier : identifiers) {

			final DocumentableType documentableType = DocumentableType.forOntologyType(identifier);
			if (documentableType != null) {

				documentables.addAll(documentableType.getDocumentables());
			}
		}

		for (final Documentable documentable : documentables) {

			handleDocumentable(documentable, ontology, sourceFile, lineNumber);
		}

		return concepts;
	}

	// ----- private methods -----
	private void handleDocumentable(final Documentable documentable, final Ontology ontology, final String sourceFile, final int lineNumber) {

		if (!documentable.isHidden()) {

			final DocumentableType conceptType = documentable.getDocumentableType();
			final Concept mainConcept = ontology.getOrCreateConcept(sourceFile, lineNumber, conceptType.getOntologyType(), documentable.getName());

			if (mainConcept != null) {

				for (final Documentable.Concept parentConcept : documentable.getParentConcepts()) {

					// every documentable has a list of parent concepts
					final Concept parent = ontology.getOrCreateConcept(sourceFile, lineNumber, parentConcept.type, parentConcept.name);
					if (parent != null) {

						parent.linkChild("has", mainConcept);
					}
				}

				for (final Documentable.Link link : documentable.getLinkedConcepts()) {

					final Concept childConcept = ontology.getOrCreateConcept(sourceFile, lineNumber, "unknown", link.name);
					if (childConcept != null) {

						mainConcept.linkChild(link.verb, childConcept);
					}
				}

				for (final String synonym : documentable.getSynonyms()) {

					final Concept synonymConcept = ontology.getOrCreateConcept(sourceFile, lineNumber, "synonym", synonym);
					if (synonymConcept != null) {

						mainConcept.linkChild("has", synonymConcept);
					}
				}
			}
		}
	}
}
