/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.autocomplete;

import org.structr.schema.action.Hint;

/**
 *
 *
 */
public abstract class NonFunctionHint extends Hint {

	private String replacement = null;

	@Override
	public String getReplacement() {

		if (replacement != null) {
			return replacement;
		}

		return getName();
	}

	public void setReplacement(final String replacement) {
		this.replacement = replacement;
	}

	public boolean hasComplexReplacement() {
		return !getName().equals(getReplacement());
	}
}
