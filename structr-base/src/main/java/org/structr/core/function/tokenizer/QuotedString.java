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
package org.structr.core.function.tokenizer;

import org.apache.commons.lang3.StringEscapeUtils;

public class QuotedString extends Tokenizer {

	private boolean includeQuotesInToken = false;
	private boolean esc = false;
	private char quoteChar = 0;
	private int state = 0;

	public QuotedString(final char quoteChar, final boolean includeQuotesInToken) {

		this.includeQuotesInToken = includeQuotesInToken;
		this.quoteChar            = quoteChar;
	}

	@Override
	public boolean accept(final char character) {

		switch (state) {

			// not started
			case 0:
				if (character == quoteChar) {
					return true;
				}
				break;

			// in group (accept all)
			case 1:
				return true;
		}

		return false;
	}

	@Override
	public void add(final char character) {

		switch (state) {

			case 0:
				if (character == quoteChar && !esc) {
					state = 1;
				}
				break;

			case 1:
				if (character == quoteChar && !esc) {
					state = 2;
				}
				break;
		}

		if (esc || (character != quoteChar && (character != '\\'))) {

			if (esc) {

				try {
					// convert back to escape code
					super.add(StringEscapeUtils.unescapeJava("\\" + Character.toString(character)).charAt(0));
				} catch (Throwable t) {
				}

			} else {

				super.add(character);
			}
		}

		if (character == '\\' && !esc) {

			esc = true;

		} else {

			esc = false;
		}
	}

	@Override
	public void reset() {
		state = 0;
	}

	@Override
	public String getType() {
		return "string";
	}

	@Override
	public String getContent() {

		if (includeQuotesInToken) {
			return "\"" + super.getContent() + "\"";
		}

		return super.getContent();
	}

	@Override
	public String getQuoteChar() {
		return Character.toString(quoteChar);
	}

	@Override
	Tokenizer newInstance() {
		return new QuotedString(quoteChar, includeQuotesInToken);
	}
}
