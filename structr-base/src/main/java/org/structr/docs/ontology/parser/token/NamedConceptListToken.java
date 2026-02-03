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
import org.structr.docs.ontology.AnnotatedConcept;
import org.structr.docs.ontology.Ontology;

import java.util.LinkedList;
import java.util.List;

public class NamedConceptListToken extends AbstractToken<List<AnnotatedConcept>> implements TokenCollection {

	private final List<NamedConceptToken> tokens = new LinkedList<>();

	public NamedConceptListToken(final NamedConceptToken... tokens) {

		for (NamedConceptToken token : tokens) {

			addToken(token);
		}
	}

	@Override
	public String toString() {
		return "NamedConceptListToken(" + tokens + ")";
	}

	public void addToken(final NamedConceptToken identifier) {

		if (identifier != null) {

			tokens.add(identifier);

			identifier.setParent(this);
		}
	}

	public void addTokens(final NamedConceptListToken tokens) {

		for (final NamedConceptToken token : tokens.getAllTokens()) {
			addToken(token);
		}
	}

	@Override
	public List<AnnotatedConcept> resolve(final Ontology ontology) {

		final List<AnnotatedConcept> result = new LinkedList<>();

		for (final NamedConceptToken token : tokens) {

			final AnnotatedConcept concept = token.resolve(ontology);
			if (concept != null) {

				result.add(concept);
			}
		}

		return result;
	}

	@Override
	public boolean isTerminal() {

		boolean terminal = true;

		for (NamedConceptToken token : tokens) {
			terminal &= token.isTerminal();
		}

		return terminal;
	}

	public  List<NamedConceptToken> getAllTokens() {
		return tokens;
	}

	@Override
	public List<Token> getAllSourceTokens() {

		final List<Token> result = new LinkedList<>();

		for (final NamedConceptToken token : tokens) {
			result.addAll(token.getAllSourceTokens());
		}

		return result;
	}

	@Override
	public Token getToken() {
		return null;
	}

	@Override
	public void renameTo(final String newName) {
		throw new UnsupportedOperationException("Cannot rename list.");
	}

	@Override
	public void updateContent(final String key, final String value) {
		throw new UnsupportedOperationException("Not supported.");
	}

	public void addChild(final String name) {

		if (!tokens.isEmpty()) {

			final NamedConceptToken namedConceptToken = tokens.getLast();
			final ConceptToken conceptToken           = namedConceptToken.getConceptToken();
			final IdentifierToken identifierToken     = namedConceptToken.getIdentifierToken();
			final List<Token> newTokens               = identifierToken.getToken().insertAfter(", \"" + name + "\"");
			final IdentifierToken newIdentifierToken  = new IdentifierToken(newTokens.getLast());

			tokens.add(new NamedConceptToken(conceptToken, newIdentifierToken));
		}
	}

	public void removeChild(final int index) {

		if (tokens.size() > index) {

			final NamedConceptToken namedConceptToken = tokens.get(index);

			tokens.remove(index);

			namedConceptToken.remove();

		}
	}

	public void moveChild(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
