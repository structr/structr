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

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.RenderMode;
import org.structr.common.renderer.ExternalTemplateRenderer;
import org.structr.core.NodeRenderer;

/**
 *
 * @author Christian Morgner
 */
public class SubmitButton extends FormField
{
	@Override
	public String getIconSrc()
	{
		return "/images/tag.png";
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers)
	{
		renderers.put(RenderMode.Default, new ExternalTemplateRenderer(false));
	}

	@Override
	public String getErrorMessage(HttpServletRequest request)
	{
		return (null);
	}

	@Override
	public Object getErrorValue(HttpServletRequest request)
	{
		return (null);
	}

	@Override
	public void setErrorValue(HttpServletRequest request, Object errorValue)
	{
	}
}
