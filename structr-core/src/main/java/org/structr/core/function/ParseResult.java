/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.core.function;

import java.util.ArrayList;
import java.util.List;
import org.structr.core.parser.Expression;

public class ParseResult {

	private List<String> tokens  = new ArrayList<>();
	private Expression root      = null;
	private String expression    = null;
	private boolean unrestricted = false;

	public List<String> getTokens() {
		return tokens;
	}

	public void setRootExpression(final Expression root) {
		this.root = root;
	}

	public Expression getRootExpression() {
		return root;
	}

	public void setExpression(final String expression) {
		this.expression = expression;
	}

	public String getExpression() {
		return expression;
	}

	public String getLastToken() {

		if (tokens.isEmpty()) {
			return "";
		}

		return tokens.get(tokens.size() - 1);
	}

	public void setUnrestricted(final boolean unrestricted) {
		this.unrestricted = unrestricted;
	}

	public boolean isUnrestricted() {
		return unrestricted;
	}
}