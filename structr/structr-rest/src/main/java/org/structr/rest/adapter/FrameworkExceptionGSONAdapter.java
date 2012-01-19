/*
 *  Copyright (C) 2012 Axel Morgner
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

package org.structr.rest.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class FrameworkExceptionGSONAdapter implements JsonSerializer<FrameworkException> {

	@Override
	public JsonElement serialize(FrameworkException src, Type typeOfSrc, JsonSerializationContext context) {

		JsonObject container = new JsonObject();
		JsonObject error = new JsonObject();

		container.add("code", new JsonPrimitive(src.getStatus()));
		container.add("errors", error);

		Map<String, List<ErrorToken>> tokens = src.getErrorBuffer().getErrorTokens();
		for(Entry<String, List<ErrorToken>> entry : tokens.entrySet()) {

			List<ErrorToken> list = entry.getValue();
			String type = entry.getKey();

			JsonArray array = new JsonArray();
			for(ErrorToken token : list) {
				array.add(token.getContent());
			}

			error.add(type, array);
		}


		return container;
	}
}
