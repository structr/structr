/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

package org.structr.core.entity.app;

import javax.servlet.http.HttpServletRequest;

/**
 *
 *
 * @author Christian Morgner
 */
public interface InteractiveNode
{
	public Class getParameterType();
	public String getName();

	/**
	 * Returns the value, or null of no value was entered or an error occurred. Note
	 * that this method must return null for an invalid value.
	 *
	 * @return the parsed value or null of an error occurred
	 */
	public Object getValue(HttpServletRequest request);
        public String getStringValue(HttpServletRequest request);

	public void setMappedName(String mappedName);
	public String getMappedName();

	/**
	 * This method will be called from the node that handles the
	 * request.
	 * 
	 * @param errorValue
	 */
	public void setErrorValue(HttpServletRequest request, Object errorValue);
}
