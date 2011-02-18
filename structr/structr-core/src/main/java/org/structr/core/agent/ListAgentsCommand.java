/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.agent;

import java.util.Collection;

/**
 * Returns a collection of the currently running agents.
 *
 * @author chrisi
 */
public class ListAgentsCommand extends AgentServiceCommand
{

	@Override
	public Object execute(Object... parameters)
	{
		AgentService agentService = (AgentService)arguments.get("agentService");
		Collection<Agent> ret = null;

		if(agentService != null)
		{
			agentService.getRunningAgents();
		}

		return(ret);
	}

}
