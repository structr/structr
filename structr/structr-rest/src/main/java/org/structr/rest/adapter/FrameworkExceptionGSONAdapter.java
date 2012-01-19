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
import org.structr.common.error.ErrorBuffer;
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

		// add message if exists
		if(src.getMessage() != null) {
			container.add("message", new JsonPrimitive(src.getMessage()));
		}

		// add errors if there are any
		ErrorBuffer errorBuffer = src.getErrorBuffer();
		if(errorBuffer != null) {

			Map<String, Map<String, List<ErrorToken>>> tokens = errorBuffer.getErrorTokens();
			if(!tokens.isEmpty()) {

				for(Entry<String, Map<String, List<ErrorToken>>> tokensEntry : tokens.entrySet()) {

					Map<String, List<ErrorToken>> map = tokensEntry.getValue();
					JsonObject typeEntry = new JsonObject();
					String type = tokensEntry.getKey();
					
					error.add(type, typeEntry);
					
					for(Entry<String, List<ErrorToken>> mapEntry : map.entrySet()) {
						
						List<ErrorToken> list = mapEntry.getValue();
						String key = mapEntry.getKey();

						if(!list.isEmpty()) {
							
							if(list.size() == 1) {

								ErrorToken token = list.get(0);
								typeEntry.add(key, token.getContent());
								
							} else {

								JsonArray array = new JsonArray();
								for(ErrorToken token : list) {
									array.add(token.getContent());
								}
								
								typeEntry.add(key, array);
							}
						}
						
					}
				}

				container.add("errors", error);
			}
		}

		return container;
	}
}
