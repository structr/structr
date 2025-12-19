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

import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Ontology;

import java.util.LinkedList;
import java.util.List;

/**
 * An identifier that is augmented with a type so we know what it is.
 */
public class NamedConceptToken extends Token<List<Concept>> {

	protected final List<NamedConceptToken> additionalNamedConcepts = new LinkedList<>();
	protected final ConceptToken conceptToken;
	protected final IdentifierToken identifierToken;

	public NamedConceptToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {

		super((conceptToken != null ? conceptToken.getName() : "") + "(" + (identifierToken != null ? identifierToken.getName() : "") + ")");

		this.identifierToken = identifierToken;
		this.conceptToken    = conceptToken;
	}

	@Override
	public boolean isUnresolved() {
		return false;
	}

	public boolean isUnknown() {
		return conceptToken != null && ConceptType.Unknown.equals(conceptToken.getType());
	}

	public void addAdditionalNamedConcept(final NamedConceptToken additionalNamedConcept) {
		additionalNamedConcepts.add(additionalNamedConcept);
	}

	public List<NamedConceptToken> getAdditionalNamedConcepts() {
		return additionalNamedConcepts;
	}

	@Override
	public List<Concept> resolve(final Ontology ontology, final String sourceFile, final int line) {

		final ConceptType type        = conceptToken.resolve(ontology, sourceFile, line);
		final List<String> identifiers = identifierToken.resolve(ontology, sourceFile, line);
		final List<Concept> concepts   = new LinkedList<>();

		for (final String identifier : identifiers) {

			final Concept concept = ontology.getOrCreateConcept(sourceFile, line, type, identifier);
			if (concept != null) {

				// additional named concepts go into metadata of a concept (for now..)
				for (final NamedConceptToken additionalNamedConcept : additionalNamedConcepts) {

					final List<Concept> additionalConcepts = additionalNamedConcept.resolve(ontology, sourceFile, line);
					for (final Concept additionalConcept : additionalConcepts) {

						concepts.add(additionalConcept);

						concept.getMetadata().put(additionalConcept.getType().getIdentifier(), additionalConcept.getName());
					}
				}
			}

			concepts.add(concept);
		}

		return concepts;
	}
}
