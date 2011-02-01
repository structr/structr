/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.agent;

import org.structr.core.Command;

/**
 *
 * @author cmorgner
 */
public abstract class AgentServiceCommand extends Command
{
	@Override
	public Class getServiceClass()
	{
		return(AgentService.class);
	}
}
