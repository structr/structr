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

import org.apache.commons.lang3.StringUtils;
import org.structr.core.function.tokenizer.Token;
import org.structr.docs.ontology.Ontology;

import java.util.LinkedList;
import java.util.List;

public class IdentifierToken extends AbstractToken<List<IdentifierToken>> {

	private final List<IdentifierToken> identifiers = new LinkedList<>();
	private final Token token;

	protected ConceptToken formatSpecification = null;

	public IdentifierToken(final Token token) {

		this.token = token;

		identifiers.add(this);
	}

	@Override
	public String toString() {
		return "IdentifierToken(" + StringUtils.join(identifiers.stream().map(IdentifierToken::getToken).iterator(), ',') + ")";
	}

	public void addIdentifier(final IdentifierToken identifier) {
		identifiers.add(identifier);
	}

	public List<IdentifierToken> getIdentifiers() {
		return identifiers;
	}

	public List<IdentifierToken> resolve(final Ontology ontology) {
		return identifiers;
	}

	public void setFormat(final ConceptToken format) {
		this.formatSpecification = format;
	}

	public ConceptToken getFormat() {
		return formatSpecification;
	}

	public Token getToken() {
		return token;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}
}
