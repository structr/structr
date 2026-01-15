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
import org.structr.docs.ontology.Ontology;
import org.structr.docs.ontology.Verb;

public class VerbToken extends StringToken<Verb> {

	private final boolean isInverted;
	private final Verb verb;

	public VerbToken(final Token token, final Verb verb, final boolean isInverted) {

		super(token);

		this.isInverted = isInverted;
		this.verb       = verb;
	}

	public String getInverse() {

		if (isInverted) {
			return verb.getRightToLeft();
		}

		return verb.getLeftToRight();
	}

	public Verb resolve(final Ontology ontology) {
		return verb;
	}

	public boolean isInverted() {
		return isInverted;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public void renameTo(final String newName) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public void updateContent(final String key, final String value) {
		throw new UnsupportedOperationException("Cannot update verb.");
	}
}
