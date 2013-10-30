/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.Property;

/**
 * Indicates that a specific property value already exists in the database.
 * 
 * @author Christian Morgner
 */
public class DuplicateRelationshipToken extends SemanticErrorToken {

	private static final Property<String> baseProperty = new GenericProperty("base");
	
	private String errorMessage = null;
	
	public DuplicateRelationshipToken(final String errorMessage) {
		
		super(baseProperty);
		
		this.errorMessage = errorMessage;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

                obj.add(getErrorToken(), new JsonPrimitive(errorMessage));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "duplicate_relationship";
	}
}
