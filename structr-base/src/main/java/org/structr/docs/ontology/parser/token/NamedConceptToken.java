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

/**
 * An identifier that is augmented with a type so we know what it is.
 */
public class NamedConceptToken extends AbstractToken<AnnotatedConcept> implements TokenCollection {

	protected final List<NamedConceptToken> additionalNamedConcepts = new LinkedList<>();
	protected final ConceptToken conceptToken;
	protected final IdentifierToken identifierToken;

	public NamedConceptToken(final ConceptToken conceptToken, final IdentifierToken identifierToken) {

		if (identifierToken != null) {
			identifierToken.setParent(this);
		}

		if (conceptToken != null) {
			conceptToken.setParent(this);
		}

		this.identifierToken = identifierToken;
		this.conceptToken    = conceptToken;
	}

	public boolean isUnknown() {
		return conceptToken != null && ConceptType.Unknown.equals(conceptToken.getType());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + conceptToken + ", " + identifierToken + ")";
	}

	public ConceptToken getConceptToken() {
		return conceptToken;
	}

	public IdentifierToken getIdentifierToken() {
		return identifierToken;
	}

	public void addAdditionalNamedConcept(final NamedConceptToken additionalNamedConcept) {
		additionalNamedConcepts.add(additionalNamedConcept);
	}

	public List<NamedConceptToken> getAdditionalNamedConcepts() {
		return additionalNamedConcepts;
	}

	public NamedConceptToken copy(final IdentifierToken newIdentifierToken) {
		return new NamedConceptToken(conceptToken, newIdentifierToken);
	}

	@Override
	public AnnotatedConcept resolve(final Ontology ontology) {

		final ConceptType type  = conceptToken.resolve(ontology);
		final String identifier = identifierToken.resolve(ontology);

		final Concept concept = ontology.getOrCreateConcept(identifierToken, type, identifier, conceptToken.allowReuse());
		if (concept != null) {

			// additional named concepts go into metadata of a concept
			for (final NamedConceptToken additionalNamedConcept : additionalNamedConcepts) {

				final AnnotatedConcept additionalAnnotatedConcept = additionalNamedConcept.resolve(ontology);
				final Concept additionalConcept                   = additionalAnnotatedConcept.getConcept();

				concept.getMetadata().put(additionalConcept.getType().getIdentifier(), additionalConcept.getName());

				ontology.removeConcept(additionalConcept);
			}

			final AnnotatedConcept annotatedConcept = new AnnotatedConcept(concept);

			if (identifierToken.getFormat() != null) {

				final ConceptToken formatToken = identifierToken.getFormat();
				final ConceptType format       = formatToken.resolve(ontology);

				annotatedConcept.setFormatSpecification(new FormatSpecification(format, formatToken));
			}

			return annotatedConcept;
		}

		return null;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public Token getToken() {

		if (identifierToken != null) {
			return identifierToken.getToken();
		}

		return null;
	}

	@Override
	public void renameTo(final String newName) {

		if (identifierToken != null) {
			identifierToken.renameTo(newName);
		}
	}

	@Override
	public void updateContent(final String key, final String value) {

		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void remove() {

		if (identifierToken != null) {

			identifierToken.getToken().remove();

			for (final Token syntaxToken : identifierToken.getSyntaxTokens()) {
				syntaxToken.remove();
			}
		}
	}

	@Override
	public List<Token> getAllSourceTokens() {

		final List<Token> allTokens = new ArrayList<>();

		allTokens.addAll(conceptToken.getAllSourceTokens());
		allTokens.addAll(identifierToken.getAllSourceTokens());

		return allTokens;
	}
}
