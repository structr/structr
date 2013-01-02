/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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

import org.structr.core.property.PropertyKey;

/**
 * Indicates that a given property value is already present in the database and
 * may only occur once.
 * 
 * @author Bastian Knerr
 */
public class LowercaseUniqueToken extends UniqueToken {

	public static final String ERROR_TOKEN = "already_taken_lowercase";

	public LowercaseUniqueToken(final String id, final PropertyKey propKey, final String val) {
		super(id, propKey, val);
	}

	@Override
	public String getErrorToken() {
		return ERROR_TOKEN;
	}

}
