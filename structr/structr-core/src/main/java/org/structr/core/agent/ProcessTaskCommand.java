/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.agent;

/**
 *
 * @author cmorgner
 */
public class ProcessTaskCommand extends AgentServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		AgentService agentService = (AgentService)arguments.get("agentService");

		if(agentService != null)
		{
			for(Object o : parameters)
			{
				if(o instanceof Task)
				{
					Task task = (Task)o;
					agentService.processTask(task);
				}

			}
		}

		return(null);
	}
}
