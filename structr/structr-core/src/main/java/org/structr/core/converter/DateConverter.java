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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.time.DateUtils;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */
public class DateConverter extends PropertyConverter {

	private static final String[] DatePatterns = new String[] { "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss", "yyyymmdd", "yyyymm", "yyyy" };
	private static final Logger logger = Logger.getLogger(DateConverter.class.getName());

	@Override
	public Object convertForSetter(Object source, Value value) {

		if(source != null) {
			
			try {

				if (source instanceof Date) {
					return ((Date)source).getTime();
				} else if (source instanceof String) {
					Date date = DateUtils.parseDate(((String)source), DatePatterns);
					return date.getTime();
				}

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Exception while parsing date", t);
				return null;
			}
		}

		return source;

	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		if(source != null) {
			return new SimpleDateFormat(DatePatterns[0]).format(source);
		}

		return source;
	}

	@Override
	public Comparable convertForSorting(Object source, Value value) {

		if (source != null) {
			
			if (source instanceof Comparable) {
				return (Comparable)source;
			}
			
			// fallback to superclass
			return super.convertForSorting(source, value);
		}

		return null;
	}
}
