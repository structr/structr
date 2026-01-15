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
package org.structr.docs.ontology.parser.token;

import org.structr.docs.Documentable;
import org.structr.docs.DocumentableType;
import org.structr.docs.ontology.*;

import java.util.LinkedList;
import java.util.List;

public class CodeSourceToken extends NamedConceptListToken {

	private final ConceptToken conceptToken;
	private final IdentifierToken identifierToken;

	public CodeSourceToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {

		super();

		this.conceptToken    = conceptToken;
		this.identifierToken = identifierToken;
	}

	@Override
	public List<AnnotatedConcept> resolve(final Ontology ontology) {

		final List<AnnotatedConcept> concepts   = new LinkedList<>();
		final List<Documentable>  documentables = new LinkedList<>();
		final String identifier                 = identifierToken.resolve(ontology);
		final ConceptType type                  = Concept.forName(identifier);
		final DocumentableType documentableType = DocumentableType.forOntologyType(type);

		if (documentableType != null) {

			documentables.addAll(documentableType.getDocumentables());
		}

		for (final Documentable documentable : documentables) {

			for (final AnnotatedConcept child : handleDocumentable(documentable, ontology)) {

				concepts.add(child);
			}
		}

		return concepts;
	}

	// ----- private methods -----
	private List<AnnotatedConcept> handleDocumentable(final Documentable documentable, final Ontology ontology) {

		final List<AnnotatedConcept> parents = new LinkedList<>();

		if (!documentable.isHidden()) {

			final DocumentableType conceptType = documentable.getDocumentableType();
			final Concept mainConcept          = ontology.getOrCreateConcept(this, conceptType.getConcept(), documentable.getDisplayName(false), true);

			if (mainConcept != null) {

				mainConcept.setDocumentable(documentable);

				// fetch table header specification from Documentable if present
				if (documentable.getTableHeaders() != null) {
					mainConcept.getMetadata().put("table-headers", documentable.getTableHeaders());
				}

				for (final Documentable.ConceptReference parentConcept : documentable.getParentConcepts()) {

					// every documentable has a list of parent concepts
					final Concept parent = ontology.getOrCreateConcept(this, parentConcept.type, parentConcept.name, true);
					if (parent != null) {

						final AnnotatedConcept parentAnnotatedConcept = new AnnotatedConcept(parent);

						ontology.createSymmetricLink(parent, Verb.Has, mainConcept);

						if (identifierToken.getFormat() != null) {

							final ConceptToken formatToken = identifierToken.getFormat();
							final ConceptType format       = formatToken.resolve(ontology);

							parentAnnotatedConcept.setFormatSpecification(new FormatSpecification(format, formatToken));
						}

						parents.add(parentAnnotatedConcept);
					}
				}

				for (final Documentable.Link link : documentable.getLinkedConcepts()) {

					final Concept childConcept = ontology.getOrCreateConcept(this, link.target.type, link.target.name, true);
					if (childConcept != null) {

						final Verb ltr = Verb.leftToRight(link.verb);
						if (ltr != null) {

							ontology.createSymmetricLink(mainConcept, ltr, childConcept);
						}

						final Verb rtl = Verb.rightToLeft(link.verb);
						if (rtl != null) {

							ontology.createSymmetricLink(childConcept, rtl, mainConcept);
						}
					}
				}

				for (final String synonym : documentable.getSynonyms()) {

					final Concept synonymConcept = ontology.getOrCreateConcept(this, ConceptType.Synonym, synonym, false);
					if (synonymConcept != null) {

						ontology.createSymmetricLink(mainConcept, Verb.Has, synonymConcept);
					}
				}
			}
		}

		return parents;
	}
}
