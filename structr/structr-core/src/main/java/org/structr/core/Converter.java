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
package org.structr.core;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class Converter<S, T> implements Value<T> {

	private PropertyConverter<S, T> converter = null;
	private Value<S> source = null;
	
	public Converter(Value<S> source, PropertyConverter<S, T> converter) {
		this.converter = converter;
		this.source = source;
	}
	
	@Override
	public void set(SecurityContext securityContext, T value) throws FrameworkException {
		source.set(securityContext, converter.convertForSetter(value, this));
	}

	@Override
	public T get(SecurityContext securityContext) {
		return converter.convertForGetter(source.get(securityContext), this);
	}
}
