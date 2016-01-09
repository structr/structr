/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.converter.PropertyConverter;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * A {@link Value} that integrates a {@link PropertyConverter} in order to
 * provide on-the-fly conversion of objects.
 * 
 *
 */
public class Converter<SourceType, TargetType> implements Value<TargetType> {

	private static final Logger logger = Logger.getLogger(Converter.class.getName());
	private PropertyConverter<SourceType, TargetType> converter = null;
	private Value<SourceType> source = null;
	
	public Converter(Value<SourceType> source, PropertyConverter<SourceType, TargetType> converter) {
		this.converter = converter;
		this.source = source;
	}
	
	@Override
	public void set(SecurityContext securityContext, TargetType value) throws FrameworkException {
		source.set(securityContext, converter.revert(value));
	}

	@Override
	public TargetType get(SecurityContext securityContext) {
		
		try {
			
			return converter.convert(source.get(securityContext));
			
		} catch(FrameworkException fex) {
			
			logger.log(Level.WARNING, "Unable to obtain value for Converter {0}", getClass().getName());
		}
		
		return null;
	}
}
