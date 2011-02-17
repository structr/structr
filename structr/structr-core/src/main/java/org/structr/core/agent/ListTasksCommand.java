/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.agent;

import java.util.Collection;

/**
 * Returns a Collection of the currently remaining Tasks.
 *
 * @author Christian Morgner
 */
public class ListTasksCommand extends AgentServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		AgentService agentService = (AgentService)arguments.get("agentService");
		Collection<Task> ret = null;

		if(agentService != null)
		{
			ret = agentService.getTaskQueue();
		}

		return(ret);
	}
}
