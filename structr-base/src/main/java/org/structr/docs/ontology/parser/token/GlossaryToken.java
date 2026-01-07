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

import org.structr.api.util.html.attr.Id;
import org.structr.docs.ontology.*;

import java.util.LinkedList;
import java.util.List;

public class GlossaryToken extends NamedConceptToken {

	public GlossaryToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {
		super(conceptToken, identifierToken);
	}

	@Override
	public List<AnnotatedConcept> resolve(final Ontology ontology, final String sourceFile, final int lineNumber) {

		final List<IdentifierToken> identifiers = identifierToken.resolve(ontology, sourceFile, lineNumber);
		final List<AnnotatedConcept> concepts   = new LinkedList<>();

		for (final IdentifierToken identifierToken : identifiers) {

			final Concept glossary = ontology.getOrCreateConcept(sourceFile, lineNumber, ConceptType.Glossary, identifierToken.getName(), true);
			if (glossary != null) {

				concepts.add(new AnnotatedConcept(glossary));

				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "2")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "A")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "B")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "C")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "D")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "E")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "F")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "G")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "H")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "I")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "J")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "K")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "L")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "M")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "N")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "O")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "P")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "Q")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "R")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "S")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "T")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "U")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "V")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "W")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "X")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "Y")));
				glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(sourceFile, lineNumber, ConceptType.GlossaryEntry, "Z")));
			}
		}

		return concepts;
	}
}
