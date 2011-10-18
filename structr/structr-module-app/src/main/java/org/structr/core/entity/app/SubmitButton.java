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

<<<<<<< HEAD
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
=======
import org.structr.common.PropertyView;
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
import org.structr.common.RenderMode;
import org.structr.common.renderer.ExternalTemplateRenderer;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class SubmitButton extends FormField {

	static {

		EntityContext.registerPropertySet(SubmitButton.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new ExternalTemplateRenderer(false));
	}

	//~--- get methods ----------------------------------------------------

	@Override
<<<<<<< HEAD
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
=======
	public String getIconSrc() {
		return "/images/tag.png";
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public Object getErrorValue() {
		return null;
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setErrorValue(Object errorValue) {}
}
