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

import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Ontology;

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

			final ConceptType type                 = Concept.forName(identifier);
			final DocumentableType documentableType = DocumentableType.forOntologyType(type);

			if (documentableType != null) {

				documentables.addAll(documentableType.getDocumentables());
			}
		}

		for (final Documentable documentable : documentables) {

			for (final Concept parent : handleDocumentable(documentable, ontology, sourceFile, lineNumber)) {

				concepts.add(parent);
			}
		}

		return concepts;
	}

	// ----- private methods -----
	private List<Concept> handleDocumentable(final Documentable documentable, final Ontology ontology, final String sourceFile, final int lineNumber) {

		final List<Concept> parents = new LinkedList<>();

		if (!documentable.isHidden()) {

			final DocumentableType conceptType = documentable.getDocumentableType();
			final Concept mainConcept          = ontology.getOrCreateConcept(sourceFile, lineNumber, conceptType.getConcept(), documentable.getDisplayName());

			if (mainConcept != null) {

				for (final Documentable.ConceptReference parentConcept : documentable.getParentConcepts()) {

					// every documentable has a list of parent concepts
					final Concept parent = ontology.getOrCreateConcept(sourceFile, lineNumber, parentConcept.type, parentConcept.name);
					if (parent != null) {

						parent.linkChild("has", mainConcept);

						// link to parents
						parents.add(parent);
					}
				}

				for (final Documentable.Link link : documentable.getLinkedConcepts()) {

					final Concept childConcept = ontology.getOrCreateConcept(sourceFile, lineNumber, ConceptType.Unknown, link.name);
					if (childConcept != null) {

						mainConcept.linkChild(link.verb, childConcept);
					}
				}

				for (final String synonym : documentable.getSynonyms()) {

					final Concept synonymConcept = ontology.getOrCreateConcept(sourceFile, lineNumber, ConceptType.Synonym, synonym);
					if (synonymConcept != null) {

						mainConcept.linkChild("has", synonymConcept);
					}
				}
			}
		}

		return parents;
	}
}
