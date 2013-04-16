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
package org.structr.core.converter;

import java.util.Date;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * Converts a Long value to a Date and back.
 *
 * @author Christian Morgner
 */
public class DateConverter extends PropertyConverter<Date, Long> {

	public DateConverter(SecurityContext securityContext) {
		super(securityContext, null);
	}
	
	@Override
	public Long convert(Date source) throws FrameworkException {

		if(source != null) {
			
			return source.getTime();
		}

		return null;
	}

	@Override
	public Date revert(Long source) throws FrameworkException {
		
		if (source != null) {
			return new Date(source);
		}
		
		return null;
	}

	@Override
	public Comparable convertForSorting(Date source) throws FrameworkException {

		if (source != null) {
			
			if (source instanceof Comparable) {
				return (Comparable)source;
			}
			
			// fallback to superclass
			return super.convertForSorting(source);
		}

		return null;
	}
}
