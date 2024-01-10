/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.rest;


import jakarta.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.Value;



/**
 *
 *
 */
public class RequestParameterValue implements Value<String> {

	private String parameterName = null;
	private String defaultValue = null;
	
	public RequestParameterValue(String parameterName) {
		this(parameterName, null);
	}
	
	public RequestParameterValue(String parameterName, String defaultValue) {
		this.parameterName = parameterName;
		this.defaultValue = defaultValue;
	}
	
	@Override
	public void set(SecurityContext securityContext, String value) {
		throw new UnsupportedOperationException("Cannot set request parameter value.");
	}

	@Override
	public String get(SecurityContext securityContext) {
		
		if (securityContext != null) {

			HttpServletRequest request = securityContext.getRequest();
			
			if (request != null) {
				
				String value = request.getParameter(parameterName);
				
				if (value != null) {
					
					return value;
				}
			}
		}
		
		return defaultValue;
	}
	
}
