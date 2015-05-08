/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.common.error;

import com.google.gson.JsonElement;
import org.structr.core.property.PropertyKey;

/**
 * Abstract base class for all error tokens.
 *
 * @author Christian Morgner
 */
public abstract class ErrorToken {

	private PropertyKey key = null;
	private int status = 0;

	public abstract JsonElement getContent();
	public abstract String getErrorToken();

	public ErrorToken(int status, PropertyKey key) {
		this.status = status;
		this.key = key;
	}

	public String getKey() {
		return key.jsonName();
	}

	public int getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof ErrorToken) {
			return ((ErrorToken)o).getErrorToken().equals(getErrorToken());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getErrorToken().hashCode();
	}
}
