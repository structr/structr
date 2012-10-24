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

import org.apache.commons.lang.StringUtils;

import java.util.logging.Logger;
import org.apache.commons.lang.ArrayUtils;
import org.structr.common.SecurityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class BooleanConverter extends PropertyConverter<Object, Boolean> {

	private static final Logger logger = Logger.getLogger(BooleanConverter.class.getName());
	private static final String[] TRUE_VALUES = { "true", "1", "on" };

	public BooleanConverter(SecurityContext securityContext) {
		super(securityContext);
	}

	@Override
	public Object convertForSetter(Boolean source) {

		if (source != null) {

			return source.toString();
		}
		
		return null;

	}

	@Override
	public Boolean convertForGetter(Object source) {
		
		if (source != null) {

			String sourceString = source.toString();
			if (StringUtils.isBlank(sourceString)) {

				return false;
			}

			return ArrayUtils.contains(TRUE_VALUES, sourceString);
		}
		
		return null;
	}

}
