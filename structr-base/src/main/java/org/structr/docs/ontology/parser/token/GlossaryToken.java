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

			final AnnotatedConcept annotatedConcept = new AnnotatedConcept(glossary);

			concepts.add(annotatedConcept);

			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "2"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "A"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "B"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "C"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "D"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "E"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "F"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "G"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "H"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "I"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "J"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "K"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "L"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "M"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "N"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "O"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "P"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "Q"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "R"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "S"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "T"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "U"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "V"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "W"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "X"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "Y"));
			ontology.createSymmetricLink(glossary, Verb.Has, Concept.create(ontology, this, ConceptType.GlossaryEntry, "Z"));

			return annotatedConcept;
		}

		return null;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}
}
