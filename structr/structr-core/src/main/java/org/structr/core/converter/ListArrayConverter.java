/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

import java.util.List;
import org.structr.core.Value;

/**
 * A property converter that can convert Lists to Arrays and back.
 *
 * @author Christian Morgner
 */
public class ListArrayConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {

		if(source != null && source instanceof List) {
			return ((List)source).toArray(new String[0]);
		}

		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		return source;
	}
}
