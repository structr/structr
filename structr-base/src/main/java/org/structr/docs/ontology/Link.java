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
package org.structr.docs.ontology;

import org.structr.core.function.tokenizer.Token;
import org.structr.docs.ontology.parser.token.AbstractToken;
import org.structr.docs.ontology.parser.token.ConceptToken;
import org.structr.docs.ontology.parser.token.IdentifierToken;

import java.util.List;
import java.util.Set;

public class Link {

	private FormatSpecification formatSpecification;
	private final Concept source;
	private final Verb verb;
	private final Concept target;

	public Link(final Concept source, final Verb verb, final Concept target) {
		this.source = source;
		this.verb   = verb;
		this.target = target;
	}

	@Override
	public String toString() {

		if (formatSpecification != null) {

			return "Link(" + source + " " + verb + " " + target + " as " + formatSpecification + ")";

		} else {

			return "Link(" + source + " " + verb + " " + target + ")";
		}
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result      = 1;

		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((verb == null) ? 0 : verb.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return obj.hashCode() == this.hashCode();
	}

	public Concept getSource() {
		return source;
	}

	public Verb getVerb() {
		return verb;
	}

	public Concept getTarget() {
		return target;
	}

	public void setFormatSpecification(final FormatSpecification format) {

		if (format != null) {

			if (this.formatSpecification == null) {

				this.formatSpecification = format;

			} else {

				this.formatSpecification.setToken(format.getToken());
				this.formatSpecification.setFormat(format.getFormat());
			}
		}
	}

	public void setFormat(final ConceptType format) {

		if (this.formatSpecification != null) {

			this.formatSpecification.setFormat(format);

		} else {

			// FIXME: which concept does the format live on?
			final Concept concept = target;

			// we need to create a new token in the source file
			final Set<AbstractToken> tokens = concept.getTokens();
			for (final AbstractToken token : tokens) {

				// first token wins
				if (token instanceof IdentifierToken identifierToken) {

					final List<Token> newTokens = identifierToken.getToken().insertAfter(" as " + format.name().toLowerCase());
					final Token newToken        = newTokens.getLast();

					this.formatSpecification = new FormatSpecification(format, new ConceptToken(format, newToken));
				}
			}
		}
	}

	public FormatSpecification getFormatSpecification() {
		return formatSpecification;
	}

	public ConceptType getType() {
		final Concept concept = target;
		return concept.getType();
	}

	public ConceptType getFormat() {

		if (formatSpecification != null) {

			return formatSpecification.getFormat();
		}

		return null;
	}
}
