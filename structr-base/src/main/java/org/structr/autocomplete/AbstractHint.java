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
package org.structr.autocomplete;

import org.structr.docs.Documentable;
import org.structr.docs.Signature;

import java.util.List;

/**
 *
 *
 */
public abstract class AbstractHint implements Documentable {

	private boolean isDynamic = false;

	public String getFirstSignature() {

		final List<Signature> sigs = getSignatures();
		if (sigs != null && !sigs.isEmpty()) {

			return sigs.getFirst().getSignature();
		}

		return null;
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

	public List<Documentable> getContextHints(final String lastToken) {
		return null;
	}
}
