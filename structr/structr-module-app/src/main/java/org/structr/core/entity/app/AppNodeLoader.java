/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class AppNodeLoader extends ActiveNode implements NodeSource
{
	private static final Logger logger = Logger.getLogger(AppNodeLoader.class.getName());

	// ----- session keys -----
	private static final String LOADER_SOURCE_PARAMETER_KEY =	"loaderSourceParameter";
	
	// ----- instance variables -----
	private AbstractNode loadedNode = null;

	@Override
	public boolean isPathSensitive()
	{
		return(false);
	}

	@Override
	public boolean doRedirectAfterExecution()
	{
		return(false);
	}

	@Override
	public boolean execute(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user)
	{
		// FIXME: HTML code hard-coded.. :(

		// maybe we can let the enclosing FORM instance decide how to pass request
		// parameters into the form.

		String loaderSourceParameter = (String)getProperty(LOADER_SOURCE_PARAMETER_KEY);
		if(loaderSourceParameter != null)
		{
			out.append("<input type='hidden' name='");
			out.append(loaderSourceParameter);
			out.append("' value='");
			out.append(CurrentRequest.getRequest().getParameter(loaderSourceParameter));
			out.append("' />");
		}

		return(true);
	}

	@Override
	public Map<String, Slot> getSlots()
	{
		return(null);
	}

	@Override
	public String getIconSrc()
	{
		return("/images/brick_edit.png");
	}

	// ----- interface NodeSource -----
	@Override
	public AbstractNode loadNode()
	{
		if(loadedNode == null)
		{
			loadedNode = loadNodeInternal();
		}

		return(loadedNode);
	}

	// ----- private methods -----
	private AbstractNode loadNodeInternal()
	{
		String loaderSourceParameter = (String)getProperty(LOADER_SOURCE_PARAMETER_KEY);
		AbstractNode ret = null;

		// load node according to source property
		if(loaderSourceParameter != null)
		{
//			String username = CurrentSession.getGlobalUsername();
//			User loginUser = (User)Services.command(FindUserCommand.class).execute(username);

			// load by path or id?
			String loaderValue = CurrentRequest.getRequest().getParameter(loaderSourceParameter);
			ret = (AbstractNode)Services.command(FindNodeCommand.class).execute(null, this, loaderValue);

		} else
		{
			logger.log(Level.WARNING, "AppNodeLoader needs {0} property", LOADER_SOURCE_PARAMETER_KEY);
		}

		return(ret);
	}

    @Override
    public void onNodeCreation()
    {
    }

    @Override
    public void onNodeInstantiation()
    {
    }
}
