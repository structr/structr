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
import org.structr.docs.ontology.*;

import java.util.LinkedList;
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

		final List<AnnotatedConcept> subjects       = new LinkedList<>();
		final List<AnnotatedConcept> objects        = new LinkedList<>();

		if (predicateToken.isInverted()) {

			objects.addAll(subjectToken.resolve(ontology, sourceFile, line));
			subjects.addAll(objectToken.resolve(ontology, sourceFile, line));

		} else {

			subjects.addAll(subjectToken.resolve(ontology, sourceFile, line));
			objects.addAll(objectToken.resolve(ontology, sourceFile, line));
		}

		final Verb verb = predicateToken.resolve(ontology, sourceFile, line);

		// this resolution refines the knowledge about the three concepts
		for (final AnnotatedConcept annotatedSubject : subjects) {

			final Concept subject = annotatedSubject.getConcept();

			ontology.setCurrentSubject(subject);

			if (subject == null) {
				System.out.println(sourceFile + ":" + line + ": subject is null!");
				continue;
			}

			for (final AnnotatedConcept annotatedObject : objects) {

				final Concept object = annotatedObject.getConcept();

				if (object == null) {
					System.out.println(sourceFile + ":" + line + ": object is null!");
					continue;
				}

				// capture "has description" fact
				if (Verb.Has.equals(verb) && ConceptType.Description.equals(object.getType())) {

					subject.setShortDescription(object.getName());

				} else {

					// this line creates the parent-child relationship
					subject.createSymmetricLink(verb, annotatedObject);
				}
			}
		}

		return null;
	}
}
