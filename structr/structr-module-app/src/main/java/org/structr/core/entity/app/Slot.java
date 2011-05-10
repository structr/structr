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

/**
 *
 * @author Christian Morgner
 */
public abstract class Slot
{
	private InteractiveNode source = null;
	private Object value = null;

	public abstract Class getParameterType();

	// ----- builtin methods -----
	public void setValue(Object value)
	{
		this.value = value;
	}

	public Object getValue()
	{
		return(value);
	}

	public void setSource(InteractiveNode source)
	{
		this.source = source;
	}

	public InteractiveNode getSource()
	{
		return(source);
	}
}
