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
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.common.RequestHelper;
import org.structr.common.SessionValue;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.core.NodeSource;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.renderer.NodeLoaderRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class AppNodeLoader extends AbstractNode implements NodeSource {

	public static final Logger logger = Logger.getLogger(AppNodeLoader.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(AppNodeDeleter.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	// ----- instance variables -----
	private SessionValue<Object> sessionValue = null;

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ idSource; }

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new NodeLoaderRenderer());
	}


	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/brick_edit.png";
	}

	public Object getValue(HttpServletRequest request)
	{
		String loaderSourceParameter = (String)getProperty(Key.idSource.name());
		Object value = request.getParameter(loaderSourceParameter);

		if(RequestHelper.isRedirected(request))
		{
			value = getLastValue().get(request);

			// otherwise, clear value in session
			getLastValue().set(request, value);
		}

		return (value);
	}

	public SessionValue<Object> getLastValue() {

		if (sessionValue == null) {
			sessionValue = new SessionValue<Object>(createUniqueIdentifier("lastValue"));
		}

		return (sessionValue);
	}

	// ----- interface NodeSource -----
	@Override
	public AbstractNode loadNode(HttpServletRequest request)
	{
		Object loaderValue = getValue(request);
		if(loaderValue != null)
		{
			return ((AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(this, loaderValue));
		}

		return(null);
	}
}
