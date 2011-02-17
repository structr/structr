/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.module;

import java.util.Map;

/**
 *
 * @author chrisi
 */
public class GetAgentsCommand extends ModuleServiceCommand
{
	@Override
	public Object execute(Object... parameters)
	{
		ModuleService service = (ModuleService)getArgument("moduleService");
		Map<String, Class> ret = null;

		if(service != null)
		{
			ret = service.getCachedAgents();
		}

		return(ret);
	}

}
