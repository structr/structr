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
import org.structr.docs.ontology.*;

import java.util.LinkedList;
import java.util.List;

public class CodeSourceToken extends NamedConceptToken {

	public CodeSourceToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {
		super(conceptToken, identifierToken);
	}

	@Override
	public List<AnnotatedConcept> resolve(final Ontology ontology, final String sourceFile, final int lineNumber) {

		final List<IdentifierToken> identifiers = identifierToken.resolve(ontology, sourceFile, lineNumber);
		final List<Documentable>  documentables = new LinkedList<>();
		final List<AnnotatedConcept> concepts   = new LinkedList<>();

		for (final IdentifierToken identifier : identifiers) {

			final ConceptType type                 = Concept.forName(identifier.getName());
			final DocumentableType documentableType = DocumentableType.forOntologyType(type);

			if (documentableType != null) {

				documentables.addAll(documentableType.getDocumentables());
			}
		}

		for (final Documentable documentable : documentables) {

			for (final AnnotatedConcept parent : handleDocumentable(documentable, ontology, sourceFile, lineNumber)) {

				concepts.add(parent);
			}
		}

		return concepts;
	}

	// ----- private methods -----
	private List<AnnotatedConcept> handleDocumentable(final Documentable documentable, final Ontology ontology, final String sourceFile, final int lineNumber) {

		final List<AnnotatedConcept> parents = new LinkedList<>();
		ConceptType format                   = null;

		if (identifierToken != null && identifierToken.getFormat() != null) {
			format = identifierToken.getFormat().resolve(ontology, sourceFile, lineNumber);
		}

		if (!documentable.isHidden()) {

			final DocumentableType conceptType = documentable.getDocumentableType();
			final Concept mainConcept          = ontology.getOrCreateConcept(sourceFile, lineNumber, conceptType.getConcept(), documentable.getDisplayName(), true);

			if (mainConcept != null) {

				mainConcept.setDocumentable(documentable);

				for (final Documentable.ConceptReference parentConcept : documentable.getParentConcepts()) {

					// every documentable has a list of parent concepts
					final Concept parent = ontology.getOrCreateConcept(sourceFile, lineNumber, parentConcept.type, parentConcept.name, true);
					if (parent != null) {

						parent.createSymmetricLink(Verb.Has, new AnnotatedConcept(mainConcept));

						// link to parents (and put annotation there as well!)
						final AnnotatedConcept annotatedConcept = new AnnotatedConcept(parent);
						if (format != null) {

							annotatedConcept.getAnnotations().put("format", format);
						}

						parents.add(annotatedConcept);
					}
				}

				for (final Documentable.Link link : documentable.getLinkedConcepts()) {

					final Concept childConcept = ontology.getOrCreateConcept(sourceFile, lineNumber, link.target.type, link.target.name, true);
					if (childConcept != null) {

						final Verb ltr = Verb.leftToRight(link.verb);
						if (ltr != null) {

							mainConcept.createSymmetricLink(ltr, new AnnotatedConcept(childConcept));
						}

						final Verb rtl = Verb.rightToLeft(link.verb);
						if (rtl != null) {

							childConcept.createSymmetricLink(rtl, new AnnotatedConcept(mainConcept));
						}
					}
				}

				for (final String synonym : documentable.getSynonyms()) {

					final Concept synonymConcept = ontology.getOrCreateConcept(sourceFile, lineNumber, ConceptType.Synonym, synonym, false);
					if (synonymConcept != null) {

						mainConcept.createSymmetricLink(Verb.Has, new AnnotatedConcept(synonymConcept));
					}
				}
			}
		}

		return parents;
	}
}
