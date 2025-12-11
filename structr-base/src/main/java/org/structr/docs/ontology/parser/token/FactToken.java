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

import org.graalvm.collections.Pair;
import org.structr.docs.ontology.Concept;
import org.structr.docs.ontology.Ontology;

import java.util.List;

public class FactToken extends Token {

	private final NamedConceptToken subjectToken;
	private final VerbToken predicateToken;
	private final NamedConceptToken objectToken;

	public FactToken(final NamedConceptToken subject, final VerbToken predicate, final NamedConceptToken object) {

		super(null);

		this.subjectToken   = subject;
		this.predicateToken = predicate;
		this.objectToken    = object;
	}

	@Override
	public String toString() {
		return "FactToken(" + subjectToken + ", " + predicateToken + ", " + objectToken + ")";
	}

	@Override
	public boolean isUnresolved() {
		return false;
	}

	public NamedConceptToken getSubjectToken() {
		return subjectToken;
	}

	public Concept resolve(final Ontology ontology, final String sourceFile, final int line) {

		final List<Concept> subjects       = subjectToken.resolve(ontology, sourceFile, line);
		final Pair<Concept, Concept> verbs = predicateToken.resolve(ontology, sourceFile, line);
		final List<Concept> objects        = objectToken.resolve(ontology, sourceFile, line);

		final String verb    = verbs.getLeft().getName();
		final String inverse = verbs.getRight().getName();

		// this resolution refines the knowledge about the three concepts
		for (final Concept subject : subjects) {

			ontology.setCurrentSubject(subject);

			for (final Concept object : objects) {

				subject.linkChild(verb, object);
				object.linkParent(inverse, subject);
			}
		}

		//return ontology.getOrCreateConcept("relation", "moep");

		return null;
	}
}
