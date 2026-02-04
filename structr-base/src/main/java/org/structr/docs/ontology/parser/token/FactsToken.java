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

import org.structr.core.function.tokenizer.Token;
import org.structr.docs.ontology.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FactsToken extends AbstractToken implements TokenCollection {

	private final NamedConceptToken subjectToken;
	private final VerbToken predicateToken;
	private final NamedConceptListToken objectsToken;

	public FactsToken(final NamedConceptToken subject, final VerbToken predicate, final NamedConceptListToken objects) {

		if (subject != null) {
			subject.setParent(this);
		}

		if (predicate != null) {
			predicate.setParent(this);
		}

		if (objects != null) {
			objects.setParent(this);
		}

		this.subjectToken   = subject;
		this.predicateToken = predicate;
		this.objectsToken   = objects;
	}

	@Override
	public String toString() {
		return "FactsToken(" + subjectToken + ", " + predicateToken + ", " + objectsToken + ")";
	}

	public NamedConceptToken getSubjectToken() {
		return subjectToken;
	}

	public Concept resolve(final Ontology ontology) {

		if (predicateToken.isInverted()) {

			resolveRightToLeft(ontology);

		} else {

			resolveLeftToRight(ontology);
		}

		return null;
	}

	@Override
	public boolean isTerminal() {
		return true;
	}

	@Override
	public Token getToken() {
		return null;
	}

	@Override
	public void renameTo(final String newName) {
		throw new UnsupportedOperationException("Cannot rename fact.");
	}

	@Override
	public void updateContent(final String key, final String value) {
		throw new UnsupportedOperationException("Cannot update fact.");
	}

	// ----- private methods -----
	private void resolveLeftToRight(final Ontology ontology) {

		// this resolution refines the knowledge about the three concepts
		final Verb verb                         = predicateToken.resolve(ontology);
		final AnnotatedConcept annotatedSubject = subjectToken.resolve(ontology);
		final List<AnnotatedConcept> objects    = objectsToken.resolve(ontology);
		final Concept subject                   = annotatedSubject.getConcept();

		if (subject == null) {
			System.out.println(subjectToken + ": subject is null!");
			return;
		}

		ontology.setCurrentSubject(subject);

		for (final AnnotatedConcept annotatedObject : objects) {

			final Concept object = annotatedObject.getConcept();
			final Link link      = ontology.createSymmetricLink(subject, verb, object);

			if (link != null) {

				if (annotatedObject.getFormatSpecification() != null) {

					link.setFormatSpecification(annotatedObject.getFormatSpecification());
				}
			}
		}
	}

	private void resolveRightToLeft(final Ontology ontology) {

		// this resolution refines the knowledge about the three concepts
		final AnnotatedConcept annotatedSubject = subjectToken.resolve(ontology);
		final List<AnnotatedConcept> objects    = objectsToken.resolve(ontology);
		final Verb verb                         = predicateToken.resolve(ontology);
		final Concept subject                   = annotatedSubject.getConcept();

		if (subject == null) {
			System.out.println(subjectToken + ": subject is null!");
			return;
		}

		for (final AnnotatedConcept annotatedObject : objects) {

			final Concept object = annotatedObject.getConcept();

			ontology.setCurrentSubject(object);

			if (object == null) {
				System.out.println(object + ": object is null!");
				return;
			}

			final Link link = ontology.createSymmetricLink(object, verb, subject);
			if (link != null) {

				link.setFormatSpecification(annotatedSubject.getFormatSpecification());
			}
		}
	}

	@Override
	public List<Token> getAllSourceTokens() {

		final List<Token> allTokens = new LinkedList<>();

		allTokens.addAll(subjectToken.getAllSourceTokens());
		allTokens.addAll(predicateToken.getAllSourceTokens());
		allTokens.addAll(objectsToken.getAllSourceTokens());


		return allTokens;
	}
}
