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

import org.structr.docs.ontology.*;

import java.util.LinkedList;
import java.util.List;

public class GlossaryToken extends NamedConceptToken {

	public GlossaryToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {
		super(conceptToken, identifierToken);
	}

	@Override
	public AnnotatedConcept resolve(final Ontology ontology) {

		final List<AnnotatedConcept> concepts = new LinkedList<>();
		final String identifier               = identifierToken.resolve(ontology);

		final Concept glossary = ontology.getOrCreateConcept(this, ConceptType.Glossary, identifier, true);
		if (glossary != null) {

			concepts.add(new AnnotatedConcept(glossary));

			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "2")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "A")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "B")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "C")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "D")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "E")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "F")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "G")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "H")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "I")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "J")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "K")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "L")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "M")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "N")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "O")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "P")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "Q")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "R")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "S")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "T")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "U")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "V")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "W")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "X")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "Y")));
			glossary.createSymmetricLink(Verb.Has, new AnnotatedConcept(Concept.create(-1, this, ConceptType.GlossaryEntry, "Z")));

			return new AnnotatedConcept(glossary);
		}

		return null;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}
}
