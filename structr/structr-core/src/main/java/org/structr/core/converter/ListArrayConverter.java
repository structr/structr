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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * A property converter that can convert Lists to Arrays and back.
 *
 * @author Christian Morgner
 */
public class ListArrayConverter extends PropertyConverter<String[], String> {
	
	public ListArrayConverter(SecurityContext securityContext) {
		super(securityContext, null);
	}
	
	public static final String SEP = ",";

	@Override
	public String[] revert(String source) throws FrameworkException {
		return StringUtils.split((String) source, SEP);
	}

	@Override
	public String convert(String[] source) {
		return StringUtils.join((String[]) source, SEP);
	}
}
