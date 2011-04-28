/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import org.structr.core.NodeSource;
import java.util.logging.Logger;
import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.common.SessionValue;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
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
	public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
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
