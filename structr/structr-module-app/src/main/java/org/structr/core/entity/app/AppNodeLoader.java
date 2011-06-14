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

import org.structr.core.NodeSource;
import java.util.logging.Logger;
import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.common.SessionValue;
import org.structr.common.StructrOutputStream;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeLoader extends AbstractNode implements NodeSource
{
	private static final Logger logger = Logger.getLogger(AppNodeLoader.class.getName());

	// ----- session keys -----
	private static final String ID_SOURCCE_KEY = "idSource";

	// ----- instance variables -----
	private SessionValue<Object> sessionValue = null;
	private AbstractNode loadedNode = null;

	@Override
	public void renderNode(StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId)
	{
		// FIXME: HTML code hard-coded..

		// maybe we can let the enclosing FORM instance decide how to pass request
		// parameters into the form.

		String loaderSourceParameter = (String)getProperty(ID_SOURCCE_KEY);
		if(loaderSourceParameter != null)
		{
			Object value = getValue();

			out.append("<input type='hidden' name='");
			out.append(loaderSourceParameter);
			out.append("' value='");
			out.append(value);
			out.append("' />");
		}
	}

	@Override
	public String getIconSrc()
	{
		return ("/images/brick_edit.png");
	}

	@Override
	public void onNodeCreation()
	{
	}

	@Override
	public void onNodeInstantiation()
	{
	}

	// ----- interface NodeSource -----
	@Override
	public AbstractNode loadNode()
	{
		if(loadedNode == null)
		{
			loadedNode = loadNodeInternal();
		}

		return (loadedNode);
	}

	// ----- private methods -----
	private AbstractNode loadNodeInternal()
	{
		Object loaderValue = getValue();
		return((AbstractNode)Services.command(FindNodeCommand.class).execute(null, this, loaderValue));
	}

	private Object getValue()
	{
		String loaderSourceParameter = (String)getProperty(ID_SOURCCE_KEY);
		Object value = CurrentRequest.getRequest().getParameter(loaderSourceParameter);

		if(CurrentSession.isRedirected())
		{
			value = getLastValue().get();

		} else
		{
			// otherwise, clear value in session
			getLastValue().set(value);
		}

		return(value);
	}

	private SessionValue<Object> getLastValue()
	{
		if(sessionValue == null)
		{
			sessionValue = new SessionValue<Object>(createUniqueIdentifier("lastValue"));
		}

		return(sessionValue);
	}
}
