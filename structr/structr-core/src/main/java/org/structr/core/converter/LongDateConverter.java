/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

import java.util.Date;
import org.structr.core.Value;

/**
 * @author Christian Morgner
 */
public class LongDateConverter extends PropertyConverter<Long, Date> {

	@Override
	public Long convertForSetter(Date source, Value value) {
		if(source != null) {
			return source.getTime();
		}

		return null;
	}

	@Override
	public Date convertForGetter(Long source, Value value) {

		if(source != null) {
			return new Date(source);
		}

		return null;
	}
}
