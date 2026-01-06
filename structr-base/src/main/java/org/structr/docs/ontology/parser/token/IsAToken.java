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

import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.Ontology;

import java.util.LinkedList;
import java.util.List;

public class IsAToken extends Token<List<AnnotatedConcept>> {

	private final NamedConceptToken namedConceptToken;
	private final ConceptToken conceptToken;

	public IsAToken(final NamedConceptToken namedConceptToken, final ConceptToken conceptToken) {

		super(null);

		this.namedConceptToken = namedConceptToken;
		this.conceptToken      = conceptToken;
	}

	@Override
	public boolean isUnresolved() {
		return false;
	}

	@Override
	public List<AnnotatedConcept> resolve(final Ontology ontology, final String sourceFile, final int line) {

		final ConceptType type    = conceptToken.resolve(ontology, sourceFile, line);
		final List<AnnotatedConcept> input  = namedConceptToken.resolve(ontology,  sourceFile, line);
		final List<AnnotatedConcept> output = new LinkedList<>();

		for (final AnnotatedConcept annotatedConcept : input) {

			final Concept concept  = annotatedConcept.getConcept();
			final Concept toRefine = ontology.getOrCreateConcept(sourceFile, line, type, concept.getName(), true);
			if (toRefine != null) {

				toRefine.setType(type);

				output.add(new AnnotatedConcept(toRefine));
			}
		}

		return output;
	}
}
