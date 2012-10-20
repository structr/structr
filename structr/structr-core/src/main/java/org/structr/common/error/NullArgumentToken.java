/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class NullArgumentToken extends ErrorToken {

	public NullArgumentToken(PropertyKey key) {
		super(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, key);
	}

	@Override
	public JsonElement getContent() {
		return new JsonPrimitive(getErrorToken());
	}

	@Override
	public String getErrorToken() {
		return "may_not_be_null";
	}
}
