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
package org.structr.web.validator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.structr.core.property.PropertyKey;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.Component;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.TypeDefinition;
import org.structr.web.error.DynamicValidationError;

/**
 *
 * @author Christian Morgner
 */
public class DynamicValidator extends PropertyValidator {

	private static final Logger logger                 = Logger.getLogger(DynamicValidator.class.getName());
	private static final Map<String, Pattern> patterns = new LinkedHashMap<String, Pattern>();
	private static final long MAX_PATTERNS             = 100;	// arbitrarily chosen, may be wrong!
	
	private PropertyKey errorKey = null;
	
	public DynamicValidator(PropertyKey errorKey) {

		this.errorKey = errorKey;
	}
	
	@Override
	public boolean isValid(GraphObject object, PropertyKey key, Object value, ErrorBuffer errorBuffer) {
		
		if (object != null) {
			
			if (object instanceof Content) {
				
				Content content = (Content) object;
				
				String dataKey = content.getProperty(Content.content);
				String parentComponentId = null;
				
				/*
				Component component = content.getParentComponent();
				if (component != null) {
					parentComponentId = component.getComponentId();
				}
				*/
				
				TypeDefinition typeDefinition = content.getTypeDefinition();
				if (typeDefinition != null) {
					
					String regex   = typeDefinition.getProperty(TypeDefinition.validationExpression);
					if (value != null && regex != null) {

						Matcher matcher = getMatcher(regex, value.toString());
						if (!matcher.matches()) {

							final String errorMessage = typeDefinition.getProperty(TypeDefinition.validationErrorMessage);
							if (errorMessage != null) {
								
								Map<String, Object> attrs = new HashMap<String, Object>();
								
								attrs.put("parentComponentId", parentComponentId);
								attrs.put("data-key", dataKey);
								attrs.put("regex", regex);
								attrs.put("error", errorMessage);
								attrs.put("value", value);

								errorBuffer.add(errorKey.jsonName(), new DynamicValidationError(errorKey, attrs));

							}

							return false;
						}
						
					}
					
				} else {
					
					logger.log(Level.WARNING, "No type definition for Content entity {0}", object.getProperty(AbstractNode.uuid));
				}
				
			} else {
					
				logger.log(Level.WARNING, "Trying to validate node of type {0} which is not Content", object.getProperty(AbstractNode.type));
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
			
			if (patterns.size() > MAX_PATTERNS) {
				
				// remove first element in this map
				Iterator it = patterns.entrySet().iterator();
				it.next();
				it.remove();
			}
		}
		
		return pattern.matcher(content);
	}
}
