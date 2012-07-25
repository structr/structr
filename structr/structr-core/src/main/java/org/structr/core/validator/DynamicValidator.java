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
package org.structr.core.validator;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.common.PropertyKey;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.SemanticErrorToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 *
 * @author Christian Morgner
 */
public class DynamicValidator extends PropertyValidator {

	private static final Map<String, Pattern> patterns = new LinkedHashMap<String, Pattern>();
	private static final long MAX_PATTERNS             = 100;	// arbitrarily chosen, may be wrong!
	
	private PropertyKey regexKey = null;
	private PropertyKey msgKey = null;
	private String errorKey = null;
	
	public DynamicValidator(String errorKey, PropertyKey regexKey, PropertyKey msgKey) {
		this.regexKey = regexKey;
		this.errorKey = errorKey;
		this.msgKey = msgKey;
	}
	
	@Override
	public boolean isValid(GraphObject object, String key, Object value, ErrorBuffer errorBuffer) {
		
		if (object != null) {
			
			String regex   = object.getStringProperty(regexKey);
			
			if(value != null && regex != null) {
				
				String content = value.toString();
				Matcher matcher = getMatcher(regex, content);
				
				if(!matcher.matches()) {
				
					final String errorMessage = object.getStringProperty(msgKey);
					if(errorMessage != null) {
						
						errorBuffer.add(errorKey, new SemanticErrorToken(errorKey) {

							@Override
							public JsonElement getContent() {
								return new JsonPrimitive(errorMessage);
							}

							@Override
							public String getErrorToken() {
								return errorKey;
							}
						});
						
					}
					
					return false;
				}
			}
		}
		
		
		// default: no validator
		return true;
	}
	
	private synchronized Matcher getMatcher(String patternSource, String content) {
		
		Pattern pattern = patterns.get(patternSource);
		if (pattern == null) {
			
			pattern = Pattern.compile(patternSource);
			patterns.put(patternSource, pattern);
			
			if(patterns.size() > MAX_PATTERNS) {
				
				// remove first element in this map
				Iterator it = patterns.entrySet().iterator();
				it.next();
				it.remove();
			}
		}
		
		return pattern.matcher(content);
	}
}
