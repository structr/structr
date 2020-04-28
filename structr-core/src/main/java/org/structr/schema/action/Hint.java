/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.schema.action;

import java.util.List;

/**
 *
 *
 */
public abstract class Hint {

	private boolean dontModify     = false;
	private boolean isDynamic      = false;

	public abstract String shortDescription();
	public abstract String getSignature();
	public abstract String getName();

	public String getDisplayName() {

		final StringBuilder buf = new StringBuilder();

		buf.append(getName());
		buf.append("(");

		final String signature = getSignature();
		if (signature != null) {

			buf.append(signature);
		}

		buf.append(")");

		return buf.toString();
	}

	public String getReplacement() {
		return getName();
	}

	public void allowNameModification(final boolean allowModification) {
		this.dontModify = !allowModification;
	}

	public boolean mayModify() {
		return !dontModify;
	}

	public void setIsDynamic(final boolean isDynamic) {
		this.isDynamic = isDynamic;
	}

	public boolean isDynamic() {
		return isDynamic;
	}

	public boolean isHidden() {
		return false;
	}

	public List<Hint> getContextHints(final String lastToken) {
		return null;
	}
}
