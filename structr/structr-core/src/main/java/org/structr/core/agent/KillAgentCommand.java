/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.agent;

/**
 *
 * @author chrisi
 */
public class KillAgentCommand extends AgentServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		AgentService agentService = (AgentService)arguments.get("agentService");
		if(agentService != null)
		{
			if(parameters.length > 0 && parameters[0] instanceof Agent)
			{
				Agent agent = (Agent)parameters[0];
				agent.killAgent();
			}
		}

		return(null);
	}
}
