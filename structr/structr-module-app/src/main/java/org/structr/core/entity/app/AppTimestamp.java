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

import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class AppTimestamp extends AbstractNode implements InteractiveNode
{
	private static final String FORMAT_KEY =		"format";

	@Override
	public String getIconSrc()
	{
		return("/images/time.png");
	}

	// ----- interface InteractiveNode -----
	@Override
	public Class getParameterType()
	{
		return(String.class);
	}

	@Override
	public Object getValue(HttpServletRequest request)
	{
		String format = getStringProperty(FORMAT_KEY);
		if(format == null)
		{
			format = "dd.MM.yyyy HH:mm";
		}

		SimpleDateFormat df = new SimpleDateFormat(format);
		return(df.format(System.currentTimeMillis()));
	}

	@Override
	public String getStringValue(HttpServletRequest request)
	{
		return(getValue(request).toString());
	}

	@Override
	public void setMappedName(String mappedName)
	{
	}

	@Override
	public String getMappedName()
	{
		return(getStringProperty(AbstractNode.Key.name.name()));
	}

	@Override
	public void setErrorValue(HttpServletRequest request, Object errorValue)
	{
	}
}
