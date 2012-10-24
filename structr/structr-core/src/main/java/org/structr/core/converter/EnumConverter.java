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
package org.structr.core.converter;

import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * A converter that translates enums to Strings and back.
 * 
 * @author Christian Morgner
 */
public class EnumConverter extends PropertyConverter<String, Enum> {

	private static final Logger logger = Logger.getLogger(EnumConverter.class.getName());
	
	private Class<? extends Enum> enumClass = null;
	
	public EnumConverter(SecurityContext securityContext, Class<? extends Enum> enumClass) {
		super(securityContext);
		
		this.enumClass = enumClass;
	}
	
	@Override
	public String convertForSetter(Enum source) throws FrameworkException {
		
		if (source != null) {
			return source.name();
		}
		
		return null;
	}

	@Override
	public Enum convertForGetter(String source) {
		
		if (source != null) {
			
			if (enumClass != null) {
					
				return Enum.valueOf(enumClass, source);
			}
		}
		
		return null;
	}
}
