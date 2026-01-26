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

import java.util.LinkedList;
import java.util.List;

public class FactToken extends AbstractToken implements TokenCollection {

	private final NamedConceptToken subjectToken;
	private final VerbToken predicateToken;
	private final NamedConceptToken objectToken;

	public FactToken(final NamedConceptToken subject, final VerbToken predicate, final NamedConceptToken object) {

		if (subject != null) {
			subject.setParent(this);
		}

		if (predicate != null) {
			predicate.setParent(this);
		}

		if (object != null) {
			object.setParent(this);
		}

		this.subjectToken   = subject;
		this.predicateToken = predicate;
		this.objectToken    = object;
	}

	@Override
	public String toString() {
		return "FactToken(" + subjectToken + ", " + predicateToken + ", " + objectToken + ")";
	}

	public NamedConceptToken getObjectToken() {
		return objectToken;
	}

	public VerbToken getVerbToken() {
		return predicateToken;
	}

	public NamedConceptToken getSubjectToken() {
		return subjectToken;
	}

	public Concept resolve(final Ontology ontology) {

		AnnotatedConcept annotatedSubject = null;
		AnnotatedConcept annotatedObject  = null;

		if (predicateToken.isInverted()) {

			annotatedObject = subjectToken.resolve(ontology);
			annotatedSubject = objectToken.resolve(ontology);

		} else {

			annotatedSubject = subjectToken.resolve(ontology);
			annotatedObject = objectToken.resolve(ontology);
		}

		if (annotatedSubject == null) {
			System.out.println(subjectToken + ": subject is null!");
			return null;
		}

		if (annotatedObject == null) {
			System.out.println(objectToken + ": object is null!");
			return null;
		}

		// this resolution refines the knowledge about the three concepts
		final Verb verb       = predicateToken.resolve(ontology);
		final Concept subject = annotatedSubject.getConcept();
		final Concept object  = annotatedObject.getConcept();

		ontology.setCurrentSubject(subject);

		if (subject == null) {
			System.out.println(subjectToken + ": subject is null!");
			return null;
		}

		if (object == null) {
			System.out.println(objectToken + ": object is null!");
			return null;
		}

		// capture "has description" fact
		if (Verb.Has.equals(verb) && ConceptType.Description.equals(object.getType())) {

			subject.setShortDescription(object.getName());
			ontology.removeConcept(object);

		} else {

			// this line creates the parent-child relationship
			final Link link = ontology.createSymmetricLink(subject, verb, object);

			// copy format specification
			link.setFormatSpecification(annotatedObject.getFormatSpecification());
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

	@Override
	public List<Token> getAllSourceTokens() {

		final List<Token> allTokens = new LinkedList<>();

		allTokens.addAll(subjectToken.getAllSourceTokens());
		allTokens.addAll(predicateToken.getAllSourceTokens());
		allTokens.addAll(objectToken.getAllSourceTokens());

		return allTokens;
	}
}
